#!/bin/sh

# Runtime verification of the fresh-realm lifecycle in an isolated disposable
# Compose project. It imports and inspects the staging realm, proves a repeated
# import and normal Keycloak recreation preserve existing state, and cleans up
# only the uniquely named test project and volumes it created.
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
BOOTSTRAP_COMPOSE_FILE="$DEPLOY_DIR/keycloak-bootstrap.compose.yaml"
HTTP_TEST_COMPOSE_FILE="$TEST_DIR/keycloak-http.compose.yaml"
ENV_TEMPLATE="$DEPLOY_DIR/pegelhub.env.example"
PROJECT_NAME="pegelhub-keycloak-test-$(date +%s)-$$"
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/$PROJECT_NAME.XXXXXX")
ENV_FILE="$TEMP_DIR/.env"
BOOTSTRAP_LOG="$TEMP_DIR/bootstrap.log"
REBOOTSTRAP_LOG="$TEMP_DIR/rebootstrap.log"
SECRET_VALUES_FILE="$TEMP_DIR/.secret-values"
REALM_STATE="$TEMP_DIR/realm.json"
CLIENTS_STATE="$TEMP_DIR/clients.json"
USERS_STATE="$TEMP_DIR/users.json"
FRONTEND_STATE="$TEMP_DIR/frontend.json"
ROLES_STATE="$TEMP_DIR/core-roles.json"
SCOPES_STATE="$TEMP_DIR/client-scopes.json"
ROLES_MAPPERS_STATE="$TEMP_DIR/core-roles-mappers.json"
AUDIENCE_MAPPERS_STATE="$TEMP_DIR/core-audience-mappers.json"
ACTOR_MAPPERS_STATE="$TEMP_DIR/client-actor-mappers.json"
GROUPS_STATE="$TEMP_DIR/groups.json"
DEFAULT_GROUPS_STATE="$TEMP_DIR/default-groups.json"
MONITORING_GROUP_ROLES_STATE="$TEMP_DIR/monitoring-group-roles.json"
REQUIRED_ACTIONS_STATE="$TEMP_DIR/required-actions.json"
MONITORING_USER_ROLES_STATE="$TEMP_DIR/monitoring-user-roles.json"
TEST_FRONTEND_HOSTNAME=frontend.keycloak.test
TEST_API_HOSTNAME=api.keycloak.test
TEST_KEYCLOAK_HOSTNAME=auth.keycloak.test
CLEANUP_ENABLED=false

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

compose() {
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" \
  COMPOSE_PROFILES=keycloak-bootstrap \
  PEGELHUB_FRONTEND_HOSTNAME="$TEST_FRONTEND_HOSTNAME" \
    docker compose \
      -p "$PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      -f "$BOOTSTRAP_COMPOSE_FILE" \
      --profile keycloak-bootstrap \
      "$@"
}

normal_compose() {
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" \
  COMPOSE_PROFILES= \
    docker compose \
      -p "$PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      -f "$HTTP_TEST_COMPOSE_FILE" \
      "$@"
}

cleanup() {
  case "$PROJECT_NAME" in
    pegelhub-keycloak-test-*) ;;
    *) return ;;
  esac
  if [ "$CLEANUP_ENABLED" = "true" ]; then
    compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -rf "$TEMP_DIR"
}

exit_on_signal() {
  exit_status="$1"
  trap - HUP INT TERM
  cleanup
  exit "$exit_status"
}

trap cleanup EXIT
trap 'exit_on_signal 129' HUP
trap 'exit_on_signal 130' INT
trap 'exit_on_signal 143' TERM

assert_local_docker() {
  if [ -n "${DOCKER_CONTEXT:-}" ]; then
    effective_context=$DOCKER_CONTEXT
    docker_endpoint=$(docker context inspect \
      --format '{{(index .Endpoints "docker").Host}}' \
      "$effective_context")
  elif [ -n "${DOCKER_HOST:-}" ]; then
    docker_endpoint=$DOCKER_HOST
  else
    effective_context=$(docker context show)
    docker_endpoint=$(docker context inspect \
      --format '{{(index .Endpoints "docker").Host}}' \
      "$effective_context")
  fi

  case "$docker_endpoint" in
    unix://*|npipe://*|tcp://localhost:*|tcp://127.0.0.1:*) ;;
    *) fail "Disposable Keycloak verification must target a local Docker endpoint." ;;
  esac

  DOCKER_HOST=$docker_endpoint
  export DOCKER_HOST
  unset DOCKER_CONTEXT
}

