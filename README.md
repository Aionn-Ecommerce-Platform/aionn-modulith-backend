# Aionn Modulith Backend

Aionn is a Spring Boot modular monolith for an e-commerce platform. The repository contains the application composition root, a small shared kernel, and independently bounded business modules.

## Modules

- `identity`
- `catalog`
- `inventory`
- `ordering`
- `payment`
- `shipping`
- `notification`
- `promotion`
- `chat`

`app` starts Spring Boot and wires the modules. Business rules stay inside their owning module. `shared-kernel` contains only contracts and primitives that are genuinely shared.

## Local development

Requirements:

- JDK 21
- Docker Desktop

Create the local environment file:

```powershell
Copy-Item .env.example .env
```

Start PostgreSQL, Redis, and OpenSearch:

```powershell
docker compose --env-file .env -f docker/docker-compose.yml up -d
```

Run the application with production and demo migrations enabled:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
Get-Content .env | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
./gradlew.bat :app:bootRun
```

The API starts on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Verification

```powershell
./gradlew.bat build
./gradlew.bat :app:test --tests com.aionn.config.FlywayMigrationIntegrationTest
```

The Flyway integration test uses Testcontainers and requires Docker.

## Documentation

- [Architecture](document/architecture.md): module boundaries, dependencies, transactions, events, and schedulers.
- [Coding conventions](document/coding-conventions.md): Java, REST, validation, mapping, configuration, and time conventions.
- [Testing](document/testing.md): test strategy, coverage, and quality gates.
- [Operations and data](document/operations-and-data.md): runtime configuration, databases, outbox operations, dependencies, and local commands.

Documentation describes the current system. Historical migration plans and completed checklists do not belong in the repository.
