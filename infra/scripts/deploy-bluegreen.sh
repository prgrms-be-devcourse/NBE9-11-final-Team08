#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <app-public-ip> <edge-public-ip> <ssh-private-key> <image-tag>" >&2
  exit 1
fi

app_host="$1"
edge_host="$2"
key_path="$3"
image_tag="$4"
app_remote="ubuntu@${app_host}"
edge_remote="ubuntu@${edge_host}"
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
ssh "${ssh_options[@]}" "${app_remote}" true
ssh "${ssh_options[@]}" "${edge_remote}" true

echo "Uploading app blue-green files..."
ssh "${ssh_options[@]}" "${app_remote}" \
  "mkdir -p /opt/team08/compose /opt/team08/scripts"
scp "${ssh_options[@]}" \
  "${infra_root}/compose/compose.yaml" \
  "${infra_root}/compose/compose.bluegreen.yaml" \
  "${infra_root}/compose/.env.example" \
  "${app_remote}:/opt/team08/compose/"
scp "${ssh_options[@]}" \
  "${infra_root}/scripts/deploy-bluegreen-app.sh" \
  "${app_remote}:/opt/team08/scripts/"

echo "Uploading edge switch files..."
ssh "${ssh_options[@]}" "${edge_remote}" \
  "mkdir -p /opt/team08/compose /opt/team08/nginx/templates /opt/team08/scripts"
scp "${ssh_options[@]}" \
  "${infra_root}/compose/compose.edge.yaml" \
  "${infra_root}/compose/.env.example" \
  "${edge_remote}:/opt/team08/compose/"
scp "${ssh_options[@]}" \
  "${infra_root}/nginx/templates/"*.template \
  "${edge_remote}:/opt/team08/nginx/templates/"
scp "${ssh_options[@]}" \
  "${infra_root}/scripts/switch-edge-upstream.sh" \
  "${edge_remote}:/opt/team08/scripts/"

ssh "${ssh_options[@]}" "${app_remote}" "chmod +x /opt/team08/scripts/deploy-bluegreen-app.sh"
ssh "${ssh_options[@]}" "${edge_remote}" "chmod +x /opt/team08/scripts/switch-edge-upstream.sh"

echo "Deploying inactive app color..."
deploy_output="$(ssh "${ssh_options[@]}" "${app_remote}" \
  "cd /opt/team08 && ./scripts/deploy-bluegreen-app.sh '${image_tag}'")"
printf '%s\n' "${deploy_output}"

target_color="$(printf '%s\n' "${deploy_output}" | awk -F= '/^TARGET_COLOR=/{print $2}' | tail -1)"
target_port="$(printf '%s\n' "${deploy_output}" | awk -F= '/^TARGET_PORT=/{print $2}' | tail -1)"

if [[ -z "${target_color}" || -z "${target_port}" ]]; then
  echo "Failed to read target color and port from app deployment output." >&2
  exit 1
fi

app_private_ip="$(ssh "${ssh_options[@]}" "${app_remote}" "hostname -I | awk '{print \$1}'")"
backend_upstream="${app_private_ip}:${target_port}"

echo "Switching edge to ${backend_upstream}..."
ssh "${ssh_options[@]}" "${edge_remote}" \
  "cd /opt/team08 && ./scripts/switch-edge-upstream.sh '${backend_upstream}'"

ssh "${ssh_options[@]}" "${app_remote}" \
  "cd /opt/team08 && mv active-backend-color.next active-backend-color"

echo "Blue-green deployment complete: ${target_color} (${backend_upstream})"
