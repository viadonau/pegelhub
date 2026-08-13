#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
IMAGE="${1:-pegelhub-frontend:production-validation}"
CONTAINER="pegelhub-frontend-validation-$$"
RUNTIME_CONFIG=$(mktemp "${TMPDIR:-/tmp}/pegelhub-frontend-config.XXXXXX")

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  rm -f "$RUNTIME_CONFIG"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

command -v curl >/dev/null 2>&1 || {
  printf '%s\n' "ERROR: curl is required." >&2
  exit 1
}
command -v docker >/dev/null 2>&1 || {
  printf '%s\n' "ERROR: docker is required." >&2
  exit 1
}

docker build --tag "$IMAGE" "$REPO_DIR"
docker run --detach \
  --name "$CONTAINER" \
  --publish 127.0.0.1::80 \
  --env PH_API_BASE_URL=/api/v1 \
  --env PH_KEYCLOAK_URL=https://auth.staging.test \
  --env PH_KEYCLOAK_REALM=pegelhub \
  --env PH_KEYCLOAK_CLIENT_ID=pegelhub-frontend \
  --env NGINX_API_UPSTREAM=http://core-app:8080 \
  "$IMAGE" >/dev/null

port=$(docker port "$CONTAINER" 80/tcp | awk -F: 'NR == 1 { print $NF }')
[ -n "$port" ] || {
  printf '%s\n' "ERROR: Could not resolve the frontend validation port." >&2
  exit 1
}
base_url="http://127.0.0.1:$port"

attempt=1
while [ "$attempt" -le 30 ]; do
  health=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$CONTAINER")
  if [ "$health" = "healthy" ]; then
    break
  fi
  if [ "$health" = "unhealthy" ] || [ "$attempt" -eq 30 ]; then
    docker logs "$CONTAINER" >&2
    printf '%s\n' "ERROR: Production frontend container health is $health." >&2
    exit 1
  fi
  sleep 1
  attempt=$((attempt + 1))
done

curl -fsS "$base_url/" | grep -F '<app-root' >/dev/null
curl -fsS "$base_url/overview" | grep -F '<app-root' >/dev/null
curl -fsS "$base_url/assets/config.json" > "$RUNTIME_CONFIG"

grep -F '"apiBaseUrl": "/api/v1"' "$RUNTIME_CONFIG" >/dev/null
grep -F '"url": "https://auth.staging.test"' "$RUNTIME_CONFIG" >/dev/null
grep -F '"realm": "pegelhub"' "$RUNTIME_CONFIG" >/dev/null
grep -F '"clientId": "pegelhub-frontend"' "$RUNTIME_CONFIG" >/dev/null
docker exec "$CONTAINER" \
  grep -F 'resolver 127.0.0.11 valid=10s ipv6=off;' \
    /etc/nginx/conf.d/default.conf >/dev/null
docker exec "$CONTAINER" \
  grep -F 'set $api_upstream http://core-app:8080;' \
    /etc/nginx/conf.d/default.conf >/dev/null
docker exec "$CONTAINER" \
  grep -F 'proxy_pass $api_upstream$request_uri;' \
    /etc/nginx/conf.d/default.conf >/dev/null

printf '%s\n' "Production frontend image validation passed for $IMAGE."
