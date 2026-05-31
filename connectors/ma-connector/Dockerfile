FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21-jammy AS build-java
WORKDIR /workspace

COPY connectors/pom.xml connectors/pom.xml
COPY connectors/library connectors/library
COPY connectors/ma-connector connectors/ma-connector
RUN mvn -B -ntp -N -f connectors/pom.xml install
RUN mvn -B -ntp -f connectors/library/pom.xml -DskipTests install
RUN mvn -B -ntp -f connectors/ma-connector/pom.xml -DskipTests package

FROM eclipse-temurin:21-jdk-jammy AS build-native
RUN apt-get update && apt-get install -y --no-install-recommends cmake g++ make

WORKDIR /native
COPY connectors/ma-connector/src/main/native/ ./
COPY --from=build-java /workspace/connectors/ma-connector/target/generated-sources/jni/ ./generated/

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build-java /workspace/connectors/ma-connector/target/ma-connector.jar /app/connector.jar
COPY --from=build-java /workspace/connectors/ma-connector/target/lib/ /app/lib/
COPY --from=build-native /native/build/libRevPiReader.so /app/native/
RUN mkdir -p /app/config /app/data

ENV LD_LIBRARY_PATH=/app/native

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.ma.MaConnectorApplication"]
