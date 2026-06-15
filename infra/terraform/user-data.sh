#!/usr/bin/env bash
set -euxo pipefail

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-v2 git cron

systemctl enable --now docker
systemctl enable --now cron
usermod -aG docker ubuntu

install -d -o ubuntu -g ubuntu -m 0755 /opt/team08
