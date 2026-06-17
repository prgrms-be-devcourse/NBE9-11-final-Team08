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

# 부하 테스트(k6) - 독립 compose (compose-dev 와 무관, 자체 MySQL+앱+k6)
PERF_DIR     := backend/src/test/k6
PERF_DC      := docker compose -f compose.perf.yml

# 모든 compose 명령은 infra/compose 디렉토리에서 실행
define run
	cd $(COMPOSE_DIR) && $(DC) $(1)
endef

# k6 부하 테스트 명령은 backend/src/test/k6 디렉토리에서 실행
define run-perf
	cd $(PERF_DIR) && $(PERF_DC) $(1)
endef

.DEFAULT_GOAL := help
.PHONY: help dev dev-down dev-logs db-reset edge edge-down prod prod-pull prod-down prod-logs ps down-all perf-build perf perf-down perf-logs

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

perf-build: ## [부하] 앱 이미지 빌드 (실행 전 1회, 코드 변경 시 다시)
	$(call run-perf,build)
	@echo "✅ 빌드 완료. 실행: make perf"

perf: ## [부하] 부하 테스트 실행 (사전 빌드 필요: make perf-build)
	$(call run-perf,up --abort-on-container-exit --exit-code-from k6)
	@echo "ℹ️  정리하려면: make perf-down"

perf-down: ## [부하] 부하 테스트 컨테이너/볼륨 정리
	$(call run-perf,down -v)
	@echo "✅ 부하 테스트 환경 정리 완료."

perf-logs: ## [부하] k6 로그 실시간 보기
	$(call run-perf,logs -f k6)


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
# ## ─────────────── 공통 ───────────────
#
# ps: ## 실행 중인 컨테이너 상태 보기
# 	$(call run,$(BASE) ps)
#
# down-all: ## 모든 컨테이너 내리기 (데이터 유지)
# 	$(call run,$(BASE) down)
#
# help: ## 이 도움말 출력
# 	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
# 		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'
