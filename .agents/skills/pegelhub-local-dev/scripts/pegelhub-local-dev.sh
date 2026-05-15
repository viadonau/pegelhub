#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${PEGELHUB_REPO_ROOT:-$(cd "$SCRIPT_DIR/../../../.." && pwd)}"
CORE_DIR="$REPO_ROOT/core"
CORE_BASE_URL="${CORE_BASE_URL:-http://localhost:8080}"
ACTUATOR_BASE_URL="${ACTUATOR_BASE_URL:-http://localhost:8081}"

usage() {
  cat <<'USAGE'
Usage: pegelhub-local-dev.sh <command> [args]

Commands:
  status                 Print a compact local runtime summary.
  doctor                 Inspect prerequisites, local env, compose status, and health.
  init-env               Create the core module .env from .env.example if missing.
  env-check              Compare .env keys with .env.example without printing values.
  test-core              Run core module tests.
  build-core             Build the core module with tests skipped.
  compose-up             Build and start the local Docker Compose stack.
  compose-up-deps        Start only meta-db and data-db for IDE/Maven app runs.
  compose-config         Validate Docker Compose config without printing resolved env.
  compose-ps             Show Docker Compose service status.
  compose-down           Stop and remove local compose containers; keep volumes.
  restart [service]      Restart one service, default: core-app.
  logs [service]         Show recent logs for core-app, meta-db, data-db, or all.
  logs-errors [service]  Show recent warning/error-looking log lines only.
  health                 Check actuator health.
  wait-health [seconds]  Wait for actuator health, default: 120 seconds.
  smoke [--raw]          Run low-risk actuator and API smoke checks; default is compact.
  api-get <path>         Run a safe GET request against CORE_BASE_URL.

Environment:
  PEGELHUB_REPO_ROOT     Override repository root when the skill is copied.
  CORE_BASE_URL          Override core API base URL. Default: http://localhost:8080
  ACTUATOR_BASE_URL      Override actuator base URL. Default: http://localhost:8081
  TAIL                   Log line count for logs command. Default: 160
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_core_dir() {
  [[ -d "$CORE_DIR" ]] || fail "Cannot find core module at $CORE_DIR"
}

require_env_file() {
  [[ -f "$CORE_DIR/.env" ]] || fail "Missing $CORE_DIR/.env. Run 'init-env' to copy local defaults from .env.example."
}

docker_compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
  else
    fail "Neither 'docker compose' nor 'docker-compose' is available."
  fi
}

run_compose() {
  require_core_dir
  require_env_file
  (cd "$CORE_DIR" && docker_compose --env-file .env "$@")
}

wait_for_http() {
  local url="$1"
  local timeout_seconds="${2:-90}"
  local elapsed=0

  printf 'Waiting for %s ...\n' "$url"
  until curl -fsS -o /dev/null "$url"; do
    elapsed=$((elapsed + 2))
    if (( elapsed >= timeout_seconds )); then
      fail "Timed out waiting for $url"
    fi
    sleep 2
  done
}

build_core() {
  require_core_dir
  mvn -f "$CORE_DIR/pom.xml" -DskipTests package
}

test_core() {
  require_core_dir
  mvn -f "$CORE_DIR/pom.xml" -DfailIfNoTests=false test
}

init_env() {
  require_core_dir
  if [[ -f "$CORE_DIR/.env" ]]; then
    printf '%s already exists; leaving it unchanged.\n' "$CORE_DIR/.env"
    return
  fi
  [[ -f "$CORE_DIR/.env.example" ]] || fail "Missing $CORE_DIR/.env.example"
  cp "$CORE_DIR/.env.example" "$CORE_DIR/.env"
  printf 'Created %s from .env.example.\n' "$CORE_DIR/.env"
}

