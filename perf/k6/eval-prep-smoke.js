/**
 * Evaluation prep/smoke script.
 *
 * This script is intentionally separate from load measurement. Use it to:
 * - verify the evaluation server is reachable,
 * - create or reuse namespaced test users,
 * - optionally create orders and mock payments for enrolled learning users,
 * - discover course/chapter/lecture ids for the real-user-flow script.
 *
 * Example:
 * BASE_URL=https://api.example.com RUN_ID=eval-001 USER_COUNT=50 PREP_SIGNUP=true PREP_PURCHASE=true \
 *   k6 run perf/k6/eval-prep-smoke.js
 */

import { check, group } from 'k6';
import { expectedStatuses, setResponseCallback } from 'k6/http';
import {
  authedGet,
  authedPost,
  envBool,
  envInt,
  login,
  parseJson,
  publicGet,
  signup,
  think,
  uuidv4,
} from './lib/eval-k6-utils.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RUN_ID = __ENV.RUN_ID || 'eval-local';
const EMAIL_PREFIX = __ENV.EMAIL_PREFIX || `k6-${RUN_ID}`;
const PASSWORD = __ENV.PASSWORD || 'Test1234!';
const USER_COUNT = envInt('USER_COUNT', 5);
const COURSE_ID = __ENV.COURSE_ID ? Number(__ENV.COURSE_ID) : null;
const PREP_SIGNUP = envBool('PREP_SIGNUP', false);
const PREP_PURCHASE = envBool('PREP_PURCHASE', false);
const THINK_MIN = envInt('THINK_MIN', 1);
const THINK_MAX = envInt('THINK_MAX', 2);

setResponseCallback(expectedStatuses({ min: 200, max: 299 }, 400, 409));

export const options = {
  scenarios: {
    prep_smoke: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '10m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
};

export default function () {
  let selectedCourseId = COURSE_ID;
  let chapterId = null;
  let lectureId = null;

  group('public smoke and discovery', () => {
    check(publicGet(BASE_URL, '/actuator/health', { api: 'health' }), {
      'health is 200': (r) => r.status === 200,
    });

    check(publicGet(BASE_URL, '/api/categories', { api: 'categories' }), {
      'categories is 200': (r) => r.status === 200,
    });

    const coursesRes = publicGet(BASE_URL, '/api/courses?page=0&size=20&sort=VIEW_DESC', { api: 'courses.list' });
    check(coursesRes, {
      'courses list is 200': (r) => r.status === 200,
    });

    if (!selectedCourseId) {
      const page = parseJson(coursesRes, {});
      const first = Array.isArray(page.content) ? page.content[0] : null;
      selectedCourseId = first && Number(first.id);
    }

    if (!selectedCourseId) {
      throw new Error('No COURSE_ID provided and no public course discovered.');
    }

    const courseRes = publicGet(BASE_URL, `/api/courses/${selectedCourseId}`, { api: 'courses.detail' });
    check(courseRes, {
      'course detail is 200': (r) => r.status === 200,
    });

    const course = parseJson(courseRes, {});
    const firstChapter = Array.isArray(course.chapters) ? course.chapters[0] : null;
    const firstLecture = firstChapter && Array.isArray(firstChapter.lectures) ? firstChapter.lectures[0] : null;
    chapterId = firstChapter && Number(firstChapter.id);
    lectureId = firstLecture && Number(firstLecture.id);
  });

  const users = [];

  group('test users', () => {
    for (let i = 1; i <= USER_COUNT; i++) {
      const email = `${EMAIL_PREFIX}-${i}@test.local`;
      const nickname = `${EMAIL_PREFIX}-${i}`;
      let csrf = null;

      if (PREP_SIGNUP) {
        const signupResult = signup(BASE_URL, email, PASSWORD, nickname);
        csrf = signupResult.csrf;
        think(THINK_MIN, THINK_MAX);
      }

      const auth = login(BASE_URL, email, PASSWORD, csrf);
      users.push({ email, auth });

      const meRes = authedGet(BASE_URL, auth, '/api/auth/me', { api: 'auth.me' });
      check(meRes, {
        'me is 200': (r) => r.status === 200,
      });
    }
  });

  if (PREP_PURCHASE) {
    group('optional purchase/enrollment prep', () => {
      for (const user of users) {
        const orderRes = authedPost(
          BASE_URL,
          user.auth,
          '/api/orders/direct',
          { courseId: selectedCourseId },
          { api: 'orders.direct' },
        );

        const orderOk = check(orderRes, {
          'direct order is created or already blocked as expected': (r) => r.status === 200 || r.status === 201 || r.status === 409 || r.status === 400,
        });

        if (orderRes.status >= 200 && orderRes.status < 300) {
          const order = parseJson(orderRes, {});
          const finalPrice = Number(order.finalPrice || order.totalPrice || 0);
          const orderId = Number(order.orderId);

          if (!orderId) {
            check(orderRes, {
              'direct order has orderId': () => false,
            });
            continue;
          }

          const confirmRes = authedPost(
            BASE_URL,
            user.auth,
            `/api/payments/${orderId}/confirm`,
            {
              paymentKey: `k6-${RUN_ID}-${user.email}-${uuidv4()}`,
              method: 'CARD',
              amount: finalPrice,
              issuedCouponId: null,
              idempotencyKey: `k6-${RUN_ID}-${orderId}-${uuidv4()}`,
            },
            { api: 'payments.mock_confirm' },
          );

          check(confirmRes, {
            'mock payment confirm is 2xx or already handled': (r) => (r.status >= 200 && r.status < 300) || r.status === 409 || r.status === 400,
          });
        }

        think(THINK_MIN, THINK_MAX);
      }
    });
  }

  console.log(`[eval-data] BASE_URL=${BASE_URL}`);
  console.log(`[eval-data] RUN_ID=${RUN_ID}`);
  console.log(`[eval-data] EMAIL_PREFIX=${EMAIL_PREFIX}`);
  console.log(`[eval-data] PASSWORD=${PASSWORD}`);
  console.log(`[eval-data] USER_COUNT=${USER_COUNT}`);
  console.log(`[eval-data] COURSE_ID=${selectedCourseId}`);
  console.log(`[eval-data] CHAPTER_ID=${chapterId || ''}`);
  console.log(`[eval-data] LECTURE_ID=${lectureId || ''}`);
  console.log('[eval-data] next example:');
  console.log(`BASE_URL=${BASE_URL} RUN_ID=${RUN_ID} EMAIL_PREFIX=${EMAIL_PREFIX} USER_COUNT=${USER_COUNT} COURSE_ID=${selectedCourseId} CHAPTER_ID=${chapterId || 1} LECTURE_ID=${lectureId || 1} k6 run perf/k6/eval-real-user-flow.js`);
}
