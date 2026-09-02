#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${PEGELHUB_REPO_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
CORE_DIR="$REPO_ROOT/core"
CORE_BASE_URL="${CORE_BASE_URL:-http://localhost:8080}"
ACTUATOR_BASE_URL="${ACTUATOR_BASE_URL:-http://localhost:8081}"
COMMAND_NAME="$(basename "$0")"

usage() {
  printf 'Usage: %s <command> [args]\n' "$COMMAND_NAME"
  cat <<'USAGE'

Commands:
  status                 Print a compact local runtime summary.
  compose-up             Build and start the local Docker Compose stack.
  compose-config         Validate Docker Compose config without printing resolved env.
  compose-ps             Show Docker Compose service status.
  compose-down           Stop and remove local compose containers; keep volumes.
  restart [service]      Restart one service, default: core-app.
  logs [service]         Show recent logs for one service or all.
  logs-errors [service]  Show warning/error-looking lines for one service or all.
  health                 Check actuator health.
  wait-health [seconds]  Wait for actuator health, default: 120 seconds.
  smoke [--raw]          Run low-risk actuator and API smoke checks; default is compact.
  api-get <path>         Run a safe GET request against CORE_BASE_URL.

Environment:
  PEGELHUB_REPO_ROOT     Override the repository root detected from this script.
  CORE_BASE_URL          Override core API base URL. Default: http://localhost:8080
  ACTUATOR_BASE_URL      Override actuator base URL. Default: http://localhost:8081
  TAIL                   Override log line count. Defaults: logs 160, logs-errors 300
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
  [[ -f "$CORE_DIR/.env" ]] || fail "Missing $CORE_DIR/.env."
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
  [[ -n "$path" ]] || fail "api-get requires a path, for example /api/v1/measurements/system-time"
  [[ "$path" == /* ]] || path="/$path"
  curl -fsS "$CORE_BASE_URL$path"
  printf '\n'
}

smoke() {
  if [[ "${1:-}" == "--raw" ]]; then
    printf 'Actuator health:\n'
    health

    printf '\nCore system time:\n'
    curl -fsS "$CORE_BASE_URL/api/v1/measurements/system-time"
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
  if curl -fs -o /dev/null "$CORE_BASE_URL/api/v1/measurements/system-time"; then
    printf 'reachable\n'
  else
    printf 'unreachable\n'
  fi

}

status() {
  require_core_dir

  printf 'repo: %s\n' "$REPO_ROOT"

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
    compose-up)
      compose_up "$@"
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
