FROM amazoncorretto:21-alpine-jdk

RUN apk --no-cache add curl

RUN addgroup -S coreapp && adduser -S coreapp -G coreapp
USER coreapp:coreapp

ENV APP_PORT=8080 \
    MANAGEMENT_PORT=8081 \
    DB_PASSWORD=null \
    DB_HOST=meta-db:5432 \
    DB_DATABASE=pegelhub \
    DB_URI=$DB_HOST/$DB_DATABASE \
    DB_USER=postgres

EXPOSE $APP_PORT $MANAGEMENT_PORT

WORKDIR /var/lib/application

ADD ./target/app.jar .

CMD ["java", "-jar", "/var/lib/application/app.jar"]

HEALTHCHECK --interval=30s --timeout=2s --retries=5 --start-period=40s \
  CMD curl --fail --silent "localhost:${MANAGEMENT_PORT}/actuator/health" | grep UP || exit 1
