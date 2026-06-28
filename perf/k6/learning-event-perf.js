/**
 * ============================================================================
 * Learning Event API - k6 Performance Test
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 온라인 강의 수강 시스템의 학습 이벤트 API 성능 측정
 *
 * 테스트 대상 서버
 * ----------------------------------------------------------------------------
 * AWS EC2 t2.small
 * - vCPU : 1
 * - Memory : 2GB
 *
 * 사용 도구
 * ----------------------------------------------------------------------------
 * k6 (https://k6.io)
 *
 * 실행 방법
 * ----------------------------------------------------------------------------
 * [A] 완전 자동 (Docker 필요, compose-dev / build.gradle 불필요)
 *   make perf          # 프로젝트 루트에서: 인프라(MySQL+앱 dev/bulk) 기동 + 이 스크립트 실행
 *   make perf-down     # 정리
 *     → compose: perf/compose.single.yml
 *     → 다른 스크립트: make perf PERF_SCRIPT=다른파일.js (perf/k6 디렉토리)
 *
 * [B] 로컬 k6 바이너리로 직접 실행 (서버가 이미 떠 있을 때)
 *   BASE_URL=http://localhost:8080 \
 *   k6 run perf/k6/learning-event-perf.js
 *
 * 시나리오 구성
 * ----------------------------------------------------------------------------
 * 1. learningFlow (쓰기 중심)
 *    강의 수강 과정 시뮬레이션
 *
 *    LECTURE_ENTER
 *      ↓
 *    VIDEO_PAUSE × 3
 *      ↓
 *    LECTURE_COMPLETE
 *
 * 2. statsQuery (읽기 중심)
 *    판매자/강사 통계 조회
 *
 * 3. adminQuery (대용량 조회)
 *    관리자 기능 조회
 *
 * 성능 목표 (t2.small 기준)
 * ----------------------------------------------------------------------------
 * POST 요청
 *   p95 < 500ms
 *   p99 < 1000ms
 *
 * GET 요청
 *   p95 < 300ms
 *   p99 < 700ms
 *
 * Error Rate
 *   < 1%
 *
 * ============================================================================
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';


// ============================================================================
// 인라인 유틸 (외부 jslib 의존 제거)
// ----------------------------------------------------------------------------
// grafana/k6 컨테이너는 인터넷이 없을 수 있어 https://jslib.k6.io 임포트가
// 실패한다. 부하 테스트가 오프라인에서도 돌도록 필요한 헬퍼만 직접 구현한다.
// ============================================================================

function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}


// ============================================================================
// 환경 변수
// ============================================================================

/**
 * 테스트 대상 서버 주소
 *
 * 예)
 * BASE_URL=http://localhost:8080
 * BASE_URL=https://api.playlearn.com
 */
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';


// ============================================================================
// 사전 준비 데이터
// ============================================================================

/**
 * 실제 DB에 존재해야 하는 데이터
 *
 * 랜덤하게 선택되어 테스트 수행
 */
const COURSE_IDS = [1, 2, 3];
const CHAPTER_IDS = [1];
const LECTURE_IDS = [1, 2, 3, 4, 5];
const USER_IDS = [1];


// ============================================================================
// 커스텀 메트릭
// ============================================================================

/**
 * 쓰기 요청 응답 시간
 *
 * POST /learning-events
 */
const writeLatency = new Trend('write_latency', true);

/**
 * 조회 요청 응답 시간
 *
 * GET API 전용
 */
const readLatency = new Trend('read_latency', true);

/**
 * 사용자 정의 에러율
 */
const errorRate = new Rate('error_rate');

/**
 * 중복 이벤트 저장 충돌 횟수
 *
 * eventKey 중복 시 발생하는 409
 */
const duplicateConflicts = new Counter('duplicate_409');


// ============================================================================
// 부하 시나리오 설정
// ============================================================================

