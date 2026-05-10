FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY ./target/icc-connector-2022.01.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
RUN mkdir -p /app/config

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.icc.Main"]
