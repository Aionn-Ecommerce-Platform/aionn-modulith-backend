# Aionn Modulith Backend

Aionn is a modern Spring Boot 3 **Modular Monolith** e-commerce platform engineered for high reliability, clean architectural boundaries, and next-generation **Agentic Commerce** interoperability via the **Universal Commerce Protocol (UCP)**.

## Module Structure

The project is structured as a multi-module Gradle build:

```text
aionn-modulith-backend/
├── app/                  # Application composition root, Spring Boot startup, security & configuration
├── shared-kernel/        # Inter-module contracts, application ports, shared Value Objects, outbox primitives
├── modules/
│   ├── identity/         # User accounts, authentication, JWT tokens, RBAC, address management
│   ├── catalog/          # Products, categories, brands, variants, OpenSearch indexing
│   ├── inventory/        # Stock levels, reservations, stock adjustments, low-stock thresholds
│   ├── ordering/         # Carts, orders, checkout lifecycle, pricing, order state machine
│   ├── payment/          # Payment methods, gateway transactions (VNPAY, Stripe sandbox), refunds
│   ├── shipping/         # Carriers, shipping methods, delivery rate calculation, tracking
│   ├── promotion/        # Coupons, vouchers, discount rules, promotion eligibility
│   ├── notification/     # Email, SMS, in-app notifications, event-driven alerts
│   ├── recommendation/   # Personalized product recommendations, collaborative filtering
│   ├── chat/             # Customer support chat, messaging persistence, WebSocket sessions
│   └── ucp/              # Universal Commerce Protocol adapter (Discovery, Cart, Checkout, Webhooks)
├── docs/                 # Architectural, convention, and operational documentation
├── docker/               # Local infrastructure (PostgreSQL, Redis, OpenSearch)
└── scripts/              # Development environment setup, seeding, and E2E test suites
```

---

## Local Development Setup

### Prerequisites

- **JDK 21** (e.g. Eclipse Temurin 21)
- **Docker Desktop** (for PostgreSQL, Redis, OpenSearch)
- **PowerShell 7+** (on Windows) or Bash

### 1. Environment Configuration

Copy the sample environment variables:

```powershell
Copy-Item .env.example .env
```

_(Alternatively, initialize per-module environment files in `envs/`: `powershell -ExecutionPolicy Bypass -File scripts/dev/init-local-env.ps1`)_

### 2. Start Infrastructure Dependencies

Spin up PostgreSQL 16, Redis 7, and OpenSearch 2.18:

```powershell
docker compose --env-file .env -f docker/docker-compose.yml up -d
```

### 3. Run the Backend Application

```powershell
# Set dev profile and export environment variables
$env:SPRING_PROFILES_ACTIVE = "dev"
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

./gradlew.bat :app:bootRun
```

_(Or run directly via script: `powershell -ExecutionPolicy Bypass -File scripts/dev/start-local-backend.ps1`)_

Once started:

- **API Base URL**: `http://localhost:8080`
- **Swagger / OpenAPI UI**: `http://localhost:8080/swagger-ui.html`
- **UCP Discovery Endpoint**: `http://localhost:8080/.well-known/ucp`
- **Health & Readiness Check**: `http://localhost:8080/actuator/health`

---

## Verification & Testing

### Architecture & Boundary Tests (ArchUnit)

Verifies that no cross-module persistence leaks or cyclic dependencies exist:

```powershell
./gradlew.bat :app:test --tests "com.aionn.arch.*"
```

### UCP Protocol Contract & Unit Tests

Executes offline schema validation tests, controller tests, and service tests:

```powershell
./gradlew.bat :modules:ucp:test
```

### Flyway Migration Tests

Tests all database migration scripts (V1.0 through V11.0) against a clean Testcontainers PostgreSQL instance:

```powershell
./gradlew.bat :app:test --tests com.aionn.config.FlywayMigrationIntegrationTest
```

### End-to-End Test Suite

Executes end-to-end operational test scripts across all 10 business modules:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/e2e/run-e2e-suite.ps1
```

### Production Build

Packages the executable bootJar:

```powershell
./gradlew.bat :app:bootJar
```

---

## Documentation

Comprehensive project documentation is organized in the [`docs/`](docs/) directory:

### Architecture

- [Modular Monolith Architecture](docs/architecture/modular-monolith.md): Module boundaries, port-based integration, Transactional Outbox, and ShedLock.
- [UCP Protocol Integration & Flow Guide](docs/architecture/ucp-protocol-flow.md): Deep dive into UCP agentic commerce flows, controllers, ports, and webhook dispatching.
- [UCP Capability Matrix](docs/architecture/ucp-capability-matrix.md): Status of advertised core capabilities and optional extensions.

### Conventions

- [Coding Conventions](docs/conventions/coding-conventions.md): Java 21 standards, REST guidelines, validation, and error envelopes.
- [Testing Strategy](docs/conventions/testing-strategy.md): Unit, integration, architecture, and E2E test guidelines.

### Operations

- [Operations & Data Guide](docs/operations/operations-and-data.md): Database schemas, Flyway migrations, outbox dispatching, and deployment operations.