assert_no_secret_output() {
  for log_file in "$@"; do
    if grep -F -f "$SECRET_VALUES_FILE" "$log_file" >/dev/null 2>&1; then
      fail "A disposable credential was emitted by the staging Keycloak bootstrap."
    fi
  done
}

configure_kcadm() {
  normal_compose exec -T keycloak sh -eu -c '
    export KC_CLI_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD"
    /opt/keycloak/bin/kcadm.sh config credentials \
      --config /tmp/pegelhub-bootstrap-test.config \
      --server http://localhost:8080 \
      --realm master \
      --user "$KC_BOOTSTRAP_ADMIN_USERNAME" >/dev/null 2>&1
    unset KC_CLI_PASSWORD KC_BOOTSTRAP_ADMIN_PASSWORD
  '
}

kcadm() {
  normal_compose exec -T keycloak \
    /opt/keycloak/bin/kcadm.sh "$@" \
    --config /tmp/pegelhub-bootstrap-test.config
}

single_id() {
  resource="$1"
  query="$2"
  result=$(kcadm get "$resource" -r pegelhub -q "$query" \
    --fields id --format csv --noquotes | tr -d '\r')
  [ -n "$result" ] || fail "Missing Keycloak object for $query."
  [ "$(printf '%s\n' "$result" | wc -l | tr -d ' ')" -eq 1 ] \
    || fail "Expected one Keycloak object for $query."
  printf '%s\n' "$result"
}

inspect_state() {
  configure_kcadm
  core_id=$(single_id clients clientId=pegelhub-core-api)
  frontend_id=$(single_id clients clientId=pegelhub-frontend)
  monitoring_group_id=$(single_id groups search=monitoring-users)

  kcadm get realms/pegelhub \
    --fields realm,enabled,displayName,loginTheme,internationalizationEnabled,supportedLocales,defaultLocale,accessTokenLifespan,resetPasswordAllowed,passwordPolicy,bruteForceProtected,permanentLockout,bruteForceStrategy,failureFactor,waitIncrementSeconds,maxFailureWaitSeconds,maxDeltaTimeSeconds,quickLoginCheckMilliSeconds,minimumQuickLoginWaitSeconds \
    > "$REALM_STATE"
  kcadm get clients -r pegelhub \
    --fields id,clientId,publicClient,serviceAccountsEnabled \
    > "$CLIENTS_STATE"
  kcadm get users -r pegelhub \
    --fields id,username,serviceAccountClientId \
    > "$USERS_STATE"
  kcadm get groups -r pegelhub \
    --fields id,name,path \
    > "$GROUPS_STATE"
  kcadm get default-groups -r pegelhub \
    --fields id,name,path \
    > "$DEFAULT_GROUPS_STATE"
  kcadm get "groups/$monitoring_group_id/role-mappings/clients/$core_id" -r pegelhub \
    --fields name,clientRole \
    > "$MONITORING_GROUP_ROLES_STATE"
  kcadm get authentication/required-actions -r pegelhub \
    --fields alias,enabled,defaultAction \
    > "$REQUIRED_ACTIONS_STATE"
  kcadm get "clients/$frontend_id" -r pegelhub \
    --fields 'clientId,publicClient,rootUrl,redirectUris,webOrigins,attributes(*)' \
    > "$FRONTEND_STATE"
  kcadm get "clients/$core_id/roles" -r pegelhub \
    --fields name,composite \
    > "$ROLES_STATE"
  kcadm get client-scopes -r pegelhub --fields id,name > "$SCOPES_STATE"

  roles_scope_id=$(jq -r '.[] | select(.name == "pegelhub-core-roles") | .id' "$SCOPES_STATE")
  audience_scope_id=$(jq -r '.[] | select(.name == "pegelhub-core-audience") | .id' "$SCOPES_STATE")
  actor_scope_id=$(jq -r '.[] | select(.name == "pegelhub-client-actor") | .id' "$SCOPES_STATE")
  [ -n "$roles_scope_id" ] && [ -n "$audience_scope_id" ] && [ -n "$actor_scope_id" ] \
    || fail "Required PegelHub client scopes are missing."

  kcadm get "client-scopes/$roles_scope_id/protocol-mappers/models" -r pegelhub \
    > "$ROLES_MAPPERS_STATE"
  kcadm get "client-scopes/$audience_scope_id/protocol-mappers/models" -r pegelhub \
    > "$AUDIENCE_MAPPERS_STATE"
  kcadm get "client-scopes/$actor_scope_id/protocol-mappers/models" -r pegelhub \
    > "$ACTOR_MAPPERS_STATE"
}

