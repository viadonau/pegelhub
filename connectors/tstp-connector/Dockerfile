FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY ./target/tstp-connector.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
RUN mkdir -p /app/config

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.tstp.Main"]
