# Single-host deployment

Reusable Docker Compose deployment for one PegelHub host. Host-specific
settings live outside the Git checkout:

```text
/etc/pegelhub/<deployment>/
  pegelhub.env
  tls/server/
  tls/trust/

/var/lib/pegelhub/<deployment>/state/
```

Set these paths before running an operational script:

```sh
export PEGELHUB_CONFIG_DIR=/etc/pegelhub/<deployment>
export PEGELHUB_STATE_DIR=/var/lib/pegelhub/<deployment>/state
```

Copy [`pegelhub.env.example`](pegelhub.env.example) into the configuration
directory as `pegelhub.env`, choose a unique `COMPOSE_PROJECT_NAME`, and replace
the example hostnames and secrets. Staging keeps
`COMPOSE_PROJECT_NAME=pegelhub-staging` so existing named volumes are reused.

## TLS and trust

| Installation | `PEGELHUB_TLS_MODE` | `PEGELHUB_TRUST_MODE` |
| --- | --- | --- |
| Public/default | `automatic` | `system` |
| Provided certificate | `provided` | `system` |
| Provided certificate and private CA | `provided` | `custom` |

`automatic` uses Caddy's built-in ACME support. `provided` loads PEM bundles
from `tls/server/current`. Install one shared SAN pair or multiple named pairs:

```sh
deploy/single-host/scripts/install-certificates.sh \
  /private/incoming/shared.fullchain.pem \
  /private/incoming/shared.privkey.pem
```

The installer checks expiration, hostname coverage, and matching private keys,
replaces the current bundles, and reloads Caddy when it is already running.

`custom` adds the `*.crt` files in the platform's `tls/trust` directory to the
Core Java truststore. Each independently deployed connector uses its own
`trust/` directory. Managed browsers must trust the company CA through the
company's normal device configuration. Certificates and CA roots are never
committed.

## Operations

The operational metadata catalog uses a clean Flyway V1 baseline. Before its
first deployment, reset the PostgreSQL metadata and InfluxDB measurement volumes
together. This is irreversible and deliberately requires the Compose project
name as confirmation:

```sh
deploy/single-host/scripts/deploy.sh \
  --reset-data pegelhub-staging sha-<short-sha>
```

The command preserves Caddy, Keycloak, and frontend state. GitHub's `Images`
workflow exposes the same operation only through the manual
`reset_staging_data` input; ordinary push deployments never reset data. See the
[Flyway guide](../../core/docs/flyway.md) for why the V2 reset is required.

Initialize missing environment keys and server-generated secrets:

```sh
deploy/single-host/scripts/sync-env-template.sh
deploy/single-host/scripts/init-env-secrets.sh
```

Validate or deploy a backend image:

```sh
deploy/single-host/scripts/deploy.sh --check sha-<short-sha>
deploy/single-host/scripts/deploy.sh sha-<short-sha>
deploy/single-host/scripts/deploy.sh --rollback
```

Deploy the independently released frontend:

```sh
deploy/single-host/scripts/deploy-frontend.sh \
  ghcr.io/viadonau/pegelhub-frontend@sha256:<64-lowercase-hex>
```

For a new or deliberately emptied Keycloak database, stop Keycloak and run:

```sh
deploy/single-host/scripts/bootstrap-keycloak.sh
```

Run route and internal health checks independently with:

```sh
deploy/single-host/scripts/smoke.sh
```

Connector workloads use the shared runner in [`../connector`](../connector/)
and are deployed separately from the platform stack.
