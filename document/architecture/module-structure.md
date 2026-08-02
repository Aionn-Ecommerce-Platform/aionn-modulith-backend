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
    │   ├── catalog/                # Sub-package for Catalog integration
    │   ├── inventory/              # Sub-package for Inventory integration
    │   ├── payment/                # Sub-package for Payment integration
    │   ├── shipping/               # Sub-package for Shipping integration
    │   └── listener/               # Sub-package for Event Listeners
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
2.  **UseCase Classes** (e.g., `CreateAddressUseCase`): These are **thin wrappers** that implement the incoming ports (`CreateAddressInputPort`). They inject the relevant Service, call the service method, and map the returned Domain Model to the target `*Result` DTO.
3.  **Controller Injection Rule**: Controllers (`@RestController` under `adapter/rest/controller/`) **must never** inject Application Service classes directly. They must only inject the incoming ports (`*InputPort` interfaces) to maintain loose coupling between adapters and services.

#### Return Type Contract

The boundary between the two layers is defined by return type:

| Layer     | Returns                                                                            | Owns the mapper?             |
| :-------- | :--------------------------------------------------------------------------------- | :--------------------------- |
| Service   | **Domain entity** (`Address`, `Shipment`) or `PageResult<Entity>` / `List<Entity>` | No                           |
| UseCase   | `*Result` DTO                                                                      | Yes, injects `*ResultMapper` |
| InputPort | `*Result` DTO (signature never changes)                                            | —                            |

```java
// Service: returns the aggregate, no mapper dependency
@Transactional
public Shipment applyCancel(String shipmentId, String reason) { ... }

// UseCase: transaction boundary + mapping
@Override
@Transactional
public ShipmentResult execute(CancelShipmentCommand command) {
    return shipmentResultMapper.toResult(shipmentService.cancelShipment(command));
}
```

`@Transactional` stays on the **Service class** even though the UseCase also declares it. Listeners, schedulers, workers, and cross-module adapters call Services directly without passing through a UseCase, so removing the service-level annotation would leave those paths untransacted.

Two documented exceptions:

1.  **Pure projections.** A `*Result` with no corresponding domain entity (analytics aggregates, search envelopes, computed summaries) stays as the Service return type, because there is nothing to map from. Examples: `ProductSearchResult`, `BulkPriceUpdateResult`, `RatingSummary`, `ReviewEligibilityResult`, `LowStockAlertResult`. Such UseCases inject no mapper and pass the value through.
2.  **Outbound network flows.** A UseCase whose flow performs network I/O must **not** carry `@Transactional`. See section 4.A.

### C. REST Layer: Strict Response Encapsulation

Application-level `*Result` objects **must never** be serialized directly to JSON. Exposing internal application data structures tightly locks the API contract.

- **Controller signatures** must return `ApiResponse<XxxResponse>`, never `ApiResponse<XxxResult>`.
- A MapStruct mapper in the adapter layer (e.g., `XxxDtoMapper`) is responsible for translating the `*Result` DTO into the adapter-specific `*Response` DTO.
- Enums must be converted to strings (e.g., `.name()`) in the DTO mapper to prevent domain enums from leaking into REST clients.

### D. REST DTO Packaging

Always enforce separate packages for input payloads and output payloads inside the adapter:

- `adapter/rest/dto/<feature>/request/*Request.java`
- `adapter/rest/dto/<feature>/response/*Response.java`

`PageResult<T>` is a shared generic wrapper for paginated collections. Do not build adapter-specific page wrappers; instead, construct `PageResult<XxxResponse>` directly in the controller using MapStruct.

### E. Scheduling & Worker Isolation

- All `@Scheduled` annotations must be localized within `infrastructure/scheduling`.
- **Self-Invocation Gotcha**: Spring's `@Transactional` is proxy-based. Calling a `@Transactional(propagation = Propagation.REQUIRES_NEW)` method from within the same class bypasses the proxy, disabling transaction boundaries.
- When executing per-item processing in an isolated transaction, separate the logic into a dedicated **Worker** class (e.g., `ReservationAutoReleaseWorker` injected into `ReservationAutoReleaseScheduler`).
- For bulk operations executed in a single transaction, a single scheduler class is sufficient (e.g., `AuthSessionCleanupScheduler`).