assert_seed_state() {
  jq -e '
    .realm == "pegelhub"
    and .enabled == true
    and .displayName == "PegelHub"
    and .loginTheme == "pegelhub"
    and .internationalizationEnabled == true
    and .supportedLocales == ["de"]
    and .defaultLocale == "de"
    and .accessTokenLifespan == 600
    and .resetPasswordAllowed == false
    and .passwordPolicy == "length(12) and notUsername and notEmail"
    and .bruteForceProtected == true
    and .permanentLockout == false
    and .bruteForceStrategy == "MULTIPLE"
    and .failureFactor == 10
    and .waitIncrementSeconds == 60
    and .maxFailureWaitSeconds == 900
    and .maxDeltaTimeSeconds == 43200
    and .quickLoginCheckMilliSeconds == 1000
    and .minimumQuickLoginWaitSeconds == 60
  ' "$REALM_STATE" >/dev/null \
    || fail "Runtime realm locale or theme state is incorrect."

  jq -e '
    ([.[] | select(.clientId | startswith("pegelhub-")) | .clientId] | sort)
      == ["pegelhub-core-api", "pegelhub-frontend"]
    and ([.[] | select(.clientId | startswith("local-"))] | length) == 0
    and ([.[] | select(.serviceAccountsEnabled == true)] | length) == 0
  ' "$CLIENTS_STATE" >/dev/null \
    || fail "Runtime staging client set is incorrect."

  jq -e 'length == 0' "$USERS_STATE" >/dev/null \
    || fail "The staging seed created a user."

  jq -e '
    map({name, path}) == [{"name":"monitoring-users","path":"/monitoring-users"}]
  ' "$GROUPS_STATE" >/dev/null \
    || fail "Runtime monitoring group state is incorrect."

  jq -e 'length == 0' "$DEFAULT_GROUPS_STATE" >/dev/null \
    || fail "The monitoring group must not be a default group."

  jq -e '
    map({name, clientRole}) | sort_by(.name) == [
      {"name":"measurement:read","clientRole":true},
      {"name":"metadata:read","clientRole":true}
    ]
  ' "$MONITORING_GROUP_ROLES_STATE" >/dev/null \
    || fail "Runtime monitoring group role mappings are incorrect."

  jq -e '
    [.[] | select(.alias == "UPDATE_PASSWORD" and .enabled == true and .defaultAction == false)]
      | length == 1
  ' "$REQUIRED_ACTIONS_STATE" >/dev/null \
    || fail "Runtime Update Password required action is not enabled."

  jq -e '
    .clientId == "pegelhub-frontend"
    and .publicClient == true
    and .rootUrl == "https://frontend.keycloak.test"
    and .redirectUris == ["https://frontend.keycloak.test/*"]
    and .webOrigins == ["https://frontend.keycloak.test"]
    and .attributes["pkce.code.challenge.method"] == "S256"
  ' "$FRONTEND_STATE" >/dev/null \
    || fail "Runtime frontend origin state is incorrect."

  jq -e '
    map({name, composite}) | sort_by(.name) == [
      {"name":"measurement:read","composite":false},
      {"name":"measurement:write","composite":false},
      {"name":"metadata:read","composite":false},
      {"name":"metadata:write","composite":false},
      {"name":"system:admin","composite":false},
      {"name":"telemetry:read","composite":false},
      {"name":"telemetry:write","composite":false}
    ]
  ' "$ROLES_STATE" >/dev/null \
    || fail "Runtime Core API roles are incorrect."

  jq -e '
    [.[] | select(.name == "basic" or .name == "profile" or (.name | startswith("pegelhub-"))) | .name]
    | sort == [
      "basic",
      "pegelhub-client-actor",
      "pegelhub-core-audience",
      "pegelhub-core-roles",
      "pegelhub-user-actor",
      "profile"
    ]
  ' "$SCOPES_STATE" >/dev/null \
    || fail "Runtime PegelHub client scopes are incorrect."

  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-usermodel-client-role-mapper"
    and .[0].config["claim.name"] == "resource_access.${client_id}.roles"
  ' "$ROLES_MAPPERS_STATE" >/dev/null \
    || fail "Runtime Core role mapper is incorrect."
  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-audience-mapper"
    and .[0].config["included.client.audience"] == "pegelhub-core-api"
  ' "$AUDIENCE_MAPPERS_STATE" >/dev/null \
    || fail "Runtime Core audience mapper is incorrect."
  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-hardcoded-claim-mapper"
    and .[0].config["claim.name"] == "pegelhub_actor_type"
    and .[0].config["claim.value"] == "CLIENT"
  ' "$ACTOR_MAPPERS_STATE" >/dev/null \
    || fail "Runtime client actor mapper is incorrect."
}

