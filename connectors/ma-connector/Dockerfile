FROM eclipse-temurin:21-jdk-jammy AS build-native
RUN apt-get update && apt-get install -y --no-install-recommends cmake g++ make

WORKDIR /native
COPY src/main/native/ ./
COPY target/generated-sources/jni/ ./generated/

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY ./target/ma-connector.jar /app/connector.jar
COPY ./target/lib/ /app/lib/
COPY --from=build-native /native/build/libRevPiReader.so /app/native/
RUN mkdir -p /app/config /app/data

ENV LD_LIBRARY_PATH=/app/native

ENTRYPOINT ["java","-cp","/app/connector.jar:/app/lib/*","at.pegelhub.connector.ma.MaConnectorApplication"]
