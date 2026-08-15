<p align="center">
  <img src="public/brand/pegelhub-logo.png" alt="PegelHub" width="220">
</p>

# PegelHub Frontend

[![Frontend Delivery](https://github.com/viadonau/pegelhub/actions/workflows/frontend-delivery.yml/badge.svg)](https://github.com/viadonau/pegelhub/actions/workflows/frontend-delivery.yml)

The frontend is PegelHub's authenticated monitoring interface for viadonau operators. This Angular
21 application presents station, measuring-point, time-series, and measurement data supplied by
[PegelHub Core](../core/README.md); it does not own or persist domain data.

The frontend is developed in the PegelHub monorepo but retains its own npm toolchain, container
image, runtime configuration, health check, and deployment path. Start with the
[repository README](../README.md) for the system architecture and full-stack workflow.

The implemented product scope is intentionally limited to monitoring: a filterable time-series
overview and a single-series detail view with metadata, the most recent reading returned from a
trailing-365-day query, and bucketed chart history. Metadata administration and other operator
configuration workflows are not implemented. The user interface is German; source code and
technical documentation are English.

## Quick start

### Prerequisites

- Node.js 24, matching CI and the Docker build stage
- npm 11.12.1, declared by the repository's `packageManager` field
- Bash and standard POSIX command-line tools for repository scripts
- Docker for the [Core local stack](../README.md#start-locally) and image validation
- `curl` for Core startup and image validation

The shell examples assume macOS, Linux, or a comparable POSIX environment such as WSL. The
checked-in `package-lock.json` is the dependency source of truth; use `npm ci` for a clean install
that fails rather than changing it. Commands in this quick start run from the repository root.

### Run the stack

The browser must be able to resolve the same Keycloak hostname that Core uses as the token issuer.
With administrative privileges, add this entry to `/etc/hosts` on macOS/Linux or to the equivalent
hosts file for your operating system:

```text
127.0.0.1 pegelhub-keycloak.test
```

From the repository root, start Core and its local dependencies:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
scripts/local-stack.sh health
```

The imported disposable realm includes a browser user authorized for this monitoring UI. Obtain its
local-only sign-in details from the repository's
[local Keycloak guide](../core/docs/keycloak-local-dev.md#local-realm-contents).
Custom browser users need the Core `metadata:read` and `measurement:read` client roles. Keycloak
owns identities; the local realm definition and operational guidance live under `core/`. Do not put
credentials in frontend configuration or duplicate them here.

Then, from the repository root, start the frontend:

```bash
npm --prefix frontend ci
npm --prefix frontend start
```

Open [http://localhost:4200/overview](http://localhost:4200/overview). Keycloak redirects the
browser to sign in before Angular starts the monitoring UI. A successful setup reaches the
**Messreihen** page after sign-in and loads its data through the frontend's `/api/v1` path.

The development server binds to `localhost:4200`. It serves the committed runtime config and
proxies `/api` to `http://localhost:8080`. To use another Core address:

```bash
PEGELHUB_API_PROXY_TARGET=http://localhost:8090 npm --prefix frontend start
```

`npm --prefix frontend run start:4201` is available when port 4200 is occupied, but it requires a
matching Keycloak browser origin. Core's default local realm is configured for
`http://localhost:4200`; see [Authentication](#authentication) before changing the port.

## Product surface

| Route                     | Behavior                                                                                       |
| ------------------------- | ---------------------------------------------------------------------------------------------- |
| `/`                       | Redirects to `/overview`.                                                                      |
| `/overview`               | Lists time series with context, filters, and each series' latest trailing-365-day reading.     |
| `/overview/:timeSeriesId` | Shows one series, metadata, its latest trailing-365-day reading, and selectable chart history. |
| `/forbidden`              | Displays the authorization error reached after a Core API `403` response.                      |

Water-level detail charts can optionally show RNW and HSW reference levels when the API supplies
them. The application also provides persistent light/dark theme selection and responsive grid
columns.

## Authentication

Angular loads the runtime configuration before bootstrap and initializes the public Keycloak SPA
client with `login-required` and PKCE S256. No Keycloak client secret is used by this frontend.
Bearer tokens are attached only to requests whose URL is under the configured `apiBaseUrl`.

Core remains responsible for authorization. An API `401` starts a new login while preserving the
current route; an API `403` navigates to `/forbidden`. The Keycloak integration attempts token
refresh for active sessions and invokes login after 30 minutes without tracked user activity.

Keycloak compares browser origins exactly. If the frontend hostname, scheme, or port changes,
update the `pegelhub-frontend` client's redirect URIs and web origins. On the disposable local
stack, `PEGELHUB_FRONTEND_URL` supplies that origin during the first realm import; changing it does
not rewrite an already imported realm.

## Configuration

### Browser runtime configuration

The application fetches `/assets/config.json` with `cache: no-store` before Angular bootstrap. All
four values are required and must be non-empty strings:

```json
{
  "apiBaseUrl": "/api/v1",
  "keycloak": {
    "url": "http://pegelhub-keycloak.test:8082",
    "realm": "pegelhub",
    "clientId": "pegelhub-frontend"
  }
}
```

For `npm start`, these values come from [public/assets/config.json](public/assets/config.json), which
Angular serves as a static asset. `PEGELHUB_API_PROXY_TARGET` changes only the development proxy's
server-side target; it does not change `apiBaseUrl` in the browser.

For a production build, `public/` is copied into the Angular bundle. The deployment container then
replaces `assets/config.json` at **container startup**, not image build time:

| Container variable      | Default                              | Browser-facing purpose                      |
| ----------------------- | ------------------------------------ | ------------------------------------------- |
| `PH_API_BASE_URL`       | `/api/v1`                            | Core API base used by API clients and auth. |
| `PH_KEYCLOAK_URL`       | `http://pegelhub-keycloak.test:8082` | Keycloak base URL reachable by the browser. |
| `PH_KEYCLOAK_REALM`     | `pegelhub`                           | Keycloak realm.                             |
| `PH_KEYCLOAK_CLIENT_ID` | `pegelhub-frontend`                  | Public browser client ID.                   |

This split lets one immutable image move between environments without rebuilding Angular. These
settings are public application configuration, not a place for passwords, tokens, or client
secrets. An unavailable file, invalid JSON, or a missing or blank required string prevents bootstrap
and produces a dedicated startup error page. The loader does not otherwise validate non-empty
values. A malformed Keycloak URL can fail during application configuration; other incorrect API or
Keycloak values fail when the corresponding service is used.

### API proxies

Local development and the deployment container use different proxy implementations:

- `proxy.conf.cjs` sends `/api` requests from `npm start` to
  `PEGELHUB_API_PROXY_TARGET` (default `http://localhost:8080`).
- The container entrypoint writes an Nginx rule that forwards `/api/` unchanged to
  `NGINX_API_UPSTREAM` (default `http://core-app:8080`). The upstream is internal to the container
  network and is never sent to the browser.

Keep `PH_API_BASE_URL=/api/v1` for the normal same-origin deployment. Nginx re-resolves its upstream
through Docker DNS every 10 seconds so a running frontend follows Core container recreation.

## Architecture

The application uses standalone Angular components, signals, `httpResource`, zoneless change
detection, and lazy-loaded feature routes.

```text
src/main.ts                              theme initialization, runtime config, bootstrap
src/app/core/                            config, auth, HTTP/API clients, theme, formatting
src/app/features/time-series-overview/   monitoring overview and AG Grid adapter
src/app/features/time-series-detail/     series metadata, readings, chart, preferences
src/app/ui/                              reusable presentation components
public/assets/config.json                local/browser runtime configuration
public/brand/                            bundled product and partner marks
docker/entrypoint.sh                     runtime config and Nginx generation
scripts/                                 image validation and live-stack smoke checks
../.github/workflows/frontend-delivery.yml  verification, image publication, staging deployment
```

Feature `model/` directories contain presentation projections and pure formatting logic, not a
second backend domain model. Shared UI stays domain-neutral; feature adapters own domain-specific
columns and behavior.

## Commands

Run these commands from `frontend/`, or prefix them with `npm --prefix frontend`
when working from the repository root.

| Command                  | Purpose and prerequisites                                                       |
| ------------------------ | ------------------------------------------------------------------------------- |
| `npm start`              | Start the dev server on port 4200 with the Core proxy.                          |
| `npm run start:4201`     | Start on port 4201; Keycloak must allow that origin.                            |
| `npm run watch`          | Rebuild the development bundle when source files change.                        |
| `npm test`               | Run the Vitest unit suite once.                                                 |
| `npm run typecheck`      | Type-check application and test TypeScript projects.                            |
| `npm run format:check`   | Check repository formatting with Prettier.                                      |
| `npm run format`         | Rewrite supported files with Prettier.                                          |
| `npm run check`          | Run formatting, both TypeScript checks, and the unit suite, as CI does.         |
| `npm run build`          | Create the production bundle under `dist/`.                                     |
| `npm run image:validate` | Build and exercise the deployment image; requires running Docker and `curl`.    |
| `npm run smoke:live`     | Check a running local frontend, Keycloak, Core proxy, auth gate, and API reads. |

`npm run smoke:live` defaults to `http://localhost:4200` and the local stack's disposable local
client-credentials service client. That smoke-test client is separate from the public
`pegelhub-frontend` browser client described under [Authentication](#authentication). Its optional
inputs are `FRONTEND_BASE_URL`, `LOCAL_OPERATOR_CLIENT_ID`, `LOCAL_OPERATOR_CLIENT_SECRET`, and
`LIVE_STACK_SMOKE_TIMEOUT_MS`. Provisioning is documented in the
[local Keycloak guide](../core/docs/keycloak-local-dev.md#local-realm-contents). Supply sensitive
overrides only through the environment; do not commit them.

Before opening a pull request, run at least:

```bash
npm run check
npm run build
```

Run `npm run image:validate` as well for changes to the Dockerfile, entrypoint, runtime config, or
Nginx behavior. Run `npm run smoke:live` when a Core stack and frontend server are already
available.

## Deployment container

Deployment operators should use the repository's
[staging frontend runbook](../deploy/staging/README.md#deploy-and-roll-back-the-frontend).
This section describes the frontend image contract that runbook consumes.

The multi-stage [Dockerfile](Dockerfile) builds with Node 24 and serves the static bundle from
Nginx 1.27 on container port 80. At startup, [docker/entrypoint.sh](docker/entrypoint.sh):

1. writes browser runtime config from the `PH_*` variables;
2. writes the Nginx API proxy from `NGINX_API_UPSTREAM`;
3. enables SPA fallback to `index.html` for routes such as `/overview/:timeSeriesId`.

The image health check requests `/` with a five-second interval, three-second timeout, ten-second
start period, and twelve retries. A healthy container therefore confirms that Nginx answers at the
application root. Frontend deployment smoke checks also exercise the public app and proxied Core
system-time route; they do not perform a Keycloak login or discovery check.

## Delivery and staging

The [Frontend Delivery workflow](../.github/workflows/frontend-delivery.yml) owns the frontend
image delivery path:

- every pull request runs `npm ci`, `npm run check`, `npm run build`, and container-image validation
  through the repository [CI workflow](../.github/workflows/ci.yml);
- every frontend-related push to `main` repeats that verification before delivery;
- after a successful `main` verification, it publishes Linux AMD64 and ARM64 images to
  `ghcr.io/viadonau/pegelhub-frontend` with the tag `sha-<full-commit-sha>`;
- it resolves the pushed image to an immutable digest and invokes the shared staging deployment
  action with that digest.

The repository owns the `staging` GitHub Environment, SSH and Compose topology, deployment lock,
frontend and Core-proxy smoke checks, release state, and the attempted restoration of the previous
digest after a failed activation. See the
[staging frontend runbook](../deploy/staging/README.md#deploy-and-roll-back-the-frontend) for the
deployment procedure.

## Troubleshooting

### Keycloak does not open or tokens have the wrong issuer

Confirm that `pegelhub-keycloak.test` resolves to `127.0.0.1` on the host and that Keycloak is
available on port 8082. Do not substitute `localhost` for this hostname: Core validates the token's
issuer exactly.

### Keycloak reports an invalid redirect URI or blocked origin

Compare the running URL with the exact client origin described under
[Authentication](#authentication). For local development, return to port 4200 or deliberately
update the disposable realm using the linked Keycloak guide.

### The application navigates to `/forbidden`

Confirm that the browser user has both `metadata:read` and `measurement:read` on the
`pegelhub-core-api` client. After changing role assignments, sign out and back in so the browser
receives fresh token claims.
Use the [local Keycloak guide](../core/docs/keycloak-local-dev.md) for the role and client model.

### API requests fail locally

Confirm Core is healthy on port 8080 and check any `PEGELHUB_API_PROXY_TARGET` override. The browser
should still request `/api/v1/...` from the frontend origin. With the full stack running,
`npm run smoke:live` identifies whether the failure is the frontend route, runtime config, Keycloak,
proxy, authentication gate, metadata relationships, or measurement reads.

### The application shows a startup configuration error

Request `/assets/config.json` from the same frontend origin and verify it is valid JSON containing
all four required strings. For a container, inspect the `PH_*` values supplied by the deployment;
for the dev server, inspect [public/assets/config.json](public/assets/config.json).

### A container is healthy but data does not load

The image health check covers Nginx only. Verify that `NGINX_API_UPSTREAM` is reachable from the
frontend container network and that the browser-facing Keycloak URL is reachable from the user's
machine.

## Further documentation

- [Operator station metadata note](docs/operator-station-metadata.md): source-data observations for
  possible future metadata work; it does not describe an implemented administration feature.
- [Brand assets](public/brand/README.md): bundled asset purpose, provenance, and maintenance rules.
- [PegelHub Core](../core/README.md): Core runtime and API documentation entry point.
- [Local Keycloak development](../core/docs/keycloak-local-dev.md): issuer, browser client, roles,
  and realm-import behavior.
- [Staging deployment](../deploy/staging/README.md#deploy-and-roll-back-the-frontend):
  frontend activation, smoke checks, and rollback ownership.
