#!/bin/sh
set -eu

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

[ "$#" -eq 2 ] || fail "Usage: $0 <custom-root-directory> <output-file>"
trust_dir=$1
output_file=$2
[ -d "$trust_dir" ] || fail "Custom trust directory is missing: $trust_dir"

temporary_file=$(mktemp "${output_file}.XXXXXX")
trap 'rm -f "$temporary_file"' EXIT HUP INT TERM
chmod 600 "$temporary_file"

system_bundle=""
for candidate in /etc/ssl/certs/ca-certificates.crt /etc/ssl/cert.pem; do
  if [ -f "$candidate" ]; then
    system_bundle=$candidate
    break
  fi
done
if [ -n "$system_bundle" ]; then
  cat "$system_bundle" >> "$temporary_file"
  printf '\n' >> "$temporary_file"
fi

certificate_count=0
for certificate in "$trust_dir"/*.crt; do
  [ -f "$certificate" ] || continue
  certificate_count=$((certificate_count + 1))
  if openssl x509 -in "$certificate" -outform PEM >> "$temporary_file" 2>/dev/null; then
    :
  elif openssl x509 -inform DER -in "$certificate" -outform PEM >> "$temporary_file" 2>/dev/null; then
    :
  else
    fail "A custom CA certificate cannot be parsed"
  fi
  printf '\n' >> "$temporary_file"
done
[ "$certificate_count" -gt 0 ] \
  || fail "Custom trust mode requires at least one *.crt certificate."

mv "$temporary_file" "$output_file"
trap - EXIT HUP INT TERM
