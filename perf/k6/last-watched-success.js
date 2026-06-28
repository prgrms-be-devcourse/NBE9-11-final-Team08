/**
 * ============================================================================
 * 강좌 내 최근 수강 강의 조회 - 성공 경로 전용 k6 스모크 테스트
 * ============================================================================
 *
 * bulk 시드 기준:
 *   - user{k}@test.com / Test1234!
 *   - user{k} 는 course{k} 에 수강등록과 시청 이력을 가진다.
 *
 * 따라서 이 스크립트는 항상 user{k} 로 로그인하고 course{k} 만 조회한다.
 * 권한 없음(COURSE_010)이나 MISS(null) 경로는 테스트하지 않는다.
 *
 * 실행:
 *   make perf-client PERF_SCRIPT=last-watched-success.js
 *   make perf-client PERF_SCRIPT=last-watched-success.js RPS=10 DURATION=30s USER_POOL=10
 * ============================================================================
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RPS = parseInt(__ENV.RPS || '10', 10);
const DURATION = __ENV.DURATION || '30s';
const USER_POOL = Math.max(1, Math.min(parseInt(__ENV.USER_POOL || '10', 10), 100));

const successRate = new Rate('success_rate');
const lastWatchedLatency = new Trend('last_watched_success_latency', true);

export const options = {
    scenarios: {
        success: {
            executor: 'constant-arrival-rate',
            rate: RPS,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Math.max(10, RPS * 2),
            maxVUs: Math.max(20, RPS * 6),
            exec: 'lastWatchedSuccess',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        success_rate: ['rate>0.99'],
        last_watched_success_latency: ['p(95)<500', 'p(99)<1000'],
    },
};

function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
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

    const cookie = res.cookies['accessToken'];
    if (!cookie || !cookie[0]) {
        throw new Error(`[setup] accessToken cookie missing email=${email}, status=${res.status}`);
    }
    return cookie[0].value;
}

export function setup() {
    const pairs = [];
    for (let k = 1; k <= USER_POOL; k++) {
        pairs.push({
            token: login(`user${k}@test.com`, 'Test1234!'),
            courseId: k,
        });
    }

    console.log(`[setup] success-only users=${pairs.length}, RPS=${RPS}, DURATION=${DURATION}`);
    return { pairs };
}

export function lastWatchedSuccess(data) {
    const p = data.pairs[randomIntBetween(0, data.pairs.length - 1)];

    group('last-watched success', () => {
        const res = http.get(
            `${BASE_URL}/api/courses/${p.courseId}/lectures/last-watched`,
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${p.token}`,
                },
            }
        );

        lastWatchedLatency.add(res.timings.duration);

        const ok = check(res, {
            'status=200': (r) => r.status === 200,
            'body exists': (r) => {
                const body = (r.body || '').trim();
                return body !== '' && body !== 'null';
            },
        });

        successRate.add(ok ? 1 : 0);

        if (!ok) {
            console.log(`[fail] courseId=${p.courseId}, status=${res.status}, body=${res.body}`);
        }
    });
}