env_check() {
  require_core_dir
  local example="$CORE_DIR/.env.example"
  local env_file="$CORE_DIR/.env"
  local example_keys=""
  local env_keys=""
  local missing=""
  local extra=""
  [[ -f "$example" ]] || fail "Missing $example"
  [[ -f "$env_file" ]] || fail "Missing $env_file. Run 'init-env' to copy local defaults from .env.example."

  example_keys="$(mktemp)"
  env_keys="$(mktemp)"
  missing="$(mktemp)"
  extra="$(mktemp)"
  trap "rm -f -- '$example_keys' '$env_keys' '$missing' '$extra'" RETURN

  sed -n 's/^[[:space:]]*\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' "$example" | sort -u > "$example_keys"
  sed -n 's/^[[:space:]]*\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' "$env_file" | sort -u > "$env_keys"
  comm -23 "$example_keys" "$env_keys" > "$missing"
  comm -13 "$example_keys" "$env_keys" > "$extra"

  printf '.env: present\n'
  printf 'expected keys: %s\n' "$(wc -l < "$example_keys" | tr -d ' ')"
  printf 'local keys: %s\n' "$(wc -l < "$env_keys" | tr -d ' ')"
  if [[ -s "$missing" ]]; then
    printf 'missing keys:\n'
    sed 's/^/  - /' "$missing"
  else
    printf 'missing keys: none\n'
  fi
  if [[ -s "$extra" ]]; then
    printf 'extra keys:\n'
    sed 's/^/  - /' "$extra"
  else
    printf 'extra keys: none\n'
  fi
}

compose_config() {
  run_compose config >/dev/null
  printf 'Docker Compose config OK.\n'
}

compose_up() {
  compose_config
  run_compose up --build -d
  run_compose ps
  wait_for_http "$ACTUATOR_BASE_URL/actuator/health" 120
}

compose_up_deps() {
  compose_config
  run_compose up -d meta-db data-db
  run_compose ps meta-db data-db
}

compose_ps() {
  run_compose ps
}

compose_down() {
  run_compose down
}

restart() {
  local service="${1:-core-app}"
  run_compose restart "$service"
}

logs() {
  local service="${1:-core-app}"
  local tail="${TAIL:-160}"

  if [[ "$service" == "all" ]]; then
    run_compose logs --tail "$tail"
  else
    run_compose logs --tail "$tail" "$service"
  fi
}

logs_errors() {
  local service="${1:-core-app}"
  local tail="${TAIL:-300}"
  local pattern='error|warn|exception|failed|refused|denied|timeout|unhealthy'

  if command -v rg >/dev/null 2>&1; then
    if [[ "$service" == "all" ]]; then
      run_compose logs --tail "$tail" | rg -i "$pattern" || true
    else
      run_compose logs --tail "$tail" "$service" | rg -i "$pattern" || true
    fi
  else
    if [[ "$service" == "all" ]]; then
      run_compose logs --tail "$tail" | grep -Ei "$pattern" || true
    else
      run_compose logs --tail "$tail" "$service" | grep -Ei "$pattern" || true
    fi
  fi
}

health() {
  curl -fsS "$ACTUATOR_BASE_URL/actuator/health"
  printf '\n'
}