http_status() {
  awk 'NR == 1 { print $2; exit }' "$1"
}

location_header() {
  awk '
    tolower($1) == "location:" {
      sub(/^[^:]*:[[:space:]]*/, "")
      sub(/\r$/, "")
      print
      exit
    }
  ' "$1"
}

internal_keycloak_url() {
  printf '%s\n' "$1" \
    | sed "s|^https://$TEST_KEYCLOAK_HOSTNAME|$KEYCLOAK_HTTP_ORIGIN|"
}

form_action() {
  action=$(sed -n 's/.*<form[^>]*action="\([^"]*\)".*/\1/p' "$1" \
    | head -n 1 \
    | sed 's/&amp;/\&/g')
  [ -n "$action" ] || fail "The Keycloak form action is missing."
  internal_keycloak_url "$action"
}

assert_http_status() {
  actual_status=$(http_status "$1")
  expected_status="$2"
  message="$3"
  [ "$actual_status" = "$expected_status" ] || fail "$message (HTTP $actual_status)."
}

start_authorization() {
  auth_flow_name="$1"
  auth_redirect_uri="$2"
  auth_cookie_file="$3"
  auth_html_file="$4"
  auth_verifier="$5"
  auth_challenge=$(printf '%s' "$auth_verifier" \
    | openssl dgst -sha256 -binary \
    | openssl base64 -A \
    | tr '+/' '-_' \
    | tr -d '=')
  : > "$auth_cookie_file"

  curl -fsS -c "$auth_cookie_file" -G \
    "$KEYCLOAK_HTTP_ORIGIN/realms/pegelhub/protocol/openid-connect/auth" \
    --data-urlencode client_id=pegelhub-frontend \
    --data-urlencode redirect_uri="$auth_redirect_uri" \
    --data-urlencode response_type=code \
    --data-urlencode scope=openid \
    --data-urlencode "state=$auth_flow_name" \
    --data-urlencode "nonce=$auth_flow_name" \
    --data-urlencode "code_challenge=$auth_challenge" \
    --data-urlencode code_challenge_method=S256 \
    > "$auth_html_file"
}

post_login() {
  login_html="$1"
  cookie_file="$2"
  username="$3"
  password="$4"
  headers_file="$5"
  body_file="$6"
  login_action=$(form_action "$login_html")

  curl -sS -D "$headers_file" -o "$body_file" \
    -b "$cookie_file" -c "$cookie_file" \
    -X POST "$login_action" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$password" \
    --data-urlencode credentialId=
}

