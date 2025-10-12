FROM eclipse-temurin:17-jdk-jammy AS build-native
RUN apt-get update && apt-get install -y --no-install-recommends cmake g++ make

WORKDIR /native
COPY src/main/native/ ./

RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY ./target/pegelhub-ma-connector-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build-native /native/build/libRevPiReader.so /app/lib/

ENV LD_LIBRARY_PATH=/app/lib

ENTRYPOINT ["java","-jar","/app/app.jar"]