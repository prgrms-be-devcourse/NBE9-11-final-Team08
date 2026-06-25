#!/usr/bin/env bash
set -euo pipefail

infra_root="$(cd "$(dirname "$0")/.." && pwd)"
compose_dir="${infra_root}/compose"
env_file="${compose_dir}/.env"

docker compose --env-file "${env_file}" \
  -f "${compose_dir}/compose.edge.yaml" \
  run --rm certbot renew --webroot --webroot-path /var/www/certbot
docker compose --env-file "${env_file}" \
  -f "${compose_dir}/compose.edge.yaml" \
  exec nginx nginx -s reload
