# Staging Host Bootstrap

This Ansible playbook prepares a Debian or Ubuntu host for the repository's
supported staging deployment. It installs Docker Engine and Compose, creates a
deploy user, installs an optional SSH public key, checks out PegelHub, and
initializes platform configuration under `/etc/pegelhub/staging`, the staging
FTP connector instance under `/etc/pegelhub/connectors/staging-ftp`, and
mutable state under `/var/lib/pegelhub/staging`.

It does not deploy application images, import or reset Keycloak, enroll the FTP
connector identity, configure DNS/TLS, or place runtime secrets in GitHub.
Application setup continues in the
[single-host deployment guide](../single-host/README.md) and
[connector runner guide](../connector/README.md).

This playbook targets **staging**, not a general-purpose production inventory.
The platform configuration, data, and FTP instance paths are fixed by assertions
in [`staging.yml`](staging.yml).

## Prerequisites

- `ansible-core` 2.15 or newer on the control machine; the playbook uses
  `ansible.builtin.deb822_repository`, introduced in 2.15
- SSH access to a Debian or Ubuntu host that can reach the configured Git
  repository and Docker's package repository
- an SSH user that can become root with `sudo`
- `openssl` on the target host for server-local secret generation

Docker group membership grants the deployment account control of the Docker
daemon. Treat that account and its SSH key as privileged access. The playbook
adds the account to `docker`; it does not grant the account general `sudo`
access.

For example:

```bash
python3 -m venv /tmp/pegelhub-ansible-venv
. /tmp/pegelhub-ansible-venv/bin/activate
python -m pip install 'ansible-core>=2.15'
ansible --version
```

The playbook installs `python3-debian` on the target before it configures the
Docker deb822 repository.

## Configure

From the repository root, create ignored working copies:

```bash
test -f deploy/ansible/inventory/staging.ini || \
  cp deploy/ansible/inventory/staging.example.ini \
    deploy/ansible/inventory/staging.ini
test -f deploy/ansible/group_vars/staging.yml || \
  cp deploy/ansible/group_vars/staging.example.yml \
    deploy/ansible/group_vars/staging.yml
```

Set the target host and bootstrap SSH user in `staging.ini`. Review all values
in `staging.yml`, especially:

- `pegelhub_staging_repo_version`: branch, tag, or commit checked out on each run
- `pegelhub_staging_deploy_authorized_key`: public half of the staging deploy key
- `pegelhub_staging_repo_dir`: must match GitHub's `STAGING_REPO_DIR`
- `pegelhub_staging_deploy_user`: must match GitHub's `STAGING_SSH_USER`

The examples default to the `pegelhub-deploy` account, `/opt/pegelhub`, and the
repository's `main` branch. The public SSH key is optional; when omitted,
`authorized_keys` must be managed separately. The playbook does not install a
private key for Git access, so the deployment account must already be able to
read the repository if it is private.

Never put a private key or runtime credential in these files. The installed
host environment holds runtime secrets; ignored inventory and variable files
should contain only host configuration and, optionally, the public key.

### Existing Host Migration

For a host migrating from the old checkout-local layout, preserve and move
existing runtime configuration and release state to the new locations **before
running the playbook**. Otherwise, it can create new credentials that do not
match existing database volumes:

- `deploy/staging/.env` to `/etc/pegelhub/staging/pegelhub.env`
- `deploy/staging/state/` to `/var/lib/pegelhub/staging/state/`
- `deploy/staging/ftp-config/` to
  `/etc/pegelhub/connectors/staging-ftp/config/`

These are migration references, not instructions to reset data. Preserve file
ownership and restrictive permissions, retain backups, and keep
`COMPOSE_PROJECT_NAME=pegelhub-staging` for existing platform volumes.

## Run

Validate the playbook locally from the repository root without connecting to
the target:

```bash
ansible-playbook \
  -i deploy/ansible/inventory/staging.ini \
  deploy/ansible/staging.yml --syntax-check
```