exchange_authorization_code() {
  redirect_location="$1"
  verifier="$2"
  redirect_uri="$3"
  output_file="$4"
  authorization_code=$(printf '%s' "$redirect_location" \
    | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
  [ -n "$authorization_code" ] || fail "The authorization redirect omitted its code."

  curl -fsS -X POST \
    "$KEYCLOAK_HTTP_ORIGIN/realms/pegelhub/protocol/openid-connect/token" \
    --data-urlencode grant_type=authorization_code \
    --data-urlencode client_id=pegelhub-frontend \
    --data-urlencode "code=$authorization_code" \
    --data-urlencode "redirect_uri=$redirect_uri" \
    --data-urlencode "code_verifier=$verifier" \
    > "$output_file"
  jq -e '.access_token and .refresh_token' "$output_file" >/dev/null \
    || fail "The authorization code exchange did not return user tokens."
}

submit_password_update() {
  update_source_html="$1"
  update_cookie_file="$2"
  update_new_password="$3"
  update_confirmation="$4"
  update_headers_file="$5"
  update_body_file="$6"
  update_action=$(form_action "$update_source_html")

  curl -sS -D "$update_headers_file" -o "$update_body_file" \
    -b "$update_cookie_file" -c "$update_cookie_file" \
    -X POST "$update_action" \
    --data-urlencode "password-new=$update_new_password" \
    --data-urlencode "password-confirm=$update_confirmation" \
    --data-urlencode logout-sessions=on
}

decode_jwt_payload() {
  jwt_payload_segment=$(printf '%s' "$1" | cut -d. -f2 | tr '_-' '/+')
  case $((${#jwt_payload_segment} % 4)) in
    0) jwt_padding= ;;
    2) jwt_padding='==' ;;
    3) jwt_padding='=' ;;
    *) fail "The access token contains an invalid base64url payload." ;;
  esac
  printf '%s%s' "$jwt_payload_segment" "$jwt_padding" | openssl base64 -d -A
}

assert_global_password_error() {
  grep -F 'id="ph-global-message"' "$1" >/dev/null \
    || fail "A password policy failure did not render the global error alert."
  grep -F 'aria-describedby="password-requirements ph-global-message"' "$1" >/dev/null \
    || fail "A password policy failure was not associated with the password field."
  grep -F 'aria-invalid="true"' "$1" >/dev/null \
    || fail "A password policy failure did not mark the password field invalid."
}

