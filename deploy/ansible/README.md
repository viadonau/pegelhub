# PegelHub Ansible Bootstrap

This Ansible setup prepares a staging host so the GitHub `Images` workflow can
SSH in and run `deploy/staging/scripts/deploy.sh`.

It owns host bootstrap only:

- install Docker Engine and the Compose plugin
- create the staging deploy user
- install the deploy user's SSH public key when provided
- clone/update the PegelHub repository
- create ignored staging directories
- create `deploy/staging/.env` from `.env.example` only when missing
- append missing keys from `.env.example` to the host `.env`
- initialize missing server-local staging secrets in `.env`

It intentionally does not store secrets in GitHub or overwrite existing real
host values. The playbook appends missing template keys and only replaces empty
or placeholder secret values in `deploy/staging/.env`. Fill hostnames, image
values, and `deploy/staging/ftp-config/` on the host after the bootstrap run.

## Prerequisites

Install Ansible locally:

```sh
python3 -m pip install --user ansible
```

The target host should be Debian or Ubuntu with SSH access for a user that can
become root through `sudo`.

## Configure Inventory

Copy the example inventory and variables:

```sh
cp deploy/ansible/inventory/staging.example.ini deploy/ansible/inventory/staging.ini
cp deploy/ansible/group_vars/staging.example.yml deploy/ansible/group_vars/staging.yml
```

Edit `staging.ini` with the staging host and SSH user used by Ansible.

Edit `staging.yml`:

- set `pegelhub_staging_repo_version`
- set `pegelhub_staging_deploy_authorized_key` to the public key matching the
  GitHub Environment secret `STAGING_SSH_PRIVATE_KEY`
- keep `pegelhub_staging_repo_dir` aligned with GitHub Environment variable
  `STAGING_REPO_DIR`
- keep `pegelhub_staging_deploy_user` aligned with GitHub Environment variable
  `STAGING_SSH_USER`

## Run

```sh
ansible-playbook -i deploy/ansible/inventory/staging.ini deploy/ansible/staging.yml
```

After the playbook:

1. Log out and back in as the deploy user if Docker group membership was just
   added.
2. Review `/opt/pegelhub/deploy/staging/.env`; database and Keycloak secrets
   are generated automatically when placeholders are present.
3. Fill the staging hostnames, image values, and optional frontend values
   in `/opt/pegelhub/deploy/staging/.env`.
4. Create `/opt/pegelhub/deploy/staging/ftp-config/connector.properties`.
5. Create `/opt/pegelhub/deploy/staging/ftp-config/pegelhub.yaml`.
6. Log in to GHCR on the staging host if images are private.
7. Run `/opt/pegelhub/deploy/staging/scripts/deploy.sh --check sha-<short-sha>`.

## GitHub Environment Values

For the default example variables, the GitHub `staging` environment should use:

```text
STAGING_REPO_DIR=/opt/pegelhub
STAGING_SSH_PORT=22
STAGING_SSH_USER=pegelhub-deploy
```

Set `STAGING_SSH_HOST`, `STAGING_SSH_PRIVATE_KEY`, and
`STAGING_SSH_FINGERPRINT` for your actual host. `STAGING_SSH_FINGERPRINT` must
contain exactly one `SHA256:...` host key fingerprint. See
`deploy/staging/README.md` for the key-type note; the default staging setup uses
the ECDSA fingerprint.
