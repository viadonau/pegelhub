FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY connectors/pom.xml connectors/pom.xml
COPY connectors/library connectors/library
COPY connectors/icc-connector connectors/icc-connector
RUN mvn -B -ntp -N -f connectors/pom.xml install
RUN mvn -B -ntp -f connectors/library/pom.xml -DskipTests install
RUN mvn -B -ntp -f connectors/icc-connector/pom.xml -DskipTests package

FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY --from=build /workspace/connectors/icc-connector/target/icc-connector.jar /app/connector.jar
COPY --from=build /workspace/connectors/icc-connector/target/lib/ /app/lib/
RUN mkdir -p /app/config

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.icc.Main"]
