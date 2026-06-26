# PegelHub Domain Model

This document describes the current branch state after the domain migration. It intentionally shows remaining legacy pieces where they still exist, especially connector contact metadata and telemetry.

## Logical Model

The diagram is logical, not a guarantee that every relationship is already enforced by a database foreign key. The first migration pass still relies partly on Hibernate-managed schema updates. Full Flyway ownership, metadata FKs, access-grant reshaping, and connector contact reshaping are follow-up work.

```mermaid
erDiagram
    StationOwner ||--o{ Station : owns
    Station ||--o{ TimeSeries : has
    Connector ||--o{ TimeSeries : "optional source"
    Connector ||--o{ AccessGrant : "is subject of"
    AccessGrant }o--|| Station : "targets when resourceType=STATION"
    AccessGrant }o--|| TimeSeries : "targets when resourceType=TIME_SERIES"
    Connector ||--o{ Measurement : submits
    TimeSeries ||--o{ Measurement : identifies

    Contact ||--o{ Connector : manufacturer
    Contact ||--o{ Connector : softwareManufacturer
    Contact ||--o{ Connector : technicallyResponsible
    Contact ||--o{ Connector : operationCompany

    StationOwner {
        uuid id PK
        string name
        string shortName "optional"
        string notes "optional"
    }

    Station {
        uuid id PK
        uuid ownerId "logical StationOwner reference"
        string stationNumber "unique"
        string name
        string waterBody
        string location "optional"
    }

    TimeSeries {
        uuid id PK
        uuid stationId "logical Station reference"
        string observedProperty "e.g. water-level"
        string unit "e.g. cm"
        double referenceLevel "optional PNP in meters over Adria"
        int referenceYear "optional reference year for RNW/MW/HSW"
        double riverKilometer "optional V1 gauge location"
        string bank "optional V1 gauge bank"
        double rnw "optional water-level reference"
        double hsw "optional water-level reference"
        double mw "optional water-level reference"
        double hw100 "optional water-level reference"
        string externalCode "optional connector mapping"
        uuid sourceConnectorId "optional source Connector"
    }

    AccessGrant {
        uuid id PK
        uuid connectorId "logical Connector reference"
        enum resourceType "STATION | TIME_SERIES"
        uuid resourceId "polymorphic resource reference"
        enum permission "READ | WRITE"
    }

    Connector {
        uuid id PK
        string connectorNumber
        uuid manufacturerId "Contact FK"
        string typeDescription
        string softwareVersion
        string worksFromDataVersion
        string dataDefinition
        uuid softwareManufacturerId "Contact FK"
        uuid technicallyResponsibleId "Contact FK"
        uuid operatingCompanyId "Contact FK"
        string notes
        string keycloakClientId "unique, optional"
        enum status "ACTIVE | SUSPENDED"
    }

    Contact {
        uuid id PK
        string organization
        string contactPerson
        string contactStreet
        string contactPlz
        string location
        string contactCountry
        string emergencyNumber
        string emergencyNumberTwo
        string emergencyMail
        string serviceNumber
        string serviceNumberTwo
        string serviceMail
        string administrationPhoneNumber
        string administrationPhoneNumberTwo
        string administrationMail
        string contactNodes
    }

    Measurement {
        uuid timeSeriesId "Influx group key"
        instant observedAt
        instant receivedAt
        double value
        uuid submittedByConnectorId
    }

    Telemetry {
        string measurement
        string stationIPAddressIntern
        string stationIPAddressExtern
        instant timestamp
        integer cycleTime
        double temperatureWater
        double temperatureAir
        double performanceVoltageBattery
        double performanceVoltageSupply
        double performanceElectricityBattery
        double performanceElectricitySupply
        double fieldStrengthTransmission
    }
```

## Deferred Model Cleanup

The current connector/contact shape is intentionally still legacy-shaped. `Contact` is still a standalone resource and `Connector` still owns four required contact references. The preferred follow-up direction is to replace that with connector-owned, role-based contact points once Flyway/schema migrations are introduced.

