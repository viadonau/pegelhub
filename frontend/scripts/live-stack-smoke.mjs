#!/usr/bin/env node

const DEFAULT_FRONTEND_BASE_URL = 'http://localhost:4200';
const DEFAULT_OPERATOR_CLIENT_ID = 'local-operator';
const DEFAULT_OPERATOR_CLIENT_SECRET = 'local-dev-operator-secret-change-me';
const DEFAULT_TIMEOUT_MS = 10000;
const METADATA_COLLECTIONS = [
  ['station owners', 'station-owners'],
  ['stations', 'stations'],
  ['measuring points', 'measuring-points'],
  ['time series', 'time-series'],
];

const timeoutMs = numberFromEnv('LIVE_STACK_SMOKE_TIMEOUT_MS', DEFAULT_TIMEOUT_MS);

async function main() {
  const frontendBaseUrl = trimTrailingSlash(
    process.env.FRONTEND_BASE_URL || DEFAULT_FRONTEND_BASE_URL,
  );

  const appRoute = new URL('/overview', frontendBaseUrl);
  const appHtml = await fetchText(appRoute);
  assert(
    appHtml.includes('<app-root></app-root>'),
    `Expected Angular app shell in ${appRoute.toString()}.`,
  );
  log('frontend route fallback', appRoute.toString());

  const configUrl = new URL('/assets/config.json', frontendBaseUrl);
  const config = await fetchJson(configUrl);
  assertRuntimeConfig(config, configUrl);
  log('runtime config', configUrl.toString());

  const discoveryUrl = keycloakDiscoveryUrl(config.keycloak.url, config.keycloak.realm);
  const discovery = await fetchJson(discoveryUrl);
  assert(
    typeof discovery.token_endpoint === 'string' && discovery.token_endpoint.length > 0,
    `Expected token_endpoint in ${discoveryUrl.toString()}.`,
  );
  log('keycloak discovery', discoveryUrl.toString());

  const systemTimeUrl = apiUrl(frontendBaseUrl, config.apiBaseUrl, 'measurements/system-time');
  await fetchText(systemTimeUrl);
  log('frontend proxy system-time', systemTimeUrl.toString());

  const stationsUrl = apiUrl(frontendBaseUrl, config.apiBaseUrl, 'stations');
  const unauthenticated = await fetch(stationsUrl, { signal: timeoutSignal() });
  assert(
    unauthenticated.status === 401,
    `Expected unauthenticated stations request to be 401, got ${unauthenticated.status}.`,
  );
  log('frontend proxy auth gate', '401');

  const token = await operatorToken(discovery.token_endpoint);
  const authenticatedHeaders = { Authorization: `Bearer ${token}` };
  let stations = [];
  let measuringPoints = [];
  let timeSeries = [];

  for (const [label, path] of METADATA_COLLECTIONS) {
    const url = apiUrl(frontendBaseUrl, config.apiBaseUrl, path);
    const collection = await fetchJson(url, { headers: authenticatedHeaders });
    assert(Array.isArray(collection), `Expected authenticated ${label} response to be an array.`);
    log(`frontend proxy authenticated ${label}`, `${collection.length} item(s)`);

    switch (path) {
      case 'stations':
        stations = collection;
        break;
      case 'measuring-points':
        measuringPoints = collection;
        break;
      case 'time-series':
        timeSeries = collection;
        break;
    }
  }

  assertMetadataRelationships(stations, measuringPoints, timeSeries);

  if (timeSeries[0]?.id) {
    await verifyMeasurementReads(
      frontendBaseUrl,
      config.apiBaseUrl,
      timeSeries[0].id,
      authenticatedHeaders,
    );
  }
}

function assertMetadataRelationships(stations, measuringPoints, timeSeries) {
  const stationIds = new Set(stations.map(({ id }) => id));
  const measuringPointIds = new Set(measuringPoints.map(({ id }) => id));

  for (const measuringPoint of measuringPoints) {
    assert(
      stationIds.has(measuringPoint.stationId),
      `Measuring point ${measuringPoint.id} references unknown station ${measuringPoint.stationId}.`,
    );
  }

  for (const series of timeSeries) {
    assert(
      measuringPointIds.has(series.measuringPointId),
      `Time series ${series.id} references unknown measuring point ${series.measuringPointId}.`,
    );
  }

  log(
    'metadata relationships',
    `${measuringPoints.length} measuring point(s), ${timeSeries.length} time series`,
  );
}

