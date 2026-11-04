FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    && cp target/veylora-0.0.1-SNAPSHOT.jar app.jar

FROM eclipse-temurin:25-jre

WORKDIR /app
ENV APP_PORT=8080 \
    AUDIO_STORAGE_DIR=/app/uploads/audio

RUN mkdir -p /app/uploads/audio \
    && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
