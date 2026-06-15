/**
 * Learning Event API - k6 Performance Test
 *
 * 대상 서버: t2.small (1 vCPU, 2 GB RAM)
 * 도구: k6 (https://k6.io)
 *
 * 실행 방법:
 *   BASE_URL=http://localhost:8080 \
 *   USER_TOKEN=<JWT> \
 *   ADMIN_TOKEN=<JWT> \
 *   SELLER_TOKEN=<JWT> \
 *   k6 run src/test/k6/learning-event-perf.js
 *
 * 토큰 발급 예시 (로컬):
 *   POST /api/auth/login  →  response.data.accessToken
 *
 * 시나리오 구성:
 *   1. learningFlow   - 일반 유저 학습 흐름 (write-heavy)
 *                       LECTURE_ENTER → VIDEO_START → POSITION_SAVE(×3) → LECTURE_COMPLETE
 *   2. statsQuery     - 판매자/관리자 통계 조회 (read)
 *                       GET /courses/{id}/stats, GET /chapters/{id}/stats
 *   3. adminQuery     - 관리자 전체 조회 (heavy read)
 *                       GET /admin, GET /users/{id}/activities
 *
 * t2.small 기준 목표치:
 *   - POST (write) : p95 < 500ms, p99 < 1s
 *   - GET  (read)  : p95 < 300ms, p99 < 700ms
 *   - 에러율       : < 1%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { randomIntBetween, uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ── 환경 변수 ──────────────────────────────────────────────────────────────
const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8080';
const USER_TOKEN  = __ENV.USER_TOKEN  || 'eyJhbGciOiJIUzI1NiJ9.user_placeholder';
const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || 'eyJhbGciOiJIUzI1NiJ9.admin_placeholder';
const SELLER_TOKEN= __ENV.SELLER_TOKEN|| 'eyJhbGciOiJIUzI1NiJ9.seller_placeholder';

// 테스트에 사용할 강좌·챕터·강의 ID (사전에 DB에 존재해야 함)
const COURSE_IDS  = [1, 2, 3, 4, 5];
const CHAPTER_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const LECTURE_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
const USER_IDS    = [1, 2, 3, 4, 5];

// ── 커스텀 메트릭 ──────────────────────────────────────────────────────────
const writeLatency = new Trend('write_latency', true);   // POST 전용 latency
const readLatency  = new Trend('read_latency', true);    // GET 전용 latency
const errorRate    = new Rate('error_rate');
const duplicateConflicts = new Counter('duplicate_409'); // 중복 이벤트 409

// ── 부하 시나리오 (t2.small 기준) ──────────────────────────────────────────
export const options = {
    scenarios: {
        /**
         * 시나리오 1: 학습 흐름 (write-heavy)
         * - 실제 학생이 강의를 듣는 패턴을 시뮬레이션
         * - t2.small에서 DB write가 병목 → VU 수를 보수적으로 설정
         */
        learningFlow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },  // 점진적 증가
                { duration: '1m',  target: 40 },  // 목표 부하 유지
                { duration: '30s', target: 60 },  // 피크 테스트
                { duration: '30s', target: 0  },  // 감소
            ],
            gracefulRampDown: '10s',
            exec: 'learningFlowScenario',
            tags: { scenario: 'learningFlow' },
        },

        /**
         * 시나리오 2: 통계 조회 (read)
         * - 판매자/관리자가 대시보드에서 통계를 확인하는 패턴
         * - read-only이므로 더 높은 VU 허용
         */
        statsQuery: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30  },
                { duration: '1m',  target: 60  },
                { duration: '30s', target: 100 },
                { duration: '30s', target: 0   },
            ],
            gracefulRampDown: '10s',
            exec: 'statsQueryScenario',
            tags: { scenario: 'statsQuery' },
            startTime: '10s',  // learningFlow와 오프셋 적용
        },

        /**
         * 시나리오 3: 관리자 전체 조회 (heavy read, 낮은 빈도)
         * - 페이지네이션 조회는 비용이 크므로 VU 수 제한
         */
        adminQuery: {
            executor: 'constant-vus',
            vus: 5,
            duration: '2m',
            exec: 'adminQueryScenario',
            tags: { scenario: 'adminQuery' },
            startTime: '15s',
        },
    },

    // ── 임계값 (t2.small 기준) ──────────────────────────────────────────────
    thresholds: {
        // 전체 응답
        'http_req_duration':          ['p(95)<500', 'p(99)<1000'],
        // write 전용
        'write_latency':              ['p(95)<500', 'p(99)<1000'],
        // read 전용
        'read_latency':               ['p(95)<300', 'p(99)<700'],
        // 에러율
        'error_rate':                 ['rate<0.01'],
        // HTTP 실패율 (4xx/5xx, 단 409는 예상 응답이므로 별도 처리)
        'http_req_failed':            ['rate<0.02'],
    },
};

// ── 공통 헤더 팩토리 ───────────────────────────────────────────────────────
function authHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };
}

function pick(arr) {
    return arr[randomIntBetween(0, arr.length - 1)];
}

