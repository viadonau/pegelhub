FROM amazoncorretto:17-alpine-jdk

RUN apk --no-cache add curl

RUN addgroup -S coreapp && adduser -S coreapp -G coreapp
USER coreapp:coreapp

EXPOSE 8080 8081

STOPSIGNAL SIGKILL

WORKDIR /var/lib/application

ADD ./target/app.jar .

CMD ["java", "-jar", "/var/lib/application/app.jar"]

HEALTHCHECK --interval=30s --timeout=2s --retries=5 --start-period=40s \
  CMD curl --fail --silent localhost:8080/actuator/health | grep UP || exit 1
