# Core App

`core-app` is the deployable Spring Boot monolith inside `pegelhub-core`.

It contains:

- the `/api/v1/**` HTTP API
- application services and authorization
- Postgres persistence
- InfluxDB persistence
- actuator and runtime configuration

Build it from the reactor root with:

```bash
mvn -f pegelhub-core/pom.xml -pl core-app -am package
```
