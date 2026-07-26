# Testing Conventions Specification

This document defines the strict rules and guidelines for unit, integration, and E2E testing in the Aionn Modulith Backend. These rules keep test coverage consistent across every business module (`identity`, `catalog`, `inventory`, and future modules).

---

## 1. Core Principles

- **SonarCloud Quality Gate**: Aim for **>= 80% coverage on new code**. In practice, each module should target ~95% test coverage.
- **Sonar Exclusions**: The root `build.gradle` defines the `sonar.coverage.exclusions` list. This is the single source of truth for excluded layers. Never narrow down Jacoco classes locally in module `build.gradle` files to bypass Sonar gates.
- **Test every non-excluded class** added or modified. Any class at 0% coverage that is not in the excluded list indicates a missing test.

---

## 2. Test Execution & Coverage Targets by Layer

| Layer (Package)                                                                           | Test Strategy & Approach                                                                                                                                                                                                                                   |
| :---------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`domain/model`** (Aggregates, Entities, Domain Services)                                | **Plain JUnit unit tests**. Exercise business logic, verify state mutations, and assert that invariants and validation rules throw expected exceptions.                                                                                                    |
| **`application/service`**                                                                 | **Mockito unit tests** (`@ExtendWith(MockitoExtension)`). Mock all output ports and construct the service under test via `new Service(...)`. Cover every public method and all code paths (happy path and exception branches).                             |
| **`infrastructure/persistence/adapter`**                                                  | **Mockito unit tests**. Mock Spring Data repositories and MapStruct mappers. `@InjectMocks` the adapter. Verify delegation, query result mapping, optional branches (present/empty), and not-found exceptions.                                             |
| **`infrastructure/security` / `auth` / `registration`** (Filters, Redis, JWT, Schedulers) | Mock `StringRedisTemplate` / `ValueOperations`, verify exact key formats, TTLs, and cache miss branches. Filters must mock the `FilterChain`.                                                                                                              |
| **`infrastructure/integration`** (Sumsub, Google, Cloudinary, etc.)                       | Mock the HTTP client or bind `MockRestServiceServer` to `RestClient`. Assert response parsing, non-2xx failures, signature verification, and error paths.                                                                                                  |
| **`adapter/rest/controller`**                                                             | **Web MVC tests** via standalone setup: `MockMvcBuilders.standaloneSetup(controller)` along with global controller advice, custom argument resolvers, and `MockSecurityInterceptor`. Mock input ports and assert API response `status()` and `jsonPath()`. |
| **`infrastructure/policy`**                                                               | Instantiate with mock configuration properties and assert that rules behave according to configuration boundaries.                                                                                                                                         |
| **`util`**                                                                                | Property-based tests (using `jqwik`) or plain unit tests.                                                                                                                                                                                                  |

---

## 3. Excluded Layers (Do Not Write Coverage Tests)

We explicitly exclude layers that represent pure declarations, configurations, boilerplate, or simple delegation (as the real logic is tested in Services):

- **Application Layer**: `application/dto/**`, `application/mapper/**`, `application/port/**`, `application/usecase/**` (thin UseCases are excluded as they delegate to Services).
- **Adapter Layer**: `adapter/rest/dto/**`, `adapter/rest/mapper/**`, `adapter/rest/support/**`, `adapter/rest/exception/**`, `adapter/rest/config/**`.
- **Domain Layer**: `domain/event/**`, `domain/valueobject/**` (unless containing complex algorithmic logic like custom parsing).
- **Infrastructure Layer**: `infrastructure/config/**`, `infrastructure/persistence/entity/**`, `infrastructure/persistence/mapper/**`, `infrastructure/persistence/repository/**`, `infrastructure/observability/**`.
- **Provider configurations**: e.g., `OpenSearchConfig`.

---

## 4. Best Practices & Anti-Patterns to Avoid

### A. Avoid Raw Class Mocks for Generic Targets

