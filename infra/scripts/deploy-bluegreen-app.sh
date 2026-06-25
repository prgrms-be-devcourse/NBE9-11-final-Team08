#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <image-tag>" >&2
  exit 1
fi

image_tag="$1"
infra_root="$(cd "$(dirname "$0")/.." && pwd)"
compose_dir="${infra_root}/compose"
env_file="${compose_dir}/.env"
state_file="${infra_root}/active-backend-color"

if [[ ! "${image_tag}" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "image-tag may contain only letters, numbers, dots, underscores, and hyphens." >&2
  exit 1
fi

if [[ ! -f "${env_file}" ]]; then
  cp "${compose_dir}/.env.example" "${env_file}"
  chmod 600 "${env_file}"
  echo "Created ${env_file}. Fill it in before running blue-green deployment." >&2
  exit 2
fi

ensure_env_value() {
  local key="$1"
  local value="$2"

  if grep -q -E "^${key}=" "${env_file}"; then
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "${env_file}"
  else
    printf '\n%s=%s\n' "${key}" "${value}" >> "${env_file}"
  fi
  rm -f "${env_file}.bak"
}

current_color="green"
if [[ -f "${state_file}" ]]; then
  current_color="$(cat "${state_file}")"
fi

case "${current_color}" in
  blue) target_color="green"; target_port="${GREEN_BACKEND_PORT:-8082}"; tag_key="GREEN_IMAGE_TAG" ;;
  green) target_color="blue"; target_port="${BLUE_BACKEND_PORT:-8081}"; tag_key="BLUE_IMAGE_TAG" ;;
  *)
    echo "Invalid active color in ${state_file}: ${current_color}" >&2
    exit 1
    ;;
esac

ensure_env_value "${tag_key}" "${image_tag}"

compose=(
  docker compose
  --env-file "${env_file}"
  -f "${compose_dir}/compose.yaml"
  -f "${compose_dir}/compose.bluegreen.yaml"
)

service="backend-${target_color}"

echo "Deploying ${service} with image tag ${image_tag}..."
"${compose[@]}" pull "${service}"
"${compose[@]}" up -d "${service}"

echo "Waiting for ${service} on localhost:${target_port}..."
for attempt in $(seq 1 40); do
  if curl --fail --silent "http://127.0.0.1:${target_port}/actuator/health" >/dev/null; then
    printf '%s' "${target_color}" > "${state_file}.next"
    echo "TARGET_COLOR=${target_color}"
    echo "TARGET_PORT=${target_port}"
    echo "TARGET_SERVICE=${service}"
    exit 0
  fi

  sleep 3
done

echo "${service} did not become healthy." >&2
"${compose[@]}" logs --tail=120 "${service}" >&2 || true
exit 1
