#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <edge-public-ip> <ssh-private-key>" >&2
  exit 1
fi

host="$1"
key_path="$2"
remote="ubuntu@${host}"
ssh_options=(
  -i "${key_path}"
  -o BatchMode=yes
  -o ConnectTimeout=10
  -o ServerAliveInterval=15
  -o ServerAliveCountMax=3
  -o StrictHostKeyChecking=accept-new
)

infra_root="$(cd "$(dirname "$0")/.." && pwd)"

echo "Checking SSH connection to ${remote}..."
ssh "${ssh_options[@]}" "${remote}" true

echo "Uploading edge deployment files to ${remote}:/opt/team08/..."
ssh "${ssh_options[@]}" "${remote}" \
  "mkdir -p /opt/team08/compose /opt/team08/nginx/templates /opt/team08/scripts"
scp "${ssh_options[@]}" \
  "${infra_root}/compose/compose.edge.yaml" \
  "${infra_root}/compose/.env.example" \
  "${remote}:/opt/team08/compose/"
scp "${ssh_options[@]}" \
  "${infra_root}/nginx/templates/"*.template \
  "${remote}:/opt/team08/nginx/templates/"
scp "${ssh_options[@]}" \
  "${infra_root}/scripts/bootstrap-https.sh" \
  "${infra_root}/scripts/install-certbot-cron.sh" \
  "${infra_root}/scripts/renew-certificates.sh" \
  "${infra_root}/scripts/switch-edge-upstream.sh" \
  "${remote}:/opt/team08/scripts/"

ssh "${ssh_options[@]}" "${remote}" \
  "cd /opt/team08 && \
   chmod +x scripts/*.sh && \
   if [ ! -f compose/.env ]; then \
     cp compose/.env.example compose/.env; \
     chmod 600 compose/.env; \
     echo 'Created /opt/team08/compose/.env. Fill DOMAIN, CERTBOT_EMAIL, and BACKEND_UPSTREAM before starting nginx.'; \
     exit 2; \
   fi && \
   chmod 600 compose/.env && \
   docker compose --env-file compose/.env -f compose/compose.edge.yaml pull nginx certbot && \
   docker compose --env-file compose/.env -f compose/compose.edge.yaml up -d nginx"

echo "Edge deployment complete. After DNS points to ${host}, run:"
echo "ssh -i ${key_path} ${remote} 'cd /opt/team08 && ./scripts/bootstrap-https.sh'"
