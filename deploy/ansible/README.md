# Staging host bootstrap

This Ansible playbook prepares a Debian or Ubuntu host for the repository's
supported staging deployment. It installs Docker Engine and Compose, creates a
deploy user, installs an optional SSH public key, checks out PegelHub, and
initializes ignored staging directories and configuration.

It does not deploy application images, import or reset Keycloak, create an FTP
client, or place runtime secrets in GitHub. Those operations belong to the
[staging deployment guide](../staging/).

## Prerequisites

- `ansible-core` 2.15 or newer on the control machine; the playbook uses
  `ansible.builtin.deb822_repository`, introduced in 2.15
- SSH access to a Debian or Ubuntu host
- an SSH user that can become root with `sudo`
- `openssl` on the target host for server-local secret generation

For example:

```bash
python3 -m pip install --user 'ansible-core>=2.15'
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

- `pegelhub_staging_repo_version`: branch, tag, or commit initially checked out
- `pegelhub_staging_deploy_authorized_key`: public half of the staging deploy key
- `pegelhub_staging_repo_dir`: must match GitHub's `STAGING_REPO_DIR`
- `pegelhub_staging_deploy_user`: must match GitHub's `STAGING_SSH_USER`

Never put a private key or runtime credential in these files.

## Run

Run the playbook on the Ansible control machine from the repository root. It
connects to the configured staging host over SSH:

```bash
ansible-playbook \
  -i deploy/ansible/inventory/staging.ini \
  deploy/ansible/staging.yml
```

The playbook is designed to preserve existing host values. It creates
`deploy/staging/.env` from `.env.example` only when missing, appends newly added
template keys without replacing existing values, and initializes empty or
placeholder database and Keycloak secrets without printing them.

## Complete the host setup

Run the following completion steps on the staging host from the repository
checkout, unless a step explicitly refers to GitHub.

1. Log out and back in as the deploy user if Docker group membership is new.
2. Review the host's `deploy/staging/.env`; replace hostname and image-tag
   placeholders while keeping `PEGELHUB_ENVIRONMENT=staging` and
   `PEGELHUB_DEPLOY_MARKER=pegelhub-staging`.
3. Log in to GHCR on the host if the published packages require authentication.
4. Enroll the FTP connector identity and create the ignored
   `deploy/staging/ftp-config/connector.yaml` and `mappings/*.yaml` as described
   in the [staging guide](../staging/#ftp-connector-configuration).
5. Validate the host configuration with the intended image tag. This check does
   not verify that the tag exists in GHCR:

```bash
deploy/staging/scripts/deploy.sh --check sha-<short-sha>
```

For a new or deliberately emptied Keycloak database, use the explicit staging
bootstrap procedure while Keycloak is stopped. Routine Ansible and image
deployment must not import the realm.

## GitHub staging environment

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
