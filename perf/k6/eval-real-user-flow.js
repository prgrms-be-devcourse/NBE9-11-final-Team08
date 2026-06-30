/**
 * Evaluation real-user flow.
 *
 * Stages:
 *   STAGE=smoke    small correctness check
 *   STAGE=baseline moderate stable baseline
 *   STAGE=load     expected production-like traffic
 *   STAGE=stress   ramp beyond target
 *   STAGE=spike    sudden traffic burst
 *   STAGE=soak     long-running stability
 *
 * Example:
 * BASE_URL=https://api.example.com STAGE=baseline RUN_ID=eval-001 EMAIL_PREFIX=k6-eval-001 \
 * USER_COUNT=50 COURSE_ID=1 CHAPTER_ID=1 LECTURE_ID=1 k6 run perf/k6/eval-real-user-flow.js
 */

import { check, group } from 'k6';
import exec from 'k6/execution';
import { expectedStatuses, setResponseCallback } from 'k6/http';
import { Counter, Rate, Trend } from 'k6/metrics';
import {
  authedGet,
  authedPatch,
  authedPost,
  envBool,
  envInt,
  login,
  parseJson,
  pick,
  publicGet,
  randomInt,
  think,
  uuidv4,
} from './lib/eval-k6-utils.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STAGE = __ENV.STAGE || 'smoke';
const RUN_ID = __ENV.RUN_ID || 'eval-local';
const EMAIL_PREFIX = __ENV.EMAIL_PREFIX || `k6-${RUN_ID}`;
const PASSWORD = __ENV.PASSWORD || 'Test1234!';
const USER_COUNT = envInt('USER_COUNT', 10);
const COURSE_ID = envInt('COURSE_ID', 1);
const CHAPTER_ID = envInt('CHAPTER_ID', 1);
const LECTURE_ID = envInt('LECTURE_ID', 1);
const INCLUDE_PURCHASE = envBool('INCLUDE_PURCHASE', false);
const INCLUDE_COMMUNITY = envBool('INCLUDE_COMMUNITY', true);
const THINK_MIN = envInt('THINK_MIN', STAGE === 'smoke' ? 1 : 2);
const THINK_MAX = envInt('THINK_MAX', STAGE === 'smoke' ? 2 : 5);

const flowLatency = new Trend('eval_flow_latency', true);
const browseCount = new Counter('eval_browse_flow_count');
const learningCount = new Counter('eval_learning_flow_count');
const authCount = new Counter('eval_auth_flow_count');
const purchaseCount = new Counter('eval_purchase_flow_count');
const communityCount = new Counter('eval_community_flow_count');
const flowFailureRate = new Rate('eval_flow_failure_rate');

setResponseCallback(expectedStatuses({ min: 200, max: 299 }, 400, 404, 409));

export const options = buildOptions(STAGE);

export function setup() {
  const users = [];
  for (let i = 1; i <= USER_COUNT; i++) {
    const email = `${EMAIL_PREFIX}-${i}@test.local`;
    users.push(login(BASE_URL, email, PASSWORD));
  }

  const courseRes = publicGet(BASE_URL, `/api/courses/${COURSE_ID}`, { api: 'setup.course_detail' });
  const course = parseJson(courseRes, {});
  const discovered = discoverLecture(course);

  const ids = {
    courseId: COURSE_ID,
    chapterId: discovered.chapterId || CHAPTER_ID,
    lectureId: discovered.lectureId || LECTURE_ID,
  };

  console.log(`[setup] stage=${STAGE}, users=${users.length}, course=${ids.courseId}, chapter=${ids.chapterId}, lecture=${ids.lectureId}`);
  return { users, ids };
}

export default function (data) {
  const auth = pick(data.users);
  const ids = data.ids;
  const roll = Math.random();

  if (STAGE === 'smoke') {
    smokeFlow(auth, ids);
    return;
  }

  if (roll < 0.40) {
    browseFlow(ids);
  } else if (roll < 0.75) {
    learningFlow(auth, ids);
  } else if (roll < 0.85) {
    authFlow(auth);
  } else if (roll < 0.95 && INCLUDE_COMMUNITY) {
    communityFlow(auth, ids);
  } else if (INCLUDE_PURCHASE) {
    purchaseFlow(auth, ids);
  } else {
    browseFlow(ids);
  }
}

function smokeFlow(auth, ids) {
  authFlow(auth);
  think(THINK_MIN, THINK_MAX);
  browseFlow(ids);
  think(THINK_MIN, THINK_MAX);
  learningFlow(auth, ids);
}

