# Java container trust

Core and connector images share `java-entrypoint.sh`. By default,
`PEGELHUB_TRUST_MODE=system` starts Java with the image's standard CA roots.

To add private roots without replacing public trust, mount individual PEM or
DER certificates with a `.crt` suffix at `/run/pegelhub/extra-ca` and set:

```yaml
environment:
  PEGELHUB_TRUST_MODE: custom
volumes:
  - ./extra-ca:/run/pegelhub/extra-ca:ro
```

At startup the entrypoint copies the image truststore, imports every mounted
certificate, and points Java at the generated copy. An invalid mode, an empty
certificate directory, or any malformed certificate stops the container.
`JAVA_TOOL_OPTIONS` is preserved and extended rather than replaced.
