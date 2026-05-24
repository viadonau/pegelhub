FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/iec-connector.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
RUN mkdir -p /app/config /app/data

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.iec.app.IecConnectorApplication"]
