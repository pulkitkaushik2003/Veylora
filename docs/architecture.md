# Architecture Overview

## System intent

The repository is being developed as a consent-based communication platform with a modular backend and future-ready integrations for messaging, calling, device management, and automation. The initial scope focuses on the backend foundation needed to support secure registration, authentication, database connectivity, and protected APIs.

## Module map

```text
veylora/
├─ src/main/java/com/example/veylora/
│  ├─ config/                 # Security, CORS, runtime config
│  ├─ Controller/             # REST API endpoints
│  ├─ Service/                # Core business logic
│  ├─ Repository/             # Data access layer
│  ├─ Model/                 # JPA entities
│  ├─ dto/                   # DTOs for request/response contracts
│  ├─ Exception/             # Global exception handling
│  └─ VeyloraApplication.java
├─ src/main/resources/
│  ├─ application.properties
│  └─ static/ + templates/
├─ docs/
│  └─ architecture.md
├─ .env.example
├─ README.md
└─ pom.xml
```

## Phase plan

### Phase 1: Foundation

- backend app configuration
- PostgreSQL datasource setup
- environment-based secrets management
- authentication and protected API defaults
- startup and documentation baseline

### Phase 2: Contacts and dashboard

- contact CRUD and import pipelines
- consent and suppression handling
- dashboard summaries and activity APIs
- role-based access checks for admin actions

### Phase 3: Device and call management

- device registration, heartbeat, and lifecycle tracking
- SIM profile management
- supported call initiation and call-state reporting

### Phase 4: Messaging and email

- provider interfaces for SMS and email
- mock providers for local development
- delivery tracking and retry handling

### Phase 5: Campaigns and audio

- campaign lifecycle and start/pause/resume workflows
- recipient preview and idempotency rules
- audio library and campaign assignments

### Phase 6: AI and automation

- optional speech and AI provider abstraction
- automation rules and follow-up execution
- safe deployment posture and consent checks

## Security principles

- Never hardcode secrets in source files.
- Public routes are restricted to authentication and health checks.
- Production traffic should be routed through HTTPS.
- Provider credentials and API keys should be injected through environment variables or a secret manager.
- Personal data must be minimized and masked where necessary in logs and operational output.

## Integration strategy

The project is intentionally designed to support provider abstraction later. The existing service layer is the correct place to insert calls to SMS, email, telephony, storage, and AI providers without coupling controllers directly to external APIs.