Then run it on the Ansible control machine. **This command changes the target
host over SSH**, including packages, user/group membership, the Git checkout,
and configuration directories:

```bash
ansible-playbook \
  -i deploy/ansible/inventory/staging.ini \
  deploy/ansible/staging.yml
```

Add `--ask-become-pass` if the bootstrap user requires a sudo password. A syntax
check is not a connectivity or privilege check, and Ansible check mode is not a
substitute for validating a complete first bootstrap.

The playbook preserves existing environment values. It creates
`/etc/pegelhub/staging/pegelhub.env` from the tracked example only when missing,
appends new template keys without replacing values, and initializes placeholder
secrets without printing them. It also updates the repository to the configured
ref on each run; do not treat this as an application release or run it over a
checkout containing uncommitted operational edits.

## Complete The Host Setup

Run the following completion steps on the staging host from the repository
checkout, unless a step explicitly refers to GitHub.

1. Log out and back in as the deploy user if Docker group membership is new.
2. Review `/etc/pegelhub/staging/pegelhub.env`; replace hostname and image-tag
   placeholders, configure TLS/trust, and keep
   `COMPOSE_PROJECT_NAME=pegelhub-staging`.
3. Log in to GHCR on the host if the published packages require authentication.
4. Complete the [platform's first-install sequence](../single-host/README.md#first-installation):
   validate the image tag, explicitly bootstrap a new Keycloak realm while
   Keycloak is stopped, deploy the backend, then deploy the frontend by digest.
5. Enroll the FTP connector identity and create the host-owned
   `/etc/pegelhub/connectors/staging-ftp/config/connector.yaml` and
   `config/mappings/*.yaml`. Use the published staging API and Keycloak FQDNs
   rather than Compose service names. Set the connector image in its
   `connector.env` before manual deployment.

For example, validate the host configuration with the intended image tag. The
tag shown here is illustrative; this check does not verify that it exists in
GHCR and does not deploy anything:

```bash
PEGELHUB_CONFIG_DIR=/etc/pegelhub/staging \
PEGELHUB_STATE_DIR=/var/lib/pegelhub/staging/state \
  deploy/single-host/scripts/deploy.sh --check sha-42bd19b
```

Validate the separate FTP Compose project with its intended image reference:

```bash
INSTANCE_DIR=/etc/pegelhub/connectors/staging-ftp
PEGELHUB_CONNECTOR_IMAGE=ghcr.io/viadonau/pegelhub-ftp-connector:sha-42bd19b \
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml config --quiet
```

This connector check validates only Compose structure, not its YAML files,
identity credentials, or protocol connectivity. Follow the
[connector runner guide](../connector/README.md) to start and inspect it.

## GitHub Staging Environment

The `Images` and `Frontend Delivery` workflows use a GitHub Environment named
`staging`. Configure these environment variables:

- `STAGING_REPO_DIR`
- `STAGING_SSH_HOST`
- `STAGING_SSH_PORT` (normally `22`)
- `STAGING_SSH_USER`

Configure these environment secrets:

- `STAGING_SSH_PRIVATE_KEY`
- `STAGING_SSH_FINGERPRINT`, containing exactly one trusted `SHA256:...` host
  key fingerprint

The public key matching `STAGING_SSH_PRIVATE_KEY` belongs in the deploy user's
`authorized_keys`; the private key belongs only in the GitHub Environment.
Runtime database, Keycloak, and FTP secrets remain on the staging host.

The [Images workflow](../../.github/workflows/images.yml) publishes Core and
connector images, then deploys the platform and the separate staging FTP
instance. The [Frontend Delivery workflow](../../.github/workflows/frontend-delivery.yml)
verifies, publishes, and deploys the frontend independently. Both use the shared
[staging action](../../.github/actions/staging-deploy/action.yml), which updates
the host checkout and refuses tracked uncommitted changes. Keep that checkout
free of host-specific configuration.

Configure and review GitHub environment access and approvals before enabling
automated deployment. The playbook does not create the GitHub Environment,
install its secrets, or verify its connection to the host.
