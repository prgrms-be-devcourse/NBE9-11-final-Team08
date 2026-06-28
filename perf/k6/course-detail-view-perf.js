/**
 * ============================================================================
 * Course Detail View Performance Test - k6 Script
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 강좌 상세조회 API(GET /api/courses/{courseId})의 동시 트래픽 처리 능력을 검증합니다.
 * 조회수 가산(RDB Write) 연산으로 인한 DB 트랜잭션 경합 및
 * HikariCP 커넥션 풀의 고갈 여부를 정량적으로 분석합니다.
 *
 * 실행 방법
 * ----------------------------------------------------------------------------
 * 1. 100 TPS 안정성 테스트:
 *    make perf-client PERF_SCRIPT=course-detail-view-perf.js TARGET_TPS=100 DURATION=30s
 * 2. 500 TPS 스트레스 테스트:
 *    make perf-client PERF_SCRIPT=course-detail-view-perf.js TARGET_TPS=500 DURATION=30s
 * ============================================================================
 */

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const COURSE_ID = parseInt(__ENV.COURSE_ID || '1', 10);
const TARGET_TPS = parseInt(__ENV.TARGET_TPS || '100', 10);
const DURATION = __ENV.DURATION || '30s';

export const options = {
    scenarios: {
        detailViewStress: {
            executor: 'constant-arrival-rate',
            rate: TARGET_TPS,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Math.min(TARGET_TPS * 2, 200),
            maxVUs: Math.min(TARGET_TPS * 5, 1000),
        },
    },
    thresholds: {
        http_req_duration: ['p(99)<1000'], // p99 지연 시간이 1초(1000ms) 이내여야 함
        http_req_failed: ['rate<0.01'],    // 실패율 1% 미만
    },
};

// 커스텀 지연 시간 메트릭
const viewLatency = new Trend('view_latency', true);
const errorRate = new Rate('error_rate');

export default function () {
    const startTime = Date.now();
    const res = http.get(`${BASE_URL}/api/courses/${COURSE_ID}`);
    const latency = Date.now() - startTime;

    viewLatency.add(latency);

    const isSuccess = res.status === 200;
    errorRate.add(!isSuccess);

    check(res, {
        'status is 200 OK': (r) => r.status === 200,
    }, { status: String(res.status), path: '/courses/detail' });
}