export const options = {

    scenarios: {

        /**
         * --------------------------------------------------------------------
         * 학습 이벤트 생성 시나리오
         * --------------------------------------------------------------------
         *
         * 점진적으로 사용자 증가
         *
         * 0 → 20 → 40 → 60 → 0
         */
        learningFlow: {
            executor: 'ramping-vus',

            startVUs: 0,

            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 40 },
                { duration: '30s', target: 60 },
                { duration: '30s', target: 0 }
            ],

            gracefulRampDown: '10s',

            exec: 'learningFlowScenario',

            tags: {
                scenario: 'learningFlow'
            }
        },

        /**
         * --------------------------------------------------------------------
         * 판매자 통계 조회
         * --------------------------------------------------------------------
         *
         * 읽기 위주 트래픽
         */
        statsQuery: {
            executor: 'ramping-vus',

            startVUs: 0,

            stages: [
                { duration: '30s', target: 30 },
                { duration: '1m', target: 60 },
                { duration: '30s', target: 100 },
                { duration: '30s', target: 0 }
            ],

            gracefulRampDown: '10s',

            exec: 'statsQueryScenario',

            tags: {
                scenario: 'statsQuery'
            },

            startTime: '10s'
        },

        /**
         * --------------------------------------------------------------------
         * 관리자 조회
         * --------------------------------------------------------------------
         *
         * 전체 이벤트 목록 조회
         * 특정 사용자 활동 조회
         */
        adminQuery: {
            executor: 'constant-vus',

            vus: 5,

            duration: '2m',

            exec: 'adminQueryScenario',

            tags: {
                scenario: 'adminQuery'
            },

            startTime: '15s'
        }
    },


    // ------------------------------------------------------------------------
    // 성능 목표 (Threshold)
    // ------------------------------------------------------------------------
    thresholds: {

        // 전체 HTTP 요청
        'http_req_duration': [
            'p(95)<500',
            'p(99)<1000'
        ],

        // 쓰기 API
        'write_latency': [
            'p(95)<500',
            'p(99)<1000'
        ],

        // 읽기 API
        'read_latency': [
            'p(95)<300',
            'p(99)<700'
        ],

        // 사용자 정의 에러율
        'error_rate': [
            'rate<0.01'
        ],

        // k6 기본 실패율
        'http_req_failed': [
            'rate<0.02'
        ]
    }
};


// ============================================================================
// Setup
// ============================================================================

/**
 * 테스트 시작 전 단 한번 수행
 *
 * 사용자 / 판매자 / 관리자 로그인 후
 * JWT 토큰을 모든 VU에 공유
 */
export function setup() {

    function login(email, password) {

        const res = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({
                email,
                password
            }),
            {
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );

        if (res.status !== 200 && res.status !== 204) {
            throw new Error(
                `[setup] 로그인 실패 (${email}) status=${res.status}`
            );
        }

        // 로그인 응답은 204 + HttpOnly 쿠키(accessToken)로 토큰을 내려준다(본문 없음).
        const cookie = res.cookies['accessToken'];
        const token =
            cookie && cookie[0] ? cookie[0].value : null;
        if (!token) {
            throw new Error(`[setup] accessToken 쿠키 없음 (${email})`);
        }

        console.log(
            `[setup] 로그인 성공: ${email}`
        );

        return token;
    }

    return {
        userToken: login(
            'user1@test.com',
            'Test1234!'
        ),

        sellerToken: login(
            'seller1@test.com',
            'Test1234!'
        ),

        adminToken: login(
            'admin@test.com',
            'Test1234!'
        )
    };
}


// ============================================================================
// 공통 유틸
// ============================================================================

function authHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

function pick(arr) {
    return arr[
        randomIntBetween(0, arr.length - 1)
        ];
}

function safeJson(body) {
    try {
        const parsed = JSON.parse(body);
        // 커넥션 실패 시 body 가 null → JSON.parse(null) 은 null 을 반환(예외 X).
        // 이후 .field 접근에서 TypeError 가 나면 iteration 이 sleep 없이 폭주하므로 방어한다.
        return parsed && typeof parsed === 'object' ? parsed : {};
    } catch (e) {
        // k6 의 babel 은 인자 없는 catch(optional catch binding)를 지원하지 않는다.
        return {};
    }
}


// ============================================================================
// 시나리오 1
// 학습 이벤트 생성
// ============================================================================

