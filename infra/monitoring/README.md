# 모니터링 (Grafana Cloud)

Alloy 가 백엔드 actuator·exporter 메트릭을 Docker 내부망으로 수집해 **Grafana Cloud** 로
push(remote_write) 한다. 호스트(4GB)에는 무거운 Prometheus/Grafana 를 두지 않는다.

```
backend-blue/green :8080/actuator/prometheus ─┐
node-exporter      :9100 ─┤
redis-exporter     :9121 ─┼─► Alloy ──(HTTPS remote_write)──► Grafana Cloud
mysqld-exporter    :9104 ─┘
```

/ 와 actuator/health 만 공개되고, `/actuator/prometheus` 는 nginx 에서 403 으로 막는다
(Alloy 는 내부망으로 직접 긁으므로 영향 없음).

## 1. Grafana Cloud 준비

1. <https://grafana.com> 가입 → 무료 스택 생성
2. **Connections → Add new connection → Prometheus** (또는 스택의 *Details*)에서
   remote_write 정보 확인:
   - URL → `GRAFANA_CLOUD_PROM_URL` (예: `https://prometheus-prod-XX-prod-REGION.grafana.net/api/prom/push`)
   - Username / Instance ID → `GRAFANA_CLOUD_PROM_USER`
3. **Access Policies** 에서 `metrics:write` 권한의 토큰 발급 → `GRAFANA_CLOUD_API_KEY` (`glc_...`)

## 2. 호스트 설정

`compose/.env` 에 아래 값을 채운다(`.env.example` 참고):

```
GRAFANA_CLOUD_PROM_URL=...
GRAFANA_CLOUD_PROM_USER=...
GRAFANA_CLOUD_API_KEY=glc_...
MYSQL_EXPORTER_USER=exporter
MYSQL_EXPORTER_PASSWORD=<강력한 비번>
```

mysqld_exporter 전용 DB 계정 생성(최초 1회):

```sql
CREATE USER 'exporter'@'%' IDENTIFIED BY '<위와 동일한 비번>' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
```

```bash
docker compose --env-file .env exec db \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "$(cat <<'SQL'
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'change-me-exporter-password' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
SQL
)"
```

## 3. 기동

```bash
cd infra/compose
docker compose --env-file .env \
  -f compose.yaml -f compose.bluegreen.yaml -f compose.monitoring.yaml up -d
```

확인:

```bash
docker compose ... logs -f alloy            # remote_write 200 응답 확인
```

수 분 뒤 Grafana Cloud > Explore 에서 `up`, `jvm_memory_used_bytes`,
`node_filesystem_avail_bytes`, `redis_up`, `mysql_up` 쿼리로 도착 확인.

## 4. 대시보드 / 알림

- **대시보드 import** (Dashboards → Import, ID 입력):
  - `1860` Node Exporter Full
  - `4701` JVM (Micrometer)
  - `763` Redis
  - `7362` MySQL Overview
- **알림**(Alerting → Contact points): Discord/Slack 웹훅 추가 →
  Alert rule 로 `up == 0`(서비스 다운), `node_filesystem_avail` 부족,
  JVM heap 포화, HTTP 5xx 비율 등 규칙 작성.

> 외부 다운 감지는 UptimeRobot(`https://<도메인>/actuator/health`)이 계속 담당한다.
> Alloy 가 호스트와 함께 죽으면 자체 알림이 안 오므로 외부 체크는 유지한다.

## 참고: dev(단일 backend) 환경

`compose.dev.yaml` 처럼 backend 컨테이너가 1개면 `config.alloy` 의 backend targets 를
`backend:8080` 하나로 바꾼다.