```mermaid
flowchart LR
    Connector["Connector (current)"] --> Manufacturer["Contact: manufacturer"]
    Connector --> SoftwareManufacturer["Contact: software manufacturer"]
    Connector --> TechnicalResponsible["Contact: technically responsible"]
    Connector --> OperationCompany["Contact: operation company"]

    Connector -. "future Flyway cleanup" .-> ContactPoints["Connector-owned contact points by role"]
```

PNP, gauge-location, and water-level reference metadata currently lives directly on `TimeSeries` as a V1 simplification. Extract a `MeasuringPoint` between `Station` and `TimeSeries` when multiple series share one bank/kilometer/reference set or when left/right/device identity needs an independent lifecycle.

## Authorization Cascade

```mermaid
flowchart TD
    grant[AccessGrant] --> check{resourceType?}

    check -->|TIME_SERIES| direct[Direct TimeSeries grant]
    check -->|STATION| stationGrant[Station grant]

    direct --> perm{permission?}
    stationGrant --> stationSeries[All TimeSeries at this station]
    stationSeries --> read[Can read]

    perm -->|READ| read
    perm -->|WRITE| writeCheck{sourceConnectorId set?}
    writeCheck -->|no| write[Can write if direct grant exists]
    writeCheck -->|yes| sameSource{connector is source?}
    sameSource -->|yes| write
    sameSource -->|no| deny[Deny]
```

Station grants are read-only and cover all TimeSeries at the station. Direct TimeSeries `WRITE` grants are rejected when the TimeSeries has a different `sourceConnectorId`.

## Measurement Write Path

```mermaid
sequenceDiagram
    participant C as Connector client
    participant MC as HttpMeasurementController
    participant CA as CurrentActor
    participant MS as MeasurementServiceImpl
    participant CR as ConnectorRepository
    participant TS as TimeSeriesService
    participant AA as AccessAuthorizationService
    participant MR as InfluxMeasurementRepository

    C->>MC: POST /api/v1/measurements<br/>{ measurements: [{ timeSeriesId, observedAt, value }] }
    MC->>MS: writeMeasurements(WriteMeasurements)
    MS->>CA: get()
    CA-->>MS: PegelHubActor(clientId, authorities)
    MS->>CR: findByKeycloakClientId(clientId)
    CR-->>MS: Connector or missing
    MS->>MS: Require Connector status ACTIVE

    loop each measurement
        MS->>TS: get(timeSeriesId)
        TS-->>MS: TimeSeries or 404
        MS->>MS: If sourceConnectorId is set, require same Connector
        MS->>AA: isAllowed(connectorId, timeSeriesRef, WRITE)
        AA-->>MS: true/false
    end

    MS->>MR: storeMeasurements([{ timeSeriesId, observedAt, receivedAt, value, submittedByConnectorId }])
    MR-->>MS: ok
    MS-->>MC: ok
    MC-->>C: 200
```

## Measurement Read Path

```mermaid
flowchart TD
    A["GET /time-series/{id}/measurements?last=..."] --> MS[MeasurementService]
    B["GET /time-series/{id}/measurements?from=...&to=..."] --> MS
    C["GET /time-series/{id}/measurements?last=365d&order=desc&limit=1"] --> MS
    E["GET /time-series/{id}/measurements/buckets?last=..."] --> MS
    D["GET /measurements/system-time"] --> MR[InfluxMeasurementRepository]

    MS --> TS["TimeSeriesService: validate TimeSeries exists"]
    MS --> Actor["CurrentActor"]
    Actor --> Admin{SYSTEM_ADMIN?}
    Admin -->|yes| MR
    Admin -->|no| ConnectorLookup["ConnectorRepository: find by clientId"]
    ConnectorLookup --> Active["Require Connector ACTIVE"]
    Active --> Auth["AccessAuthorizationService: READ"]
    Auth --> MR
```

## API Surface

