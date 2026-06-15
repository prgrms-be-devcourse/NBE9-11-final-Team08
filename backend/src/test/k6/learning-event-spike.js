/**
 * Learning Event API - Spike & Idempotency Test
 *
 * 목적:
 *   1. 스파이크 테스트: 갑작스러운 트래픽 급증 시 서버가 회복하는지 확인
 *   2. 멱등성 테스트: 동일 eventKey 재전송 시 409가 반환되는지 확인
 *
 * 실행 (토큰 자동 발급):
 *   BASE_URL=http://localhost:8080 k6 run src/test/k6/learning-event-spike.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate     = new Rate('error_rate');
const idempotencyOk = new Rate('idempotency_ok');

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            stages: [
                { duration: '20s', target: 20  },
                { duration: '5s',  target: 150 },
                { duration: '30s', target: 150 },
                { duration: '10s', target: 20  },
                { duration: '20s', target: 20  },
                { duration: '5s',  target: 0   },
            ],
            exec: 'spikeScenario',
            tags: { scenario: 'spike' },
        },
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
        'error_rate':                            ['rate<0.05'],
        'http_req_duration{scenario:spike}':     ['p(95)<800'],
        'idempotency_ok':                        ['rate>0.99'],
    },
};

// ── 1회 로그인 후 토큰을 모든 VU에 전달 ────────────────────────────────────
export function setup() {
    const res = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: 'user1@playlearn.com', password: 'user1234!' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) {
        throw new Error(`[setup] 로그인 실패: ${res.status} ${res.body}`);
    }
    const token = JSON.parse(res.body).accessToken;
    console.log('[setup] 로그인 성공: user1@playlearn.com');
    return { userToken: token };
}

// ── 시나리오 1: 스파이크 ──────────────────────────────────────────────────
export function spikeScenario(data) {
    const res = http.post(
        `${BASE_URL}/api/learning-events`,
        JSON.stringify({
            courseId:        1,
            chapterId:       1,
            lectureId:       1,
            eventType:       'LECTURE_ENTER',
            positionSeconds: null,
            eventTime:       new Date().toISOString().replace('Z', ''),
            eventKey:        uuidv4(),
        }),
        { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${data.userToken}` } }
    );

    const ok = check(res, {
        '201 또는 429(과부하 차단)': (r) => r.status === 201 || r.status === 429,
        '5xx 없음':                 (r) => r.status < 500,
    });
    errorRate.add(ok ? 0 : 1);

    sleep(0.5);
}

// ── 시나리오 2: 멱등성 ──────────────────────────────────────────────────
export function idempotencyScenario(data) {
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
        'Authorization': `Bearer ${data.userToken}`,
    };

    group('멱등성 - 첫 번째 전송 (201 기대)', () => {
        const res = http.post(`${BASE_URL}/api/learning-events`, payload, { headers });
        check(res, { '첫 전송 201': (r) => r.status === 201 });
    });

    group('멱등성 - 동일 키 재전송 (409 기대)', () => {
        const res = http.post(`${BASE_URL}/api/learning-events`, payload, { headers });
        const got409 = res.status === 409;
        idempotencyOk.add(got409 ? 1 : 0);
        check(res, { '재전송 409 Conflict': (r) => r.status === 409 });
    });

    sleep(randomIntBetween(1, 3));
}

function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
