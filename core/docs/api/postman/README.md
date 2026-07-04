# Postman Collection: Pegelhub Core

This directory contains an importable Postman collection for Pegelhub Core.
The checked-in collection still contains some legacy supplier and measurement
requests from before the station/time-series API migration. Treat the route
summary below as the current Core API surface until the collection is regenerated.

## Usage Instructions

1. Install [Postman](https://www.postman.com/downloads/), if you haven't already.
2. Import `pegelhub.postman_collection.json`.
3. Run the Pegelhub core.
4. Set the required variables in the collection or environment to match your specific setup.
   - `baseAddress`: The IP address of your Pegelhub core instance.
   - `port`: The port of the Pegelhub core application.
   - `apiPath`: Use `api/v1` for the current Core API.
   - `accessToken`: A Keycloak access token for the request you want to execute.

## Current API Surface

- Station owners: `/api/v1/station-owners`
- Stations: `/api/v1/stations`
- Time series: `/api/v1/time-series`
- Measurements: `/api/v1/measurements` and `/api/v1/time-series/{timeSeriesId}/measurements`
- Measurement buckets: `/api/v1/time-series/{timeSeriesId}/measurements/buckets`
- Connectors: `/api/v1/connectors`
- Connector identity binding: `/api/v1/admin/connectors`
- Access grants: `/api/v1/access-grants`
- Legacy contacts: `/api/v1/contact`
- Telemetry: `/api/v1/telemetry`

Useful measurement requests:

- `POST /api/v1/measurements`: write one or more `{ timeSeriesId, observedAt, value }` measurements.
- `GET /api/v1/time-series/{timeSeriesId}/measurements?last=24h`: read raw measurements in a relative window.
- `GET /api/v1/time-series/{timeSeriesId}/measurements?from=<instant>&to=<instant>`: read raw measurements in an explicit window.
- `GET /api/v1/time-series/{timeSeriesId}/measurements?last=365d&order=desc&limit=1`: read the latest value.
- `GET /api/v1/time-series/{timeSeriesId}/measurements/buckets?last=24h&maxPoints=200`: read chart-ready average buckets.
- `GET /api/v1/measurements/system-time`: check the InfluxDB system time.

## Executing Requests

You can execute the requests individually or run them as part of a sequence.

The requests in this collection use `Authorization: Bearer {{accessToken}}`. Request a token from Keycloak with client credentials, then paste the access token into the `accessToken` collection or environment variable. Use an operator token for metadata writes and a connector token for connector-owned data writes.

Some requests require values for database objects that already exist in your local database:

- Metadata reads by id require the UUID of the target resource.
- Measurement reads require a `timeSeriesId` and either `last` or `from` plus `to`.
- Measurement writes require an authenticated connector with `MEASUREMENT_WRITE`
  and access to the submitted time series.

For more information on each request's details and parameters, refer to the request documentation within the collection.
