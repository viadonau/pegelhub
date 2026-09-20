#!/bin/sh
set -eu
config=$(cd "$(dirname "$0")/image-config" && pwd)
name="ph-watchdog-image-test-$$"
trap 'docker rm -f -v "$name" >/dev/null 2>&1 || true' EXIT INT TERM
docker run -d --name "$name" --read-only --tmpfs /tmp:exec,size=32m \
  -v /var/lib/pegelhub-watchdog -v "$config:/app/config:ro" "${1:?Image required}" >/dev/null
sleep 5
[ "$(docker exec "$name" id -u)" = 10001 ]
docker exec "$name" java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication health
status=$(docker exec "$name" java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication status)
printf '%s' "$status" | grep -q 'UNKNOWN'
if printf '%s' "$status" | grep -q 'image-check-secret'; then echo 'Secret leaked'; exit 1; fi
docker exec "$name" sh -c '! test -f /app/tests.jar && ! ls /app/lib/j60870* >/dev/null 2>&1'
docker stop --time 45 "$name" >/dev/null
docker start "$name" >/dev/null
sleep 2
docker exec "$name" java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication health
echo 'Image runs non-root with read-only root, persists SQLite, and stays healthy while Core is unavailable.'