export function learningFlowScenario(data) {

    const courseId = pick(COURSE_IDS);
    const chapterId = pick(CHAPTER_IDS);
    const lectureId = pick(LECTURE_IDS);

    const headers =
        authHeaders(data.userToken);

    /**
     * 강의 입장
     */
    group('LECTURE_ENTER', () => {

        const res = postEvent(headers, {
            courseId,
            chapterId,
            lectureId,

            eventType: 'LECTURE_ENTER',

            positionSeconds: null,

            eventTime:
                new Date()
                    .toISOString()
                    .replace('Z', ''),

            eventKey: uuidv4()
        });

        checkWrite(res, 'LECTURE_ENTER');
    });

    sleep(randomIntBetween(1, 2));

    /**
     * 멈춤(일시정지/중단) × 3 — 멈춘 위치를 어려운 구간 분석에 사용.
     * (구 POSITION_SAVE 하트비트는 PATCH /api/lectures/{id}/progress 로 분리되어 이벤트로 적재하지 않는다.)
     */
    for (let i = 1; i <= 3; i++) {

        sleep(randomIntBetween(1, 3));

        group(`VIDEO_PAUSE_${i}`, () => {

            const res = postEvent(headers, {
                courseId,
                chapterId,
                lectureId,

                eventType: 'VIDEO_PAUSE',

                positionSeconds: i * 30,

                eventTime:
                    new Date()
                        .toISOString()
                        .replace('Z', ''),

                eventKey: uuidv4()
            });

            checkWrite(res, 'VIDEO_PAUSE');
        });
    }

    sleep(randomIntBetween(1, 2));

    /**
     * 수강 완료
     */
    group('LECTURE_COMPLETE', () => {

        const res = postEvent(headers, {
            courseId,
            chapterId,
            lectureId,

            eventType: 'LECTURE_COMPLETE',

            positionSeconds: null,

            eventTime:
                new Date()
                    .toISOString()
                    .replace('Z', ''),

            eventKey: uuidv4()
        });

        checkWrite(
            res,
            'LECTURE_COMPLETE'
        );
    });

    sleep(randomIntBetween(2, 4));
}


// ============================================================================
// 시나리오 2
// 판매자 통계 조회
// ============================================================================

export function statsQueryScenario(data) {

    const courseId = pick(COURSE_IDS);
    const chapterId = pick(CHAPTER_IDS);

    group('강좌 통계 조회', () => {

        const res = http.get(
            `${BASE_URL}/api/learning-events/courses/${courseId}/stats`,
            {
                headers:
                    authHeaders(
                        data.sellerToken
                    )
            }
        );

        readLatency.add(
            res.timings.duration
        );

        check(res, {
            'status=200':
                (r) => r.status === 200,

            'courseId 존재':
                (r) =>
                    safeJson(r.body)
                        .courseId !== undefined
        });
    });

    sleep(randomIntBetween(1, 2));

    group('챕터 통계 조회', () => {

        const res = http.get(
            `${BASE_URL}/api/learning-events/chapters/${chapterId}/stats`,
            {
                headers:
                    authHeaders(
                        data.sellerToken
                    )
            }
        );

        readLatency.add(
            res.timings.duration
        );

        check(res, {
            'status=200':
                (r) => r.status === 200,

            'chapterId 존재':
                (r) =>
                    safeJson(r.body)
                        .chapterId !== undefined
        });
    });

    sleep(randomIntBetween(1, 3));
}


// ============================================================================
// 시나리오 3
// 관리자 조회
// ============================================================================

export function adminQueryScenario(data) {

    const userId = pick(USER_IDS);

    group('전체 이벤트 조회', () => {

        const res = http.get(
            `${BASE_URL}/api/learning-events/admin?page=0&size=50`,
            {
                headers:
                    authHeaders(
                        data.adminToken
                    )
            }
        );

        readLatency.add(
            res.timings.duration
        );

        check(res, {
            'status=200':
                (r) => r.status === 200,

            'content 존재':
                (r) =>
                    safeJson(r.body)
                        .content !== undefined
        });
    });

    sleep(randomIntBetween(2, 4));

    group('사용자 활동 조회', () => {

        const res = http.get(
            `${BASE_URL}/api/learning-events/users/${userId}/activities?page=0&size=20`,
            {
                headers:
                    authHeaders(
                        data.adminToken
                    )
            }
        );

        readLatency.add(
            res.timings.duration
        );

        check(res, {
            'status=200':
                (r) => r.status === 200
        });
    });

    sleep(randomIntBetween(3, 6));
}


// ============================================================================
// HTTP Helper
// ============================================================================

/**
 * 학습 이벤트 생성 API 호출
 */
function postEvent(headers, payload) {

    return http.post(
        `${BASE_URL}/api/learning-events`,
        JSON.stringify(payload),
        { headers }
    );
}


/**
 * 쓰기 API 검증
 *
 * - 응답시간 측정
 * - 상태코드 검증
 * - 응답 내용 검증
 * - 에러율 기록
 */
function checkWrite(res, eventType) {

    writeLatency.add(
        res.timings.duration
    );

    // 중복 이벤트
    if (res.status === 409) {

        duplicateConflicts.add(1);

        return;
    }

    const ok = check(res, {

        [`${eventType} 201`]:
            (r) => r.status === 201,

        [`${eventType} id 반환`]:
            (r) =>
                safeJson(r.body).id
                !== undefined,

        [`${eventType} eventType 일치`]:
            (r) =>
                safeJson(r.body)
                    .eventType
                === eventType
    });

    errorRate.add(ok ? 0 : 1);

    if (res.status >= 500) {
        errorRate.add(1);
    }
}