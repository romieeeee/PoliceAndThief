# =========================
# 1️⃣ Build stage
# =========================
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Gradle 캐시 최적화
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon


# =========================
# 2️⃣ Runtime stage
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드 결과물 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 포트
EXPOSE 8080

# JVM 옵션 (운영/개발 공통 안전값)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