assert_onboarding_flow() {
  configure_kcadm
  core_id=$(single_id clients clientId=pegelhub-core-api)
  monitoring_group_id=$(single_id groups search=monitoring-users)
  monitoring_user_id=$(kcadm create users -r pegelhub \
    -s username=integration-monitoring-user \
    -s enabled=true \
    -s firstName=Integration \
    -s lastName=Monitoring \
    -s email=integration-monitoring@example.invalid \
    -i)

  kcadm update "users/$monitoring_user_id/groups/$monitoring_group_id" \
    -r pegelhub -n >/dev/null
  kcadm set-password -r pegelhub \
    --userid "$monitoring_user_id" \
    --new-password integration-initial-passphrase >/dev/null
  kcadm get "users/$monitoring_user_id/role-mappings/clients/$core_id/composite" \
    -r pegelhub --fields name,clientRole > "$MONITORING_USER_ROLES_STATE"

  jq -e '
    map({name, clientRole}) | sort_by(.name) == [
      {"name":"measurement:read","clientRole":true},
      {"name":"metadata:read","clientRole":true}
    ]
  ' "$MONITORING_USER_ROLES_STATE" >/dev/null \
    || fail "A monitoring group member did not inherit the expected roles."

  onboarding_redirect="https://$TEST_FRONTEND_HOSTNAME/overview?station=42"
  existing_cookie="$TEMP_DIR/existing-session.cookies"
  existing_html="$TEMP_DIR/existing-session-login.html"
  existing_verifier=integration-existing-session-verifier-abcdefghijklmnopqrstuvwxyz0123456789
  start_authorization existing-session "$onboarding_redirect" \
    "$existing_cookie" "$existing_html" "$existing_verifier"
  existing_headers="$TEMP_DIR/existing-session.headers"
  existing_body="$TEMP_DIR/existing-session.body"
  post_login "$existing_html" "$existing_cookie" \
    integration-monitoring-user integration-initial-passphrase \
    "$existing_headers" "$existing_body"
  assert_http_status "$existing_headers" 302 \
    "The initial permanent-password login did not complete"
  existing_location=$(location_header "$existing_headers")
  case "$existing_location" in
    "$onboarding_redirect"\&*) ;;
    *) fail "The initial login did not resume the requested frontend route." ;;
  esac
  existing_tokens="$TEMP_DIR/existing-session-tokens.json"
  exchange_authorization_code "$existing_location" "$existing_verifier" \
    "$onboarding_redirect" "$existing_tokens"

  kcadm set-password -r pegelhub \
    --userid "$monitoring_user_id" \
    --new-password integration-temporary-passphrase \
    --temporary >/dev/null
  kcadm get "users/$monitoring_user_id" -r pegelhub --fields requiredActions \
    | jq -e '.requiredActions == ["UPDATE_PASSWORD"]' >/dev/null \
    || fail "A temporary credential did not require a password update."

  existing_refresh_token=$(jq -r '.refresh_token' "$existing_tokens")
  refreshed_tokens="$TEMP_DIR/pre-update-refreshed-tokens.json"
  curl -fsS -X POST \
    "$KEYCLOAK_HTTP_ORIGIN/realms/pegelhub/protocol/openid-connect/token" \
    --data-urlencode grant_type=refresh_token \
    --data-urlencode client_id=pegelhub-frontend \
    --data-urlencode "refresh_token=$existing_refresh_token" \
    > "$refreshed_tokens"
  jq -e '.access_token and .refresh_token' "$refreshed_tokens" >/dev/null \
    || fail "The pre-existing session was not active before the password update."

  temporary_cookie="$TEMP_DIR/temporary-password.cookies"
  temporary_html="$TEMP_DIR/temporary-password-login.html"
  temporary_verifier=integration-temporary-password-verifier-abcdefghijklmnopqrstuvwxyz0123456789
  start_authorization temporary-password "$onboarding_redirect" \
    "$temporary_cookie" "$temporary_html" "$temporary_verifier"
  temporary_headers="$TEMP_DIR/temporary-password.headers"
  temporary_body="$TEMP_DIR/temporary-password.body"
  post_login "$temporary_html" "$temporary_cookie" \
    integration-monitoring-user integration-temporary-passphrase \
    "$temporary_headers" "$temporary_body"
  assert_http_status "$temporary_headers" 302 \
    "The temporary-password login did not reach its required action"
  required_action_location=$(location_header "$temporary_headers")
  required_action_url=$(internal_keycloak_url "$required_action_location")
  update_page="$TEMP_DIR/update-password.html"
  curl -fsS -b "$temporary_cookie" -c "$temporary_cookie" \
    "$required_action_url" > "$update_page"

  grep -F 'id="kc-passwd-update-form"' "$update_page" >/dev/null \
    || fail "The branded update-password form did not render."
  grep -F 'class="ph-field-help"' "$update_page" >/dev/null \
    || fail "The password policy guidance did not render."
  grep -E 'id="logout-sessions"[^>]*checked' "$update_page" >/dev/null \
    || fail "The logout-other-sessions control was not checked by default."

  mismatch_headers="$TEMP_DIR/mismatch.headers"
  mismatch_body="$TEMP_DIR/mismatch.html"
  submit_password_update "$update_page" "$temporary_cookie" \
    integration-valid-passphrase integration-different-passphrase \
    "$mismatch_headers" "$mismatch_body"
  assert_http_status "$mismatch_headers" 200 "A mismatched confirmation was accepted"
  grep -F 'id="input-error-password-confirm"' "$mismatch_body" >/dev/null \
    || fail "A mismatched confirmation did not render its field error."
  grep -F 'aria-describedby="password-requirements"' "$mismatch_body" >/dev/null \
    || fail "A confirmation mismatch left a broken global-error reference."

  short_headers="$TEMP_DIR/short.headers"
  short_body="$TEMP_DIR/short.html"
  submit_password_update "$mismatch_body" "$temporary_cookie" short-pass short-pass \
    "$short_headers" "$short_body"
  assert_http_status "$short_headers" 200 "A short password was accepted"
  assert_global_password_error "$short_body"

  username_headers="$TEMP_DIR/username.headers"
  username_body="$TEMP_DIR/username.html"
  submit_password_update "$short_body" "$temporary_cookie" \
    integration-monitoring-user integration-monitoring-user \
    "$username_headers" "$username_body"
  assert_http_status "$username_headers" 200 "A username-equal password was accepted"
  assert_global_password_error "$username_body"

  email_headers="$TEMP_DIR/email.headers"
  email_body="$TEMP_DIR/email.html"
  submit_password_update "$username_body" "$temporary_cookie" \
    integration-monitoring@example.invalid integration-monitoring@example.invalid \
    "$email_headers" "$email_body"
  assert_http_status "$email_headers" 200 "An email-equal password was accepted"
  assert_global_password_error "$email_body"

  valid_headers="$TEMP_DIR/valid.headers"
  valid_body="$TEMP_DIR/valid.html"
  submit_password_update "$email_body" "$temporary_cookie" \
    integration-valid-passphrase integration-valid-passphrase \
    "$valid_headers" "$valid_body"
  assert_http_status "$valid_headers" 302 "A valid password update did not complete"
  completed_location=$(location_header "$valid_headers")
  case "$completed_location" in
    "$onboarding_redirect"\&*) ;;
    *) fail "Password onboarding did not resume the requested frontend route." ;;
  esac

  onboarding_tokens="$TEMP_DIR/onboarding-tokens.json"
  exchange_authorization_code "$completed_location" "$temporary_verifier" \
    "$onboarding_redirect" "$onboarding_tokens"
  access_token=$(jq -r '.access_token' "$onboarding_tokens")
  access_payload=$(decode_jwt_payload "$access_token")
  printf '%s\n' "$access_payload" \
    | jq -e '.resource_access["pegelhub-core-api"].roles | sort == ["measurement:read","metadata:read"]' \
      >/dev/null \
    || fail "The onboarded user's token contained the wrong Core API roles."

  active_refresh_token=$(jq -r '.refresh_token' "$refreshed_tokens")
  invalid_refresh_response="$TEMP_DIR/invalid-refresh.json"
  refresh_status=$(curl -sS -o "$invalid_refresh_response" -w '%{http_code}' \
    -X POST "$KEYCLOAK_HTTP_ORIGIN/realms/pegelhub/protocol/openid-connect/token" \
    --data-urlencode grant_type=refresh_token \
    --data-urlencode client_id=pegelhub-frontend \
    --data-urlencode "refresh_token=$active_refresh_token")
  [ "$refresh_status" = "400" ] \
    || fail "The password update left another refresh session active."
  jq -e '.error == "invalid_grant"' "$invalid_refresh_response" >/dev/null \
    || fail "The invalidated refresh session returned an unexpected response."

  kcadm delete "users/$monitoring_user_id" -r pegelhub >/dev/null
}

