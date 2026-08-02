# System Architecture

## 1. System model

Aionn Backend is a modular monolith. Every business module owns its domain model, application flows, REST adapters, and infrastructure adapters. Modules run in one process but retain boundaries comparable to independent bounded contexts.

Dependencies point inward:

```text
adapter ---------> application ---------> domain
infrastructure --> application ---------> domain
app ------------> module wiring
```

The domain has no dependency on Spring, application, adapters, or infrastructure. The application layer must not import JPA entities, Spring Data repositories, provider SDKs, or infrastructure configuration classes.

## 2. Canonical module structure

```text
com.aionn.<module>/
  domain/
    model/
    valueobject/
    event/
    exception/
  application/
    service/
    usecase/
    policy/
    port/in/
    port/out/
    dto/
    mapper/
  adapter/rest/
    controller/
    dto/<feature>/request/
    dto/<feature>/response/
    mapper/
    exception/
    support/
  infrastructure/
    config/
    persistence/entity/
    persistence/repository/
    persistence/mapper/
    persistence/adapter/
    integration/<target-or-purpose>/
    scheduling/
```

Create a package only when it has a real responsibility. External providers may use explicit packages such as `infrastructure/provider`, `infrastructure/carrier`, `infrastructure/search`, or `infrastructure/media`.

## 3. Layer responsibilities

### Domain

- Owns aggregates, entities, value objects, invariants, domain events, and domain exceptions.
- Knows nothing about HTTP, JSON, databases, Spring, third-party SDKs, or other modules.
- State changes happen through behavior whose name expresses business intent.

### Application

- `service`: owns business orchestration and substantive application flows.
- `port/in`: defines contracts invoked by primary adapters.
- `usecase`: provides thin input-port implementations that delegate to services and map domain objects to results.
- `port/out`: defines persistence, provider, and other secondary-adapter contracts.
- `policy`: contains pure rules or application-facing interfaces. Implementations that read Spring properties belong in infrastructure.

Application services depend only on domain types, application ports and policies, and valid shared-kernel contracts. A type named `*PersistencePort` is not a Spring Data repository; it is an application abstraction implemented by infrastructure.

### Adapter

- Controllers inject input or query ports, never application services.
- Requests and responses are HTTP contracts and must not double as application or domain models.
- Controllers map requests to commands or queries and results to responses through DTO mappers.
- Event listeners receiving input from outside the module are primary adapters even when they are not REST endpoints.

### Infrastructure

- Owns JPA entities and repositories, persistence adapters, provider clients, security implementations, Spring configuration, and schedulers.
- Persistence adapters are the only boundary that converts between JPA entities and domain models.
- A class that reads `@ConfigurationProperties` belongs in infrastructure and implements an application policy or port when application code needs that value.

## 4. Module boundaries

- A business module must not import classes from another business module.
- Synchronous cross-module communication uses interfaces in `shared-kernel/integration/port`.
- Asynchronous communication uses integration events from the shared kernel.
- Do not move a module's private domain model into the shared kernel to bypass the dependency rule.
- Adapters implementing shared integration ports need globally unique Spring bean names. Use a distinctive class name or an explicit component name.

## 5. Transactions and external I/O

Database-only write operations define their transaction boundary at the application service. Database reads may use read-only transactions.

Never perform HTTP, payment, carrier, SMS/email, captcha, KYC, media, or search-engine calls while a database transaction is open. A read-only transaction is still a transaction and is not an exception.

A `read database -> call provider -> write database` flow is split into phases:

1. Read an immutable snapshot in a short transaction.
2. End the transaction and call the external provider.
3. Persist the outcome in a new short transaction.

Use a non-transactional orchestrator, or a `NOT_SUPPORTED` method with explicit `TransactionTemplate` phases. Do not call a transactional method on the same class through `this`; self-invocation bypasses the Spring proxy.

An in-process cross-module adapter is not automatically external I/O. Classify it by its implementation, not by its type name.

## 6. Events and the outbox

- Aggregates register domain events when a state change requires a side effect.
- Services persist the aggregate, retrieve events through `pullEvents()`, and publish them with `EventPublisher`.
- Events requiring durable delivery use the transactional outbox so the mutation and event commit or roll back together.
- Integration events have a stable `eventId`, an explicit aggregate scope or ordering key, and backward-compatible payloads.
- Consumers are idempotent because delivery is at least once.
- Non-rollbackable provider effects use an idempotency key derived from the `eventId` or a stable business-operation identifier.

## 7. Schedulers

- `@Scheduled` exists only under `infrastructure/scheduling`.
- Jobs that must run on one application instance use ShedLock with a globally unique lock name.
- `lockAtMostFor` exceeds the measured worst-case runtime; `lockAtLeastFor` is used only when closely spaced runs must be prevented.
- Per-item processing that needs an independent transaction separates the scheduler from a worker so calls pass through a Spring proxy.
- The outbox dispatcher does not use a singleton lock because it coordinates workers with row locks, leases, and `FOR UPDATE SKIP LOCKED`.

## 8. Definition of done for a module or feature

- Dependencies point in the permitted direction and architecture tests pass.
- No external I/O occurs inside a database transaction.
- REST exposes request and response contracts, not application results or domain models.
- Database changes have immutable Flyway migrations.
- Configuration uses type-safe `@ConfigurationProperties` and appropriate environment validation.
- Unit, integration, and controller tests cover important behavior and failure paths.
- The module is wired into `app`, its configuration is imported, and the application context starts.
- The build, Sonar quality gate, and relevant E2E checks pass.
