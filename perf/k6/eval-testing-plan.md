# Evaluation Load Test Plan

This folder keeps evaluation-only k6 scripts separate from the existing focused performance scripts.

The evaluation server does not need InfluxDB or local Grafana. The server-side application metrics are observed through the existing Alloy -> Grafana Cloud pipeline. Run k6 from the client machine and correlate the k6 result time window with Grafana Cloud dashboards.

## 1. Prep and Smoke

Use a namespaced `RUN_ID` so evaluation data can be identified later.

```bash
cd perf
docker compose -f compose.client.yml run --rm \
  -e BASE_URL=https://your-eval-api.example.com \
  -e RUN_ID=eval-001 \
  -e EMAIL_PREFIX=k6-eval-001 \
  -e USER_COUNT=50 \
  -e PREP_SIGNUP=true \
  -e PREP_PURCHASE=true \
  k6 run /scripts/eval-prep-smoke.js
```

The script prints the discovered `COURSE_ID`, `CHAPTER_ID`, and `LECTURE_ID`.

## 2. Stage Runs

Run each stage with the ids printed by prep.

```bash
cd perf
docker compose -f compose.client.yml run --rm \
  -e BASE_URL=https://your-eval-api.example.com \
  -e RUN_ID=eval-001 \
  -e EMAIL_PREFIX=k6-eval-001 \
  -e USER_COUNT=50 \
  -e COURSE_ID=5 \
  -e CHAPTER_ID=4 \
  -e LECTURE_ID=4 \
  -e STAGE=smoke \
  k6 run /scripts/eval-real-user-flow.js
```

If you want k6 metrics in Grafana Cloud as well, run with your Grafana Cloud k6 output configuration from the load-generating client. Keep that separate from the server; the evaluated server should only receive normal HTTP traffic.

Recommended order:

```text
smoke -> baseline -> load -> stress -> spike -> soak
```

## 3. Stage Defaults

```text
smoke    small correctness check
baseline 30 RPS for 5 minutes
load     ramps to RPS=50 and holds for 10 minutes
stress   ramps through RPS1=50, RPS2=100, RPS3=200
spike    jumps from BASE_RPS=20 to SPIKE_RPS=250
soak     RPS=30 for 30 minutes
```

Override with environment variables such as `RPS`, `DURATION`, `VUS`, `MAX_VUS`, `RPS1`, `RPS2`, `RPS3`, `BASE_RPS`, and `SPIKE_RPS`.

## 4. Data Safety

Prefer additive evaluation data over full database reset on shared servers.

```text
email: k6-{RUN_ID}-{seq}@test.local
nickname: k6-{RUN_ID}-{seq}
paymentKey/idempotencyKey: k6-{RUN_ID}-...
```

Purchase traffic is disabled in the real-user flow by default. Enable it only when the target course and test users are safe to mutate:

```bash
cd perf
docker compose -f compose.client.yml run --rm \
  -e BASE_URL=https://your-eval-api.example.com \
  -e RUN_ID=eval-001 \
  -e EMAIL_PREFIX=k6-eval-001 \
  -e USER_COUNT=50 \
  -e COURSE_ID=1 \
  -e CHAPTER_ID=1 \
  -e LECTURE_ID=1 \
  -e STAGE=load \
  -e INCLUDE_PURCHASE=true \
  k6 run /scripts/eval-real-user-flow.js
```

## 5. Observability During Evaluation

Use two views during each stage:

```text
k6 client output:
- request rate
- p95/p99 latency
- HTTP failure rate
- flow failure rate

Grafana Cloud via Alloy:
- Spring/HTTP request metrics
- JVM memory and GC
- CPU and memory
- DB connection pool
- Redis and MySQL host/container metrics if available
```

Record the absolute start/end time of each k6 run so the Grafana Cloud dashboard can be filtered to the exact evaluation window.
