/**
 * ============================================================================
 * Video Rollback DLQ - k6 Performance Test (Phase 0: AS-IS 결함 정량 측정)
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 동일 서버에 다수의 동시 영상 업로드를 쏟아부어 videoEncodingExecutor 스레드 풀을
 * 포화시킵니다. 각 업로드는 FFmpeg 인코딩 실패(더미/정상 영상 모두) 또는 인코딩 중
 * 트랜잭션 롤백을 유발하여 AFTER_ROLLBACK 이벤트를 발행합니다.
 * 이때 삭제 태스크가 동일 스레드 풀에 제출되면 풀 포화 시 RejectedExecutionException으로
 * 소리 없이 소실되어 로컬 찌꺼기 파일이 방치됩니다.
 *
 * 실행 방법 (로컬)
 * ----------------------------------------------------------------------------
 * 1. make dev → IntelliJ에서 !prod 프로파일로 백엔드 기동
 * 2. make perf-client PERF_SCRIPT=video-rollback-dlq-perf.js
 * ============================================================================
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL  = __ENV.BASE_URL  || 'http://localhost:8080';
const LECTURE_ID = parseInt(__ENV.LECTURE_ID || '1', 10);
const PASSWORD  = __ENV.PASSWORD  || 'Test1234!';

export const options = {
    scenarios: {
        baseline: {
            executor: 'constant-vus',
            vus: 2,
            duration: '10s',
            startTime: '0s',
            exec: 'uploadVideoScenario',
        },
        stress: {
            executor: 'constant-vus',
            vus: 8,
            duration: '20s',
            startTime: '10s',
            exec: 'uploadVideoScenario',
        },
        recovery: {
            executor: 'constant-vus',
            vus: 2,
            duration: '10s',
            startTime: '30s',
            exec: 'uploadVideoScenario',
        },
    },
    thresholds: {
        rejected_rate:  ['rate<0.05'],
        upload_latency: ['p(95)<30000'],
    },
};

// ── 커스텀 메트릭 ────────────────────────────────────────────────────────────
const uploadLatency  = new Trend('upload_latency', true);
const successCount   = new Counter('upload_success');
const rejectedCount  = new Counter('upload_rejected');
const rejectedRate   = new Rate('rejected_rate');

// ── 실제 영상 파일을 K6 바이너리로 로드 ─────────────────────────────────────
const VIDEO_FILE_PATH = __ENV.VIDEO_FILE_PATH || '/scripts/test-video.mp4';
const videoData = open(VIDEO_FILE_PATH, 'b');


// ── 로그인 세션 준비 ─────────────────────────────────────────────────────────
export function setup() {
    const csrfRes = http.get(`${BASE_URL}/api/auth/csrf`);
    if (csrfRes.status !== 204) {
        throw new Error(`[setup] CSRF 발급 실패: status=${csrfRes.status}`);
    }

    const jar = http.cookieJar();
    let cookies = jar.cookiesForURL(BASE_URL);
    const xsrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : '';

    if (!xsrfToken) {
        throw new Error('[setup] XSRF-TOKEN 쿠키 획득 실패');
    }

    const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: 'seller1@test.com', password: PASSWORD }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken,
            },
        }
    );

    if (loginRes.status !== 204) {
        throw new Error(`[setup] 강사 로그인 실패: status=${loginRes.status}`);
    }

    cookies = jar.cookiesForURL(BASE_URL);
    const token = cookies['accessToken'] ? cookies['accessToken'][0] : '';
    if (!token) {
        throw new Error('[setup] accessToken 쿠키 획득 실패');
    }

    console.log(`[setup] 로그인 완료. 대상 LectureId: ${LECTURE_ID}`);
    return { token, xsrfToken };
}


// ── 메인 시나리오 ─────────────────────────────────────────────────────────────
export function uploadVideoScenario(data) {
    const jar = http.cookieJar();
    let cookies = jar.cookiesForURL(BASE_URL);
    let xsrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : '';

    if (!xsrfToken) {
        const csrfRes = http.get(`${BASE_URL}/api/auth/csrf`);
        if (csrfRes.status === 204) {
            cookies = jar.cookiesForURL(BASE_URL);
            xsrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : '';
        }
    }

    const payload = {
        file: http.file(videoData, 'test-video.mp4', 'video/mp4'),
    };

    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'X-XSRF-TOKEN': xsrfToken,
        },
    };

    group('video upload (thread pool stress)', () => {
        const startTime = Date.now();
        const res = http.post(
            `${BASE_URL}/api/courses/lectures/${LECTURE_ID}/videos`,
            payload,
            params
        );
        uploadLatency.add(Date.now() - startTime);

        // 202: 인코딩 큐 제출 성공
        // 500/503: 스레드 풀 포화 거절 (RejectedExecutionException)
        const isAccepted = res.status === 202;
        const isRejected = res.status === 500 || res.status === 503;

        successCount.add(isAccepted ? 1 : 0);
        rejectedCount.add(isRejected ? 1 : 0);
        rejectedRate.add(isRejected ? 1 : 0);

        check(res, {
            'status is 202 Accepted (인코딩 큐 제출 성공)': (r) => r.status === 202,
            'not rejected by thread pool':                  (r) => r.status !== 500 && r.status !== 503,
        });
    });

    sleep(0.1);
}
