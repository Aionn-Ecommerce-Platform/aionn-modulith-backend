# Testing and Quality

## 1. Purpose

Tests protect valuable behavior, boundaries, and failure modes. Do not write tests solely to increase a coverage number, and do not change exclusions to hide untested code.

The Sonar configuration in the root `build.gradle` is the source of truth for coverage exclusions and the quality gate. All new code must satisfy the active quality gate.

## 2. Strategy by layer

| Package | Test strategy |
| --- | --- |
| `domain/model` | Plain JUnit tests for invariants, state transitions, events, and errors |
| `application/service` | Mockito unit tests with mocked output ports and policies; cover success, compensation, and failure paths |
| `application/usecase` | Do not test one-line wrappers; move meaningful logic to a service or policy |
| `adapter/rest/controller` | MockMvc tests with mocked input ports and mappers; verify status, response, validation, and authorization |
| `infrastructure/persistence/adapter` | Unit tests for mapping and delegation; integration tests when database or query semantics matter |
| provider/client/security | Test success, network failure, non-2xx responses, malformed payloads, signatures, and configuration boundaries |
| scheduler/worker | Test delegation, batching, retries, and transaction isolation |
| architecture | ArchUnit tests in `app` protect system-wide dependency rules |

## 3. Test rules

- Tests are deterministic. Use fixed clocks and stable identifiers when contracts assert exact values.
- Tests never call the public internet.
- Injectable HTTP clients use an appropriate mock server. Clients owning their own HTTP transport - a JDK `HttpClient`, or a library client built on Apache HttpClient 5 - use a loopback server on an ephemeral port, because wire-level behaviour such as content-encoding negotiation cannot be reproduced by a mock.
- Provider tests cover at least success, a business error in a successful HTTP response, non-2xx status, malformed response, and connection failure.
- Controller authorization tests send otherwise valid input because validation may run before method security.
- Use typed mocks and captors for generic APIs. Do not hide raw `Collection` or raw `Class` warnings with `@SuppressWarnings`.
- Test names describe behavior and conditions, not internal use-case identifiers.

## 4. Test infrastructure

Shared fixtures live in `shared-kernel/src/testFixtures`. After a clean build, or when the IDE cannot resolve the test-fixtures JAR, run:

```powershell
.\gradlew.bat testFixturesJar
```

Tests requiring PostgreSQL, Redis, or OpenSearch use Testcontainers or infrastructure managed by repository scripts. Do not load the full Spring context for a unit test that does not need it.

### Local E2E setup (including a clean checkout)

Run from the repository root with PowerShell, Docker Compose, the project JDK, and Bash available. Ignored `envs/*.env` files are not included in a checkout. Create the complete set first:

```powershell
powershell -File scripts/dev/init-local-env.ps1
```

The initializer creates only missing files and never overwrites existing configuration. It copies tracked `.env.example` to `envs/common.env` and creates comment-only override files for `identity`, `catalog`, `inventory`, `ordering`, `payment`, `shipping`, `promotion`, `notification`, `chat`, and `recommendation` (11 files total). Unset options retain application YAML defaults. Re-running repairs missing files but does not update existing files when the template changes.

Edit `envs/common.env` to replace local password/key placeholders and supply sandbox credentials for the provider flows you intend to exercise. Template values are not working external-provider credentials. Optional module-specific `KEY=value` entries override common values; the runner loads common first, then modules in the order listed above, then applies its E2E overrides. Keep credentials in these ignored local files, not tracked files.

Start infrastructure with the same common configuration, wait for healthy services, then run the suite:

```powershell
docker compose -f docker/docker-compose.yml --env-file envs/common.env up -d --wait
powershell -File scripts/e2e/run-e2e-suite.ps1 -Module all
```

The runner checks all 11 files before stopping Gradle, reporting only missing paths and the initializer command. Smoke-test create-only behavior in a fresh temporary directory without touching local environment files or running Gradle:

```powershell
powershell -File scripts/dev/test-init-local-env.ps1
```

## 5. Verification commands

```powershell
# One module
.\gradlew.bat :modules:<module>:test

# Architecture rules
.\gradlew.bat :app:test --tests com.aionn.arch.*

# Full build
.\gradlew.bat build

# Effective module coverage
powershell -ExecutionPolicy Bypass -File scripts/coverage/effective-coverage.ps1 -Module <module>

# End-to-end checks
powershell -ExecutionPolicy Bypass -File scripts/e2e/run-e2e-suite.ps1 -Module all
```

Interpret coverage using Sonar's real exclusions rather than the raw JaCoCo percentage alone. Provider, security, and orchestration code deserves deeper testing than declaration-only DTO and configuration code.

## 6. Completion criteria

- Code compiles without new warnings.
- Relevant unit, controller, and integration tests pass.
- Architecture tests pass.
- Assertions are not weakened merely to make a test green.
- The build and Sonar quality gate pass.
- Wiring, database, and provider changes pass their relevant E2E or smoke checks.
