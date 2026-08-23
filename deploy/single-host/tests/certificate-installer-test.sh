#!/bin/sh
set -eu

DEPLOY_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
INSTALLER="$DEPLOY_DIR/scripts/install-certificates.sh"
TEST_DIR=$(mktemp -d)
CONFIG_DIR="$TEST_DIR/config"
FAKE_BIN="$TEST_DIR/bin"

cleanup() {
  rm -rf "$TEST_DIR"
}
trap cleanup EXIT HUP INT TERM

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

create_certificate() {
  name=$1
  san=$2
  openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
    -subj "/CN=$san" \
    -addext "subjectAltName=DNS:$san" \
    -keyout "$TEST_DIR/$name.privkey.pem" \
    -out "$TEST_DIR/$name.fullchain.pem" >/dev/null 2>&1
}

mkdir -p "$CONFIG_DIR/tls/server" "$FAKE_BIN"
cat > "$CONFIG_DIR/pegelhub.env" <<'EOF'
COMPOSE_PROJECT_NAME=pegelhub-certificate-test
PEGELHUB_TLS_MODE=provided
PEGELHUB_FRONTEND_HOSTNAME=frontend.example.test
PEGELHUB_API_HOSTNAME=api.example.test
PEGELHUB_KEYCLOAK_HOSTNAME=auth.example.test
EOF

cat > "$FAKE_BIN/docker" <<'EOF'
#!/bin/sh
[ "$1" = compose ]
EOF
chmod +x "$FAKE_BIN/docker"

openssl req -x509 -newkey rsa:2048 -nodes -days 30 \
  -subj '/CN=frontend.example.test' \
  -addext 'subjectAltName=DNS:frontend.example.test,DNS:api.example.test,DNS:auth.example.test' \
  -keyout "$TEST_DIR/shared.privkey.pem" \
  -out "$TEST_DIR/shared.fullchain.pem" >/dev/null 2>&1

PATH="$FAKE_BIN:$PATH" PEGELHUB_CONFIG_DIR="$CONFIG_DIR" \
  "$INSTALLER" "$TEST_DIR/shared.fullchain.pem" "$TEST_DIR/shared.privkey.pem" \
  >/dev/null
[ -f "$CONFIG_DIR/tls/server/current/shared.pem" ] \
  || fail "A valid SAN certificate was not installed."
installed_digest=$(openssl dgst -sha256 "$CONFIG_DIR/tls/server/current/shared.pem")

create_certificate wrong wrong.example.test
if PATH="$FAKE_BIN:$PATH" PEGELHUB_CONFIG_DIR="$CONFIG_DIR" \
  "$INSTALLER" "$TEST_DIR/wrong.fullchain.pem" "$TEST_DIR/wrong.privkey.pem" \
  >/dev/null 2>&1; then
  fail "A certificate for the wrong hostnames was accepted."
fi
[ "$(openssl dgst -sha256 "$CONFIG_DIR/tls/server/current/shared.pem")" = "$installed_digest" ] \
  || fail "A rejected certificate changed the installed certificate."

printf 'Certificate installer checks passed.\n'