The security column names the effective Spring Security rule. Most metadata routes accept `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` for reads, and `METADATA_WRITE` or `SYSTEM_ADMIN` for writes/deletes.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/measurements` | `MEASUREMENT_WRITE` | Write measurements |
| GET | `/api/v1/time-series/{timeSeriesId}/measurements?last={duration}` | `MEASUREMENT_READ` or `SYSTEM_ADMIN` | Raw TimeSeries measurements in a relative window |
| GET | `/api/v1/time-series/{timeSeriesId}/measurements?from={instant}&to={instant}` | `MEASUREMENT_READ` or `SYSTEM_ADMIN` | Raw TimeSeries measurements in an explicit window |
| GET | `/api/v1/time-series/{timeSeriesId}/measurements?last={duration}&order=desc&limit=1` | `MEASUREMENT_READ` or `SYSTEM_ADMIN` | Latest value for TimeSeries through the paged raw query |
| GET | `/api/v1/time-series/{timeSeriesId}/measurements/buckets?last={duration}` | `MEASUREMENT_READ` or `SYSTEM_ADMIN` | Average buckets for chart-ready TimeSeries reads |
| GET | `/api/v1/measurements/system-time` | public | InfluxDB system time |
| POST | `/api/v1/admin/connectors` | `SYSTEM_ADMIN` | Register connector identity binding |
| POST | `/api/v1/connectors` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create connector metadata |
| GET | `/api/v1/connectors` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List connectors |
| GET | `/api/v1/connectors/{uuid}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get connector |
| DELETE | `/api/v1/connectors/{uuid}` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Delete connector |
| POST | `/api/v1/contact` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create legacy contact |
| GET | `/api/v1/contact` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List legacy contacts |
| GET | `/api/v1/contact/{uuid}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get legacy contact |
| DELETE | `/api/v1/contact/{uuid}` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Delete legacy contact |
| POST | `/api/v1/station-owners` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create station owner |
| GET | `/api/v1/station-owners` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List station owners |
| GET | `/api/v1/station-owners/{id}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get station owner |
| POST | `/api/v1/stations` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create station |
| GET | `/api/v1/stations` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List stations |
| GET | `/api/v1/stations/{id}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get station |
| POST | `/api/v1/time-series` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create time series |
| GET | `/api/v1/time-series` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List time series, optionally filtered by `stationId` |
| GET | `/api/v1/time-series/{id}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get time series |
| POST | `/api/v1/access-grants` | `METADATA_WRITE` or `SYSTEM_ADMIN` | Create access grant |
| GET | `/api/v1/access-grants` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | List access grants, optionally filtered by `connectorId` |
| GET | `/api/v1/access-grants/{id}` | `METADATA_READ`, `METADATA_WRITE`, or `SYSTEM_ADMIN` | Get access grant |
| POST | `/api/v1/telemetry` | `TELEMETRY_WRITE` or `SYSTEM_ADMIN` | Write technical telemetry |
| GET | `/api/v1/telemetry/{range}` | `TELEMETRY_READ` or `SYSTEM_ADMIN` | Query telemetry by range |
| GET | `/api/v1/telemetry/last/{uuid}` | `TELEMETRY_READ` or `SYSTEM_ADMIN` | Query latest telemetry for id |

## Package Structure

```text
core/src/main/java/at/pegelhub/
├── stationowner/       StationOwner API/application/domain/persistence
├── station/            Station API/application/domain/persistence
├── timeseries/         TimeSeries API/application/domain/persistence
├── access/             AccessGrant API/application/domain/persistence
├── measurement/        TimeSeries-backed Measurement write/read API and Influx persistence
├── connector/          Connector metadata, Keycloak client binding, legacy contact references
├── contact/            Legacy standalone Contact resource used by Connector
├── telemetry/          Technical telemetry, still domain-as-API
├── security/           Keycloak resource server, authority mapping, current actor
└── shared/
    ├── influx/         InfluxDB configuration, query helpers, point helpers
    ├── persistence/    Legacy Contact entity/domain converters
    ├── validation/     Validation and normalization helpers
    └── web/            Legacy Contact DTO/domain converters
```

## Known Follow-Up Areas

- Introduce Flyway with a curated baseline and move Hibernate to schema validation.
- Add database-level FKs and indexes for metadata relationships.
- Reshape `AccessGrant` persistence away from polymorphic `resourceType/resourceId`.
- Replace standalone Contact CRUD and four connector contact FKs with connector-owned role-based contact points.
- Standardize measurement and telemetry response DTOs.
- Refactor connector configuration and remove remaining legacy contact/config duplication.