command -v jq >/dev/null 2>&1 || fail "jq is required."
command -v docker >/dev/null 2>&1 || fail "docker is required."
command -v curl >/dev/null 2>&1 || fail "curl is required."
command -v openssl >/dev/null 2>&1 || fail "openssl is required."
assert_local_docker
CLEANUP_ENABLED=true
umask 077

sed \
  -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=$PROJECT_NAME/" \
  -e "s/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=$TEST_FRONTEND_HOSTNAME/" \
  -e "s/^PEGELHUB_API_HOSTNAME=.*/PEGELHUB_API_HOSTNAME=$TEST_API_HOSTNAME/" \
  -e "s/^PEGELHUB_KEYCLOAK_HOSTNAME=.*/PEGELHUB_KEYCLOAK_HOSTNAME=$TEST_KEYCLOAK_HOSTNAME/" \
  -e "s|^PEGELHUB_TLS_SERVER_DIR=.*|PEGELHUB_TLS_SERVER_DIR=$TEMP_DIR/tls/server|" \
  -e "s|^PEGELHUB_TRUST_DIR=.*|PEGELHUB_TRUST_DIR=$TEMP_DIR/tls/trust|" \
  "$ENV_TEMPLATE" > "$ENV_FILE"
mkdir -p "$TEMP_DIR/state" "$TEMP_DIR/tls/server" "$TEMP_DIR/tls/trust"
chmod 600 "$ENV_FILE"
PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$TEMP_DIR/state" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/init-env-secrets.sh" >/dev/null

