/**
 * ============================================================================
 * Video Encoding Thread Pool - k6 Performance Test
 * ============================================================================
 *
 * 목적
 * ----------------------------------------------------------------------------
 * 비동기 동영상 인코딩 스레드 풀(videoEncodingExecutor)의 임계치를 측정합니다.
 * 동시 업로드 요청을 쏟아부어 비동기 큐(Capacity: 10)를 채우고,
 * RejectedExecutionException(HTTP 500)이 발생하는 지점을 정량적으로 확인합니다.
 *
 * 실행 방법
 * ----------------------------------------------------------------------------
 * make perf-client PERF_SCRIPT=video-encoding-perf.js
 * ============================================================================
 */

import http from 'k6/http';
import { check, group } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const LECTURE_ID = parseInt(__ENV.LECTURE_ID || '1', 10);
const VUS = parseInt(__ENV.VUS || '30', 10);
const ITERATIONS = parseInt(__ENV.ITERATIONS || '60', 10);
const PASSWORD = __ENV.PASSWORD || 'Test1234!';

export const options = {
    scenarios: {
        encodingStress: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '2m',
            exec: 'uploadVideoScenario',
        },
    },
    thresholds: {
        upload_latency: ['p(95)<2000'],
        rejected_rate: ['rate<0.01'], // 튜닝 후 Reject 비율을 0%에 수렴하게 만들기 위함
    },
};

// 메트릭 정의
const uploadLatency = new Trend('upload_latency', true);
const attemptCount = new Counter('upload_attempt');
const successCount = new Counter('upload_success');
const rejectCount = new Counter('upload_rejected');
const errorRate = new Rate('rejected_rate');

// 더미 MP4 파일 내용 구성 (최소 바이트 배열)
const dummyMp4Content = new Uint8Array([
    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, // ftyp
    0x6d, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00, // mp42
    0x6d, 0x70, 0x34, 0x31, 0x69, 0x73, 0x6f, 0x6d  // mp41isom
]);

export function setup() {
    // 1. CSRF 토큰 발급 요청 (시큐리티 통과 필수 단계 - 204 No Content 응답)
    const csrfRes = http.get(`${BASE_URL}/api/auth/csrf`);
    if (csrfRes.status !== 204) {
        throw new Error(`[setup] CSRF 발급 실패: status=${csrfRes.status}`);
    }

    const jar = http.cookieJar();
    let cookies = jar.cookiesForURL(BASE_URL);
    const xsrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : '';

    if (!xsrfToken) {
        throw new Error(`[setup] XSRF-TOKEN 쿠키를 획득하지 못했습니다.`);
    }

    // 2. 강사 계정으로 로그인하여 토큰 획득 (204 No Content 응답 및 쿠키 기반 토큰 수령)
    const email = 'seller1@test.com';
    const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: email, password: PASSWORD }),
        { 
            headers: { 
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken
            } 
        }
    );

    if (loginRes.status !== 204) {
        throw new Error(`[setup] 강사 로그인 실패: status=${loginRes.status}, body=${loginRes.body}`);
    }

    // 쿠키 저장소에서 발급된 accessToken 추출
    cookies = jar.cookiesForURL(BASE_URL);
    const token = cookies['accessToken'] ? cookies['accessToken'][0] : '';

    if (!token) {
        throw new Error(`[setup] accessToken 쿠키를 획득하지 못했습니다.`);
    }

    console.log(`[setup] 로그인 완료 (Instructor Token 확보). VUs=${VUS}, Iterations=${ITERATIONS}`);
    return { token };
}

export function uploadVideoScenario(data) {
    attemptCount.add(1);

    // 각 VU별로 독립적인 쿠키 저장소 및 CSRF 값 동적 동기화
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

    // multipart/form-data 요청 바디 구성
    const payload = {
        file: http.file(dummyMp4Content, 'dummy.mp4', 'video/mp4'),
    };

    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'X-XSRF-TOKEN': xsrfToken
        },
    };

    group('video upload process', () => {
        const startTime = Date.now();
        const res = http.post(
            `${BASE_URL}/api/courses/lectures/${LECTURE_ID}/videos`,
            payload,
            params
        );
        uploadLatency.add(Date.now() - startTime);

        // 202 Accepted 면 성공
        const isAccepted = res.status === 202;
        // 500 이면 스레드 풀 거절(RejectedExecutionException) 혹은 서버 에러
        const isRejected = res.status === 500;

        successCount.add(isAccepted ? 1 : 0);
        rejectCount.add(isRejected ? 1 : 0);
        errorRate.add(isRejected ? 1 : 0);

        check(res, {
            'status is 202 Accepted': (r) => r.status === 202,
        });

        if (res.status !== 202) {
            console.warn(`[warn] Upload failed with status=${res.status}, body=${res.body}`);
        }
    });
}