async function verifyMeasurementReads(frontendBaseUrl, apiBaseUrl, timeSeriesId, headers) {
  const encodedId = encodeURIComponent(timeSeriesId);
  const rawUrl = apiUrl(
    frontendBaseUrl,
    apiBaseUrl,
    `time-series/${encodedId}/measurements?last=24h&order=desc&limit=10`,
  );
  const raw = await fetchJson(rawUrl, { headers });
  assert(
    Array.isArray(raw.measurements),
    'Expected raw measurement response to contain measurements.',
  );
  log('frontend proxy raw measurements', `${raw.measurements.length} item(s)`);

  const bucketUrl = apiUrl(
    frontendBaseUrl,
    apiBaseUrl,
    `time-series/${encodedId}/measurements/buckets?last=24h&maxPoints=48`,
  );
  const buckets = await fetchJson(bucketUrl, { headers });
  assert(Array.isArray(buckets.points), 'Expected bucket response to contain points.');
  log('frontend proxy measurement buckets', `${buckets.points.length} item(s)`);
}

function assertRuntimeConfig(config, configUrl) {
  const fields = [
    ['apiBaseUrl', config.apiBaseUrl],
    ['keycloak.url', config.keycloak?.url],
    ['keycloak.realm', config.keycloak?.realm],
    ['keycloak.clientId', config.keycloak?.clientId],
  ];
  const missing = fields
    .filter(([, value]) => typeof value !== 'string' || value.trim() === '')
    .map(([field]) => field);

  assert(
    missing.length === 0,
    `Runtime config ${configUrl.toString()} is missing: ${missing.join(', ')}.`,
  );
}

async function operatorToken(tokenEndpoint) {
  const clientId = process.env.LOCAL_OPERATOR_CLIENT_ID || DEFAULT_OPERATOR_CLIENT_ID;
  const clientSecret = process.env.LOCAL_OPERATOR_CLIENT_SECRET || DEFAULT_OPERATOR_CLIENT_SECRET;
  const body = new URLSearchParams({
    grant_type: 'client_credentials',
    client_id: clientId,
    client_secret: clientSecret,
  });

  const response = await fetch(tokenEndpoint, {
    method: 'POST',
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
    },
    body,
    signal: timeoutSignal(),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(
      `Could not obtain local operator token from ${tokenEndpoint}: ${response.status} ${text}`,
    );
  }

  const payload = await response.json();
  assert(typeof payload.access_token === 'string', 'Token response did not contain access_token.');

  return payload.access_token;
}

function keycloakDiscoveryUrl(keycloakUrl, realm) {
  return new URL(
    `${trimTrailingSlash(keycloakUrl)}/realms/${encodeURIComponent(realm)}/.well-known/openid-configuration`,
  );
}

function apiUrl(frontendBaseUrl, apiBaseUrl, path) {
  const base = new URL(ensureTrailingSlash(apiBaseUrl), frontendBaseUrl);
  return new URL(path.replace(/^\/+/, ''), base);
}

async function fetchJson(url, init = {}) {
  const text = await fetchText(url, init);

  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(`Expected JSON from ${url.toString()}: ${errorMessage(error)}`);
  }
}

async function fetchText(url, init = {}) {
  const response = await fetch(url, {
    ...init,
    signal: init.signal || timeoutSignal(),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`${url.toString()} returned ${response.status}: ${body}`);
  }

  return response.text();
}

function timeoutSignal() {
  return AbortSignal.timeout(timeoutMs);
}

function numberFromEnv(name, fallback) {
  const value = process.env[name];

  if (!value) {
    return fallback;
  }

  const parsed = Number(value);
  assert(Number.isFinite(parsed) && parsed > 0, `${name} must be a positive number.`);

  return parsed;
}

function ensureTrailingSlash(value) {
  return value.endsWith('/') ? value : `${value}/`;
}

function trimTrailingSlash(value) {
  return value.replace(/\/+$/, '');
}

function log(label, detail) {
  console.log(`[live-smoke] ${label}: ${detail}`);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function errorMessage(error) {
  return error instanceof Error && error.message ? error.message : 'Unknown error';
}

main().catch((error) => {
  console.error(`[live-smoke] failed: ${errorMessage(error)}`);
  process.exitCode = 1;
});