### F. Eventing Architecture

Choose the eventing pattern based on requirements:

1.  **Imperative Integration Event Port**: Useful when the module does not have internal side effects that need decoupling. The Application Service directly invokes an output port (e.g., `IdentityIntegrationEventPublisherPort`) to publish integration events directly to external modules.
2.  **Aggregate Domain Events (Transactional Outbox)**: Useful when side effects must be decoupled from the primary business logic (e.g., updating search indexes or handling notifications asynchronously).
    - The Domain Aggregate registers events using `registerEvent(...)`.
    - The Service saves the aggregate, pulls events via `pullEvents()`, and publishes them internally using `EventPublisher`.
    - Internal listeners (`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`) process the events.
    - An integration event publisher in `infrastructure/integration` converts internal domain events to public integration events using an `IntegrationEventMapper` and publishes them to the message broker.

### G. Integration Layer Package Subdivision

To maintain clean modular boundaries and avoid cluttered directories, the `infrastructure/integration` layer in **all modules** must be structured into functional sub-packages matching target domains or patterns instead of keeping a flat directory. Common sub-packages include:

- `infrastructure/integration/catalog/`: Adapters for Catalog & Voucher services.
- `infrastructure/integration/inventory/`: Adapters for Stock reservation services.
- `infrastructure/integration/payment/`: Adapters for Payment gateways.
- `infrastructure/integration/shipping/`: Adapters for Shipping fulfillment.
- `infrastructure/integration/listener/`: Multi-module event listeners (e.g. `PaymentCapturedListener`, `ShipmentDeliveredListener`).

This sub-packaging structure is mandatory for all modules to ensure consistent package organization and clear domain isolation across the entire project codebase.

### H. Package Placement is Load-Bearing

The Sonar exclusion list in the root `build.gradle` matches on package paths. Placing a class outside its canonical package silently changes whether it is measured for coverage.

- Persistence adapters belong in `infrastructure/persistence/adapter/`, never in a shortened `infrastructure/adapter/`.
- Module-specific Spring configuration belongs in `infrastructure/config/`, including `@EnableConfigurationProperties` holders.
- Clients for third-party providers belong in a package that is expected to be tested, following `payment`'s `infrastructure/provider/` (Stripe, VNPay) and `shipping`'s `infrastructure/carrier/` (GHN). These are **not** excluded from coverage and require real tests.
- Before inventing any new `infrastructure/*` sub-package, decide whether it should be excluded and update `sonar.coverage.exclusions` accordingly.

### I. Unique Bean Names Across Modules

Spring derives bean names from the simple class name, and the application context spans every module. Two modules declaring the same class name for adapters of the same concept produce `ConflictingBeanDefinitionException` at startup.

When implementing a `:shared-kernel` integration port, either choose a globally distinct class name or pin the bean name explicitly:

```java
@Component("shippingFulfillmentPortAdapter")
public class ShippingFulfillmentAdapter implements ShippingFulfillmentPort { ... }
```

---

## 3. Shared Kernel Interactivity Rules

To maintain strict modular boundaries, individual modules are **forbidden** from directly importing code from other modules. Instead, all cross-module interactions and common utilities must go through the `:shared-kernel` library.

The components permitted for usage from the `:shared-kernel` are:

### A. Cross-Module Ports (Integration Interfaces)

- **Integration Ports** (under `com.aionn.sharedkernel.integration.port.*`): Define the interfaces for cross-module queries or command dispatch (e.g., `MerchantQueryPort`, `IdentityAddressLookupAdapter` in identity implements `AddressLookupPort` from shared-kernel).

### B. Core Utilities & Base Classes

- **`IdGenerator`** (`com.aionn.sharedkernel.util.IdGenerator`): Used to generate ULIDs for entity and aggregate primary keys. Do not hardcode custom ULID generator libraries.
- **`ApiResponse`** (`com.aionn.sharedkernel.adapter.web.response.ApiResponse`): The standard wrapper for all REST API response bodies.
- **`EventPublisher`** (`com.aionn.sharedkernel.application.port.EventPublisher`): The standard publisher for dispatching domain events internally.
- **`EventEnvelope`** (`com.aionn.sharedkernel.domain.model.EventEnvelope`): The wrapper metadata class for all published events.
- **Common Value Objects** (under `com.aionn.sharedkernel.domain.vo.*`): Shared value objects such as `PhoneNumber` or base pagination parameters.

