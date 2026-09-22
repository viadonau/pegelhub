# Java Container Trust

Core and all five connector images share
[`java-entrypoint.sh`](java-entrypoint.sh). It prepares Java's certificate
truststore and then executes the image's normal Java command, preserving
container signal delivery. It does not configure Caddy's server certificates
or the browser's trust settings.

## Trust Modes

| `PEGELHUB_TRUST_MODE` | Behavior |
| --- | --- |
| `system` (default) | Leaves the image's Java truststore and `JAVA_TOOL_OPTIONS` unchanged. |
| `custom` | Copies the image's Java truststore, adds mounted CA certificates, and directs Java to the generated copy. |

To add private roots without replacing public trust, mount individual PEM or
DER certificates with a `.crt` suffix at `/run/pegelhub/extra-ca`. The following
is a Compose **service fragment**, not a standalone Compose file:

```yaml
environment:
  PEGELHUB_TRUST_MODE: custom
volumes:
  - ./extra-ca:/run/pegelhub/extra-ca:ro
```

Use one certificate per file, avoid duplicate certificates, and do not place
private keys in this directory. In `custom` mode, a missing or empty certificate
directory, an invalid certificate, or an import failure stops startup before
the application runs. Unknown trust modes also fail startup. Other filename
suffixes and nested directories are not imported.

The original truststore remains unchanged. The generated copy defaults to
`${TMPDIR:-/tmp}/pegelhub-truststore/cacerts`, with directory mode `0700` and
file mode `0600`. Existing `JAVA_TOOL_OPTIONS` are preserved, then the generated
truststore path and password options are appended. Certificate subjects and
SHA-256 fingerprints are logged during import; private key material is not
required.

Restart the Java container after replacing or removing CA files: the truststore
is rebuilt at process startup, not reloaded while the application is running.
Use Compose recreation when changing an environment value. Advanced overrides
are `PEGELHUB_EXTRA_CA_DIR` for the mounted certificate directory and
`PEGELHUB_TRUSTSTORE_DIR` for the writable generated-truststore directory.

The supported deployment layouts already mount these directories. See
[single-host TLS and trust](../deploy/single-host/README.md#tls-and-trust) and
the [connector runner](../deploy/connector/README.md#network-and-trust).

## Verification

From the repository root, with Java 21, `keytool` on `PATH`, and `JAVA_HOME`
set to the JDK:

```sh
docker/tests/java-entrypoint-test.sh
```

The test uses temporary certificates and truststores without starting a
container. To also build and run disposable test images for the Corretto and
Temurin runtime families, with Docker available:

```sh
docker/tests/java-entrypoint-image-test.sh
```

The image test removes its containers and temporary certificates, but leaves
its locally built test images in Docker's image cache.
