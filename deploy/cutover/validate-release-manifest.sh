#!/bin/sh
set -eu

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

[ "$#" -eq 1 ] || fail "Usage: $0 RELEASE-MANIFEST.json"
manifest="$1"
[ -f "$manifest" ] || fail "Release manifest does not exist: $manifest"
command -v jq >/dev/null 2>&1 || fail "jq is required."

jq -e '
  def digest: type == "string" and test("^sha256:[0-9a-f]{64}$") and (test("^sha256:0{64}$") | not);
  def sha_tag: type == "string" and test("^sha-[0-9a-f]{7,40}$");
  .version == 1
  and (.sourceGitSha | type == "string" and test("^[0-9a-f]{40}$"))
  and (.core.tag | sha_tag)
  and (.core.digest | digest)
  and (.connectors | keys == ["ftp", "icc", "iec", "ma", "tstp"])
  and (all(.connectors[]; (.tag | sha_tag) and (.digest | digest)))
  and (.frontend.digest | digest)
  and .migration.tag == "v2"
  and (.migration.digest | digest)
' "$manifest" >/dev/null \
  || fail "Release manifest is incomplete or contains a mutable/placeholder reference."

printf '%s\n' "Release manifest is complete and immutable."
