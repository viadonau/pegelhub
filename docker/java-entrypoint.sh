#!/bin/sh
set -eu

fail() {
  printf 'pegelhub-java-entrypoint: %s\n' "$*" >&2
  exit 1
}

trust_mode=${PEGELHUB_TRUST_MODE:-system}

# Select the runtime trust strategy before launching the Java process.
case "$trust_mode" in
  system)
    ;;
  custom)
    # Validate the mounted CA directory and the JVM truststore it extends.
    ca_directory=${PEGELHUB_EXTRA_CA_DIR:-/run/pegelhub/extra-ca}
    [ -d "$ca_directory" ] || fail "custom trust directory is missing: $ca_directory"

    java_home=${JAVA_HOME:-}
    [ -n "$java_home" ] || fail "JAVA_HOME is not set"
    source_truststore="$java_home/lib/security/cacerts"
    [ -r "$source_truststore" ] || fail "JVM truststore is not readable"

    # Stage a restricted copy so the image's standard public roots are retained.
    truststore_directory=${PEGELHUB_TRUSTSTORE_DIR:-${TMPDIR:-/tmp}/pegelhub-truststore}
    mkdir -p "$truststore_directory" \
      || fail "cannot create the runtime truststore directory"
    chmod 700 "$truststore_directory" \
      || fail "cannot restrict the runtime truststore directory"
    truststore="$truststore_directory/cacerts"
    rm -f "$truststore_directory"/cacerts.*
    truststore_tmp=$(mktemp "$truststore_directory/cacerts.XXXXXX") \
      || fail "cannot stage the runtime truststore"
    trap 'rm -f "$truststore_tmp"' EXIT HUP INT TERM
    cp "$source_truststore" "$truststore_tmp" || fail "cannot copy the JVM truststore"
    chmod 600 "$truststore_tmp" || fail "cannot restrict the generated truststore"

    # Import every mounted custom root under an alias derived from its fingerprint.
    certificate_count=0
    for certificate in "$ca_directory"/*.crt; do
      [ -f "$certificate" ] || continue
      certificate_count=$((certificate_count + 1))

      certificate_details=$(keytool -J-Duser.language=en -J-Duser.country=US \
        -printcert -file "$certificate" 2>/dev/null) \
        || fail "a custom CA certificate cannot be parsed"
      subject=$(printf '%s\n' "$certificate_details" | sed -n 's/^Owner: //p' | head -n 1)
      fingerprint=$(printf '%s\n' "$certificate_details" | sed -n 's/^[[:space:]]*SHA256: //p' | head -n 1)
      [ -n "$subject" ] || fail "cannot read a custom CA certificate subject"
      [ -n "$fingerprint" ] || fail "cannot read a custom CA certificate fingerprint"

      printf 'Adding custom CA: subject=%s sha256=%s\n' "$subject" "$fingerprint" >&2
      alias="pegelhub-$(printf '%s' "$fingerprint" | tr -d ':')"
      keytool -importcert -noprompt -trustcacerts \
        -alias "$alias" \
        -file "$certificate" \
        -keystore "$truststore_tmp" \
        -storepass changeit >/dev/null 2>&1 \
        || fail "a custom CA certificate import failed"
    done
    [ "$certificate_count" -gt 0 ] \
      || fail "custom trust mode requires at least one *.crt certificate"

    # Activate only the completed truststore, leaving no partially imported copy.
    mv "$truststore_tmp" "$truststore" || fail "cannot activate the generated truststore"
    trap - EXIT HUP INT TERM

    # Direct the application JVM to the generated private truststore.
    JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Djavax.net.ssl.trustStore=$truststore -Djavax.net.ssl.trustStorePassword=changeit"
    export JAVA_TOOL_OPTIONS
    ;;
  *)
    fail "unknown PEGELHUB_TRUST_MODE: $trust_mode"
    ;;
esac

# Replace the entrypoint so the Java command receives container signals directly.
[ "$#" -gt 0 ] || fail "no Java command was provided"
exec "$@"
