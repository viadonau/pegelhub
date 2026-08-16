#!/bin/sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
ENTRYPOINT="$REPO_DIR/docker/java-entrypoint.sh"
TEST_DIR=$(mktemp -d)
trap 'rm -rf "$TEST_DIR"' EXIT HUP INT TERM

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_fails() {
  description=$1
  shift
  if "$@" >/dev/null 2>&1; then
    fail "$description"
  fi
}

create_ca() {
  name=$1
  keytool -genkeypair -noprompt \
    -alias "$name" \
    -dname "CN=$name, O=PegelHub Test" \
    -keyalg RSA \
    -validity 365 \
    -keystore "$TEST_DIR/$name.p12" \
    -storetype PKCS12 \
    -storepass changeit >/dev/null 2>&1
  keytool -exportcert -rfc \
    -alias "$name" \
    -keystore "$TEST_DIR/$name.p12" \
    -storepass changeit \
    -file "$TEST_DIR/certs/$name.crt" >/dev/null 2>&1
}

# Create two independent roots used to exercise additive custom trust.
mkdir -p "$TEST_DIR/certs" "$TEST_DIR/empty"
create_ca first-root
create_ca second-root

# System mode must leave existing JVM options unchanged.
system_output=$(JAVA_TOOL_OPTIONS='-Dpegelhub.existing=true' \
  PEGELHUB_TRUST_MODE=system "$ENTRYPOINT" sh -c 'printf %s "$JAVA_TOOL_OPTIONS"')
[ "$system_output" = '-Dpegelhub.existing=true' ] || fail "system mode changed JAVA_TOOL_OPTIONS"

# Custom mode must preserve public roots, existing options, and both added roots.
custom_output=$(JAVA_TOOL_OPTIONS='-Dpegelhub.existing=true' \
  PEGELHUB_TRUST_MODE=custom PEGELHUB_EXTRA_CA_DIR="$TEST_DIR/certs" \
  PEGELHUB_TRUSTSTORE_DIR="$TEST_DIR/runtime-truststore" \
  "$ENTRYPOINT" sh -c '
    case "$JAVA_TOOL_OPTIONS" in
      -Dpegelhub.existing=true\ *) ;;
      *) exit 10 ;;
    esac
    truststore=$(printf "%s\n" "$JAVA_TOOL_OPTIONS" | sed -n "s/.*-Djavax.net.ssl.trustStore=\([^ ]*\).*/\1/p")
    [ "$(keytool -list -keystore "$truststore" -storepass changeit 2>/dev/null | grep -c "^pegelhub-")" -eq 2 ]
    original_count=$(keytool -list -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit 2>/dev/null | sed -n "s/^Your keystore contains \([0-9][0-9]*\) entries/\1/p")
    generated_count=$(keytool -list -keystore "$truststore" -storepass changeit 2>/dev/null | sed -n "s/^Your keystore contains \([0-9][0-9]*\) entries/\1/p")
    [ "$generated_count" -eq "$((original_count + 2))" ]
  ' 2>"$TEST_DIR/custom.log") || fail "custom roots were not added while public roots were retained"

# Logs identify imported certificates without revealing their mounted paths.
[ -z "$custom_output" ] || fail "custom test emitted unexpected stdout"
grep -q '^Adding custom CA: subject=' "$TEST_DIR/custom.log" || fail "certificate subject was not logged"
grep -q ' sha256=' "$TEST_DIR/custom.log" || fail "certificate fingerprint was not logged"
if grep -q "$TEST_DIR/certs" "$TEST_DIR/custom.log"; then
  fail "certificate paths leaked into logs"
fi

# Rebuilding after CA rotation must replace old roots without accumulating files.
mkdir "$TEST_DIR/rotated-certs"
mv "$TEST_DIR/certs/first-root.crt" "$TEST_DIR/certs/second-root.crt" "$TEST_DIR/rotated-certs/"
create_ca rotated-root
mv "$TEST_DIR/certs/rotated-root.crt" "$TEST_DIR/rotated-certs/current-root.crt"
rm "$TEST_DIR/rotated-certs/first-root.crt" "$TEST_DIR/rotated-certs/second-root.crt"
PEGELHUB_TRUST_MODE=custom PEGELHUB_EXTRA_CA_DIR="$TEST_DIR/rotated-certs" \
  PEGELHUB_TRUSTSTORE_DIR="$TEST_DIR/runtime-truststore" \
  "$ENTRYPOINT" sh -c '
    truststore=$(printf "%s\n" "$JAVA_TOOL_OPTIONS" | sed -n "s/.*-Djavax.net.ssl.trustStore=\([^ ]*\).*/\1/p")
    [ "$(keytool -list -keystore "$truststore" -storepass changeit 2>/dev/null | grep -c "^pegelhub-")" -eq 1 ]
  ' 2>"$TEST_DIR/rotated.log" || fail "rotated roots were not reflected in a fresh truststore"
[ "$(find "$TEST_DIR/runtime-truststore" -type f | wc -l | tr -d ' ')" -eq 1 ] \
  || fail "runtime truststore rebuilds accumulated stale copies"
mv "$TEST_DIR/rotated-certs/current-root.crt" "$TEST_DIR/certs/rotated-root.crt"

# Invalid trust configuration must fail before the application command starts.
printf 'not a certificate\n' >"$TEST_DIR/certs/broken.crt"
assert_fails "malformed certificates must fail" env \
  PEGELHUB_TRUST_MODE=custom PEGELHUB_EXTRA_CA_DIR="$TEST_DIR/certs" "$ENTRYPOINT" true
rm "$TEST_DIR/certs/broken.crt"

assert_fails "an empty certificate directory must fail" env \
  PEGELHUB_TRUST_MODE=custom PEGELHUB_EXTRA_CA_DIR="$TEST_DIR/empty" "$ENTRYPOINT" true
assert_fails "a missing certificate directory must fail" env \
  PEGELHUB_TRUST_MODE=custom PEGELHUB_EXTRA_CA_DIR="$TEST_DIR/missing" "$ENTRYPOINT" true
assert_fails "unknown trust modes must fail" env PEGELHUB_TRUST_MODE=surprise "$ENTRYPOINT" true

printf 'Java trust entrypoint tests passed.\n'
