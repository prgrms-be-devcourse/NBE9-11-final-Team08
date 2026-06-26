/**
 * ============================================================================
 * 강좌 내 최근 수강 강의 조회 (getLastWatchedLecture) - k6 Baseline
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 최적화 "이전(baseline)" 성능을 측정해 기록으로 남긴다.
 * 이 스크립트는 단일 엔드포인트만 두드린다:
 *
 *   GET /api/courses/{courseId}/lectures/last-watched
 *
 * 현재 구현은 한 번의 요청에 3개의 쿼리를 던진다(루프 없는 고정 3쿼리):
 *   1) findIdsByCourseId            - 강좌의 모든 강의 ID (bulk 시드 기준 30개)
 *   2) findTop...LectureIdIn...Desc - lecture_id IN (...30개...) ORDER BY updated_at DESC LIMIT 1
 *   3) findById                     - 매칭된 강의 1건
 *
 * 개선(단일 조인 쿼리 + 인덱스) 후 "같은 시나리오"로 다시 돌려 p95/p99 델타를 비교한다.
 *
 * 측정 방법론
 * ----------------------------------------------------------------------------
 * constant-arrival-rate(open 모델)를 쓴다. 고정 RPS를 주입하므로 응답이 빨라져도
 * 부하량이 변하지 않는다 → before/after 가 "동일 부하"에서 비교돼 p95 개선이 증거가 된다.
 * (ramping-vus(closed 모델)는 응답이 빨라지면 총 요청 수가 늘어 비교가 흐려진다.)
 *
 * 시드 데이터 가정 (dev 프로필 + APP_DATA_INIT_MODE=bulk)
 * ----------------------------------------------------------------------------
 *   - 강좌 100개(id 1~100), 강좌당 강의 30개
 *   - 수강생 1000명(user1~user1000)
 *   - 학습 진행은 앞 100명만: user{k} 가 course{k} 에 progress 1건
 *     → user{k} + course{k}      = "이력 있음" (3쿼리 풀패스)
 *     → user{k} + 다른 course    = "이력 없음" (null, 2쿼리)
 *
 * 실행 방법
 * ----------------------------------------------------------------------------
 * [A] 완전 자동 (Docker)
 *   make perf PERF_SCRIPT=last-watched-baseline.js
 *   make perf-down
 *
 * [B] 로컬 k6 바이너리 (서버가 이미 떠 있을 때)
 *   BASE_URL=http://localhost:8080 k6 run k6/last-watched-baseline.js
 *
 * 튜닝 가능한 환경 변수 (모두 선택)
 * ----------------------------------------------------------------------------
 *   BASE_URL    대상 서버           (기본 http://localhost:8080)
 *   RPS         초당 요청 수        (기본 50)
 *   DURATION    측정 구간 길이      (기본 2m)
 *   USER_POOL   로그인할 유저 수    (기본 40, 1~100 사이여야 이력이 존재)
 *   HIT_RATIO   "이력 있음" 비율    (기본 0.7)
 *
 * ============================================================================
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';


// ============================================================================
// 환경 변수
// ============================================================================

const BASE_URL   = __ENV.BASE_URL || 'http://localhost:8080';
const RPS        = parseInt(__ENV.RPS || '50', 10);
const DURATION   = __ENV.DURATION || '2m';
const USER_POOL  = Math.max(1, Math.min(parseInt(__ENV.USER_POOL || '40', 10), 100));
const HIT_RATIO  = parseFloat(__ENV.HIT_RATIO || '0.7');

// bulk 시드 기준 강좌 수 (miss 경로에서 "다른 강좌"를 고를 때 사용)
const TOTAL_COURSES = 100;


// ============================================================================
// 인라인 유틸 (오프라인 컨테이너에서도 동작하도록 외부 import 제거)
// ============================================================================

function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}


// ============================================================================
// 커스텀 메트릭
// ============================================================================

// 엔드포인트 전체 응답시간
const lastWatchedLatency = new Trend('last_watched_latency', true);

// "이력 있음(3쿼리 풀패스)" 경로 응답시간 — 진짜 병목 후보
const hitLatency  = new Trend('last_watched_hit_latency', true);

// "이력 없음(null, 2쿼리)" 경로 응답시간
const missLatency = new Trend('last_watched_miss_latency', true);

const errorRate    = new Rate('error_rate');
const nonNullBody  = new Counter('body_non_null'); // 이력 반환된 횟수
const nullBody     = new Counter('body_null');     // null 반환된 횟수


// ============================================================================
// 부하 시나리오
// ============================================================================

export const options = {

    scenarios: {

        // 짧은 워밍업: JIT/커넥션풀/캐시 예열 (측정에서 제외하려고 태그 분리)
        warmup: {
            executor: 'constant-arrival-rate',
            rate: Math.max(5, Math.floor(RPS / 5)),
            timeUnit: '1s',
            duration: '20s',
            preAllocatedVUs: 20,
            maxVUs: 100,
            exec: 'lastWatched',
            tags: { phase: 'warmup' },
        },

        // 본 측정: 고정 RPS 주입
        steady: {
            executor: 'constant-arrival-rate',
            rate: RPS,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: RPS * 2,
            maxVUs: RPS * 6,
            startTime: '20s',
            exec: 'lastWatched',
            tags: { phase: 'steady' },
        },
    },

    // 성능 목표 — GET 기준(p95<300, p99<700).
    // baseline 단계에서는 "통과"가 목적이 아니라 "기록"이 목적이다.
    // 최적화 전에는 여기서 FAIL 이 떠도 정상이며, 그게 곧 개선 여지의 증거다.
    thresholds: {
        // steady 구간만 평가 (워밍업 제외)
        'last_watched_latency{phase:steady}': ['p(95)<300', 'p(99)<700'],
        'error_rate': ['rate<0.01'],
        'http_req_failed': ['rate<0.02'],
    },
};


// ============================================================================
// Setup — user1..userN 로그인 후 (토큰, 본인 강좌) 쌍을 공유
// ============================================================================

export function setup() {

    function login(email, password) {
        const res = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({ email, password }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        if (res.status !== 200 && res.status !== 204) {
            throw new Error(`[setup] 로그인 실패 (${email}) status=${res.status}`);
        }
        // 로그인 응답은 204 + HttpOnly 쿠키(accessToken)로 토큰을 내려준다(본문 없음).
        // k6에서 다중 Set-Cookie는 res.cookies 로 안전하게 읽는다.
        const cookie = res.cookies['accessToken'];
        if (!cookie || !cookie[0]) {
            throw new Error(`[setup] accessToken 쿠키 없음 (${email}) status=${res.status}`);
        }
        return cookie[0].value;
    }

    const pairs = [];
    for (let k = 1; k <= USER_POOL; k++) {
        pairs.push({
            token: login(`user${k}@test.com`, 'Test1234!'),
            ownCourseId: k, // user{k} 는 course{k} 에 시청 이력 보유
        });
    }
    console.log(`[setup] ${pairs.length}명 로그인 완료 (HIT_RATIO=${HIT_RATIO}, RPS=${RPS})`);
    return { pairs };
}


// ============================================================================
// 시나리오 본문
// ============================================================================

export function lastWatched(data) {

    const pairs = data.pairs;
    const p = pairs[randomIntBetween(0, pairs.length - 1)];

    // HIT_RATIO 확률로 "이력 있음" 경로, 나머지는 "이력 없음" 경로
    const isHit = Math.random() < HIT_RATIO;

    let courseId;
    if (isHit) {
        courseId = p.ownCourseId; // 본인 강좌 → progress 존재
    } else {
        // 본인 강좌가 아닌 임의 강좌 → progress 없음 → null
        do {
            courseId = randomIntBetween(1, TOTAL_COURSES);
        } while (courseId === p.ownCourseId);
    }

    group(isHit ? 'last-watched (HIT)' : 'last-watched (MISS)', () => {

        const res = http.get(
            `${BASE_URL}/api/courses/${courseId}/lectures/last-watched`,
            {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${p.token}`,
                },
            }
        );

        lastWatchedLatency.add(res.timings.duration);
        (isHit ? hitLatency : missLatency).add(res.timings.duration);

        const ok = check(res, {
            'status=200': (r) => r.status === 200,
        });
        errorRate.add(ok ? 0 : 1);

        // 200 이지만 본문이 null 일 수 있음(이력 없음). body 길이로 구분.
        // (LectureEnterResponse JSON 이면 non-null, 빈/"null" 이면 null)
        const body = (res.body || '').trim();
        if (body === '' || body === 'null') {
            nullBody.add(1);
        } else {
            nonNullBody.add(1);
        }
    });
}
