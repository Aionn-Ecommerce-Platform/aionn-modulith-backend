# Operations, Configuration, and Data

## 1. Running the system

Prefer repository commands that provide a consistent local environment:

```powershell
make infra-up
make run
make test
make infra-down
```

When GNU Make is unavailable:

```powershell
.\gradlew.bat compileJava compileTestJava testFixturesJar
.\gradlew.bat :app:bootRun
.\gradlew.bat build
```

Direct `bootRun` requires environment variables to be loaded in the current process. The application port must be available. An E2E runner may deliberately terminate its background `bootRun`; use the script summary rather than the terminated process's exit code as the test result.

## 2. Profiles and configuration

- `application.yml` contains safe defaults and imports each module's configuration.
- `dev` is the local-development profile.
- `prod` enables strict production provider and secret validation.
- Test overrides live in test sources or test infrastructure when needed. Do not maintain a production-resource application YAML that merely duplicates test configuration.
- Every configuration prefix has exactly one `@ConfigurationProperties` owner.
- Secrets are never committed. Production validation fails fast when required configuration is missing or unsafe.

## 3. Flyway and data

- Production migrations under `classpath:db` contain schema changes and approved reference data.
- Demo and test fixtures belong under `classpath:db-demo` and load only in the appropriate environment.
- Never edit a migration already applied to a shared environment. Add a new migration with a higher version.
- Do not duplicate a migration under both `db` and `db/migration`; duplicate versions prevent startup.
- Timestamp columns use `TIMESTAMPTZ`.
- Production migrations must not insert sample users, credentials, merchants, products, inventory, orders, payments, promotion claims, notification inbox entries, or chat messages.
- Acceptable reference data is stable system data such as geography or approved default configuration.
- Do not run `flyway repair` against production without a reviewed recovery plan.

## 4. Transactional outbox

Durable events are written to `outbox_events` in the same transaction as the business mutation. The dispatcher claims rows through locks, leases, and `FOR UPDATE SKIP LOCKED`, records inbox receipts, and then marks events as published.

Runtime properties use the `aionn.outbox` prefix. Read exact property names and defaults from the configuration-properties class so documentation does not drift from executable configuration.

Operations should monitor:

- pending age and queue depth;
- retry and dead-letter counts;
- expired leases;
- deserialization and event-compatibility failures;
- consumer idempotency failures.

Do not delete dead-letter events before preserving diagnostic evidence and defining a replay or compensation plan.

## 5. Identity and catalog data invariants

- Account deletion is completed only after its grace period. Completion tombstones direct identifiers, removes authentication material, revokes sessions, and retains the opaque user ID needed by historical business records.
- Email, phone, and username uniqueness applies to non-deleted accounts. Once deletion completes, those identifiers may be registered again without exposing whether an older account existed.
- `product_variants.sku_id` is a globally unique technical identifier used across catalog, inventory, pricing, and ordering integrations.
- A future merchant-facing stock code must use a separate field such as `seller_sku`, scoped by merchant. Do not change `sku_id` into a composite identity without migrating every SKU-keyed contract.

## 6. Distributed schedulers

Singleton business schedulers use ShedLock and PostgreSQL database time. Lock names are globally unique. The outbox dispatcher is the exception because it already provides row-level concurrency control.

Before changing an interval or lock duration, measure worst-case runtime and verify behavior during process termination, multi-instance execution, and overlapping schedules.

## 7. Dependencies and security

- Read the Java and Spring baseline from the Gradle build rather than copying fixed versions into documentation.
- Patch updates still require review and tests. Minor and major updates require a full build and E2E checks because they can alter serialization, APIs, or auto-configuration.
- Payment, messaging, search, authentication, and media SDK upgrades use focused changes with relevant provider tests.
- Dependabot version-update pull requests may be limited to reduce noise. Security alerts and security updates are managed separately in the GitHub repository settings.
- Critical or actively exploited vulnerabilities receive immediate priority. A suppression needs an owner, evidence that the vulnerable path is unreachable, and an expiry date.

## 8. Release verification

```powershell
.\gradlew.bat build
powershell -ExecutionPolicy Bypass -File scripts/run-e2e-suite.ps1 -Module all
```

In addition to green tests, verify that:

- Flyway succeeds against a database with a valid migration history.
- Production configuration validation passes when real secrets are provided through the environment or a secret manager.
- No external provider call runs inside a database transaction.
- Outbox and scheduler metrics and logs are sufficient for incident investigation.
- Production resources contain no fixture, log, or temporary artifacts.
