#!/bin/sh
set -eu
cd "$(dirname "$0")"
compose() { docker compose -p ph-return-watchdog-lab -f compose.yaml "$@"; }
case "${1:-inspect}" in
  start) compose up -d --build ;;
  inspect)
    compose ps
    compose exec -T watchdog java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication status
    curl --fail --silent http://localhost:18030/status
    ;;
  traps) curl --fail --silent --show-error http://localhost:18030/traps ;;
  stop) compose down ;;
  reset)
    [ "${2:-}" = 'DELETE-LAB-DATA' ] || { echo 'Use reset DELETE-LAB-DATA to delete only this lab data'; exit 2; }
    compose down --volumes
    ;;
  fault) curl --fail -X POST "http://localhost:18030/control?${2:?Supply iec=paused or tstp=drop/corrupt/delay/normal}" ;;
  *) echo 'Usage: lab.sh start|inspect|traps|stop|reset DELETE-LAB-DATA|fault query'; exit 2 ;;
esac
