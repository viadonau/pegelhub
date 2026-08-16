#!/bin/sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TEST_DIR=$(mktemp -d)
trap 'rm -rf "$TEST_DIR"' EXIT HUP INT TERM

# Create one custom root shared by the runtime-image compatibility checks.
mkdir -p "$TEST_DIR/certs"
keytool -genkeypair -noprompt \
  -alias image-root \
  -dname 'CN=PegelHub image test root' \
  -keyalg RSA \
  -validity 365 \
  -keystore "$TEST_DIR/root.p12" \
  -storetype PKCS12 \
  -storepass changeit >/dev/null 2>&1
keytool -exportcert -rfc \
  -alias image-root \
  -keystore "$TEST_DIR/root.p12" \
  -storepass changeit \
  -file "$TEST_DIR/certs/root.crt" >/dev/null 2>&1

# Exercise the entrypoint in both Java image families used by PegelHub.
for base_image in amazoncorretto:21-alpine-jdk eclipse-temurin:21-jre-jammy; do
  image_name="pegelhub-java-entrypoint-test:$(printf '%s' "$base_image" | tr ':/' '--')"
  docker build --quiet \
    --build-arg "BASE_IMAGE=$base_image" \
    --file "$REPO_DIR/docker/tests/Dockerfile" \
    --tag "$image_name" \
    "$REPO_DIR" >/dev/null

  # System mode must preserve options without introducing a custom truststore.
  system_options=$(docker run --rm \
    -e JAVA_TOOL_OPTIONS=-Dpegelhub.existing=true \
    "$image_name" sh -c 'printf %s "$JAVA_TOOL_OPTIONS"')
  [ "$system_options" = '-Dpegelhub.existing=true' ]

  # Custom mode must add the mounted root while retaining a known public root.
  docker run --rm \
    -e JAVA_TOOL_OPTIONS=-Dpegelhub.existing=true \
    -e PEGELHUB_TRUST_MODE=custom \
    -v "$TEST_DIR/certs:/run/pegelhub/extra-ca:ro" \
    "$image_name" sh -c '
      truststore=$(printf "%s\n" "$JAVA_TOOL_OPTIONS" | sed -n "s/.*-Djavax.net.ssl.trustStore=\([^ ]*\).*/\1/p")
      keytool -list -keystore "$truststore" -storepass changeit 2>/dev/null | grep -q "^pegelhub-"
      keytool -list -v -keystore "$truststore" -storepass changeit 2>/dev/null | grep -qi "isrg root x1"
    '
done

printf 'Corretto and Temurin trust image tests passed.\n'
