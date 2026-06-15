#!/usr/bin/env bash
set -euo pipefail

infra_root="$(cd "$(dirname "$0")/.." && pwd)"
compose_dir="${infra_root}/compose"
env_file="${compose_dir}/.env"

if [[ ! -f "${env_file}" ]]; then
  echo "Missing ${env_file}. Copy .env.example to .env and fill in production values first." >&2
  exit 1
fi

set -a
source "${env_file}"
set +a

: "${DOMAIN:?DOMAIN is required in .env}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL is required in .env}"

compose=(
  docker compose
  --env-file "${env_file}"
  -f "${compose_dir}/compose.yaml"
  -f "${compose_dir}/compose.prod.yaml"
)

echo "Creating a temporary certificate for ${DOMAIN}..."
"${compose[@]}" run --rm --entrypoint sh certbot -c \
  "mkdir -p /etc/letsencrypt/live/${DOMAIN} && \
   openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
   -keyout /etc/letsencrypt/live/${DOMAIN}/privkey.pem \
   -out /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \
   -subj '/CN=localhost'"

"${compose[@]}" up -d db backend nginx

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
