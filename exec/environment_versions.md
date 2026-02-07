# S14P11D104 프로젝트 개발 및 배포 매뉴얼

## 목차
1. [개발환경](#1-개발환경)
    *   [1.1. Frontend (Android / Kotlin)](#11-frontend-android--kotlin)
    *   [1.2. Backend (Java / Spring Boot)](#12-backend-java--spring-boot)
    *   [1.3. Backend (Node.js)](#13-backend-nodejs)
    *   [1.4. AI Server (Python)](#14-ai-server-python)
    *   [1.5. Database & Infrastructure](#15-database--infrastructure)
    *   [1.6. IDE & Tools](#16-ide--tools)
2. [환경변수](#2-환경변수)
    *   [2.1. Frontend (local.properties)](#21-frontend-localproperties)
    *   [2.2. Backend (.env, application.yml)](#22-backend-env-applicationyml)
3. [EC2 세팅](#3-ec2-세팅)
    *   [3.1. Docker Engine 설치](#31-docker-engine-설치)
    *   [3.2. SSL 인증서 발급](#32-ssl-인증서-발급)
    *   [3.3. EC2 포트 정리](#33-ec2-포트-정리)
    *   [3.4. 방화벽(UFW) 설정](#34-방화벽ufw-설정)
4. [빌드 및 배포](#4-빌드-및-배포)
    *   [4.1. Nginx Reverse Proxy](#41-nginx-reverse-proxy)
    *   [4.2. Frontend (Android)](#42-frontend-android)
    *   [4.3. Backend (Spring Boot)](#43-backend-spring-boot)
    *   [4.4. Backend (Node.js)](#44-backend-nodejs)
    *   [4.5. AI Server (Python)](#45-ai-server-python)
    *   [4.6. DB (EC2 Local)](#46-db)
        *   [4.6.1. Docker Network 생성](#461-docker-network-생성)
        *   [4.6.2. PostgreSQL (PostGIS)](#462-postgresql-postgis)
        *   [4.6.3. MongoDB](#463-mongodb)
        *   [4.6.4. Redis](#464-redis)
        *   [4.6.5. RabbitMQ](#465-rabbitmq)
    *   [4.7. docker-compose.yml](#47-docker-composeyml)
5. [외부서비스](#5-외부서비스)
    *   [5.1. 소셜 로그인 - Kakao](#51-소셜-로그인---kakao)
    *   [5.2. AWS S3](#52-aws-s3)
    *   [5.3. LiveKit Cloud](#53-livekit-cloud)

---

## 1. 개발환경
### 1.1. Frontend (Android / Kotlin)
*   **Platform**: Android
*   **Language**: Kotlin (JVM Target 11)
*   **SDK Versions**:
    *   `compileSdk`: 36
    *   `targetSdk`: 36
    *   `minSdk`: 24
*   **Framework**: Jetpack Compose (via AndroidX Compose BOM)
*   **Key Dependencies**:
    *   `retrofit`: 2.9.0 (HTTP Client)
    *   `okhttp`: 4.12.0
    *   `room`: 2.6.1 (Local Database)
    *   `play-services-maps`: 18.2.0 (Google Maps)
    *   `play-services-location`: 21.1.0
    *   `maps-compose`: 4.3.3
    *   `camera-camera2`: 1.3.1 (CameraX)
    *   `mlkit:barcode-scanning`: 17.2.0 (QR Code)
    *   `livekit-android`: 2.9.0
    *   `firebase-messaging-ktx`: (Firebase Cloud Messaging)
    *   `coil-compose`: 2.5.0 (Image Loading)
    *   `timber`: 5.0.1 (Logging)

### 1.2. Backend (Java / Spring Boot)
*   **Java**: OpenJDK 17
*   **Framework**: Spring Boot 3.3.6
*   **Build Tool**: Gradle (Wrapper 사용)
*   **Key Dependencies**:
    *   `spring-boot-starter-data-jpa`: 3.3.6 (Hibernate Core 6.5.x)
    *   `spring-boot-starter-security`: 3.3.6 (Spring Security 6.3.x)
    *   `spring-boot-starter-data-redis`: 3.3.6
    *   `spring-boot-starter-data-mongodb`: 3.3.6
    *   `spring-boot-starter-amqp` (RabbitMQ): 3.3.6
    *   `jjwt-api`: 0.11.5 (JWT)
    *   `software.amazon.awssdk:s3`: 2.21.1 (AWS S3)
    *   `io.livekit:livekit-server`: 0.8.0
    *   `hibernate-spatial`: 6.5.x (PostGIS 지원)
    *   `springdoc-openapi-starter-webmvc-ui`: 2.6.0 (Swagger)

### 1.3. Backend (Node.js)
*   **Runtime**: Node.js 24.12.0 (Alpine Linux 기반 Docker)
*   **Services**:
    *   `websocket_server`
    *   `gps_consumer_server`
    *   `gps_worker`
*   **Key Dependencies**:
    *   `express`: 5.2.1
    *   `socket.io`: 4.8.3
    *   `mongoose`: 9.1.5 (MongoDB ODM)
    *   `sequelize`: 6.37.7 (PostgreSQL ORM)
    *   `ioredis`: 5.9.2
    *   `redis`: 5.10.0
    *   `redis-streams-nodejs`: 1.1.5
    *   `@socket.io/redis-adapter`: 8.3.0
    *   `@socket.io/redis-emitter`: 5.1.0
    *   `winston`: 3.19.0 (Logging)
    *   `pm2`: 6.0.14 (Process Manager)
    *   `@turf/turf`: 7.3.2 (Geospatial Analysis)

### 1.4. AI Server (Python)
*   **Framework**: FastAPI 0.109.0
*   **Runtime**: Python 3.10+ (Inferred)
*   **Key Dependencies**:
    *   **Machine Learning / Vision**:
        *   `torch`: 2.5.1+cu121 (PyTorch with CUDA 12.1)
        *   `torchvision`: 0.20.1+cu121
        *   `ultralytics`: 8.4.9 (YOLOv8)
        *   `opencv-python`: 4.13.0.90
        *   `pillow`: 12.0.0
    *   **LLM / LangChain**:
        *   `langchain-core`: 1.2.7
        *   `langgraph`: 1.0.7
        *   `openai`: 2.16.0
    *   **Backend / Server**:
        *   `uvicorn`: 0.27.0
        *   `websockets`: 16.0
        *   `pydantic`: 2.12.5
        *   `requests`: 2.32.5

### 1.5. Database & Infrastructure
*   **OS**: Ubuntu 24.04.3 LTS (Noble)
*   **Docker**: 29.1.5 
*   **Docker Compose**: 1.29.2 
*   **Web Server**: Nginx `stable-alpine`
*   **Databases**:
    *   **PostgreSQL**: `postgis/postgis:15-3.3` (PostgreSQL 15 + PostGIS 3.3)
    *   **MongoDB**: `mongo:6.0`
    *   **Redis**: `redis:7-alpine`
    *   **RabbitMQ**: `rabbitmq:3-management-alpine`

### 1.6. IDE & Tools
*   **IDE**: IntelliJ IDEA, VS Code, Android Studio
*   **SCM**: GitLab
*   **Issue Tracker**: Jira
*   **Design**: Figma

## 2. 환경변수
### 2.1. Frontend (`local.properties`)
*   `MAPS_API_KEY`
*   `KAKAO_NATIVE_APP_KEY`
*   `LIVEKIT_URL`

### 2.2. Backend (`.env`, `application.yml`)
*   `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB_NAME`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
*   `MONGO_HOST`, `MONGO_PORT`, `MONGO_DB_NAME`, `MONGO_USER`, `MONGO_PASSWORD`
*   `REDIS_HOST`, `REDIS_PORT`
*   `MQ_HOST`, `MQ_PORT`, `MQ_USER`, `MQ_PASSWORD`, `MQ_MISSION`, `MQ_ALARM`, `MQ_NEWS`, `EXCHANGE_NAME`
*   `JWT_SECRET`
*   `KAKAO_APP_ID`
*   `AWS_S3_BUCKET_NAME_SECRET`, `AWS_ACCESS_KEY_SECRET`, `AWS_SECRET_KEY_SECRET`, `AWS_REGION_SECRET`
*   `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`
*   `SPRING_BOOT_URL`

## 3. EC2 세팅
### 3.1. Docker Engine 설치
**1. 필수 패키지 설치 및 GPG Key 등록**
```bash
# 구 버전 삭제
sudo apt-get remove docker docker-engine docker.io containerd runc

# 패키지 업데이트 및 필수 패키지 설치
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# Docker 공식 GPG Key 추가
sudo mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Repository 설정 (Ubuntu 24.04 / noble)
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

**2. Docker Engine 설치**
```bash
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 설치 확인
sudo docker version
sudo docker compose version
```

**3. Docker 권한 설정 (sudo 없이 실행)**
```bash
sudo usermod -aG docker $USER
newgrp docker
```

### 3.2. SSL 인증서 발급
**Nginx는 `docker-compose.yml`을 통해 컨테이너로 실행합니다.** (Host 직접 설치 X)

**1. SSL 인증서 발급 (Certbot 이용)**
```bash
# Certbot 설치
sudo snap install core; sudo snap refresh core
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot

# 인증서 발급 (Nginx 중지 상태에서 실행 권장 또는 webroot 방식 사용)
# -d 뒤에 도메인 주소 입력
sudo certbot certonly --standalone -d your-domain.com
```

**2. 인증서 경로 확인**
*   발급된 인증서는 `/etc/letsencrypt/live/your-domain.com/` 위치에 저장됩니다.
*   이 경로를 `docker-compose.yml`의 Nginx 볼륨으로 마운트하여 사용합니다.

### 3.3. EC2 포트 정리
*   **Inbound**:
    *   80 (HTTP)
    *   443 (HTTPS)
    *   22 (SSH)
    *   **8930 (RabbitMQ)** - 외부 연동 필요 시
*   **Internal (Docker Network)**:
    *   8080 (Spring Boot)
    *   8090-8091 (Socket Server)
    *   8095 (Consumer Server)
    *   8930 (RabbitMQ)

### 3.4. 방화벽(UFW) 설정
```bash
# 기본 정책 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH, HTTP, HTTPS, RabbitMQ 허용
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8930/tcp

# UFW 활성화
sudo ufw enable
sudo ufw status
```

## 4. 빌드 및 배포

### 4.1. Nginx Reverse Proxy
```bash
# 1. Nginx 설정 파일 및 인증서 디렉토리 확인
# /home/ubuntu/nginx/conf.d  (설정 파일 위치)
# /etc/letsencrypt           (SSL 인증서 위치)

# 2. Docker 컨테이너 실행
docker run -d \
  --name nginx-proxy \
  -p 80:80 \
  -p 443:443 \
  -v ./nginx/conf.d:/etc/nginx/conf.d \
  -v /etc/letsencrypt:/etc/letsencrypt \
  --network backend-network \
  --restart always \
  nginx:stable-alpine

# 3. 설정 재로딩 (필요 시)
docker exec nginx-proxy nginx -s reload
```

### 4.2. Frontend (Android)
```bash
# 1. 프로젝트 클론
git clone https://lab.ssafy.com/s14-webmobile4-sub1/S14P11D104.git
cd S14P11D104/fe/D104

# 2. local.properties 생성 (SDK 경로 및 API 키 설정)
echo "sdk.dir=/Users/username/Library/Android/sdk" > local.properties
echo "MAPS_API_KEY=YOUR_KEY" >> local.properties
echo "KAKAO_NATIVE_APP_KEY=YOUR_KEY" >> local.properties
echo "LIVEKIT_URL=wss://your-livekit-url" >> local.properties

# 3. APK 빌드
./gradlew assembleRelease

# 4. 생성된 APK 확인
# app/build/outputs/apk/release/app-release.apk
```

### 4.3. Backend (Spring Boot)
```bash
# 1. 프로젝트 이동
cd S14P11D104/be/pnt_spring

# 2. Dockerfile 생성
vi Dockerfile
##########
FROM gradle:8.14-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN gradle dependencies || true
COPY src src
RUN gradle clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
##########

# 3. Docker 이미지 빌드
docker build -t spring-backend .

# 4. 컨테이너 실행
docker run -d -p 8080:8080 \
  --name spring-backend \
  --env-file ../../.env \
  spring-backend
```

### 4.4. Backend (Node.js)
```bash
# [Websocket Server]
cd S14P11D104/be/pnt_node/websocket_server

# 1. Dockerfile 생성
vi Dockerfile
##########
FROM node:24.12.0-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install -g pm2 && npm install
COPY . .
EXPOSE 8090 8091
CMD ["pm2-runtime", "start", "ecosystem.config.js"]
##########

# 2. 빌드 및 실행
docker build -t websocket-server .
docker run -d -p 8090:8090 -p 8091:8091 \
  --name websocket-server \
  --env-file ../../../.env \
  websocket-server

# [Consumer Server]
cd S14P11D104/be/pnt_node/consumer_server
# (Dockerfile 내용은 위와 유사, 포트 8095)
docker build -t consumer-server .
docker run -d -p 8095:8095 \
  --name consumer-server \
  --env-file ../../../.env \
  consumer-server

# [GPS Worker / GPS Consumer]
# (경로 이동 후 동일한 방식으로 빌드 및 실행)
```

### 4.5. AI Server (Python)
```bash
# 1. 프로젝트 이동
cd S14P11D104/ai

# 2. Dockerfile 생성
vi Dockerfile
##########
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
##########

# 3. 빌드 및 실행
docker build -t ai-server .
docker run -d -p 8000:8000 \
  --name ai-server \
  ai-server
```

### 4.6. DB
**모든 DB는 Docker 컨테이너로 실행하며, `backend-network`를 통해 서로 통신합니다.**

#### 4.6.1. Docker Network 생성
```bash
docker network create backend-network
```

#### 4.6.2. PostgreSQL (PostGIS)
```bash
# 1. 실행 (기본 포트 5432 -> 호스트 8900 매핑)
docker run -d \
  --name postgres-db \
  -p 8900:5432 \
  -e POSTGRES_USER=${POSTGRES_USER} \
  -e POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
  -e POSTGRES_DB=${POSTGRES_DB} \
  -e TZ=Asia/Seoul \
  -v postgres_data:/var/lib/postgresql/data \
  --network backend-network \
  --restart always \
  postgis/postgis:15-3.3

# 2. 실행 확인
docker logs postgres-db
```

#### 4.6.3. MongoDB
```bash
# 1. 실행 (기본 포트 27017 -> 호스트 8910 매핑)
docker run -d \
  --name mongodb \
  -p 8910:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=${MONGO_INITDB_ROOT_USERNAME} \
  -e MONGO_INITDB_ROOT_PASSWORD=${MONGO_INITDB_ROOT_PASSWORD} \
  -e TZ=Asia/Seoul \
  -v mongo_data:/data/db \
  --network backend-network \
  --restart always \
  mongo:6.0

# 2. 실행 확인
docker logs mongodb
```

#### 4.6.4. Redis
```bash
# 1. 실행 (기본 포트 6379 -> 호스트 8920 매핑)
docker run -d \
  --name redis-cache \
  -p 8920:6379 \
  -e TZ=Asia/Seoul \
  --network backend-network \
  --restart always \
  redis:7-alpine

# 2. 실행 확인
docker exec -it redis-cache redis-cli ping
# PONG 응답 시 정상
```

#### 4.6.5. RabbitMQ
```bash
# 1. 실행 (5672->8930, 15672->8931 매핑)
docker run -d \
  --name rabbitmq \
  -p 8930:5672 \
  -p 8931:15672 \
  -e RABBITMQ_DEFAULT_USER=${RABBITMQ_DEFAULT_USER} \
  -e RABBITMQ_DEFAULT_PASS=${RABBITMQ_DEFAULT_PASS} \
  -e TZ=Asia/Seoul \
  --network backend-network \
  --restart always \
  rabbitmq:3-management-alpine

# 2. Management UI 접속 확인
# http://[EC2_IP]:8931
```

### 4.7. docker-compose.yml
도커 이미지 빌드 및 실행을 자동화하는 파일 (db 까지 포함)

```yaml
services:
  # 1. Nginx
  nginx:
    image: nginx:stable-alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d
      - /etc/letsencrypt:/etc/letsencrypt
    depends_on:
      - spring-app
      - websocket-server
      - consumer-server
    networks:
      - backend-network

  # 2. PostgreSQL + PostGIS
  postgres:
    image: postgis/postgis:15-3.3
    container_name: postgres-db
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB_NAME}
      TZ: Asia/Seoul
    ports:
      - "8900:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB_NAME}" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend-network

  # 3. MongoDB
  mongodb:
    image: mongo:6.0
    container_name: mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD}
      TZ: Asia/Seoul
    ports:
      - "8910:27017"
    volumes:
      - mongo_data:/data/db
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    networks:
      - backend-network
    healthcheck:
      test: [ "CMD-SHELL", "mongosh -u $MONGO_INITDB_ROOT_USERNAME -p $MONGO_INITDB_ROOT_PASSWORD --authenticationDatabase admin --eval \"db.adminCommand('ping')\"" ]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  # 4. Redis
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "8920:6379"
    environment:
      - TZ=Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 5s
      timeout: 3s
      retries: 5
    networks:
      - backend-network

  # 5. RabbitMQ
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: rabbitmq
    ports:
      - "8930:5672"
      - "8931:15672"
    environment:
      RABBITMQ_DEFAULT_USER: ${MQ_USER}
      RABBITMQ_DEFAULT_PASS: ${MQ_PASSWORD}
      TZ: Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    healthcheck:
      test: [ "CMD", "rabbitmq-diagnostics", "check_running" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - backend-network

  # 6. Spring Boot Application
  spring-app:
    build:
      context: ./be/pnt_spring
      dockerfile: Dockerfile
    container_name: spring-backend
    ports:
      - "8080:8080"
    env_file:
      - .env
    environment:
      - TZ=Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - backend-network

  # 7. WebSocket Server
  websocket-server:
    build:
      context: ./be/pnt_node/websocket_server
      dockerfile: Dockerfile
    container_name: websocket-server
    ports:
      - "8090-8091:8090-8091"
    env_file:
      - .env
    environment:
      - TZ=Asia/Seoul
      - SPRING_BOOT_URL=http://spring-app:8080/spring
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
      - ./logs:/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      mongodb:
        condition: service_healthy
    networks:
      - backend-network

  # 8. Consumer Server
  consumer-server:
    build:
      context: ./be/pnt_node/consumer_server
      dockerfile: Dockerfile
    container_name: consumer-server
    ports:
      - "8095:8095"
    env_file:
      - .env
    environment:
      - TZ=Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      mongodb:
        condition: service_healthy
      websocket-server:
        condition: service_started
    networks:
      - backend-network

  # 9. GPS Worker
  gps-worker:
    build:
      context: ./be/pnt_node/gps_worker
      dockerfile: Dockerfile
    container_name: gps-worker
    env_file:
      - .env
    environment:
      - TZ=Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    depends_on:
      redis:
        condition: service_healthy
      websocket-server:
        condition: service_started
    networks:
      - backend-network

  # 10. GPS Consumer Server
  gps-consumer-server:
    build:
      context: ./be/pnt_node/gps_consumer_server
      dockerfile: Dockerfile
    container_name: gps-consumer-server
    env_file:
      - .env
    environment:
      - TZ=Asia/Seoul
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /etc/timezone:/etc/timezone:ro
    depends_on:
      redis:
        condition: service_healthy
      websocket-server:
        condition: service_started
    networks:
      - backend-network

networks:
  backend-network:
    driver: bridge

volumes:
  postgres_data:
  mongo_data:
```

**[Docker Compose 실행 명령어]**
```bash
# 1. 빌드 및 백그라운드 실행
docker-compose up -d --build

# 2. 컨테이너 상태 확인
docker-compose ps

# 3. 로그 확인
docker-compose logs -f
```

## 5. 외부서비스
### 5.1. 소셜 로그인 - Kakao
#### 5.1.1. 애플리케이션 생성
*   Kakao Developers 사이트에서 앱 생성

#### 5.1.2. 키 생성, 동의 항목 선택
*   REST API 키 발급 및 사용자 정보 동의 항목 설정

#### 5.1.3. application.yml 작성
*   Client ID, Redirect URI 등 설정 파일 작성

#### 5.1.4. 카카오로부터 사용자 정보 얻어오기
*   OAuth 2.0 흐름을 통한 Access Token 발급 및 사용자 프로필 조회

### 5.2. AWS S3
*   프로필 사진, 게시글 이미지 등 정적 파일 저장소로 활용
*   Spring Boot (`software.amazon.awssdk:s3`) 및 Node.js (`@aws-sdk/client-s3`)에서 업로드/다운로드 연동

### 5.3. LiveKit Cloud
*   실시간 음성/화상 채팅 및 화면 공유 기능 구현
*   Client SDK (Android, Web) 및 Server SDK (Node.js, Java)를 통한 Room/Participant 관리
