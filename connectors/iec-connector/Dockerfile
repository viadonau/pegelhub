FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/iec-connector-1.0.0-SNAPSHOT.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
RUN mkdir -p /app/config /app/data

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.iec.app.IecConnectorApplication"]
