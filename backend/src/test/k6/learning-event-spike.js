/**
 * Learning Event API - Spike & Idempotency Test
 *
 * 목적:
 *   1. 스파이크 테스트: 갑작스러운 트래픽 급증 시 서버가 회복하는지 확인
 *   2. 멱등성 테스트: 동일 eventKey 재전송 시 409가 반환되는지 확인
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 USER_TOKEN=<JWT> k6 run learning-event-spike.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8080';
const USER_TOKEN = __ENV.USER_TOKEN || 'eyJhbGciOiJIUzI1NiJ9.user_placeholder';

const errorRate        = new Rate('error_rate');
const idempotencyOk    = new Rate('idempotency_ok');   // 재전송 → 409 비율 (높을수록 정상)

export const options = {
    scenarios: {
        /**
         * 스파이크 테스트
         * t2.small 기준: 평상시 20 VU → 순간 150 VU → 복귀
         * 서버가 과부하 후 정상 응답을 회복하는지 확인
         */
        spike: {
            executor: 'ramping-vus',
            stages: [
                { duration: '20s', target: 20  },  // 워밍업
                { duration: '5s',  target: 150 },  // 스파이크
                { duration: '30s', target: 150 },  // 스파이크 유지
                { duration: '10s', target: 20  },  // 회복
                { duration: '20s', target: 20  },  // 회복 후 안정 확인
                { duration: '5s',  target: 0   },
            ],
            exec: 'spikeScenario',
            tags: { scenario: 'spike' },
        },

        /**
         * 멱등성(중복 이벤트 방지) 테스트
         * 동일 eventKey를 가진 요청을 연속 2번 전송 → 두 번째는 반드시 409
         */
        idempotency: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
            exec: 'idempotencyScenario',
            tags: { scenario: 'idempotency' },
            startTime: '10s',
        },
    },

    thresholds: {
        // 스파이크 중에도 에러율 5% 이하 (t2.small 한계 고려해 완화)
        'error_rate':                 ['rate<0.05'],
        // 스파이크 후 회복 단계(마지막 20s)에서 p95 < 800ms
        'http_req_duration{scenario:spike}': ['p(95)<800'],
        // 멱등성: 재전송의 99% 이상이 409로 처리되어야 함
        'idempotency_ok':             ['rate>0.99'],
    },
};

export function spikeScenario() {
    const res = http.post(
        `${BASE_URL}/api/learning-events`,
        JSON.stringify({
            courseId:        1,
            chapterId:       1,
            lectureId:       1,
            eventType:       'LECTURE_ENTER',
            positionSeconds: null,
            eventTime:       new Date().toISOString().replace('Z', ''),
            eventKey:        uuidv4(),  // 매 요청마다 새 키 → 중복 방지 로직 미발동
        }),
        { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${USER_TOKEN}` } }
    );

    const ok = check(res, {
        '201 또는 429(과부하 차단)': (r) => r.status === 201 || r.status === 429,
        '5xx 없음':               (r) => r.status < 500,
    });
    errorRate.add(ok ? 0 : 1);

    sleep(0.5);
}

export function idempotencyScenario() {
    // 동일 eventKey를 공유하는 두 요청
    const sharedKey = uuidv4();
    const payload = JSON.stringify({
        courseId:        1,
        chapterId:       1,
        lectureId:       1,
        eventType:       'LECTURE_ENTER',
        positionSeconds: null,
        eventTime:       new Date().toISOString().replace('Z', ''),
        eventKey:        sharedKey,
    });
    const headers = {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${USER_TOKEN}`,
    };

    group('멱등성 - 첫 번째 전송 (201 기대)', () => {
        const res = http.post(`${BASE_URL}/api/learning-events`, payload, { headers });
        check(res, { '첫 전송 201': (r) => r.status === 201 });
    });

    // 네트워크 재전송 시뮬레이션: 즉시 재전송
    group('멱등성 - 동일 키 재전송 (409 기대)', () => {
        const res = http.post(`${BASE_URL}/api/learning-events`, payload, { headers });
        const got409 = res.status === 409;
        idempotencyOk.add(got409 ? 1 : 0);
        check(res, {
            '재전송 409 Conflict': (r) => r.status === 409,
        });
    });

    sleep(randomIntBetween(1, 3));
}

function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
