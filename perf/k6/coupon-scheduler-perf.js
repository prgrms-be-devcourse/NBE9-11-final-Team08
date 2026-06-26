/**
 * ============================================================================
 * Scheduler Bulk Update Interference - k6 Performance Test
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 마감 처리 스케줄러(단일 Bulk Update)가 동작할 때 발생하는 DB Lock이나 리소스 점유가
 * 일반 사용자의 API(쿠폰 목록 조회 등) 응답 속도에 어떤 영향을 미치는지 측정.
 *
 * 실행 예시
 * ----------------------------------------------------------------------------
 * BASE_URL=http://localhost:8080 USER_COUNT=100 k6 run perf/k6/coupon-scheduler-perf.js
 *
 * 시나리오 구조 (Multi-Scenario)
 * ----------------------------------------------------------------------------
 * 1. user_traffic: 지속적으로 쿠폰 목록을 조회(일반 트래픽 시뮬레이션)
 * 2. scheduler_trigger: 테스트 시작 후 10초 뒤에 만료 스케줄러 강제 1회 실행
 *
 * 지표 구간
 * ----------------------------------------------------------------------------
 * pre_batch_*     : 스케줄러 트리거 전 구간
 * during_batch_*  : 스케줄러 트리거 직후 BATCH_MEASURE_SECONDS 동안
 * post_batch_*    : during 구간 이후
 *
 * 테스트 전 주의사항
 * ----------------------------------------------------------------------------
 * 만료 대상인 쿠폰 데이터가 DB에 많을수록(10만 건 이상) 유의미한 결과가 나옵니다.
 * ============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_START = parseInt(__ENV.USER_START || '1', 10);
const USER_COUNT = parseInt(__ENV.USER_COUNT || '100', 10);
const PASSWORD = __ENV.PASSWORD || 'Test1234!';
const VUS = parseInt(__ENV.VUS || '100', 10);
const USER_TRAFFIC_DURATION = __ENV.USER_TRAFFIC_DURATION || '1m';
const BATCH_START_SECONDS = parseInt(__ENV.BATCH_START_SECONDS || '10', 10);
const BATCH_MEASURE_SECONDS = parseInt(__ENV.BATCH_MEASURE_SECONDS || '30', 10);

// 매트릭스 설정
const userTrafficLatency = new Trend('user_traffic_latency', true);
const preBatchUserTrafficLatency = new Trend('pre_batch_user_traffic_latency', true);
const duringBatchUserTrafficLatency = new Trend('during_batch_user_traffic_latency', true);
const postBatchUserTrafficLatency = new Trend('post_batch_user_traffic_latency', true);
const schedulerLatency = new Trend('scheduler_execution_latency', true);
const successRate = new Rate('success_rate');
const preBatchSuccessRate = new Rate('pre_batch_success_rate');
const duringBatchSuccessRate = new Rate('during_batch_success_rate');
const postBatchSuccessRate = new Rate('post_batch_success_rate');

export const options = {
    setupTimeout: '3m',
    scenarios: {
        // 1. 일반 유저 트래픽 (1분 동안 지속적인 부하)
        user_traffic: {
            executor: 'constant-vus',
            vus: VUS,
            duration: USER_TRAFFIC_DURATION,
            exec: 'simulateUserTraffic',
        },
        // 2. 스케줄러 트리거 (10초 뒤에 딱 한 번 실행)
        scheduler_trigger: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            startTime: `${BATCH_START_SECONDS}s`,
            exec: 'triggerScheduler',
        },
    },
    thresholds: {
        // 일반 API 호출의 95%가 500ms 이내, 99%가 1초 이내여야 함 (간섭 확인용)
        user_traffic_latency: ['p(95)<500', 'p(99)<1000'],
        during_batch_user_traffic_latency: ['p(95)<500', 'p(99)<1000'],
        success_rate: ['rate>0.99'], 
        during_batch_success_rate: ['rate>0.99'],
    },
};

// 테스트 준비: 유저 로그인 및 토큰 발급
export function setup() {
    const users = [];
    for (let i = 0; i < USER_COUNT; i++) {
        const userNo = USER_START + i;
        const email = `user${userNo}@test.com`;
        
        const res = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({ email, password: PASSWORD }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (res.status === 200) {
            users.push({
                userNo,
                token: JSON.parse(res.body).accessToken,
            });
        }
    }
    
    console.log(`[setup] Logged in ${users.length} users for test.`);
    console.log(`[setup] scheduler starts at T+${BATCH_START_SECONDS}s, during window=${BATCH_MEASURE_SECONDS}s.`);
    // 스케줄러 트리거용 관리자 토큰 (그냥 1번 유저 토큰 사용)
    const adminToken = users.length > 0 ? users[0].token : null;
    
    return {
        users,
        adminToken,
        trafficStartedAt: Date.now(),
    };
}

function trafficPhase(data) {
    const elapsedSeconds = (Date.now() - data.trafficStartedAt) / 1000;
    if (elapsedSeconds < BATCH_START_SECONDS) return 'pre';
    if (elapsedSeconds < BATCH_START_SECONDS + BATCH_MEASURE_SECONDS) return 'during';
    return 'post';
}

function recordUserTrafficMetrics(phase, duration, isOk) {
    successRate.add(isOk ? 1 : 0);

    if (phase === 'pre') {
        preBatchSuccessRate.add(isOk ? 1 : 0);
        if (isOk) preBatchUserTrafficLatency.add(duration);
        return;
    }

    if (phase === 'during') {
        duringBatchSuccessRate.add(isOk ? 1 : 0);
        if (isOk) duringBatchUserTrafficLatency.add(duration);
        return;
    }

    postBatchSuccessRate.add(isOk ? 1 : 0);
    if (isOk) postBatchUserTrafficLatency.add(duration);
}

// 시나리오 1: 일반 유저가 본인의 쿠폰 목록을 조회하는 동작
export function simulateUserTraffic(data) {
    if (!data.users || data.users.length === 0) return;

    // 현재 VU와 iteration 정보로 유저 한 명을 고름
    const userIndex = exec.scenario.iterationInTest % data.users.length;
    const user = data.users[userIndex];

    const res = http.get(`${BASE_URL}/api/coupons/me`, {
        headers: {
            'Authorization': `Bearer ${user.token}`,
        },
    });
    
    const isOk = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    if (isOk) {
        userTrafficLatency.add(res.timings.duration);
    }
    recordUserTrafficMetrics(trafficPhase(data), res.timings.duration, isOk);
}

// 시나리오 2: 스케줄러 강제 트리거 (Bulk Update 발생)
export function triggerScheduler(data) {
    if (!data.adminToken) return;

    console.log('[triggerScheduler] 마감 처리 스케줄러 실행 요청 보냄...');
    
    const res = http.post(`${BASE_URL}/api/test/coupons/expire-trigger`, null, {
        headers: {
            'Authorization': `Bearer ${data.adminToken}`,
        },
        timeout: '10m',
    });

    schedulerLatency.add(res.timings.duration);
    
    check(res, {
        'scheduler triggered successfully': (r) => r.status === 200,
    });
    
    console.log(`[triggerScheduler] 완료. 소요 시간: ${(res.timings.duration / 1000).toFixed(1)}s`);
}