function browseFlow(ids) {
  const started = Date.now();
  let ok = true;

  group('browse', () => {
    browseCount.add(1);
    ok = check(publicGet(BASE_URL, '/api/categories', { flow: 'browse', api: 'categories' }), {
      'categories 200': (r) => r.status === 200,
    }) && ok;

    think(THINK_MIN, THINK_MAX);

    ok = check(publicGet(BASE_URL, `/api/courses?page=${randomInt(0, 2)}&size=20&sort=VIEW_DESC`, { flow: 'browse', api: 'courses.list' }), {
      'courses list 200': (r) => r.status === 200,
    }) && ok;

    think(THINK_MIN, THINK_MAX);

    ok = check(publicGet(BASE_URL, `/api/courses/${ids.courseId}`, { flow: 'browse', api: 'courses.detail' }), {
      'course detail 200': (r) => r.status === 200,
    }) && ok;

    ok = check(publicGet(BASE_URL, '/api/coupons', { flow: 'browse', api: 'coupons.public' }), {
      'public coupons 2xx': (r) => r.status >= 200 && r.status < 300,
    }) && ok;
  });

  recordFlow(started, ok);
}

function authFlow(auth) {
  const started = Date.now();
  let ok = true;

  group('auth', () => {
    authCount.add(1);
    ok = check(authedGet(BASE_URL, auth, '/api/auth/me', { flow: 'auth', api: 'auth.me' }), {
      'me 200': (r) => r.status === 200,
    }) && ok;
  });

  recordFlow(started, ok);
}

function learningFlow(auth, ids) {
  const started = Date.now();
  let ok = true;
  const position = randomInt(10, 600);

  group('learning', () => {
    learningCount.add(1);

    ok = check(authedGet(BASE_URL, auth, '/api/studies/me', { flow: 'learning', api: 'studies.me' }), {
      'my studies 200': (r) => r.status === 200,
    }) && ok;

    think(THINK_MIN, THINK_MAX);

    ok = check(authedGet(BASE_URL, auth, `/api/courses/${ids.courseId}/lectures/last-watched`, { flow: 'learning', api: 'lectures.last_watched' }), {
      'last watched 200': (r) => r.status === 200,
    }) && ok;

    ok = check(authedGet(BASE_URL, auth, `/api/courses/${ids.courseId}/chapters/${ids.chapterId}/lectures/${ids.lectureId}/enter`, { flow: 'learning', api: 'lectures.enter' }), {
      'lecture enter 2xx': (r) => r.status >= 200 && r.status < 300,
    }) && ok;

    think(THINK_MIN, THINK_MAX);

    ok = check(authedGet(BASE_URL, auth, `/api/courses/${ids.courseId}/chapters/${ids.chapterId}/lectures/${ids.lectureId}/stream`, { flow: 'learning', api: 'lectures.stream' }), {
      'stream url 2xx or unavailable': (r) => (r.status >= 200 && r.status < 300) || r.status === 404,
    }) && ok;

    ok = check(authedPatch(BASE_URL, auth, `/api/lectures/${ids.lectureId}/progress`, {
      positionSeconds: position,
      watchedDeltaSeconds: randomInt(5, 30),
    }, { flow: 'learning', api: 'lectures.progress' }), {
      'progress patch 2xx': (r) => r.status >= 200 && r.status < 300,
    }) && ok;

    ok = check(authedPost(BASE_URL, auth, '/api/learning-events', {
      courseId: ids.courseId,
      chapterId: ids.chapterId,
      lectureId: ids.lectureId,
      eventType: pick(['LECTURE_ENTER', 'VIDEO_PAUSE', 'LECTURE_EXIT']),
      positionSeconds: position,
      eventKey: `k6-${RUN_ID}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${uuidv4()}`,
    }, { flow: 'learning', api: 'learning_events.record' }), {
      'learning event created': (r) => r.status === 201 || r.status === 200,
    }) && ok;

    ok = check(authedGet(BASE_URL, auth, `/api/courses/${ids.courseId}/lectures/progress`, { flow: 'learning', api: 'lectures.course_progress' }), {
      'course progress 200': (r) => r.status === 200,
    }) && ok;
  });

  recordFlow(started, ok);
}

function communityFlow(auth, ids) {
  const started = Date.now();
  let ok = true;

  group('community', () => {
    communityCount.add(1);

    ok = check(authedGet(BASE_URL, auth, `/api/lectures/${ids.lectureId}/qna`, { flow: 'community', api: 'qna.list' }), {
      'qna list 200': (r) => r.status === 200,
    }) && ok;

    ok = check(authedGet(BASE_URL, auth, `/api/lectures/${ids.lectureId}/reflections`, { flow: 'community', api: 'reflections.get' }), {
      'reflection get 2xx or empty': (r) => r.status >= 200 && r.status < 300,
    }) && ok;
  });

  recordFlow(started, ok);
}

