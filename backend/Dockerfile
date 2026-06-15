# ─────────────────────────────────────────────────────────────────
# Stage 1 : Build
# ─────────────────────────────────────────────────────────────────

FROM eclipse-temurin:17 AS builder

WORKDIR /workspace

# 의존성 캐싱: Gradle wrapper + 설정 파일만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# 소스 복사 후 빌드 (테스트 제외)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test --stacktrace

# ─────────────────────────────────────────────────────────────────
# Stage 2 : Run
# ─────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17 AS runner

# 보안: non-root 실행
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

USER appuser

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

# t2.small (1 vCPU, 2 GB) 기준 JVM 튜닝
# - Xms/Xmx: MySQL + OS가 ~1 GB 차지하므로 앱에 512 MB 할당
# - G1GC: 짧은 GC pause 우선
# - ExitOnOutOfMemoryError: OOM 시 컨테이너 재시작으로 복구
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
