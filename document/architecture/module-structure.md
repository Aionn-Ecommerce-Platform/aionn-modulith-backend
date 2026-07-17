# Canonical Module Structure Specification

This document defines the canonical software architecture and directory layout conventions for all business modules in the Aionn Modulith Backend (e.g., `identity`, `catalog`, `inventory`, and all future modules to be migrated: `ordering`, `payment`, `shipping`, `notification`, `promotion`, `chat`).

We adhere strictly to a **clean, hexagonal architecture** decomposed into four primary layers: `domain`, `application`, `adapter`, and `infrastructure`, using **`identity`** as the architectural source of truth.

---

## 1. Directory Blueprint

A standard module must follow the package layout below, using the **`identity`** module as the model template:

```text
com.aionn.identity
│
├── domain/                         # Pure business logic (Framework-agnostic)
│   ├── model/                      # Rich domain entities & aggregates (e.g., User, Address, Consent)
│   ├── valueobject/                # Domain value objects & enums (e.g., UserRole, UserStatus, Phone)
│   ├── event/                      # Internal domain events (e.g., UserEvents)
│   └── exception/                  # Domain-specific exceptions (e.g., IdentityException, IdentityErrorCode)
│
├── application/                    # Application orchestration & ports
│   ├── service/                    # Business services containing real orchestration logic & transaction boundaries (e.g., AddressService)
│   ├── usecase/                    # Thin wrappers implementing Input Ports, delegating to Services, mapping to *Result (e.g., CreateAddressUseCase)
│   ├── policy/                     # Application-specific policies/rules (e.g., AddressPolicy)
│   ├── port/
│   │   ├── in/                     # Incoming ports / Use case interfaces (e.g., CreateAddressInputPort)
│   │   └── out/                    # Outgoing ports / SPIs (e.g., UserPersistencePort, AddressPersistencePort)
│   ├── dto/
│   │   ├── command/                # Application write DTOs (e.g., CreateAddressCommand)
│   │   ├── query/                  # Application read DTOs (e.g., ListAddressesQuery)
│   │   └── result/                 # Application return DTOs (e.g., AddressResult)
│   └── mapper/                     # MapStruct mappers (Domain Entity <-> Result DTO) (e.g., AddressResultMapper)
│
├── adapter/                        # Primary adapters (REST, Web, Event Handlers)
│   └── rest/
│       ├── controller/             # REST controllers exposing APIs (e.g., AddressController)
│       ├── dto/                    # Controller-level request and response DTOs
│       │   ├── request/            # Inbound HTTP payloads (e.g., CreateAddressRequest)
│       │   └── response/           # Outbound HTTP payloads (e.g., AddressResponse)
│       ├── mapper/                 # MapStruct mappers (Request -> Command, Result -> Response) (e.g., AddressDtoMapper)
│       └── exception/              # REST controller exception advice
│
└── infrastructure/                 # Secondary adapters & framework configuration
    ├── persistence/                # Database integration layer
    │   ├── entity/                 # Database entity mappings (e.g., UserEntity, AddressEntity)
    │   ├── repository/             # Spring Data JPA repositories (extends JpaRepository)
    │   ├── adapter/                # Implements persistence ports (e.g., AddressPersistenceAdapter)
    │   └── mapper/                 # MapStruct mappers (Domain Model <-> JPA Entity) (e.g., AddressDomainMapper)
    ├── integration/                # Cross-module communication (e.g., IdentityAccessTokenVerifierAdapter)
    ├── scheduling/                 # Background tasks, cron jobs, and cleanups (e.g., AuthSessionCleanupScheduler)
    ├── security/                   # Spring Security filters and configurations (e.g., BearerAuthenticationFilter)
    └── config/                     # Module-specific Spring configurations
```

---

## 2. Core Architectural Principles

### A. The Dependency Rule
Dependencies must always point inwards:
$$\text{Adapter} \longrightarrow \text{Infrastructure} \longrightarrow \text{Application} \longrightarrow \text{Domain}$$
The **Domain** layer must remain pure, with zero dependencies on Spring Framework annotations or other modules.

### B. Business Logic Orchestration: Service vs. UseCase
To align with the architecture demonstrated in the `identity` module:
1.  **Service Classes** (e.g., `AddressService`): These classes contain **all the actual business logic**, coordinate domain models, manage transaction boundaries (`@Transactional`), and call output ports.
2.  **UseCase Classes** (e.g., `CreateAddressUseCase`): These are **thin wrappers** that implement the incoming ports (`CreateAddressInputPort`). They inject the relevant Service, call the service method, and map the returned Domain Model/Internal DTO to the target `*Result` DTO.

### C. REST Layer: Strict Response Encapsulation
Application-level `*Result` objects **must never** be serialized directly to JSON. Exposing internal application data structures tightly locks the API contract.
*   **Controller signatures** must return `ApiResponse<XxxResponse>`, never `ApiResponse<XxxResult>`.
*   A MapStruct mapper in the adapter layer (e.g., `XxxDtoMapper`) is responsible for translating the `*Result` DTO into the adapter-specific `*Response` DTO.
*   Enums must be converted to strings (e.g., `.name()`) in the DTO mapper to prevent domain enums from leaking into REST clients.