---

## 4. Coding & Implementation Guidelines for AI

When generating code for a new module, strictly adhere to the following technical guidelines to ensure seamless compilation and test execution:

### A. Database Transactions (`@Transactional`)

- **Location**: Always place the `@Transactional` annotations on the **Application Services** (in `application/service/`), as they hold the orchestrating business logic.
- Annotate at **class level** and override read methods with `@Transactional(readOnly = true)`. This is deliberate: a write method added later inherits a transaction instead of silently running with per-statement auto-commit.
- Avoid placing `@Transactional` directly on domain models or infrastructure adapters (unless handling specific propagation like `REQUIRES_NEW`).

#### Never Call an External System Inside a Transaction

A transaction holds a pooled DB connection and any row locks it has taken until it commits. Wrapping a network call in one therefore causes three distinct problems:

1.  **Resource exhaustion.** The connection and locks stay pinned for the duration of the remote call. With the default Hikari pool of 10, a slow provider stalls the entire application, not just the calling flow.
2.  **Non-atomic dual write.** The remote side effect cannot be rolled back. If the commit fails after a successful charge, the customer is debited with no order recorded.
3.  **Availability coupling.** A failing auxiliary system blocks core business operations — an unreachable search index preventing product publication, for example.

This applies to payment gateways, carrier APIs, search indexes, media uploads, SMS/email, captcha and KYC providers. It does **not** apply to in-process `:shared-kernel` integration ports such as `MerchantQueryPort` or `OrderQueryPort`.

#### The Orchestrator Pattern

When a flow is "read → call external system → write", place it in a dedicated orchestrator in `application/service/` that carries **no** `@Transactional`, and let it call the transactional Service as a separate bean:

```java
@Service
@RequiredArgsConstructor
public class ShipmentCarrierOrchestrator {

    private final ShipmentService shipmentService;   // @Transactional bean, reached through the proxy
    private final CarrierClient carrierClient;

    public Shipment cancelShipment(CancelShipmentCommand command) {
        Shipment shipment = shipmentService.loadShipment(command.shipmentId());      // short tx, closed
        carrierClient.cancel(shipment.getTrackingCode(), command.reason());          // no tx held
        return shipmentService.applyCancel(command.shipmentId(), command.reason());  // short tx, closed
    }
}
```

The UseCase then injects the orchestrator instead of the Service and stays a one-liner, so the layering rules in section 2.B still hold. UseCases on these flows must not declare `@Transactional`.

Two things that do **not** work as substitutes:

- Moving `@Transactional` from the class onto individual methods. The orchestrating method still needs a transaction for its writes, so the remote call stays inside it.
- Marking the orchestrating method `NOT_SUPPORTED` while leaving it in the Service. Calling a sibling method through `this` bypasses the Spring proxy, so the write would then run with no transaction at all.

Reference implementations include `ShipmentCarrierOrchestrator` in `shipping`, the phase-based ordering services, and
the payment provider ports. `identity` and `inventory` still require the same outbound-call audit.

### B. Persistence Layer Separation

- **Strict Separation**: Application Services and Domain Models **must never** import or reference JPA Entities (`*Entity` classes under `infrastructure/persistence/entity/*`).
- **The Adapter's Role**: The Persistence Adapter (e.g., `AddressPersistenceAdapter`) acts as the gatekeeper. It must fetch the JPA Entity, map it to the pure Domain Model using a MapStruct `*DomainMapper`, and return the Domain Model to the Application Service.
- When saving, the Adapter maps the modified Domain Model back into a JPA Entity before invoking Spring Data JPA repositories.

### C. MapStruct & Lombok Integration

- All mappers must be declared as interfaces annotated with `@Mapper(componentModel = "spring")`.
- Ensure that you compile the project (`.\gradlew compileJava`) after creating or editing mappers to generate the MapStruct implementation classes (`*Impl.java`) before running tests.
