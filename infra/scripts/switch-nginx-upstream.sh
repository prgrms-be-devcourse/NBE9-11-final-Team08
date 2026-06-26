#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backend-upstream-host:port>" >&2
  exit 1
fi

backend_upstream="$1"
infra_root="$(cd "$(dirname "$0")/.." && pwd)"
compose_dir="${infra_root}/compose"
env_file="${compose_dir}/.env"

if [[ ! "${backend_upstream}" =~ ^[A-Za-z0-9._:-]+$ ]]; then
  echo "backend upstream may contain only letters, numbers, dots, underscores, colons, and hyphens." >&2
  exit 1
fi

if [[ ! -f "${env_file}" ]]; then
  cp "${compose_dir}/.env.example" "${env_file}"
  chmod 600 "${env_file}"
  echo "Created ${env_file}. Fill DOMAIN and CERTBOT_EMAIL before switching upstream." >&2
  exit 2
fi

if grep -q -E "^BACKEND_UPSTREAM=" "${env_file}"; then
  sed -i.bak "s|^BACKEND_UPSTREAM=.*|BACKEND_UPSTREAM=${backend_upstream}|" "${env_file}"
else
  printf '\nBACKEND_UPSTREAM=%s\n' "${backend_upstream}" >> "${env_file}"
fi
rm -f "${env_file}.bak"

domain="$(grep -m 1 -E "^DOMAIN=" "${env_file}" | cut -d= -f2- || true)"

compose=(
  docker compose
  --env-file "${env_file}"
  -f "${compose_dir}/compose.yaml"
  -f "${compose_dir}/compose.bluegreen.yaml"
)

if [[ -z "${domain}" ]] || ! "${compose[@]}" run --rm --entrypoint sh certbot -c \
  "test -f /etc/letsencrypt/live/${domain}/fullchain.pem && test -f /etc/letsencrypt/live/${domain}/privkey.pem"; then
  echo "Updated BACKEND_UPSTREAM to ${backend_upstream}. TLS certificate is not ready yet; run ./scripts/bootstrap-https.sh before starting nginx."
  exit 0
fi

"${compose[@]}" up -d nginx
"${compose[@]}" exec nginx nginx -s reload

echo "Switched nginx upstream to ${backend_upstream}"
