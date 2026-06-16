#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <elastic-ip> <ssh-private-key> [image-tag]" >&2
  exit 1
fi

host="$1"
key_path="$2"
image_tag="${3:-latest}"
remote="ubuntu@${host}"
ssh_options=(
  -i "${key_path}"
  -o BatchMode=yes
  -o ConnectTimeout=10
  -o ServerAliveInterval=15
  -o ServerAliveCountMax=3
  -o StrictHostKeyChecking=accept-new
)

if [[ ! "${image_tag}" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "image-tag may contain only letters, numbers, dots, underscores, and hyphens." >&2
  exit 1
fi

infra_root="$(cd "$(dirname "$0")/.." && pwd)"

echo "Checking SSH connection to ${remote}..."
ssh "${ssh_options[@]}" "${remote}" true

echo "Uploading deployment files to ${remote}:/opt/team08/..."
ssh "${ssh_options[@]}" "${remote}" \
  "mkdir -p /opt/team08/compose /opt/team08/nginx/templates /opt/team08/scripts"
scp "${ssh_options[@]}" \
  "${infra_root}/compose/compose.yaml" \
  "${infra_root}/compose/compose.prod.yaml" \
  "${infra_root}/compose/.env.example" \
  "${remote}:/opt/team08/compose/"
scp "${ssh_options[@]}" \
  "${infra_root}/nginx/templates/"*.template \
  "${remote}:/opt/team08/nginx/templates/"
scp "${ssh_options[@]}" \
  "${infra_root}/scripts/"*.sh \
  "${remote}:/opt/team08/scripts/"

echo "Pulling image and starting the database and backend..."
ssh "${ssh_options[@]}" "${remote}" \
  "cd /opt/team08 && \
   chmod +x scripts/*.sh && \
   if [ ! -f compose/.env ]; then \
     cp compose/.env.example compose/.env; \
     chmod 600 compose/.env; \
     echo 'Created /opt/team08/compose/.env. Fill it in before starting the stack.'; \
     exit 2; \
   fi && \
   chmod 600 compose/.env && \
   sed -i.bak 's/^IMAGE_TAG=.*/IMAGE_TAG=${image_tag}/' compose/.env && \
   rm -f compose/.env.bak && \
   docker compose --env-file compose/.env \
     -f compose/compose.yaml -f compose/compose.prod.yaml pull backend && \
   docker compose --env-file compose/.env \
     -f compose/compose.yaml -f compose/compose.prod.yaml up -d db backend"

echo "Deployment files uploaded. After DNS points to ${host}, run:"
echo "ssh -i ${key_path} ${remote} 'cd /opt/team08 && ./scripts/bootstrap-https.sh'"
