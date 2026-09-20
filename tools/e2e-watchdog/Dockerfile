FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY connectors/pom.xml connectors/pom.xml
COPY connectors/library connectors/library
COPY tools/e2e-watchdog tools/e2e-watchdog
RUN mvn -B -ntp -N -f connectors/pom.xml install
RUN mvn -B -ntp -f connectors/library/pom.xml -DskipTests install
RUN mvn -B -ntp -f tools/e2e-watchdog/pom.xml -DskipTests package

FROM amazoncorretto:21-alpine-jdk AS runtime
WORKDIR /app
RUN addgroup -g 10001 -S watchdog && adduser -u 10001 -S watchdog -G watchdog \
    && mkdir -p /var/lib/pegelhub-watchdog /app/config \
    && chown watchdog:watchdog /var/lib/pegelhub-watchdog
COPY --from=build /workspace/tools/e2e-watchdog/target/watchdog.jar /app/watchdog.jar
COPY --from=build /workspace/tools/e2e-watchdog/target/lib/ /app/lib/
COPY --chmod=755 docker/java-entrypoint.sh /usr/local/bin/pegelhub-java-entrypoint
USER watchdog:watchdog
ENTRYPOINT ["/usr/local/bin/pegelhub-java-entrypoint", "java", "-cp", "/app/watchdog.jar:/app/lib/*", "at.pegelhub.watchdog.WatchdogApplication"]
CMD ["run"]
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication health

# Protocol fixtures are test-only and are never copied into the production target.
FROM build AS lab-build
RUN mvn -B -ntp -f tools/e2e-watchdog/pom.xml dependency:copy-dependencies -DincludeScope=test -DoutputDirectory=target/lab-lib
FROM amazoncorretto:21-alpine-jdk AS lab
WORKDIR /app
COPY --from=lab-build /workspace/tools/e2e-watchdog/target/watchdog-tests.jar /app/tests.jar
COPY --from=lab-build /workspace/tools/e2e-watchdog/target/lab-lib/ /app/lib/
ENTRYPOINT ["java", "-cp", "/app/tests.jar:/app/lib/*", "at.pegelhub.watchdog.lab.LabServer"]

FROM runtime AS production
