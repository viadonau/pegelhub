FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM amazoncorretto:21-alpine-jdk

RUN apk --no-cache add curl

RUN addgroup -S coreapp && adduser -S coreapp -G coreapp

ENV APP_PORT=8080 \
    MANAGEMENT_PORT=8081 \
    DB_URI=meta-db:5432/pegelhub \
    DB_USER=postgres

EXPOSE $APP_PORT $MANAGEMENT_PORT

WORKDIR /var/lib/application

COPY --from=build --chown=coreapp:coreapp /workspace/target/app.jar .

USER coreapp:coreapp

CMD ["java", "-jar", "/var/lib/application/app.jar"]

HEALTHCHECK --interval=30s --timeout=2s --retries=5 --start-period=40s \
  CMD curl --fail --silent "localhost:${MANAGEMENT_PORT}/actuator/health" | grep UP || exit 1
