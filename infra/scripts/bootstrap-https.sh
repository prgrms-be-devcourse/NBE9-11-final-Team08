#!/usr/bin/env bash
set -euo pipefail

infra_root="$(cd "$(dirname "$0")/.." && pwd)"
compose_dir="${infra_root}/compose"
env_file="${compose_dir}/.env"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Copy .env.example to .env and fill in production values first." >&2
  exit 1
fi

read_env_value() {
  local key="$1"
  local line

  line="$(grep -m 1 -E "^${key}=" "${env_file}" || true)"
  if [[ -z "${line}" ]]; then
    return 1
  fi

  printf '%s' "${line#*=}"
}

DOMAIN="$(read_env_value DOMAIN)"
CERTBOT_EMAIL="$(read_env_value CERTBOT_EMAIL)"
BACKEND_UPSTREAM="$(read_env_value BACKEND_UPSTREAM || true)"

: "${DOMAIN:?DOMAIN is required in .env}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL is required in .env}"
if [[ -z "${BACKEND_UPSTREAM}" ]]; then
  BACKEND_UPSTREAM="backend-blue:8080"
  if grep -q -E "^BACKEND_UPSTREAM=" "${env_file}"; then
    sed -i.bak "s|^BACKEND_UPSTREAM=.*|BACKEND_UPSTREAM=${BACKEND_UPSTREAM}|" "${env_file}"
  else
    printf '\nBACKEND_UPSTREAM=%s\n' "${BACKEND_UPSTREAM}" >> "${env_file}"
  fi
  rm -f "${env_file}.bak"
fi

compose=(
  docker compose
  --env-file "${env_file}"
  -f "${compose_dir}/compose.yaml"
  -f "${compose_dir}/compose.bluegreen.yaml"
)

echo "Creating a temporary certificate for ${DOMAIN}..."
"${compose[@]}" run --rm --entrypoint sh certbot -c \
  "mkdir -p /etc/letsencrypt/live/${DOMAIN} && \
   openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
   -keyout /etc/letsencrypt/live/${DOMAIN}/privkey.pem \
   -out /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \
   -subj '/CN=localhost'"

"${compose[@]}" up -d nginx

echo "Removing the temporary certificate..."
"${compose[@]}" run --rm --entrypoint sh certbot -c \
  "rm -rf /etc/letsencrypt/live/${DOMAIN} \
          /etc/letsencrypt/archive/${DOMAIN} \
          /etc/letsencrypt/renewal/${DOMAIN}.conf"

echo "Requesting a Let's Encrypt certificate..."
"${compose[@]}" run --rm certbot certonly \
  --webroot \
  --webroot-path /var/www/certbot \
  --email "${CERTBOT_EMAIL}" \
  --agree-tos \
  --no-eff-email \
  --non-interactive \
  -d "${DOMAIN}"

"${compose[@]}" exec nginx nginx -s reload
sudo "${infra_root}/scripts/install-certbot-cron.sh"

echo "HTTPS is ready at https://${DOMAIN}"