### D. REST DTO Packaging
Always enforce separate packages for input payloads and output payloads inside the adapter:
*   `adapter/rest/dto/<feature>/request/*Request.java`
*   `adapter/rest/dto/<feature>/response/*Response.java`

`PageResult<T>` is a shared generic wrapper for paginated collections. Do not build adapter-specific page wrappers; instead, construct `PageResult<XxxResponse>` directly in the controller using MapStruct.

### E. Scheduling & Worker Isolation
*   All `@Scheduled` annotations must be localized within `infrastructure/scheduling`.
*   **Self-Invocation Gotcha**: Spring's `@Transactional` is proxy-based. Calling a `@Transactional(propagation = Propagation.REQUIRES_NEW)` method from within the same class bypasses the proxy, disabling transaction boundaries.
*   When executing per-item processing in an isolated transaction, separate the logic into a dedicated **Worker** class (e.g., `ReservationAutoReleaseWorker` injected into `ReservationAutoReleaseScheduler`).
*   For bulk operations executed in a single transaction, a single scheduler class is sufficient (e.g., `AuthSessionCleanupScheduler`).

### F. Eventing Architecture
Choose the eventing pattern based on requirements:
1.  **Imperative Integration Event Port**: Useful when the module does not have internal side effects that need decoupling. The Application Service directly invokes an output port (e.g., `IdentityIntegrationEventPublisherPort`) to publish integration events directly to external modules.
2.  **Aggregate Domain Events (Transactional Outbox)**: Useful when side effects must be decoupled from the primary business logic (e.g., updating search indexes or handling notifications asynchronously).
    *   The Domain Aggregate registers events using `registerEvent(...)`.
    *   The Service saves the aggregate, pulls events via `pullEvents()`, and publishes them internally using `EventPublisher`.
    *   Internal listeners (`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`) process the events.
    *   An integration event publisher in `infrastructure/integration` converts internal domain events to public integration events using an `IntegrationEventMapper` and publishes them to the message broker.

---

## 3. Shared Kernel Interactivity Rules

To maintain strict modular boundaries, individual modules are **forbidden** from directly importing code from other modules. Instead, all cross-module interactions and common utilities must go through the `:shared-kernel` library.

The components permitted for usage from the `:shared-kernel` are:

### A. Cross-Module Ports (Integration Interfaces)
*   **Integration Ports** (under `com.aionn.sharedkernel.integration.port.*`): Define the interfaces for cross-module queries or command dispatch (e.g., `MerchantQueryPort`, `IdentityAddressLookupAdapter` in identity implements `AddressLookupPort` from shared-kernel).

### B. Core Utilities & Base Classes
*   **`IdGenerator`** (`com.aionn.sharedkernel.util.IdGenerator`): Used to generate ULIDs for entity and aggregate primary keys. Do not hardcode custom ULID generator libraries.
*   **`ApiResponse`** (`com.aionn.sharedkernel.adapter.web.response.ApiResponse`): The standard wrapper for all REST API response bodies.
*   **`EventPublisher`** (`com.aionn.sharedkernel.application.port.EventPublisher`): The standard publisher for dispatching domain events internally.
*   **`EventEnvelope`** (`com.aionn.sharedkernel.domain.model.EventEnvelope`): The wrapper metadata class for all published events.
*   **Common Value Objects** (under `com.aionn.sharedkernel.domain.vo.*`): Shared value objects such as `PhoneNumber` or base pagination parameters.

---

## 4. Coding & Implementation Guidelines for AI

When generating code for a new module, strictly adhere to the following technical guidelines to ensure seamless compilation and test execution:

### A. Database Transactions (`@Transactional`)
*   **Location**: Always place the `@Transactional` annotations on the **Application Services** (in `application/service/`), as they hold the orchestrating business logic.
*   Use `@Transactional(readOnly = true)` for read-only query methods in services.
*   Avoid placing `@Transactional` directly on domain models or infrastructure adapters (unless handling specific propagation like `REQUIRES_NEW`).

### B. Persistence Layer Separation
*   **Strict Separation**: Application Services and Domain Models **must never** import or reference JPA Entities (`*Entity` classes under `infrastructure/persistence/entity/*`).
*   **The Adapter's Role**: The Persistence Adapter (e.g., `AddressPersistenceAdapter`) acts as the gatekeeper. It must fetch the JPA Entity, map it to the pure Domain Model using a MapStruct `*DomainMapper`, and return the Domain Model to the Application Service.
*   When saving, the Adapter maps the modified Domain Model back into a JPA Entity before invoking Spring Data JPA repositories.

### C. MapStruct & Lombok Integration
*   All mappers must be declared as interfaces annotated with `@Mapper(componentModel = "spring")`.
*   Ensure that you compile the project (`.\gradlew compileJava`) after creating or editing mappers to generate the MapStruct implementation classes (`*Impl.java`) before running tests.
