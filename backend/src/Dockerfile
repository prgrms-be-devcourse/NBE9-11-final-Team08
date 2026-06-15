# ─────────────────────────────────────────────────────────────────
# Stage 1 : Build
# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q 2>/dev/null || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test -q

# ─────────────────────────────────────────────────────────────────
# Stage 2 : Run
# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

# curl 설치 (Docker health check에 필요)
RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

# t2.small (1 vCPU, 2 GB) JVM 튜닝
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
