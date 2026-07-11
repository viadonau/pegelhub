#!/usr/bin/env sh
set -eu

TEMPLATE_FILE=${CADDY_TEMPLATE_FILE:-/etc/caddy/Caddyfile.template}
CADDY_FILE=${CADDY_FILE:-/etc/caddy/Caddyfile}
INGRESS_MODE=${PEGELHUB_INGRESS_MODE:-public}

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

case "$INGRESS_MODE" in
  public)
    PEGELHUB_CADDY_TLS_DIRECTIVE=""
    ;;
  company)
    [ -r /certs/fullchain.pem ] || fail "company ingress mode requires readable /certs/fullchain.pem"
    [ -r /certs/privkey.pem ] || fail "company ingress mode requires readable /certs/privkey.pem"
    PEGELHUB_CADDY_TLS_DIRECTIVE="$(printf '\ttls /certs/fullchain.pem /certs/privkey.pem')"
    ;;
  *)
    fail "PEGELHUB_INGRESS_MODE must be public or company, got: $INGRESS_MODE"
    ;;
esac

export PEGELHUB_CADDY_TLS_DIRECTIVE

[ -r "$TEMPLATE_FILE" ] || fail "Missing Caddy template: $TEMPLATE_FILE"

render_envsubst() {
  if command -v envsubst >/dev/null 2>&1; then
    envsubst
    return
  fi

  awk '
    {
      for (name in ENVIRON) {
        gsub("\\$\\{" name "\\}", ENVIRON[name])
        gsub("\\$" name, ENVIRON[name])
      }
      print
    }
  '
}

render_envsubst < "$TEMPLATE_FILE" > "$CADDY_FILE"
caddy fmt --overwrite "$CADDY_FILE"
caddy validate --config "$CADDY_FILE" --adapter caddyfile

if [ "${CADDY_RENDER_ONLY:-false}" = "true" ]; then
  exit 0
fi

exec caddy run --config "$CADDY_FILE" --adapter caddyfile
