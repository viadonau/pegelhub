"""Disposable lab provisioning through Keycloak and Core APIs; never run against production."""
import json
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path('/config')
API = 'pegelhub-core-api'
SECRET = 'watchdog-lab-only-secret'


def request(url, method='GET', body=None, token=None, form=None):
    headers = {}
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        headers['Content-Type'] = 'application/json'
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    if token:
        headers['Authorization'] = 'Bearer ' + token
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data, headers, method=method), timeout=30) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        # The lab contains fixed, disposable credentials; do not print tokens or request bodies.
        raise RuntimeError(f'{method} {url}: HTTP {error.code}') from None


def token(side, admin=False):
    realm = 'master' if admin else 'pegelhub'
    return request(f'http://keycloak-{side}:8080/realms/{realm}/protocol/openid-connect/token', 'POST', form={
        'grant_type': 'password', 'client_id': 'admin-cli' if admin else 'lab-operator',
        'username': 'admin' if admin else 'operator', 'password': 'lab-password',
    })['access_token']


def mapper(name, kind, **config):
    return {'name': name, 'protocol': 'openid-connect', 'protocolMapper': kind,
            'config': {'access.token.claim': 'true', **config}}


def client(name, user=False):
    return {'clientId': name, 'enabled': True, 'protocol': 'openid-connect', 'publicClient': user,
            'secret': SECRET, 'serviceAccountsEnabled': not user, 'directAccessGrantsEnabled': user,
            'standardFlowEnabled': False, 'fullScopeAllowed': True,
            'protocolMappers': [
                mapper('audience', 'oidc-audience-mapper', **{'included.client.audience': API}),
                mapper('actor', 'oidc-hardcoded-claim-mapper', **{'claim.name': 'pegelhub_actor_type',
                       'claim.value': 'USER' if user else 'CLIENT', 'claim.value.type': 'String'}),
                mapper('roles', 'oidc-usermodel-client-role-mapper', **{'claim.name': 'resource_access.${client_id}.roles',
                       'jsonType.label': 'String', 'multivalued': 'true'}),
            ]}


def realms():
    for side in ('a', 'b'):
        auth = token(side, True)
        base = f'http://keycloak-{side}:8080/admin/realms'
        if any(realm['realm'] == 'pegelhub' for realm in request(base, token=auth)):
            continue
        services = ('watchdog', 'icc', 'iec') if side == 'a' else ('icc', 'tstp')
        # Only the interactive provisioning user is an administrator. Runtime service accounts get measurement roles.
        users = [{'username': 'operator', 'enabled': True, 'emailVerified': True,
                  'firstName': 'Lab', 'lastName': 'Operator', 'email': 'operator@example.test',
                  'credentials': [{'type': 'password', 'value': 'lab-password', 'temporary': False}],
                  'clientRoles': {API: ['system:admin']}}]
        users += [{'username': 'service-account-' + name, 'enabled': True, 'serviceAccountClientId': name,
                   'clientRoles': {API: ['measurement:read'] if name == 'watchdog'
                                   else ['measurement:read', 'measurement:write']}} for name in services]
        request(base, 'POST', {'realm': 'pegelhub', 'enabled': True, 'sslRequired': 'none',
            'roles': {'client': {API: [{'name': role} for role in ['system:admin', 'measurement:read', 'measurement:write']]}},
            'clients': [{'clientId': API, 'bearerOnly': True, 'enabled': True}, client('lab-operator', True)]
                       + [client(name) for name in services], 'users': users}, auth)
    print('Lab realms ready')


def write(path, value):
    destination = ROOT / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(value, indent=2) + '\n')


def configure():
    checkpoint = ROOT / 'metadata.json'
    ids = json.loads(checkpoint.read_text()) if checkpoint.exists() else {}
    for side in ('a', 'b'):
        provision_core(side, ids)
    write_connector_configs(ids)
    write_watchdog_config(ids)
    print('Lab metadata, exclusive writers, exact grants, and configuration ready')


