#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <server-public-ip> <ssh-private-key> <image-tag>" >&2
  exit 1
fi

host="$1"
key_path="$2"
image_tag="$3"
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

echo "Checking SSH connections..."
ssh "${ssh_options[@]}" "${remote}" true

echo "Uploading single-server blue-green files..."
ssh "${ssh_options[@]}" "${remote}" \
  "mkdir -p /opt/team08/compose /opt/team08/nginx/templates /opt/team08/scripts"
scp "${ssh_options[@]}" \
  "${infra_root}/compose/compose.yaml" \
  "${infra_root}/compose/compose.bluegreen.yaml" \
  "${infra_root}/compose/.env.example" \
  "${remote}:/opt/team08/compose/"
scp "${ssh_options[@]}" \
  "${infra_root}/nginx/templates/prod.conf.template" \
  "${remote}:/opt/team08/nginx/templates/"
scp "${ssh_options[@]}" \
  "${infra_root}/scripts/deploy-bluegreen-app.sh" \
  "${infra_root}/scripts/switch-nginx-upstream.sh" \
  "${infra_root}/scripts/bootstrap-https.sh" \
  "${infra_root}/scripts/install-certbot-cron.sh" \
  "${infra_root}/scripts/renew-certificates.sh" \
  "${remote}:/opt/team08/scripts/"

ssh "${ssh_options[@]}" "${remote}" "chmod +x /opt/team08/scripts/*.sh"

echo "Deploying inactive app color..."
deploy_output="$(ssh "${ssh_options[@]}" "${remote}" \
  "cd /opt/team08 && ./scripts/deploy-bluegreen-app.sh '${image_tag}'")"
printf '%s\n' "${deploy_output}"

target_color="$(printf '%s\n' "${deploy_output}" | awk -F= '/^TARGET_COLOR=/{print $2}' | tail -1)"
target_port="$(printf '%s\n' "${deploy_output}" | awk -F= '/^TARGET_PORT=/{print $2}' | tail -1)"
target_upstream="$(printf '%s\n' "${deploy_output}" | awk -F= '/^TARGET_UPSTREAM=/{print $2}' | tail -1)"

if [[ -z "${target_color}" || -z "${target_port}" || -z "${target_upstream}" ]]; then
  echo "Failed to read target color, port, and upstream from app deployment output." >&2
  exit 1
fi

echo "Switching nginx to ${target_upstream}..."
ssh "${ssh_options[@]}" "${remote}" \
  "cd /opt/team08 && ./scripts/switch-nginx-upstream.sh '${target_upstream}'"

ssh "${ssh_options[@]}" "${remote}" \
  "cd /opt/team08 && mv active-backend-color.next active-backend-color"

echo "Blue-green deployment complete: ${target_color} (${target_upstream}, localhost:${target_port})"
