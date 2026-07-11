FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY connectors/pom.xml connectors/pom.xml
COPY connectors/live-test-suite connectors/live-test-suite

RUN mvn -B -ntp -N -f connectors/pom.xml install
RUN mvn -B -ntp -f connectors/live-test-suite/pom.xml -DskipTests package

FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

COPY --from=build /workspace/connectors/live-test-suite/target/live-test-suite.jar /app/live-test-suite.jar
COPY --from=build /workspace/connectors/live-test-suite/target/lib/ /app/lib/
COPY connectors/live-test-suite/fixtures /app/fixtures

ENV LIVE_FIXTURES_DIR=/app/fixtures

EXPOSE 19000 18080 18081 18082 18083 18030 2121 2404

ENTRYPOINT ["java","-cp","/app/live-test-suite.jar:/app/lib/*","at.pegelhub.connector.livetest.LiveTestSuiteApplication"]