function purchaseFlow(auth, ids) {
  const started = Date.now();
  let ok = true;

  group('purchase', () => {
    purchaseCount.add(1);
    const orderRes = authedPost(BASE_URL, auth, '/api/orders/direct', {
      courseId: ids.courseId,
    }, { flow: 'purchase', api: 'orders.direct' });

    ok = check(orderRes, {
      'direct order 2xx or already purchased': (r) => (r.status >= 200 && r.status < 300) || r.status === 400 || r.status === 409,
    }) && ok;

    const order = parseJson(orderRes, {});
    if (order && order.orderId) {
      const confirmRes = authedPost(BASE_URL, auth, `/api/payments/${order.orderId}/confirm`, {
        paymentKey: `k6-${RUN_ID}-${uuidv4()}`,
        method: 'CARD',
        amount: Number(order.finalPrice || order.totalPrice || 0),
        issuedCouponId: null,
        idempotencyKey: `k6-${RUN_ID}-${order.orderId}-${uuidv4()}`,
      }, { flow: 'purchase', api: 'payments.mock_confirm' });

      ok = check(confirmRes, {
        'mock confirm 2xx or already handled': (r) => (r.status >= 200 && r.status < 300) || r.status === 400 || r.status === 409,
      }) && ok;
    }
  });

  recordFlow(started, ok);
}

function discoverLecture(course) {
  const firstChapter = Array.isArray(course.chapters) ? course.chapters[0] : null;
  const firstLecture = firstChapter && Array.isArray(firstChapter.lectures) ? firstChapter.lectures[0] : null;
  return {
    chapterId: firstChapter && Number(firstChapter.id),
    lectureId: firstLecture && Number(firstLecture.id),
  };
}

function recordFlow(started, ok) {
  flowLatency.add(Date.now() - started);
  flowFailureRate.add(ok ? 0 : 1);
}

function buildOptions(stage) {
  const common = {
    setupTimeout: '5m',
    thresholds: {
      http_req_failed: ['rate<0.02'],
      eval_flow_failure_rate: ['rate<0.02'],
      http_req_duration: ['p(95)<1000', 'p(99)<2500'],
    },
  };

  const table = {
    smoke: {
      scenarios: {
        smoke: {
          executor: 'shared-iterations',
          vus: envInt('VUS', 2),
          iterations: envInt('ITERATIONS', 4),
          maxDuration: __ENV.DURATION || '3m',
        },
      },
    },
    baseline: {
      scenarios: {
        baseline: {
          executor: 'constant-arrival-rate',
          rate: envInt('RPS', 30),
          timeUnit: '1s',
          duration: __ENV.DURATION || '5m',
          preAllocatedVUs: envInt('PRE_ALLOCATED_VUS', 80),
          maxVUs: envInt('MAX_VUS', 200),
        },
      },
    },
    load: {
      scenarios: {
        load: {
          executor: 'ramping-arrival-rate',
          startRate: 10,
          timeUnit: '1s',
          preAllocatedVUs: envInt('PRE_ALLOCATED_VUS', 120),
          maxVUs: envInt('MAX_VUS', 400),
          stages: [
            { duration: '2m', target: envInt('RPS', 50) },
            { duration: __ENV.DURATION || '10m', target: envInt('RPS', 50) },
            { duration: '1m', target: 0 },
          ],
        },
      },
    },
    stress: {
      scenarios: {
        stress: {
          executor: 'ramping-arrival-rate',
          startRate: 20,
          timeUnit: '1s',
          preAllocatedVUs: envInt('PRE_ALLOCATED_VUS', 200),
          maxVUs: envInt('MAX_VUS', 800),
          stages: [
            { duration: '2m', target: envInt('RPS1', 50) },
            { duration: '3m', target: envInt('RPS2', 100) },
            { duration: '3m', target: envInt('RPS3', 200) },
            { duration: '2m', target: 0 },
          ],
        },
      },
    },
    spike: {
      scenarios: {
        spike: {
          executor: 'ramping-arrival-rate',
          startRate: envInt('BASE_RPS', 20),
          timeUnit: '1s',
          preAllocatedVUs: envInt('PRE_ALLOCATED_VUS', 200),
          maxVUs: envInt('MAX_VUS', 800),
          stages: [
            { duration: '1m', target: envInt('BASE_RPS', 20) },
            { duration: '30s', target: envInt('SPIKE_RPS', 250) },
            { duration: '2m', target: envInt('BASE_RPS', 20) },
            { duration: '30s', target: 0 },
          ],
        },
      },
    },
    soak: {
      scenarios: {
        soak: {
          executor: 'constant-arrival-rate',
          rate: envInt('RPS', 30),
          timeUnit: '1s',
          duration: __ENV.DURATION || '30m',
          preAllocatedVUs: envInt('PRE_ALLOCATED_VUS', 120),
          maxVUs: envInt('MAX_VUS', 400),
        },
      },
    },
  };

  return Object.assign({}, common, table[stage] || table.smoke);
}