awk -F= '
  ($1 == "META_PASSWORD" ||
    $1 == "INFLUX_ADMIN_PASSWORD" ||
    $1 == "INFLUX_TOKEN" ||
    $1 == "KEYCLOAK_DB_PASSWORD" ||
    $1 == "KEYCLOAK_ADMIN_PASSWORD") {
      value = substr($0, length($1) + 2)
      if (length(value) > 0) print value
    }
' "$ENV_FILE" > "$SECRET_VALUES_FILE"
chmod 600 "$SECRET_VALUES_FILE"

if ! PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$TEMP_DIR/state" PEGELHUB_ENV_FILE="$ENV_FILE" \
  PEGELHUB_KEYCLOAK_HOSTNAME=caller-override.invalid \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  > "$BOOTSTRAP_LOG" 2>&1; then
  grep '^ERROR:' "$BOOTSTRAP_LOG" >&2 || true
  fail "Disposable staging Keycloak bootstrap failed; protected output was withheld."
fi
assert_no_secret_output "$BOOTSTRAP_LOG"
runtime_keycloak_hostname=$(normal_compose exec -T keycloak printenv KC_HOSTNAME | tr -d '\r')
[ "$runtime_keycloak_hostname" = "https://$TEST_KEYCLOAK_HOSTNAME" ] \
  || fail "Caller environment overrode the protected staging Keycloak hostname."
inspect_state
assert_seed_state
normal_compose up -d --wait --force-recreate keycloak >/dev/null
keycloak_http_address=$(normal_compose port keycloak 8080 | tail -n 1 | tr -d '\r')
[ -n "$keycloak_http_address" ] || fail "The disposable Keycloak HTTP port is unavailable."
KEYCLOAK_HTTP_ORIGIN="http://$keycloak_http_address"
assert_onboarding_flow

if PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$TEMP_DIR/state" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" >/dev/null 2>&1; then
  fail "Offline realm bootstrap did not refuse to run while Keycloak was online."
fi

kcadm create clients -r pegelhub \
  -s clientId=integration-preserved-client \
  -s enabled=false \
  -s publicClient=true >/dev/null 2>&1
normal_compose stop keycloak >/dev/null

if ! PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$TEMP_DIR/state" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  > "$REBOOTSTRAP_LOG" 2>&1; then
  grep '^ERROR:' "$REBOOTSTRAP_LOG" >&2 || true
  fail "Repeated staging Keycloak bootstrap failed; protected output was withheld."
fi
assert_no_secret_output "$REBOOTSTRAP_LOG"
inspect_state
assert_seed_state

marker_count=$(jq '[.[] | select(.clientId == "integration-preserved-client")] | length' \
  "$CLIENTS_STATE")
[ "$marker_count" -eq 1 ] \
  || fail "Repeated bootstrap overwrote or duplicated existing identity state."

normal_compose up -d --wait --force-recreate keycloak >/dev/null
configure_kcadm
marker_count=$(kcadm get clients -r pegelhub \
  -q clientId=integration-preserved-client --fields id \
  | jq 'length')
[ "$marker_count" -eq 1 ] \
  || fail "Normal Keycloak recreation changed existing identity state."

printf '%s\n' "Disposable staging Keycloak bootstrap integration checks passed."