api_get() {
  local path="${1:-}"
  [[ -n "$path" ]] || fail "api-get requires a path, for example /api/v1/connector"
  [[ "$path" == /* ]] || path="/$path"
  curl -fsS "$CORE_BASE_URL$path"
  printf '\n'
}

smoke() {
  local connector_payload=""

  if [[ "${1:-}" == "--raw" ]]; then
    printf 'Actuator health:\n'
    health

    printf '\nCore system time:\n'
    curl -fsS "$CORE_BASE_URL/api/v1/measurement/systemTime"
    printf '\n'

    printf '\nConnector list:\n'
    curl -fsS "$CORE_BASE_URL/api/v1/connector"
    printf '\n'
    return
  fi

  printf 'actuator: '
  if curl -fs -o /dev/null "$ACTUATOR_BASE_URL/actuator/health"; then
    printf 'reachable\n'
  else
    printf 'unreachable\n'
  fi

  printf 'systemTime: '
  if curl -fs -o /dev/null "$CORE_BASE_URL/api/v1/measurement/systemTime"; then
    printf 'reachable\n'
  else
    printf 'unreachable\n'
  fi

  connector_payload="$(mktemp)"
  trap "rm -f -- '$connector_payload'" RETURN
  printf 'connector endpoint: '
  if curl -fs -o "$connector_payload" "$CORE_BASE_URL/api/v1/connector"; then
    printf 'reachable (%s bytes)\n' "$(wc -c < "$connector_payload" | tr -d ' ')"
  else
    printf 'unreachable\n'
  fi
}

status() {
  require_core_dir

  printf 'repo: %s\n' "$REPO_ROOT"
  printf 'git changes: '
  git -C "$REPO_ROOT" status --porcelain 2>/dev/null | wc -l | tr -d ' '
  printf '\n'

  if [[ -f "$CORE_DIR/.env" ]]; then
    printf '.env: present\n'
  else
    printf '.env: missing\n'
  fi

  printf 'docker: '
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    printf 'reachable\n'
  elif command -v docker >/dev/null 2>&1; then
    printf 'installed but daemon unavailable\n'
  else
    printf 'missing\n'
  fi

  if [[ -f "$CORE_DIR/.env" ]] && command -v docker >/dev/null 2>&1; then
    printf '\ncompose:\n'
    run_compose ps --format "table {{.Service}}\t{{.State}}\t{{.Status}}" 2>/dev/null || run_compose ps || true
  else
    printf '\ncompose: skipped\n'
  fi

  printf '\nhealth: '
  if curl -fs -o /dev/null "$ACTUATOR_BASE_URL/actuator/health"; then
    printf 'reachable\n'
  else
    printf 'unreachable\n'
  fi
}

doctor() {
  require_core_dir

  printf 'Repository: %s\n' "$REPO_ROOT"
  printf 'Core: %s\n' "$CORE_DIR"

  printf '\nTools:\n'
  if command -v java >/dev/null 2>&1; then
    java -version 2>&1 | head -n 1
  else
    printf 'java: missing\n'
  fi
  if command -v mvn >/dev/null 2>&1; then
    mvn -version 2>&1 | head -n 1
  else
    printf 'mvn: missing\n'
  fi
  if command -v docker >/dev/null 2>&1; then
    docker --version
    if docker compose version >/dev/null 2>&1; then
      docker compose version
    elif command -v docker-compose >/dev/null 2>&1; then
      docker-compose --version
    else
      printf 'docker compose: missing\n'
    fi
  else
    printf 'docker: missing\n'
  fi

  printf '\nLocal config:\n'
  if [[ -f "$CORE_DIR/.env" ]]; then
    printf '.env: present\n'
  else
    printf '.env: missing; run init-env to copy local defaults\n'
  fi

  printf '\nGit status:\n'
  git -C "$REPO_ROOT" status --short || true

  if [[ -f "$CORE_DIR/.env" ]] && command -v docker >/dev/null 2>&1; then
    printf '\nCompose config:\n'
    compose_config || true
    printf '\nCompose status:\n'
    run_compose ps || true
  fi

  printf '\nActuator health:\n'
  curl -fsS "$ACTUATOR_BASE_URL/actuator/health" || printf 'unavailable'
  printf '\n'
}

main() {
  local command="${1:-}"
  [[ -n "$command" ]] || {
    usage
    exit 2
  }
  shift || true

  case "$command" in
    status)
      status "$@"
      ;;
    doctor)
      doctor "$@"
      ;;
    init-env)
      init_env "$@"
      ;;
    env-check)
      env_check "$@"
      ;;
    test-core)
      test_core "$@"
      ;;
    build-core)
      build_core "$@"
      ;;
    compose-up)
      compose_up "$@"
      ;;
    compose-up-deps)
      compose_up_deps "$@"
      ;;
    compose-config)
      compose_config "$@"
      ;;
    compose-ps)
      compose_ps "$@"
      ;;
    compose-down)
      compose_down "$@"
      ;;
    restart)
      restart "$@"
      ;;
    logs)
      logs "$@"
      ;;
    logs-errors)
      logs_errors "$@"
      ;;
    health)
      health "$@"
      ;;
    wait-health)
      wait_for_http "$ACTUATOR_BASE_URL/actuator/health" "${1:-120}"
      ;;
    smoke)
      smoke "$@"
      ;;
    api-get)
      api_get "$@"
      ;;
    -h|--help|help)
      usage
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
}

main "$@"
