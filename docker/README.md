# Java container trust

Core and all connector images use `java-entrypoint.sh`. Its trust mode is selected at each
container start:

| Variable                  | Default                               | Purpose                                                |
| ------------------------- | ------------------------------------- | ------------------------------------------------------ |
| `PEGELHUB_TRUST_MODE`     | `system`                              | Use `system`, or select `custom` to add mounted roots. |
| `PEGELHUB_EXTRA_CA_DIR`   | `/run/pegelhub/extra-ca`              | Directory read in `custom` mode.                       |
| `PEGELHUB_TRUSTSTORE_DIR` | `${TMPDIR:-/tmp}/pegelhub-truststore` | Private directory for the generated JVM truststore.    |

`system` starts Java with the image's existing CA roots and does not change `JAVA_TOOL_OPTIONS`.

To add private roots without replacing public trust, mount individual PEM or
DER certificates with a `.crt` suffix at `/run/pegelhub/extra-ca` and set:

```yaml
environment:
  PEGELHUB_TRUST_MODE: custom
volumes:
  - ./extra-ca:/run/pegelhub/extra-ca:ro
```

In `custom` mode, the entrypoint copies the image truststore, imports every top-level `*.crt`
certificate, and points Java at the completed private copy. It rebuilds that copy on every
container start, so removed or rotated roots do not accumulate. An invalid mode, a missing or empty
certificate directory, or any malformed certificate stops the container before Java starts.
Existing `JAVA_TOOL_OPTIONS` are preserved and extended.

Run the focused checks from the repository root:

```bash
docker/tests/java-entrypoint-test.sh
docker/tests/java-entrypoint-image-test.sh
```

The first command requires a Java 21 `keytool`. The image compatibility check also requires Docker
and builds against the Corretto and Temurin runtime families used by PegelHub.
