# Veylora / Connectra Backend Foundation

This repository contains the backend foundation for the Connectra communication-management platform. The project is organized as a Spring Boot application with a modular service-driven architecture, designed to support contact management, dashboard analytics, device registration, campaigns, SMS/email flows, and real provider integrations in later phases.

## Architecture summary

The application is organized around a standard layered backend structure:

- `config/` - security, CORS, and runtime configuration
- `Controller/` - REST endpoints under `/api/v1`
- `Service/` - business logic and orchestration
- `Repository/` - Spring Data JPA repositories
- `Model/` - JPA entities and persistence models
- `dto/` - request/response payloads
- `Exception/` - common error handling

## Current Phase 1 foundation

Completed in this stage:

- secure API defaults for authentication endpoints
- environment-based PostgreSQL configuration
- sanitized configuration guidance with `.env.example`
- backend documentation and architecture overview
- clear separation between public auth endpoints and protected application endpoints

## Local setup

1. Copy `.env.example` to `.env` and fill in your real values.
2. Start PostgreSQL and create a local database named `veylora`.
3. Run:

```bash
./mvnw spring-boot:run
```

## Run with Docker

Build the image and run the container with the application settings from `.env`:

```bash
docker build -t veylora .
docker run --env-file .env -p 8080:8080 veylora
```

The container listens on port `8080`. Make sure `.env` includes the required application and database settings.

## Security baseline

- Auth endpoints are public by default: `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/csrf`.
- Other API routes require authentication.
- Database credentials, JWT secrets, and provider credentials are expected from environment variables instead of being committed to Git.

## Next planned work

Phase 2 will focus on:

- dashboard analytics APIs
- contact manage CRUD + import
- consent and suppression handling
- stronger admin/operator authorization flows

This is intentionally an incremental foundation for the broader Connectra platform, not a single monolith dump.
