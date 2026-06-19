# team08 - Docker Compose 단축 명령
#
# 사용법:  make <target>
# 도움말:  make help
#
# 개발: DB만 Docker로 띄우고 Spring Boot는 IntelliJ에서 dev 프로파일로 직접 실행.

COMPOSE_DIR := infra/compose
ENV_FILE    := --env-file .env.dev
DC          := docker compose ${ENV_FILE}
BASE        := -f compose.yaml
DEV         := $(BASE) -f compose.dev.yaml
PROD        := $(BASE) -f compose.prod.yaml

# 부하 테스트(k6) - 독립 compose (perf/ 폴더에 자기완결적으로 모음)
PERF_DIR     := perf
PERF_DC      := docker compose -f compose.single.yml    # 한 머신 모드 (올인원)
SERVER_DC    := docker compose -f compose.server.yml    # 분리 모드: 서버 stack
CLIENT_DC    := docker compose -f compose.client.yml    # 분리 모드: 부하 stack
PERF_SCRIPT  ?= last-watched-baseline.js   # make perf PERF_SCRIPT=other.js 로 교체 가능

# 모든 compose 명령은 infra/compose 디렉토리에서 실행
define run
	cd $(COMPOSE_DIR) && $(DC) $(1)
endef

# k6 부하 테스트 명령은 perf 디렉토리에서 실행
define run-perf
	cd $(PERF_DIR) && $(PERF_DC) $(1)
endef

.DEFAULT_GOAL := help
.PHONY: help dev dev-down dev-logs db-reset edge edge-down prod prod-pull prod-down prod-logs ps down-all perf-server perf-client perf-server-down perf-client-down perf-all-down perf-server-reset perf-client-reset perf-all-reset

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

## ─────────────── 부하 테스트 — 분리 모드 (client/server 흉내) ───────────────
perf-server: ## [분리] 서버 stack(db+app) 기동 — 측정 대상, 8080 노출
	cd $(PERF_DIR) && $(SERVER_DC) up -d --build --wait app
	@echo "✅ 서버 준비됨 → http://localhost:8080"

perf-client: ## [분리] 부하 stack(k6+influx+grafana)에서 k6 실행 — host.docker.internal:8080 타격
	cd $(PERF_DIR) && $(CLIENT_DC) up -d influxdb grafana
	cd $(PERF_DIR) && $(CLIENT_DC) run --rm k6 run --out influxdb=http://influxdb:8086/k6 /scripts/$(PERF_SCRIPT)
	@echo "📊 Grafana → http://localhost:3000"

# 정리 — 볼륨 유지 (DB 시드 / Grafana 대시보드 / Influx 이력 보존, 빠른 재기동)
perf-server-down: ## [분리] 서버 stack 정리 (볼륨 유지 → 시드 보존)
	cd $(PERF_DIR) && $(SERVER_DC) down
	@echo "✅ 서버 stack 내림 (볼륨 유지)."

perf-client-down: ## [분리] 부하 stack 정리 (볼륨 유지 → 대시보드/이력 보존)
	cd $(PERF_DIR) && $(CLIENT_DC) --profile k6 down
	@echo "✅ 부하 stack 내림 (볼륨 유지)."

perf-all-down: perf-client-down perf-server-down ## [분리] 서버+부하 stack 정리 (볼륨 유지)
	@echo "✅ 분리 모드 내림 (볼륨 유지)."

# 완전 초기화 — 볼륨까지 삭제 (DB/대시보드/이력 전부 날림)
perf-server-reset: ## [분리] 서버 stack 완전 초기화 (볼륨 삭제 → 재시딩)
	cd $(PERF_DIR) && $(SERVER_DC) down -v
	@echo "🗑️  서버 stack 초기화 (볼륨 삭제)."

perf-client-reset: ## [분리] 부하 stack 완전 초기화 (볼륨 삭제 → 대시보드 재import)
	cd $(PERF_DIR) && $(CLIENT_DC) --profile k6 down -v
	@echo "🗑️  부하 stack 초기화 (볼륨 삭제)."

perf-all-reset: perf-client-reset perf-server-reset ## [분리] 서버+부하 stack 완전 초기화 (볼륨 삭제)
	@echo "🗑️  분리 모드 완전 초기화 완료."


# 이후 flyway 자세한 설정 issue에서 MAKEFILE을 하나의 PR로 제대로 다룰 예정
# 위 코드까지는 임시편의상 만든 make 명령

## ─────────────── 개발 + nginx (선택) ───────────────

# edge: ## [개발] nginx까지 띄워 host.docker.internal로 프록시 검증 (localhost:8080)
# 	$(call run,$(DEV) --profile edge up -d db nginx-dev)
#
# edge-down: ## [개발] nginx 포함 내리기
# 	$(call run,$(DEV) --profile edge down)
#
# ## ─────────────── 배포 ───────────────
#
# prod: ## [배포] 전체 스택 기동 (db + backend + nginx)
# 	$(call run,$(PROD) up -d)
#
# prod-pull: ## [배포] 최신 이미지 pull 후 재기동
# 	$(call run,$(PROD) pull)
# 	$(call run,$(PROD) up -d)
#
# prod-down: ## [배포] 전체 스택 내리기 (데이터 유지)
# 	$(call run,$(PROD) down)
#
# prod-logs: ## [배포] 백엔드 로그 실시간 보기
# 	$(call run,$(PROD) logs -f backend)
#
## ─────────────── 공통 ───────────────

ps: ## 실행 중인 컨테이너 상태 보기
	$(call run,$(BASE) ps)

down-all: ## 모든 컨테이너 내리기 (데이터 유지)
	$(call run,$(BASE) down)

help: ## 이 도움말 출력
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'
