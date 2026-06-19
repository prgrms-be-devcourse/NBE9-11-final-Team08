# team08 - Docker Compose 단축 명령
#
# 사용법:  make <target>
# 도움말:  make help
#
# 개발: DB만 Docker로 띄우고 Spring Boot는 IntelliJ에서 dev 프로파일로 직접 실행.

COMPOSE_DIR := infra/compose
ENV_FILE    := --env-file .env.dev
DC          := docker compose ${ENV_FILE}              # dev/edge 공통 (.env.dev)
PROD_DC     := docker compose --env-file .env          # 배포 전용 (.env — 운영 비밀값)
BASE        := -f compose.yaml
DEV         := -f compose.dev.yaml             # dev 는 단독 실행 (base 머지 안 함)
PROD        := $(BASE) -f compose.prod.yaml    # prod 는 base 에 덮어쓰기

# 부하 테스트(k6)
BASE_DC      := docker compose $(BASE)  # 부하 stack (k6 + influx + grafana)
PERF_DIR     := perf
PERF_DC      := docker compose -f compose.perf.yml      # 부하 stack (k6 + influx + grafana)
PERF_SCRIPT  ?= last-watched-baseline.js   # make perf PERF_SCRIPT=other.js 로 교체 가능

# 개발(dev/edge) compose 명령은 .env.dev 로 실행
define run
	cd $(COMPOSE_DIR) && $(DC) $(1)
endef

# 배포(prod) compose 명령은 .env 로 실행
define run-prod
	cd $(COMPOSE_DIR) && $(PROD_DC) $(1)
endef

.DEFAULT_GOAL := help
.PHONY: help dev dev-down dev-logs db-reset edge edge-down prod prod-pull prod-down prod-logs ps down-all perf perf-down perf-reset

## ─────────────── 개발 (DB만) ───────────────

dev: ## [개발] DB만 기동 (IntelliJ에서 백엔드 직접 실행)
	$(call run,$(DEV) up -d db)
	@echo "✅ DB 기동 완료 → localhost:3306. IntelliJ에서 dev 프로파일로 BackendApplication 실행하세요."

dev-down: ## [개발] DB 컨테이너 내리기 (데이터 유지)
	$(call run,$(DEV) down)

dev-logs: ## [개발] DB 로그 실시간 보기
	$(call run,$(DEV) logs -f db)

db-reset: ## [개발] DB 완전 초기화 (볼륨 삭제 후 재기동) ⚠️ 데이터 전부 삭제
	$(call run,$(DEV) down -v)
	$(call run,$(DEV) up -d db)
	@echo "✅ DB 초기화 완료. Flyway 마이그레이션 + dev 더미데이터가 백엔드 기동 시 다시 생성됩니다."

## ─────────────── 부하 테스트 (k6) ───────────────
# 측정 대상 서버는 먼저 deploy 쪽에서 띄운다:
#   dev  : make dev  + IntelliJ 로 backend 실행 (host:8080)
#   prod : make prod
# 그다음 make perf 가 host.docker.internal:8080 으로 그 서버를 친다.

perf-server: ## [분리] 서버 stack(db+app) 기동 — 측정 대상, 8080 노출
	$(call run,$(BASE) up -d db)
	@echo "✅ 서버 준비됨 → http://localhost:8080"

perf-server-down: ## DB 컨테이너 내리기 (데이터 유지)
	$(call run,$(base) down)
	@echo "🗑️ 서버 스택 내림 (볼륨 유지)"

perf-server-reset: ## DB 컨테이너 삭제 (데이터 삭제)
	$(call run,$(BASE) down -v)
	@echo "🗑️ 서버 스택 삭제 (볼륨 삭제)"

perf-client: ## [부하] k6 실행 (host.docker.internal:8080 타격) + influx/grafana 기동
	cd $(PERF_DIR) && $(PERF_DC) up -d influxdb grafana
	cd $(PERF_DIR) && $(PERF_DC) run --rm k6 run --out influxdb=http://influxdb:8086/k6 /scripts/$(PERF_SCRIPT)
	@echo "📊 Grafana → http://localhost:3000"

perf-client-down: ## [부하] 부하 stack 정리 (볼륨 유지 → 대시보드/이력 보존)
	cd $(PERF_DIR) && $(PERF_DC) --profile k6 down
	@echo "✅ 부하 stack 내림 (볼륨 유지)."

perf-client-reset: ## [부하] 부하 stack 완전 초기화 (볼륨 삭제 → 대시보드 재import)
	cd $(PERF_DIR) && $(PERF_DC) --profile k6 down -v
	@echo "🗑️  부하 stack 초기화 (볼륨 삭제)."

perf-all-down:  perf-client-down perf-server-down

# 이후 flyway 자세한 설정 issue에서 MAKEFILE을 하나의 PR로 제대로 다룰 예정
# 위 코드까지는 임시편의상 만든 make 명령

# ## ─────────────── 개발 + nginx (선택) ───────────────
#
# edge: ## [개발] nginx-dev까지 띄워 host.docker.internal로 프록시 검증 (localhost:80)
# 	$(call run,$(DEV) --profile edge up -d db nginx-dev)
#
# edge-down: ## [개발] nginx-dev 포함 내리기
# 	$(call run,$(DEV) --profile edge down)
#
# ## ─────────────── 배포 (base + prod, .env 사용) ───────────────
# # 운영 비밀값은 infra/compose/.env 에 둔다 (.env.example 복사해서 채움, git 추적 X).
#
# prod: ## [배포] base+prod 전체 스택 기동 (db + backend + nginx) — .env 사용
# 	$(call run-prod,$(PROD) up -d)
#
# prod-pull: ## [배포] 최신 이미지 pull 후 재기동
# 	$(call run-prod,$(PROD) pull)
# 	$(call run-prod,$(PROD) up -d)
#
# prod-down: ## [배포] 전체 스택 내리기 (데이터 유지)
# 	$(call run-prod,$(PROD) down)
#
# prod-logs: ## [배포] 백엔드 로그 실시간 보기
# 	$(call run-prod,$(PROD) logs -f backend)
# ## ─────────────── 공통 ───────────────

ps: ## 실행 중인 컨테이너 상태 보기
	$(call run,$(BASE) ps)

down-all: ## 모든 컨테이너 내리기 (데이터 유지)
	$(call run,$(BASE) down)

help: ## 이 도움말 출력
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'
