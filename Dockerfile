# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system trinity \
    && useradd --system --gid trinity --home-dir /app trinity

WORKDIR /app
COPY --from=builder --chown=trinity:trinity \
    /workspace/target/trinity-financial-api-0.0.1-SNAPSHOT.jar app.jar

USER trinity
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl --fail --silent http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