- Do not write `mock(Foo.class)` or `any(Foo.class)` when handling generics.
- Use type-inferred mock creation (e.g., `mock()`), `ArgumentCaptor.captor()`, and typed matchers to prevent compilation warnings and avoid using `@SuppressWarnings` annotations.

### B. Fixed Clocks in Tests

- Never write naked `Instant.now()` in timing checks or assertions.
- Always use `Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)` to mock time deterministically in tests.

### C. Never Test the UseCase Layer

`application/usecase/**` is Sonar-excluded, so tests written there do not count toward the gate. More importantly, a UseCase that is worth testing is a UseCase that is doing too much: move the logic into a Service (pure business flow) or an orchestrator (flow that calls an external system), and test it there.

### D. Stubbing Provider Clients That Own Their HTTP Client

`MockRestServiceServer` only works when the client under test receives an injectable `RestClient` / `RestTemplate`. Several provider clients instead build a `java.net.http.HttpClient` as a private final field, which cannot be intercepted.

For those, start a real loopback server on an ephemeral port with the JDK's `com.sun.net.httpserver.HttpServer` and point the properties record's `baseUrl` at it. This needs no extra dependency and no production change. See `GhnStubServer` plus `GhnCarrierClientTest` / `GhnAddressResolverTest` in the `shipping` module; the helper also records request bodies, headers and hit counts so tests can assert the outgoing payload and verify caching.

Cover these paths for every provider client: success, business error code in a 200 body, non-2xx status, unparsable body, and connection failure (point `baseUrl` at `http://127.0.0.1:1`).

### E. Verify Module Jacoco Reports Locally

To verify coverage of a specific module before committing:

```bash
./gradlew :modules:<module-name>:test :modules:<module-name>:jacocoTestReport
```

Open `modules/<module-name>/build/reports/jacoco/test/html/index.html` to inspect coverage details.

Read that number with care: the raw Jacoco total includes the excluded layers (DTOs, mappers, ports, usecases, JPA entities, config) and therefore understates the figure Sonar actually gates on. What matters is **effective coverage** — the ratio computed after dropping the packages listed in `sonar.coverage.exclusions`. The gap is large: `shipping` reported 60.5% raw while sitting at 97% effective.

Compute it with the helper instead of doing the arithmetic by hand. It reads the Jacoco XML report, applies the same exclusion list as Sonar and prints the files that still have uncovered lines:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/coverage/effective-coverage.ps1 -Module promotion
```

Effective line coverage per module, as a baseline for new work:

| Module    | Effective line | Effective branch |
| :-------- | :------------- | :--------------- |
| promotion | 99.6%          | 90.5%            |
| shipping  | 97.0%          | 82.8%            |
| identity  | 94.2%          | 75.3%            |
| catalog   | 93.3%          | 72.7%            |
| payment   | 88.1%          | 65.4%            |
| ordering  | 87.1%          | 62.8%            |
| inventory | 84.9%          | 71.8%            |

---

## 5. `:shared-kernel` Testing Strategy

As the foundation library, `:shared-kernel` requires special care and high coverage:

- **Pure Functions & Primitives** (`domain`, `domain/vo`, `util`): Use property-based testing with `jqwik` (`*PropertyTest`) to verify algebraic invariants and edge cases.
- **Behavioural Utilities**: Direct unit tests targeting exact branches (e.g., `ApiResponse`, `DomainException`).
- **Integration Contracts** (`integration/event/**`, `integration/port/**`): Assert compact constructor validation, list immutability, and default method behavior directly in behavioral tests.
- **Shared test infrastructure** lives in `src/testFixtures` (`BaseUnitTest`, `BaseIntegrationTest`, `FakeEventPublisher`, fixtures) and is reused by module tests.
  > [!IMPORTANT]
  > Because other modules import the shared test infrastructure, you must compile it by running `./gradlew testFixturesJar` (or `.\gradlew.bat testFixturesJar` on Windows). If you clean the workspace or face "missing required library" errors in your IDE related to `shared-kernel-*-test-fixtures.jar`, run this task once to resolve the issues.
