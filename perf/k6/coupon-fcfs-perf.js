/**
 * ============================================================================
 * FCFS Coupon Download - k6 Performance Test
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 선착순 쿠폰 다운로드 API의 성능/정합성 비교.
 *
 * 비교 대상
 * ----------------------------------------------------------------------------
 * - DB 비관적 락 기반 동기 발급
 * - Redis Lua 선처리 + Stream/Job 기반 비동기 반영
 *
 * 실행 예시
 * ----------------------------------------------------------------------------
 * BASE_URL=http://localhost:8080 POLICY_ID=1 SCENARIO=oversell \
 *   k6 run perf/k6/coupon-fcfs-perf.js
 *
 * make 사용:
 * make perf-client PERF_SCRIPT=coupon-fcfs-perf.js BASE_URL=http://host.docker.internal:8080
 *
 * 주요 환경 변수
 * ----------------------------------------------------------------------------
 * POLICY_ID     테스트할 쿠폰 정책 ID (필수)
 * SCENARIO      oversell | burst | duplicate (기본 oversell)
 * USER_START    로그인 시작 유저 번호 (기본 1)
 * USER_COUNT    사용할 유저 수 (기본 300)
 * VUS           동시 VU 수 (기본 100)
 * ITERATIONS    요청 수. 미지정 시 시나리오별 기본값 사용
 *
 * 시나리오
 * ----------------------------------------------------------------------------
 * 1. oversell
 *    재고보다 많은 사용자가 1회씩 다운로드 요청.
 *
 * 2. burst
 *    짧은 시간에 많은 요청을 몰아 고경합 상황 측정.
 *
 * 3. duplicate
 *    같은 사용자가 같은 쿠폰을 반복 다운로드 요청.
 *
 * 테스트 후 DB 확인 권장
 * ----------------------------------------------------------------------------
 * select count(*) from issued_coupons where policy_id = ?;
 * select total_quantity from coupon_policies where id = ?;
 * select status, count(*) from issued_coupon_jobs where policy_id = ? group by status;
 * ============================================================================
 */

import http from 'k6/http';
import { check, group } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const POLICY_ID = __ENV.POLICY_ID;
const SCENARIO = __ENV.SCENARIO || 'oversell';
const USER_START = parseInt(__ENV.USER_START || '1', 10);
const USER_COUNT = parseInt(__ENV.USER_COUNT || '300', 10);
const VUS = parseInt(__ENV.VUS || '100', 10);
const ITERATIONS = parseInt(__ENV.ITERATIONS || String(defaultIterations()), 10);
const PASSWORD = __ENV.PASSWORD || 'Test1234!';
const DEBUG_RESPONSE = (__ENV.DEBUG_RESPONSE || 'false') === 'true' || __ENV.DEBUG_RESPONSE === '1';

if (!POLICY_ID) {
    throw new Error('POLICY_ID env is required. Example: POLICY_ID=1');
}

function defaultIterations() {
    if (SCENARIO === 'duplicate') {
        return 100;
    }
    return USER_COUNT;
}

function selectedScenario() {
    const scenarios = {
        oversell: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '2m',
            exec: 'downloadOnce',
            tags: { scenario: 'oversell' },
        },
        burst: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '1m',
            exec: 'downloadOnce',
            tags: { scenario: 'burst' },
        },
        duplicate: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '1m',
            exec: 'downloadDuplicate',
            tags: { scenario: 'duplicate' },
        },
    };

    if (!scenarios[SCENARIO]) {
        throw new Error(`Unsupported SCENARIO=${SCENARIO}. Use oversell, burst, or duplicate.`);
    }
    return { [SCENARIO]: scenarios[SCENARIO] };
}

export const options = {
    setupTimeout: '3m',
    scenarios: selectedScenario(),
    thresholds: {
        coupon_download_latency: ['p(95)<1000', 'p(99)<2000'],
        coupon_unexpected_failure: ['rate<0.01'],
    },
};

const couponDownloadLatency = new Trend('coupon_download_latency', true);
const attemptCount = new Counter('coupon_attempt');
const successCount = new Counter('coupon_success');
const failureCount = new Counter('coupon_failure');
const duplicateCount = new Counter('coupon_duplicate');
const exhaustedCount = new Counter('coupon_exhausted');
const conflictCount = new Counter('coupon_conflict');
const unexpectedFailureCount = new Counter('coupon_unexpected_failure_count');
const unexpectedFailureRate = new Rate('coupon_unexpected_failure');
const successRate = new Rate('coupon_success_rate');

export function setup() {
    const users = [];
    const loginCount = SCENARIO === 'duplicate' ? 1 : USER_COUNT;

    for (let i = 0; i < loginCount; i++) {
        const userNo = USER_START + i;
        users.push({
            userNo,
            token: login(`user${userNo}@test.com`, PASSWORD),
        });
    }

    console.log(`[setup] scenario=${SCENARIO}, policyId=${POLICY_ID}, users=${users.length}, vus=${VUS}, iterations=${ITERATIONS}`);
    return { users };
}

export function downloadOnce(data) {
    const user = data.users[(exec.scenario.iterationInTest % data.users.length)];
    download(user.token);
}

export function downloadDuplicate(data) {
    download(data.users[0].token);
}

function login(email, password) {
    const res = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status !== 200 && res.status !== 204) {
        throw new Error(`[setup] login failed email=${email}, status=${res.status}, body=${res.body}`);
    }
    // 로그인 응답은 204 + HttpOnly 쿠키(accessToken)로 토큰을 내려준다(본문 없음).
    const cookie = res.cookies['accessToken'];
    if (!cookie || !cookie[0]) {
        throw new Error(`[setup] login failed email=${email}, no accessToken cookie, status=${res.status}`);
    }
    return cookie[0].value;
}

function download(token) {
    group(`coupon download (${SCENARIO})`, () => {
        attemptCount.add(1);

        const res = http.post(
            `${BASE_URL}/api/coupons/${POLICY_ID}/download`,
            null,
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            }
        );

        if (DEBUG_RESPONSE && exec.scenario.iterationInTest < 5) {
            console.log(`[debug] status=${res.status}, body=${res.body}`);
        }

        couponDownloadLatency.add(res.timings.duration);

        const ok = check(res, {
            '2xx or expected conflict': (r) => isSuccess(r) || isExpectedConflict(r),
        });

        const success = isSuccess(res);
        const duplicate = isDuplicate(res);
        const exhausted = isExhausted(res);

        successCount.add(success ? 1 : 0);
        failureCount.add(success ? 0 : 1);
        duplicateCount.add(duplicate ? 1 : 0);
        exhaustedCount.add(exhausted ? 1 : 0);
        conflictCount.add(duplicate || exhausted ? 1 : 0);
        successRate.add(success ? 1 : 0);
        unexpectedFailureCount.add(ok ? 0 : 1);
        unexpectedFailureRate.add(ok ? 0 : 1);
    });
}

function isSuccess(res) {
    return res.status >= 200 && res.status < 300;
}

function isExpectedConflict(res) {
    if (res.status !== 409) {
        return false;
    }
    return isDuplicate(res) || isExhausted(res);
}

function isDuplicate(res) {
    return res.status === 409 && (res.body || '').includes('COUPON_002');
}

function isExhausted(res) {
    return res.status === 409 && (res.body || '').includes('COUPON_004');
}
