#!/bin/sh
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
REPO_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
ENV_FILE="$DEPLOY_DIR/pegelhub.env.example"
PROJECT_ONE=pegelhub-v2-alpha
PROJECT_TWO=pegelhub-v2-beta
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/pegelhub-cutover-test.XXXXXX")

cleanup() {
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT HUP INT TERM

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

render_platform() {
  project_name="$1"
  output_file="$2"
  COMPOSE_PROJECT_NAME="$project_name" \
  PEGELHUB_HTTP_BIND=127.0.0.1:18080 \
  PEGELHUB_HTTPS_BIND=127.0.0.1:18443 \
  PEGELHUB_HTTPS_URL_SUFFIX=:18443 \
  PEGELHUB_HTTPS_CONTAINER_PORT=18443 \
  PEGELHUB_FRONTEND_IMAGE=ghcr.io/viadonau/pegelhub-frontend@sha256:0000000000000000000000000000000000000000000000000000000000000000 \
    docker compose \
      -p "$project_name" \
      --env-file "$ENV_FILE" \
      -f "$DEPLOY_DIR/compose.yaml" \
      -f "$DEPLOY_DIR/frontend.compose.yaml" \
      config --format json > "$output_file"
}

command -v docker >/dev/null 2>&1 || fail "docker is required."
command -v jq >/dev/null 2>&1 || fail "jq is required."

render_platform "$PROJECT_ONE" "$TEMP_DIR/one.json"
render_platform "$PROJECT_TWO" "$TEMP_DIR/two.json"

jq -e --arg project "$PROJECT_ONE" '
  .services.caddy.container_name == ($project + "-reverse-proxy")
  and .services["meta-db"].container_name == ($project + "-meta-db")
  and .services["data-db"].container_name == ($project + "-data-db")
  and .services["keycloak-db"].container_name == ($project + "-keycloak-db")
  and .services.keycloak.container_name == ($project + "-keycloak")
  and .services["core-app"].container_name == ($project + "-core")
  and .services.frontend.container_name == ($project + "-frontend")
  and .services.keycloak.environment.KC_HOSTNAME == "https://auth.pegelhub.example.com:18443"
  and .services["core-app"].environment.KEYCLOAK_ISSUER_URI == "https://auth.pegelhub.example.com:18443/realms/pegelhub"
  and .services.frontend.environment.PH_KEYCLOAK_URL == "https://auth.pegelhub.example.com:18443"
  and (.services["influx-bucket-setup"] | has("container_name") | not)
  and ([.services.caddy.ports[] | select(.target == 80)] | .[0].host_ip == "127.0.0.1" and .[0].published == "18080")
  and ([.services.caddy.ports[] | select(.target == 18443)] | .[0].host_ip == "127.0.0.1" and .[0].published == "18443")
' "$TEMP_DIR/one.json" >/dev/null \
  || fail "The rehearsal platform model has incorrect names or bindings."

jq -e --arg project "$PROJECT_TWO" '
  [.services[] | select(.container_name != null) | .container_name]
  | all(startswith($project + "-"))
' "$TEMP_DIR/two.json" >/dev/null \
  || fail "A permanent service name is not isolated by Compose project."

COMPOSE_PROJECT_NAME=pegelhub-v2-iec \
PEGELHUB_CONNECTOR_IMAGE=ghcr.io/viadonau/pegelhub-connector:sha-test \
  docker compose \
    -p pegelhub-v2-iec \
    --env-file "$REPO_DIR/deploy/connector/connector.env.example" \
    --project-directory "$REPO_DIR" \
    -f "$REPO_DIR/deploy/connector/compose.yaml" \
    config --format json > "$TEMP_DIR/connector.json"

jq -e '.services.connector.container_name == "pegelhub-v2-iec-connector"' \
  "$TEMP_DIR/connector.json" >/dev/null \
  || fail "The connector name is not deterministic."

jq -n \
  --arg sha 1111111111111111111111111111111111111111 \
  --arg tag sha-1111111 \
  --arg digest sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
  '{
    version: 1,
    sourceGitSha: $sha,
    core: {tag: $tag, digest: $digest},
    connectors: {
      ftp: {tag: $tag, digest: $digest},
      icc: {tag: $tag, digest: $digest},
      iec: {tag: $tag, digest: $digest},
      ma: {tag: $tag, digest: $digest},
      tstp: {tag: $tag, digest: $digest}
    },
    frontend: {digest: $digest},
    migration: {tag: "v2", digest: $digest}
  }' > "$TEMP_DIR/release.json"
"$REPO_DIR/deploy/cutover/validate-release-manifest.sh" \
  "$TEMP_DIR/release.json" >/dev/null

printf '%s\n' "Cutover Compose readiness checks passed."
