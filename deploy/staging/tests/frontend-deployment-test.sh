#!/bin/sh
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
REPO_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
ENV_EXAMPLE="$DEPLOY_DIR/.env.example"
DEPLOY_SCRIPT="$DEPLOY_DIR/scripts/deploy-frontend.sh"
STAGING_ACTION="$REPO_DIR/.github/actions/staging-deploy/action.yml"

IMAGE_ONE=ghcr.io/viadonau/pegelhub-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111
IMAGE_TWO=ghcr.io/viadonau/pegelhub-frontend@sha256:2222222222222222222222222222222222222222222222222222222222222222

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "docker is required."
command -v jq >/dev/null 2>&1 || fail "jq is required."

for script in "$DEPLOY_DIR"/scripts/*.sh "$DEPLOY_DIR"/tests/*.sh; do
  sh -n "$script"
done

test_root=$(mktemp -d "${TMPDIR:-/tmp}/pegelhub-frontend-deploy-test.XXXXXX")
base_compose_json="$test_root/base-compose.json"
frontend_compose_json="$test_root/frontend-compose.json"
test_env="$test_root/staging.env"
fake_bin="$test_root/bin"
fake_state="$test_root/state"
fake_lock="$test_root/operation.lock"
fake_log="$test_root/docker.log"
fake_active_image="$test_root/active-image"
test_output="$test_root/output.log"
ftp_config="$test_root/ftp-config"

cleanup() {
  rm -rf "$test_root"
}
trap cleanup EXIT

docker compose \
  --env-file "$ENV_EXAMPLE" \
  -f "$DEPLOY_DIR/compose.yaml" \
  config --format json > "$base_compose_json"
PEGELHUB_FRONTEND_IMAGE="$IMAGE_ONE" docker compose \
  --env-file "$ENV_EXAMPLE" \
  -f "$DEPLOY_DIR/compose.yaml" \
  -f "$DEPLOY_DIR/frontend.compose.yaml" \
  config --format json > "$frontend_compose_json"

jq -e '
  (.services | has("frontend") | not)
  and .services.caddy != null
  and .services["core-app"] != null
  and .networks["pegelhub-staging"] != null
' "$base_compose_json" >/dev/null \
  || fail "Backend-only staging Compose must remain valid."

jq -e --arg image "$IMAGE_ONE" '
  .name == "pegelhub-staging"
  and .services.frontend.image == $image
  and .services.frontend.environment == {
    "NGINX_API_UPSTREAM": "http://core-app:8080",
    "PH_API_BASE_URL": "/api/v1",
    "PH_KEYCLOAK_CLIENT_ID": "pegelhub-frontend",
    "PH_KEYCLOAK_REALM": "pegelhub",
    "PH_KEYCLOAK_URL": "https://auth-pegelhub-staging.example.com"
  }
  and .services.frontend.logging == {
    "driver": "json-file",
    "options": {"max-file": "5", "max-size": "10m"}
  }
  and .services.frontend.healthcheck != null
  and (.services.frontend | has("ports") | not)
  and (.services.frontend | has("build") | not)
  and (.services.frontend.networks | keys) == ["pegelhub-staging"]
' "$frontend_compose_json" >/dev/null \
  || fail "Frontend staging Compose policy check failed."

grep -F 'COMPOSE_IGNORE_ORPHANS=true' "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Backend deploys must preserve the separately managed frontend."
grep -F 'COMPOSE_REMOVE_ORPHANS=false' "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Backend deploys must disable inherited orphan removal."
grep -F 'keycloak-bootstrap.lock' "$DEPLOY_SCRIPT" >/dev/null \
  || fail "Frontend deploys must share the staging operation lock."
for workflow in \
  "$REPO_DIR/.github/workflows/images.yml" \
  "$REPO_DIR/.github/workflows/frontend-delivery.yml"; do
  grep -F 'uses: ./.github/actions/staging-deploy' "$workflow" >/dev/null \
    || fail "Staging workflows must share the remote deployment action."
done
grep -F 'deploy/staging/scripts/deploy.sh "$DEPLOY_IMAGE"' \
  "$STAGING_ACTION" >/dev/null \
  || fail "The staging action must support backend deployment."
grep -F 'deploy/staging/scripts/deploy-frontend.sh "$DEPLOY_IMAGE"' \
  "$STAGING_ACTION" >/dev/null \
  || fail "The staging action must support frontend deployment."
git -C "$REPO_DIR" check-ignore -q deploy/staging/state/frontend-release.env \
  || fail "Frontend release state must remain ignored."

mkdir -p "$ftp_config/mappings" "$fake_bin" "$fake_state"
: > "$ftp_config/connector.yaml"
: > "$fake_log"

sed \
  -e 's/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=pegelhub-staging/' \
  -e 's/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=frontend.staging.example.net/' \
  -e "s|^FTP_CONFIG_DIR=.*|FTP_CONFIG_DIR=$ftp_config|" \
  "$ENV_EXAMPLE" > "$test_env"

cat > "$fake_bin/docker" <<'FAKE_DOCKER'
#!/bin/sh
set -eu

printf '%s | image=%s\n' "$*" "$PEGELHUB_FRONTEND_IMAGE" >> "$FAKE_DOCKER_LOG"

[ "$1" = "compose" ] || exit 1
shift
while [ "$#" -gt 0 ]; do
  case "$1" in
    -p|--env-file|-f) shift 2 ;;
    *) command_name="$1"; shift; break ;;
  esac
done

case "$command_name" in
  pull) ;;
  up) printf '%s\n' "$PEGELHUB_FRONTEND_IMAGE" > "$FAKE_ACTIVE_IMAGE_FILE" ;;
  rm) rm -f "$FAKE_ACTIVE_IMAGE_FILE" ;;
  *) exit 1 ;;
esac
FAKE_DOCKER

cat > "$fake_bin/curl" <<'FAKE_CURL'
#!/bin/sh
set -eu

active_image=""
if [ -f "$FAKE_ACTIVE_IMAGE_FILE" ]; then
  IFS= read -r active_image < "$FAKE_ACTIVE_IMAGE_FILE"
fi
[ "${FAKE_SMOKE_FAIL_IMAGE:-}" != "$active_image" ]
FAKE_CURL

chmod +x "$fake_bin/docker" "$fake_bin/curl"

run_deploy() {
  env \
    PATH="$fake_bin:$PATH" \
    FAKE_ACTIVE_IMAGE_FILE="$fake_active_image" \
    FAKE_DOCKER_LOG="$fake_log" \
    FAKE_SMOKE_FAIL_IMAGE="${FAKE_SMOKE_FAIL_IMAGE:-}" \
    FRONTEND_SMOKE_RETRIES=1 \
    FRONTEND_SMOKE_RETRY_DELAY_SECONDS=0 \
    PEGELHUB_STAGING_ENV_FILE="$test_env" \
    PEGELHUB_STAGING_LOCK_DIR="$fake_lock" \
    PEGELHUB_STAGING_STATE_DIR="$fake_state" \
    "$DEPLOY_SCRIPT" "$@"
}

if run_deploy ghcr.io/viadonau/pegelhub-frontend:latest > "$test_output" 2>&1; then
  fail "Frontend deployment accepted a mutable image tag."
fi

run_deploy "$IMAGE_ONE" > "$test_output"
grep -Fx "FRONTEND_IMAGE=$IMAGE_ONE" "$fake_state/frontend-release.env" >/dev/null \
  || fail "Successful deployment did not record the current image."
grep -Fx 'PREVIOUS_FRONTEND_IMAGE=' "$fake_state/frontend-release.env" >/dev/null \
  || fail "First deployment must record an empty previous image."
grep -F ' pull frontend |' "$fake_log" >/dev/null \
  || fail "Deployment must pull only the frontend service."
grep -F ' up -d --no-deps --force-recreate --wait --wait-timeout 150 frontend |' \
  "$fake_log" >/dev/null \
  || fail "Deployment must wait for frontend health without starting dependencies."

: > "$fake_log"
FAKE_SMOKE_FAIL_IMAGE="$IMAGE_TWO"
export FAKE_SMOKE_FAIL_IMAGE
if run_deploy "$IMAGE_TWO" > "$test_output" 2>&1; then
  fail "A failed smoke check incorrectly completed deployment."
fi
unset FAKE_SMOKE_FAIL_IMAGE
grep -Fx "$IMAGE_ONE" "$fake_active_image" >/dev/null \
  || fail "A failed activation did not restore the previous image."
grep -Fx "FRONTEND_IMAGE=$IMAGE_ONE" "$fake_state/frontend-release.env" >/dev/null \
  || fail "A failed activation changed release state."

run_deploy "$IMAGE_TWO" > "$test_output"
grep -Fx "FRONTEND_IMAGE=$IMAGE_TWO" "$fake_state/frontend-release.env" >/dev/null \
  || fail "Second deployment did not record the current image."
grep -Fx "PREVIOUS_FRONTEND_IMAGE=$IMAGE_ONE" "$fake_state/frontend-release.env" >/dev/null \
  || fail "Second deployment did not preserve the previous image."

: > "$fake_log"
run_deploy --rollback > "$test_output"
grep -Fx "FRONTEND_IMAGE=$IMAGE_ONE" "$fake_state/frontend-release.env" >/dev/null \
  || fail "Explicit rollback did not activate the previous image."
grep -Fx "PREVIOUS_FRONTEND_IMAGE=$IMAGE_TWO" "$fake_state/frontend-release.env" >/dev/null \
  || fail "Explicit rollback did not preserve the replaced image."
if grep -F ' pull frontend |' "$fake_log" >/dev/null; then
  fail "Explicit rollback must use the locally cached digest."
fi

rm -f "$fake_state/frontend-release.env" "$fake_active_image"
FAKE_SMOKE_FAIL_IMAGE="$IMAGE_TWO"
export FAKE_SMOKE_FAIL_IMAGE
if run_deploy "$IMAGE_TWO" > "$test_output" 2>&1; then
  fail "A failed first activation incorrectly completed deployment."
fi
unset FAKE_SMOKE_FAIL_IMAGE
[ ! -e "$fake_active_image" ] \
  || fail "A failed first activation did not remove its frontend container."

mkdir "$fake_lock"
if run_deploy "$IMAGE_ONE" > "$test_output" 2>&1; then
  fail "Frontend deployment ignored the shared staging operation lock."
fi

printf '%s\n' "Staging frontend deployment checks passed."