// ── 시나리오 1: 학습 흐름 ─────────────────────────────────────────────────
export function learningFlowScenario() {
    const courseId  = pick(COURSE_IDS);
    const chapterId = pick(CHAPTER_IDS);
    const lectureId = pick(LECTURE_IDS);
    const headers   = authHeaders(USER_TOKEN);

    group('학습 흐름 - 강의 입장', () => {
        const res = postEvent(headers, {
            courseId,
            chapterId,
            lectureId,
            eventType: 'LECTURE_ENTER',
            positionSeconds: null,
            eventTime: new Date().toISOString().replace('Z', ''),
            eventKey: uuidv4(),  // 멱등성 키: 재전송 방지
        });
        checkWrite(res, 'LECTURE_ENTER');
    });

    sleep(randomIntBetween(1, 2));

    group('학습 흐름 - 영상 재생 시작', () => {
        const res = postEvent(headers, {
            courseId, chapterId, lectureId,
            eventType: 'VIDEO_START',
            positionSeconds: 0,
            eventTime: new Date().toISOString().replace('Z', ''),
            eventKey: uuidv4(),
        });
        checkWrite(res, 'VIDEO_START');
    });

    // 재생 중 위치 저장 (30초 간격으로 3회 시뮬레이션)
    for (let i = 1; i <= 3; i++) {
        sleep(randomIntBetween(1, 3));
        group(`학습 흐름 - 재생 위치 저장 (${i}회)`, () => {
            const res = postEvent(headers, {
                courseId, chapterId, lectureId,
                eventType: 'POSITION_SAVE',
                positionSeconds: i * 30,
                eventTime: new Date().toISOString().replace('Z', ''),
                eventKey: null,  // POSITION_SAVE는 멱등성 키 없이 매번 기록
            });
            checkWrite(res, 'POSITION_SAVE');
        });
    }

    sleep(randomIntBetween(1, 2));

    group('학습 흐름 - 수강 완료', () => {
        const res = postEvent(headers, {
            courseId, chapterId, lectureId,
            eventType: 'LECTURE_COMPLETE',
            positionSeconds: null,
            eventTime: new Date().toISOString().replace('Z', ''),
            eventKey: uuidv4(),
        });
        checkWrite(res, 'LECTURE_COMPLETE');
    });

    sleep(randomIntBetween(2, 4));
}

// ── 시나리오 2: 통계 조회 ─────────────────────────────────────────────────
export function statsQueryScenario() {
    const courseId  = pick(COURSE_IDS);
    const chapterId = pick(CHAPTER_IDS);

    group('통계 조회 - 강좌별 통계', () => {
        const res = http.get(
            `${BASE_URL}/api/learning-events/courses/${courseId}/stats`,
            { headers: authHeaders(SELLER_TOKEN) }
        );
        readLatency.add(res.timings.duration);
        check(res, {
            'getCourseStats 200': (r) => r.status === 200,
            'courseId 존재':      (r) => JSON.parse(r.body).courseId !== undefined,
        }) || errorRate.add(1);
        errorRate.add(res.status >= 400 && res.status !== 403 ? 1 : 0);
    });

    sleep(randomIntBetween(1, 2));

    group('통계 조회 - 챕터별 통계', () => {
        const res = http.get(
            `${BASE_URL}/api/learning-events/chapters/${chapterId}/stats`,
            { headers: authHeaders(SELLER_TOKEN) }
        );
        readLatency.add(res.timings.duration);
        check(res, {
            'getChapterStats 200': (r) => r.status === 200,
            'chapterId 존재':      (r) => JSON.parse(r.body).chapterId !== undefined,
        }) || errorRate.add(1);
        errorRate.add(res.status >= 400 && res.status !== 403 ? 1 : 0);
    });

    sleep(randomIntBetween(1, 3));
}

// ── 시나리오 3: 관리자 전체 조회 ─────────────────────────────────────────
export function adminQueryScenario() {
    const userId = pick(USER_IDS);

    group('관리자 - 전체 이벤트 목록 (페이지네이션)', () => {
        const res = http.get(
            `${BASE_URL}/api/learning-events/admin?page=0&size=50`,
            { headers: authHeaders(ADMIN_TOKEN) }
        );
        readLatency.add(res.timings.duration);
        check(res, {
            'getAllEvents 200': (r) => r.status === 200,
            '페이지 응답 구조': (r) => JSON.parse(r.body).content !== undefined,
        }) || errorRate.add(1);
        errorRate.add(res.status >= 500 ? 1 : 0);
    });

    sleep(randomIntBetween(2, 4));

    group('관리자 - 특정 유저 활동 조회', () => {
        const res = http.get(
            `${BASE_URL}/api/learning-events/users/${userId}/activities?page=0&size=20`,
            { headers: authHeaders(ADMIN_TOKEN) }
        );
        readLatency.add(res.timings.duration);
        check(res, {
            'getUserActivities 200': (r) => r.status === 200,
        }) || errorRate.add(1);
        errorRate.add(res.status >= 500 ? 1 : 0);
    });

    sleep(randomIntBetween(3, 6));
}

// ── 헬퍼 ──────────────────────────────────────────────────────────────────
function postEvent(headers, payload) {
    return http.post(
        `${BASE_URL}/api/learning-events`,
        JSON.stringify(payload),
        { headers }
    );
}

function checkWrite(res, eventType) {
    writeLatency.add(res.timings.duration);

    if (res.status === 409) {
        // 멱등성 키 중복 → 예상된 응답, 에러로 집계하지 않음
        duplicateConflicts.add(1);
        return;
    }

    const ok = check(res, {
        [`${eventType} 201`]:         (r) => r.status === 201,
        [`${eventType} id 반환`]:     (r) => {
            try { return JSON.parse(r.body).id !== undefined; } catch { return false; }
        },
        [`${eventType} eventType 일치`]: (r) => {
            try { return JSON.parse(r.body).eventType === eventType; } catch { return false; }
        },
    });

    errorRate.add(ok ? 0 : 1);
    if (res.status >= 500) errorRate.add(1);
}
