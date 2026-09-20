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
- Production migrations must not insert sample users, credentials, merchants, products, inventory, orders, payments, promotion claims, notification inbox entries, chat messages, or recommendation interactions.
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

## 6. Behavioural data

- Interaction rows are personal data. They are deleted outright when an account is deleted rather than retained against a tombstoned user ID, because unlike historical business records there is no obligation behind them. The derived affinity profile goes with them; the aggregate similarity and popularity tables do not identify anyone and stay.
- Erasure leaves one row per erased account in `recommendation_erased_users`, holding only the opaque user ID and the erasure timestamp. Deleting rows alone cannot distinguish "this account was erased" from "this account never had any behaviour", so an event still in flight through the outbox would rebuild a profile from nothing and silently resurrect the data. This is a deliberate retention decision, not an oversight: the table is never pruned, because pruning it reopens exactly the hole it closes.
- Erasure and ingest take a per-user PostgreSQL advisory transaction lock (`pg_advisory_xact_lock(hashtext(user_id))`), so the check for an erasure mark and the write that follows it cannot interleave with a concurrent erasure. Without it the two are a check-then-act race and the losing order writes a row for an account that no longer exists.
- Ingest is idempotent on `source_event_id`, enforced by a partial unique index and an `INSERT ... ON CONFLICT DO NOTHING`. Delivery is at least once, so the same business action can arrive twice; the index makes the second arrival a no-op rather than a doubled signal. Interactions with no source event are excluded from the index and are never deduplicated, because two genuine repeat actions have nothing to collide on.
- Interactions are stored with their base weight and decayed at read time. A stored decayed value would be wrong the moment the row aged.
- Never log which product a user interacted with above debug level. Browsing history can reveal sensitive interests and is not needed for operational diagnosis; log counts instead.
- Availability is applied to a recommendation slate after ranking and outside the cache. Caching the availability decision would serve unbuyable products for the life of the entry.

## 7. Distributed schedulers

Singleton business schedulers use ShedLock and PostgreSQL database time. A dedicated `schedulerLockExtensionExecutor` renews leases halfway through `lockAtMostFor` using `KeepAliveLockProvider`, independently of the business scheduler threads. Renewal stops on unlock; after process termination the last lease expires normally. Lock names are globally unique. The outbox dispatcher is the exception because it already provides row-level concurrency control.

Business jobs that do not name a scheduler share the pool bean named `taskScheduler`, sized by `SCHEDULER_POOL_SIZE`. The pool needs one thread per such job: a slow job that takes the only free thread stalls every other background job, including the outbox dispatcher if it were sharing the pool. `ApplicationSchedulingConfigTest` counts `@Scheduled` methods off the classpath bytecode and fails the build when the pool is smaller than the number of jobs, so the invariant cannot rot silently when a job is added. The outbox dispatcher names its own single-threaded `outboxTaskScheduler` and is excluded from that count.

Before changing an interval or lock duration, measure worst-case runtime and verify behavior during process termination, multi-instance execution, and overlapping schedules.

### Recommendation offline jobs

| Lock name                          | Default cadence | `lockAtMostFor` | `lockAtLeastFor` |
| ---------------------------------- | --------------- | --------------- | ---------------- |
| `recommendation-profile-refresh`   | 15 min          | PT10M           | PT30S            |
| `recommendation-item-similarity`   | 1 h             | PT30M           | PT1M             |
| `recommendation-popularity`        | 15 min          | PT30M           | PT30S            |
| `recommendation-interaction-prune` | 24 h            | PT1H            | PT1M             |

`RECOMMENDATION_EXECUTION_COMPUTE_TIMEOUT_SECONDS` overrides the application-wide transaction timeout for the heavy read and batched-write phases of the two rebuilds, which scan the interaction log and legitimately need longer than a request budget. Its validated range is 1 to 1740 seconds inclusive (default 900), below the initial PT30M lease. This is not a whole-rebuild deadline: the lease is automatically renewed for the entire read/write/cleanup sequence, so a large number of batches does not alone cause lease expiry. Monitor renewal failures and job runtime: a prolonged database outage or process pause can still prevent lease extension.

`RECOMMENDATION_EXECUTION_UPSERT_BATCH_SIZE` bounds each write transaction so a rebuild commits progressively instead of holding one transaction open across the whole result set. Lowering it shortens the blast radius of a failed run at the cost of more commits; raising it does the reverse.

The remaining knobs - signal weights and half-lives, hybrid ranking weights, cold-start thresholds, per-job lookback windows and batch sizes, and cache TTLs - are listed as commented examples in `.env.example`, with authoritative defaults in `application-recommendation.yml`. The module holds no credentials and calls no external provider, so none of them are secret. Two of them fail the application at startup rather than degrading quietly: a ranking weight sum that is not positive, and any value outside the bounds declared on the `@ConfigurationProperties` records.

### Settlement reconciliation

Each settlement entry records explicit `pending_delta`, `available_delta`, and `receivable_delta` values. For every merchant and currency, the authoritative equations are:

```text
merchant_balances.pending    = SUM(settlement_ledger.pending_delta)
merchant_balances.available  = SUM(settlement_ledger.available_delta)
merchant_balances.receivable = SUM(settlement_ledger.receivable_delta)
```

`MOVE_AVAILABLE` subtracts pending and adds the same amount to available. A refund consumes available, then pending, and records any uncovered amount as a receivable. Later sales repay receivables before crediting pending. Payout debits and reversals affect only available.

The scheduled reconciliation job reports mismatches through metrics and error logs. Never repair a mismatch by editing balances or ledger rows without preserving evidence and reviewing the complete economic event chain.

## 8. Dependencies and security

- Read the Java and Spring baseline from the Gradle build rather than copying fixed versions into documentation.
- Patch updates still require review and tests. Minor and major updates require a full build and E2E checks because they can alter serialization, APIs, or auto-configuration.
- Payment, messaging, search, authentication, and media SDK upgrades use focused changes with relevant provider tests.
- Dependabot version-update pull requests may be limited to reduce noise. Security alerts and security updates are managed separately in the GitHub repository settings.
- Critical or actively exploited vulnerabilities receive immediate priority. A suppression needs an owner, evidence that the vulnerable path is unreachable, and an expiry date.

## 9. Release verification

```powershell
.\gradlew.bat build
powershell -ExecutionPolicy Bypass -File scripts/e2e/run-e2e-suite.ps1 -Module all
```

`-Module all` runs every module script under `scripts/e2e/<module>/test-<module>-e2e.sh`, including `scripts/e2e/recommendation/`. A single module can be run on its own with `-Module recommendation`, which is faster when iterating but is not a substitute for the full suite before a release. The runner starts the application against an isolated database, applies `scripts/e2e/e2e-prerequisites.sql`, and shortens the recommendation offline-job cadence so the behavioural chain can be observed inside one run; the recommendation script therefore depends on being launched by the runner rather than against a default-configured application.

In addition to green tests, verify that:

- Flyway succeeds against a database with a valid migration history.
- Production configuration validation passes when real secrets are provided through the environment or a secret manager.
- No external provider call runs inside a database transaction.
- Outbox and scheduler metrics and logs are sufficient for incident investigation.
- Production resources contain no fixture, log, or temporary artifacts.
