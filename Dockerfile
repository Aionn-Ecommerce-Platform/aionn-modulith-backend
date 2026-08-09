# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy@sha256:55fb9bf738f5d9b4a6c01b39337e3070d3e27370dd3c478fd1d5d3cd2233c6d8 AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
COPY app/build.gradle app/build.gradle
COPY shared-kernel/build.gradle shared-kernel/build.gradle
COPY modules modules
RUN --mount=type=cache,target=/root/.gradle ./gradlew :app:dependencies --no-daemon

COPY app/src app/src
COPY shared-kernel/src shared-kernel/src
RUN --mount=type=cache,target=/root/.gradle ./gradlew :app:bootJar --no-daemon \
    && cp app/build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine@sha256:3f08b13888f595cc49edabea7250ba69499ba25602b267da591720769400e08c AS runtime
RUN apk add --no-cache curl \
    && addgroup -S -g 10001 aionn \
    && adduser -S -D -H -u 10001 -G aionn aionn

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
