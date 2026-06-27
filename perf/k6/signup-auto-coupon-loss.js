/**
 * ============================================================================
 * Signup Auto Coupon Loss Check - k6 Scenario
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 회원가입 성공 수와 자동 지급 쿠폰 발급 수가 일치하는지 확인한다.
 * 현재 Spring 이벤트 방식과 추후 Outbox 방식에서 같은 시나리오로 재사용한다.
 *
 * 실행 예시
 * ----------------------------------------------------------------------------
 * RUN_ID=before-outbox-001 BASE_URL=http://localhost:8080 \
 *   k6 run perf/k6/signup-auto-coupon-loss.js
 *
 * 주요 환경 변수
 * ----------------------------------------------------------------------------
 * BASE_URL    API 서버 주소 (기본 http://localhost:8080)
 * RUN_ID      테스트 데이터 구분값. 미지정 시 현재 timestamp 사용
 * USER_COUNT  회원가입 요청 수 (기본 300)
 * VUS         동시 VU 수 (기본 50)
 * PASSWORD    가입 비밀번호 (기본 Test1234!)
 *
 * 테스트 후 DB 확인 쿼리
 * ----------------------------------------------------------------------------
 * -- 1. 가입 성공 유저 수
 * SELECT COUNT(*)
 * FROM users
 * WHERE email LIKE 'k6-auto-coupon-{RUN_ID}-%@test.local';
 *
 * -- 2. SIGNUP 보상 처리 이력 수
 * SELECT COUNT(*)
 * FROM coupon_reward_histories crh
 * JOIN users u ON u.id = crh.user_id
 * WHERE u.email LIKE 'k6-auto-coupon-{RUN_ID}-%@test.local'
 *   AND crh.reward_key = 'SIGNUP';
 *
 * -- 3. 실제 SIGNUP 쿠폰 발급 수
 * SELECT COUNT(*)
 * FROM issued_coupons ic
 * JOIN users u ON u.id = ic.user_id
 * WHERE u.email LIKE 'k6-auto-coupon-{RUN_ID}-%@test.local'
 *   AND ic.issue_key = 'SIGNUP';
 *
 * 기대 결과
 * ----------------------------------------------------------------------------
 * users count == coupon_reward_histories count == issued_coupons count
 * ============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RUN_ID = __ENV.RUN_ID || String(Date.now());
const USER_COUNT = parseInt(__ENV.USER_COUNT || '300', 10);
const VUS = parseInt(__ENV.VUS || '50', 10);
const PASSWORD = __ENV.PASSWORD || 'Test1234!';
const DEBUG_RESPONSE = (__ENV.DEBUG_RESPONSE || 'false') === 'true' || __ENV.DEBUG_RESPONSE === '1';

export const options = {
    scenarios: {
        signup_auto_coupon_loss: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: USER_COUNT,
            maxDuration: '3m',
            exec: 'signup',
        },
    },
    thresholds: {
        signup_success_rate: ['rate>0.99'],
        signup_latency: ['p(95)<1500', 'p(99)<3000'],
    },
};

const signupLatency = new Trend('signup_latency', true);
const signupAttemptCount = new Counter('signup_attempt');
const signupSuccessCount = new Counter('signup_success');
const signupFailureCount = new Counter('signup_failure');
const signupSuccessRate = new Rate('signup_success_rate');

export function setup() {
    console.log(`[setup] RUN_ID=${RUN_ID}`);
    console.log(`[setup] USER_COUNT=${USER_COUNT}, VUS=${VUS}, BASE_URL=${BASE_URL}`);
    console.log(`[verify] email pattern: k6-auto-coupon-${RUN_ID}-%@test.local`);
    return { runId: RUN_ID };
}

export function signup(data) {
    const seq = exec.scenario.iterationInTest + 1;
    const email = `k6-auto-coupon-${data.runId}-${seq}@test.local`;
    const payload = {
        email,
        password: PASSWORD,
        nickname: `k6-auto-${data.runId}-${seq}`,
        profileImage: null,
        userRole: 'USER',
    };

    signupAttemptCount.add(1);

    const csrfToken = getCsrfToken();
    const res = http.post(
        `${BASE_URL}/api/auth/signup`,
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken,
            },
            tags: { api: 'signup' },
        }
    );

    signupLatency.add(res.timings.duration);

    const ok = check(res, {
        'signup status is 201': (r) => r.status === 201,
    });

    signupSuccessRate.add(ok);

    if (ok) {
        signupSuccessCount.add(1);
    } else {
        signupFailureCount.add(1);
        if (DEBUG_RESPONSE) {
            console.log(`[failure] email=${email}, status=${res.status}, body=${res.body}`);
        }
    }
}

function getCsrfToken() {
    const csrfRes = http.get(`${BASE_URL}/api/auth/csrf`, {
        tags: { api: 'csrf' },
    });

    const jar = http.cookieJar();
    const cookies = jar.cookiesForURL(BASE_URL);
    const xsrfCookies = cookies['XSRF-TOKEN'];
    const token = xsrfCookies && xsrfCookies.length > 0 ? xsrfCookies[0] : '';

    if (!token && DEBUG_RESPONSE) {
        console.log(`[csrf failure] status=${csrfRes.status}, body=${csrfRes.body}`);
    }

    return token;
}
