# Module Migration Checklist

Steps required to bring a new business module (`promotion`, `notification`, `chat`) into the running application. Every item below corresponds to a failure that has actually broken startup or CI in this project — none of them are theoretical.

Work top to bottom. Do not skip the wiring section: a module can compile, pass all its tests, and still crash the application on boot.

---

## 1. Gradle & module skeleton

- [ ] Register the module in `settings.gradle`.
- [ ] Create `modules/<module>/build.gradle`. Copy the dependency block from a comparable migrated module (`shipping` for outbound-integration modules, `catalog` for read-heavy ones).
- [ ] Add `implementation project(':modules:<module>')` to `app/build.gradle`.
- [ ] Follow the package blueprint in `document/architecture/module-structure.md` exactly. Do not invent new `infrastructure/*` sub-packages (see section 5).
- [ ] Delete the `.gitkeep` placeholders under `src/main/java`, `src/main/resources`, `src/test/java` once real sources exist.

## 2. Spring configuration wiring

- [ ] Create `modules/<module>/src/main/resources/application-<module>.yml` for module-specific properties.
- [ ] **Add `- classpath:application-<module>.yml` to `spring.config.import` in `app/src/main/resources/application.yml`.**

> [!WARNING]
> Declaring the Gradle dependency is _not_ enough. If the config import is missing, every `@ConfigurationProperties` binding in the module silently resolves to `null`. Any `@PostConstruct` validation that requires a value will then throw and the whole application fails to start. This is exactly how `shipping` broke: `GhnCarrierClient.init()` threw `"GHN token is missing"` because `application-shipping.yml` was never imported.

- [ ] Register the `@ConfigurationProperties` record via an `@EnableConfigurationProperties` class under `infrastructure/config/`.

## 3. Database migrations

- [ ] Place migrations at `modules/<module>/src/main/resources/db/V<major>.<minor>__<description>.sql`.
- [ ] Use the next free major version. Currently taken: `V1.x` identity, `V2.x` catalog, `V3.x` inventory, `V4.x` ordering, `V5.x` payment, `V6.x` shipping, `V7.x` reserved for notification, `V8.x` promotion.
- [ ] Use `TIMESTAMPTZ` for every timestamp column (see `document/conventions/time-convention.md`).

> [!WARNING]
> The application scans `classpath:db` **recursively**. A file placed in both `db/` and `db/migration/` produces two migrations with the same version and Flyway aborts on startup. Pick `db/` and keep exactly one copy.

## 4. Environment variables

- [ ] Create `envs/<module>.env`. The `envs/` directory is gitignored, so also record the required keys somewhere the team can see them.
- [ ] Write the file **without a BOM**. A UTF-8 BOM makes `bash` read the first line as a command and `make run` fails with a syntax error. In PowerShell use `[IO.File]::WriteAllLines(...)`, not `Set-Content -Encoding utf8`.
- [ ] Add the file to `LOAD_ENV` in the `Makefile`.
- [ ] Add the file to the `Get-Content envs/...` list in `scripts/run-e2e-suite.ps1`.

## 5. Sonar coverage exclusions

- [ ] Confirm every package you created is already covered by `sonar.coverage.exclusions` in the root `build.gradle`, or is genuinely meant to be tested.
- [ ] If you introduce a new `infrastructure/*` sub-package, either add it to the exclusion list or accept that it counts toward coverage.

Two concrete traps seen in `shipping`:

| Mistake                                                                                                | Consequence                                                                                    |
| :----------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------- |
| Persistence adapters put in `infrastructure/adapter/` instead of `infrastructure/persistence/adapter/` | Exclusion pattern no longer matches; adapters counted as 0% covered                            |
| Outbound provider clients put in a new `infrastructure/carrier/` package                               | Not excluded, and legitimately needs tests — treat like `payment`'s `infrastructure/provider/` |

## 6. Bean naming across modules

- [ ] Give adapters that implement a `:shared-kernel` integration port a class name that is unique across the whole application.

> [!WARNING]
> Spring derives bean names from the simple class name. Two modules each declaring `ShippingFulfillmentAdapter` caused `ConflictingBeanDefinitionException` at boot. Either name them distinctly (`ShippingCarrierFulfillmentAdapter`) or pin an explicit bean name: `@Component("shippingFulfillmentPortAdapter")`.

## 7. Cross-module integration

- [ ] Implement cross-module access only through `:shared-kernel` integration ports.
- [ ] Check `app/src/main/java/com/aionn/config/StubIntegrationConfig.java`. It provides `@ConditionalOnMissingBean` stubs for not-yet-migrated modules; once the real adapter exists, the stub steps aside automatically. Remove the stub and its stale log message when the module is done.

## 8. Transaction boundaries for outbound calls

- [ ] Keep every network call (HTTP, SMS, email, search index, payment gateway) **outside** transaction boundaries. See `document/architecture/module-structure.md` section 4.A.
- [ ] If a flow needs "read → call external system → write", put that flow in a dedicated orchestrator class with no `@Transactional`, and let it call the transactional service. Reference implementation: `ShipmentCarrierOrchestrator`.

## 9. Tests

- [ ] Follow `document/conventions/testing-conventions.md` for per-layer strategy.
- [ ] Target ≥ 90% effective line coverage (coverage measured after applying the Sonar exclusions, not the raw Jacoco number). Current baseline: `shipping` 97%, `identity` 94%, `catalog` 93%.
- [ ] Do **not** write tests for `application/usecase/**`. That layer is Sonar-excluded and must stay thin enough not to need them. If a usecase feels worth testing, its logic belongs in a service or orchestrator instead.
- [ ] Run `.\gradlew.bat :modules:<module>:compileJava` before tests so MapStruct implementations are generated.

## 10. End-to-end smoke script

- [ ] Add `scripts/<module>/test-<module>-e2e.sh`, modelled on `scripts/shipping/test-shipping-e2e.sh`.
- [ ] Register the module in `scripts/run-e2e-suite.ps1`: add it to the `ValidateSet` and add the invocation branch.
- [ ] Note the API behaviours the scripts must assert against:
  - Unauthenticated requests return **403**, not 401.
  - Bean validation runs **before** method security, so a request with an invalid body returns 400 even when the caller lacks the required role. To assert a 403 authorization guard, send a fully valid payload.
- [ ] Scope the script to what a self-service run can actually reach. Endpoints requiring `ROLE_MERCHANT` are out of reach because that role is only granted through the admin API; registering a merchant in `catalog` does not elevate the identity role.

## 11. Final verification

```bash
.\gradlew.bat build
make infra-up
powershell.exe -ExecutionPolicy Bypass -File scripts/run-e2e-suite.ps1 -Module <module>
```

- [ ] `build` is green, including the ArchUnit rules in `:app:test`.
- [ ] The E2E script reports all checks passed.
- [ ] No stray log or temp files left in the repository root.
