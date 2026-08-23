# Two-host production cutover

This runbook upgrades the two production instances independently. It does not
copy historical measurements, activate Callisto/viadonau ICC traffic, or merge
the instances into HA.

## Release gate

1. Merge the staging reset recovery and production readiness PRs with green CI.
2. Perform one ordinary staging deployment from the final `main` commit.
3. Copy `release-manifest.example.json` outside Git, fill it from observed
   registry digests, and run `validate-release-manifest.sh`.
4. Run the migration V2 tests and record the `v2` multi-architecture digest.
5. Keep the reviewed manifest with the deployment record.

Never deploy a moving tag without its observed digest in the manifest. If GHCR
is unreachable from a target architecture, create a matching `docker save`
archive on a trusted machine, checksum it, transfer it as an explicit bridge
job, and run `docker load` before the maintenance window.

## Inventory gate

Run migration `inspect-host.sh` on both platform hosts and every discovered
connector host. The returned reports must account for architecture, Compose
projects, TCP listeners, volumes and sizes, mounts, connector filenames, disk
capacity, and required image reachability. Perform one inventory-only round
trip through the real RDP bridge before sending any mutating job.

Raw connector files, credentials, tokens, rendered configuration, and backups
stay on Linux. Only sanitized summaries and reports return through the bridge.

## Host preparation

For each host, create independent paths and names such as:

```text
COMPOSE_PROJECT_NAME=pegelhub-v2-host-a
/etc/pegelhub/host-a-v2/
/var/lib/pegelhub/host-a-v2/state/
```

Do not reuse or remove a legacy volume. Install certificates and CA roots,
initialize only the V2 secrets, bootstrap its fresh Keycloak realm, provision
durable connector clients with exact roles, and seed its catalog using the
temporary cutover identity. Delete that identity and prove its grant fails.

Render final connector YAML on the target. For every connector image run:

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  run --rm -e PEGELHUB_VALIDATE_CONFIG=true connector
```

Render every platform and connector Compose model before starting anything.

## Side-by-side rehearsal

Use the final V2 project with fresh volumes and these temporary values:

```dotenv
PEGELHUB_HTTP_BIND=127.0.0.1:18080
PEGELHUB_HTTPS_BIND=127.0.0.1:18443
PEGELHUB_HTTPS_URL_SUFFIX=:18443
PEGELHUB_HTTPS_CONTAINER_PORT=18443
```

Start the complete platform and frontend, but leave every V2 connector stopped.
Check TLS, public routes with `:18443`, Core and Keycloak health, login, catalog
counts, exact client token claims, deterministic Checkmk names, and restart
behavior. Deliberately stop the V2 project and prove the legacy platform and
connectors are unaffected, then restore the rehearsed V2 platform on its
alternate ports.

## Activation

Choose the host with fewer active connectors first. At its maintenance start:

1. Stop all corresponding legacy connectors and verify they are stopped.
2. Stop the legacy platform without `down -v`, volume removal, or configuration
   deletion.
3. Change V2 bindings to `80` and `443`; clear
   `PEGELHUB_HTTPS_URL_SUFFIX`, and restore
   `PEGELHUB_HTTPS_CONTAINER_PORT=443`.
4. Redeploy the already rehearsed V2 project. Do not bootstrap or reseed again.
5. Verify public routes, login, API, internal health, and catalog counts.
6. Start replacement connectors one at a time. For each, verify token claims,
   protocol connection, and the Core read/write path.

Record acceptance independently per host. At minute 30 decide to continue or
roll back that host. Preserve the final 30 minutes of the 90-minute window for
diagnosis or rollback. A host-specific infrastructure failure does not undo a
healthy first host; a shared release or migration defect blocks activation of
the second host.

## Rollback

The order is fixed:

1. Stop all V2 connectors.
2. Stop the V2 platform without removing volumes.
3. Restore routing if it changed.
4. Start the untouched legacy platform.
5. Start legacy connectors one at a time and verify them.

Never allow a legacy/new pair for the same protocol path to run concurrently.
Retain stopped legacy projects, volumes, configuration, and verified backups
indefinitely until a separate explicit removal approval.
