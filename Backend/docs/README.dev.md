# Local Developer & Environment Guide

This document describes how to run, test, and develop the **Telegram-Inspired Real-Time Messaging Backend** built as a Spring Boot Modular Monolith.

## Architecture Overview

- **Framework**: Spring Boot 3.4.3 (Java 21)
- **Database**: PostgreSQL 16 + Flyway Migrations
- **Cache & Pub/Sub**: Redis 7
- **Object Storage**: MinIO (S3 Compatible)
- **Security & Realtime**: Spring Security (JWT) + Spring WebSocket (STOMP)

---

## 1. Quick Start with Infrastructure

To launch local PostgreSQL, Redis, and MinIO instances using Docker Compose:

```bash
make dev-up
# OR
docker compose -f docker-compose.dev.yml up -d
```

Service details:
- **PostgreSQL**: `localhost:5432` (db: `tuto_db`, user: `postgres`, pass: `postgres`)
- **Redis**: `localhost:6379`
- **MinIO S3**: `http://localhost:9000` (console: `http://localhost:9001`, user: `minioadmin`, pass: `minioadmin`)

---

## 2. Running the Backend Application

Run the Spring Boot application locally with the `dev` profile:

```bash
make run
# OR
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Once started:
- **OpenAPI / Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **WebSocket Endpoint**: `ws://localhost:8080/ws`

---

## 3. Core REST Endpoints

### Authentication
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### User Profile & Search
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `GET /api/v1/users/search?q={query}`
- `POST /api/v1/users/{id}/block`

### Conversations & Messages
- `GET /api/v1/conversations`
- `POST /api/v1/conversations/private`
- `GET /api/v1/conversations/{id}/messages`
- `POST /api/v1/conversations/{id}/messages`

### File Storage (S3 / MinIO)
- `POST /api/v1/files/upload-url` (Request presigned upload URL)
- `POST /api/v1/files/{id}/complete` (Notify upload completed)

---

## 4. Running Tests

Execute the unit test suite:

```bash
make test
# OR
./mvnw clean test
```
