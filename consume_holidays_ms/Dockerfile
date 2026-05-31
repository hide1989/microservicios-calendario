# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package && \
    cp target/calendario-*.jar /build/calendario.jar

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /build/calendario.jar /app/calendario.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/calendario.jar"]
