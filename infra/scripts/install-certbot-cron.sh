#!/usr/bin/env bash
set -euo pipefail

install -m 0644 /dev/stdin /etc/cron.d/team08-certbot <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

17 2,14 * * * ubuntu /opt/team08/scripts/renew-certificates.sh >> /var/log/team08-certbot.log 2>&1
EOF

touch /var/log/team08-certbot.log
chown ubuntu:ubuntu /var/log/team08-certbot.log