def provision_core(side, ids):
    auth = token(side)
    base = f'http://core-{side}:8080/api/v1'

    def create(key, endpoint, body):
        # Preserve Core-generated IDs across ordinary lab restarts; never replace source ownership by accident.
        key = side + '.' + key
        if key not in ids:
            ids[key] = request(base + endpoint, 'POST', body, auth)['id']
            write('metadata.json', ids)
        return ids[key]

    services = {'watchdog': 'other', 'icc': 'icc', 'iec': 'iec'} if side == 'a' else {'icc': 'icc', 'tstp': 'tstp'}
    connectors = {
        name: create(name, '/admin/connectors', {
            'keycloakClientId': name, 'connector': {'name': 'Lab ' + name, 'type': kind},
        })
        for name, kind in services.items()
    }
    owner = create('owner', '/station-owners', {'name': 'Watchdog Lab', 'shortName': 'E2E', 'notes': 'Synthetic only'})
    station = create('station', '/stations', {'ownerId': owner, 'name': 'Synthetic lab', 'waterBody': 'Test'})

    # The IEC gauge traverses the entire route. The watchdog has no source assignment and cannot write.
    series = {'outgoing': 'iec', 'return': 'icc'} if side == 'a' else {'outgoing': 'icc', 'return': 'tstp'}
    for name, writer in series.items():
        point = create(name + '.point', '/measuring-points', {'stationId': station, 'name': name, 'status': 'active'})
        create(name, '/time-series', {
            'measuringPointId': point, 'observedProperty': 'water-level', 'status': 'active',
            'sourceAssignment': {'connectorId': connectors[writer], 'representation': 'canonical'},
        })

    grants = {'watchdog': ['return'], 'icc': ['outgoing']} if side == 'a' else {'icc': ['return'], 'tstp': ['outgoing']}
    for name, readable in grants.items():
        for series_name in readable:
            series_id = ids[side + '.' + series_name]
            request(base + f'/connectors/{connectors[name]}/read-access/time-series/{series_id}', 'PUT', token=auth)


def core_connection(side, name):
    return {
        'baseUrl': f'http://core-{side}:8080/',
        'authentication': {
            'tokenUrl': f'http://keycloak-{side}:8080/realms/pegelhub/protocol/openid-connect/token',
            'clientId': name, 'clientSecret': SECRET,
        },
    }


def write_connector_configs(ids):
    polling = {'polling': {'interval': '2s', 'overlap': '1h'}, 'mappings': {'directory': 'mappings'}}
    write('icc/connector.yaml', {**polling, 'localCore': core_connection('a', 'icc'), 'remoteCore': core_connection('b', 'icc')})
    for name, direction in [('outgoing', 'core-to-external'), ('return', 'external-to-core')]:
        write(f'icc/mappings/{name}.yaml', {'timeSeriesId': ids['a.' + name], 'externalTimeSeriesId': ids['b.' + name], 'direction': direction})
        write(f'tstp/mappings/{name}.yaml', {'timeSeriesId': ids['b.' + name], 'stationId': 10001373, 'direction': direction})
    write('tstp/connector.yaml', {**polling, 'core': core_connection('b', 'tstp'), 'tstp': {'server': {'host': 'fixtures', 'port': 8030}}})
    write('iec/connector.yaml', {'core': core_connection('a', 'iec'), 'polling': {'interval': '2s'}, 'mappings': {'directory': 'mappings'},
                               'iec': {'server': {'host': 'fixtures', 'port': 2404, 'commonAddress': 1}}})
    write('iec/mappings/outgoing.yaml', {'timeSeriesId': ids['a.outgoing'], 'iecIoa': 66051, 'direction': 'external-to-core'})


def write_watchdog_config(ids):
    write('watchdog/watchdog.yaml', {'watchdogId': 'lab', 'core': {'baseUrl': 'http://core-a:8080/',
        'tokenUrl': 'http://keycloak-a:8080/realms/pegelhub/protocol/openid-connect/token', 'clientId': 'watchdog', 'clientSecretFile': 'secret'},
        'timeSeriesId': ids['a.return'], 'silenceSeconds': 10, 'pollSeconds': 1,
        'snmp': {'receivers': [{'host': 'fixtures', 'port': 1162}, {'host': 'fixtures', 'port': 1163}],
                 'version': 'v3', 'username': 'test',
                 'trapOid': '1.3.6.1.4.1.32473.1', 'messageOid': '1.3.6.1.4.1.32473.2',
                 'authPasswordFile': 'auth', 'privacyPasswordFile': 'privacy'}})
    for name, value in {'secret': SECRET, 'auth': 'test-auth-password', 'privacy': 'test-privacy-password'}.items():
        (ROOT / 'watchdog' / name).write_text(value)


if __name__ == '__main__':
    realms() if sys.argv[1] == 'realms' else configure()
