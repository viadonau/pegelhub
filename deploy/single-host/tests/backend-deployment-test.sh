#!/bin/sh
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
REPO_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
DEPLOY_SCRIPT="$DEPLOY_DIR/scripts/deploy.sh"
STAGING_ACTION="$REPO_DIR/.github/actions/staging-deploy/action.yml"
IMAGES_WORKFLOW="$REPO_DIR/.github/workflows/images.yml"
PROJECT_NAME=pegelhub-reset-test
IMAGE_TAG=sha-reset-test

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

test_root=$(mktemp -d "${TMPDIR:-/tmp}/pegelhub-backend-deploy-test.XXXXXX")
test_env="$test_root/pegelhub.env"
fake_bin="$test_root/bin"
fake_log="$test_root/docker.log"
state_dir="$test_root/state"
config_dir="$test_root/config"

cleanup() {
  rm -rf "$test_root"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$fake_bin" "$state_dir" "$config_dir/tls/server" "$config_dir/tls/trust"
: > "$fake_log"

sed \
  -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=$PROJECT_NAME/" \
  -e 's/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=frontend.reset.example.net/' \
  -e 's/^PEGELHUB_API_HOSTNAME=.*/PEGELHUB_API_HOSTNAME=api.reset.example.net/' \
  -e 's/^PEGELHUB_KEYCLOAK_HOSTNAME=.*/PEGELHUB_KEYCLOAK_HOSTNAME=auth.reset.example.net/' \
  "$DEPLOY_DIR/pegelhub.env.example" > "$test_env"

cat > "$fake_bin/docker" <<'FAKE_DOCKER'
#!/bin/sh
set -eu

printf '%s\n' "$*" >> "$FAKE_DOCKER_LOG"

case "$1" in
  compose)
    shift
    while [ "$#" -gt 0 ]; do
      case "$1" in
        -p|--env-file|-f) shift 2 ;;
        *) command_name="$1"; shift; break ;;
      esac
    done
    case "$command_name" in
      config)
        case " $* " in
          *' --images '*) printf 'ghcr.io/viadonau/pegelhub-core:%s\n' "$PEGELHUB_IMAGE_TAG" ;;
          *' --no-interpolate '*) printf 'services:\n  core-app:\n    image: core\n' ;;
        esac
        ;;
      exec) printf '{"status":"UP"}\n' ;;
      pull|rm|up) ;;
      *) exit 1 ;;
    esac
    ;;
  ps) ;;
  volume)
    [ "$2" = rm ]
    ;;
  *) exit 1 ;;
esac
FAKE_DOCKER

cat > "$fake_bin/curl" <<'FAKE_CURL'
#!/bin/sh
printf '%s\n' '{"issuer":"https://auth.reset.example.net/realms/pegelhub"}'
FAKE_CURL

chmod +x "$fake_bin/docker" "$fake_bin/curl"

run_deploy() {
  PATH="$fake_bin:$PATH" \
  FAKE_DOCKER_LOG="$fake_log" \
  PEGELHUB_CONFIG_DIR="$config_dir" \
  PEGELHUB_ENV_FILE="$test_env" \
  PEGELHUB_STATE_DIR="$state_dir" \
    "$DEPLOY_SCRIPT" "$@"
}

if run_deploy --reset-data wrong-project "$IMAGE_TAG" >/dev/null 2>&1; then
  fail "A mismatched reset confirmation was accepted."
fi
[ ! -s "$fake_log" ] || fail "A rejected reset changed Docker state."

cat > "$state_dir/current-release.env" <<'EOF'
PEGELHUB_IMAGE_TAG=sha-old-schema
PREVIOUS_PEGELHUB_IMAGE_TAG=
EOF

run_deploy --reset-data "$PROJECT_NAME" "$IMAGE_TAG" >/dev/null

grep -F "rm --stop --force core-app influx-bucket-setup data-db meta-db" "$fake_log" >/dev/null \
  || fail "The reset did not remove the Core and data containers."
grep -F "volume rm ${PROJECT_NAME}_metastore-data ${PROJECT_NAME}_datastore-data" "$fake_log" >/dev/null \
  || fail "The reset did not remove exactly the metadata and measurement volumes."
if grep -E 'rm .*caddy|rm .*keycloak|rm .*frontend' "$fake_log" >/dev/null; then
  fail "The data reset attempted to remove preserved platform state."
fi
grep -Fx "PEGELHUB_IMAGE_TAG=$IMAGE_TAG" "$state_dir/current-release.env" >/dev/null \
  || fail "The reset deployment did not record the new image."
grep -Fx 'PREVIOUS_PEGELHUB_IMAGE_TAG=' "$state_dir/current-release.env" >/dev/null \
  || fail "The reset retained an incompatible rollback image."

: > "$fake_log"
run_deploy sha-next >/dev/null
if grep -F 'volume rm' "$fake_log" >/dev/null; then
  fail "An ordinary deployment reset data."
fi

sed \
  -e 's/^PEGELHUB_HTTP_BIND=.*/PEGELHUB_HTTP_BIND=127.0.0.1:18080/' \
  -e 's/^PEGELHUB_HTTPS_BIND=.*/PEGELHUB_HTTPS_BIND=127.0.0.1:18443/' \
  -e 's/^PEGELHUB_HTTPS_URL_SUFFIX=.*/PEGELHUB_HTTPS_URL_SUFFIX=:18443/' \
  -e 's/^PEGELHUB_HTTPS_CONTAINER_PORT=.*/PEGELHUB_HTTPS_CONTAINER_PORT=18443/' \
  "$test_env" > "$test_root/rehearsal.env"
test_env="$test_root/rehearsal.env"
run_deploy --check sha-rehearsal >/dev/null

sed 's/^PEGELHUB_HTTPS_CONTAINER_PORT=.*/PEGELHUB_HTTPS_CONTAINER_PORT=443/' \
  "$test_env" > "$test_root/mismatched-rehearsal.env"
test_env="$test_root/mismatched-rehearsal.env"
if run_deploy --check sha-rehearsal >/dev/null 2>&1; then
  fail "A mismatched rehearsal HTTPS listener was accepted."
fi

grep -F 'default: false' "$IMAGES_WORKFLOW" >/dev/null \
  || fail "The destructive workflow input must default to false."
grep -F 'reset_data: ${{ github.event_name == '\''workflow_dispatch'\'' && inputs.reset_staging_data }}' \
  "$IMAGES_WORKFLOW" >/dev/null \
  || fail "The workflow does not restrict reset requests to manual dispatch."
grep -F 'deploy/single-host/scripts/deploy.sh "$@"' "$STAGING_ACTION" >/dev/null \
  || fail "The staging action does not forward reset arguments to the backend deploy script."

printf '%s\n' "Staging backend deployment checks passed."
