# syntax=docker/dockerfile:1.6

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -e -ntp clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/skyrescue-api.jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1001 skyrescue \
    && useradd  --system --uid 1001 --gid 1001 --home-dir /app --shell /usr/sbin/nologin skyrescue

WORKDIR /app

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

COPY --from=build --chown=skyrescue:skyrescue /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=skyrescue:skyrescue /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=skyrescue:skyrescue /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=skyrescue:skyrescue /workspace/target/extracted/application/ ./

USER 1001:1001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
