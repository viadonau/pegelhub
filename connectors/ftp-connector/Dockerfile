FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY ./target/ftp-connector-2023.12.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
RUN mkdir -p /app/config

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.ftp.Main"]
