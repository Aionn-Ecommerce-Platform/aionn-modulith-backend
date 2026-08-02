# Code and API Conventions

## 1. Naming

- Domain model or aggregate: a plain business noun with no technical suffix.
- Application service: `*Service`.
- Input port: an action followed by `InputPort`; query port: `*QueryPort`.
- Use case: an action followed by `UseCase`.
- Persistence contract: `*PersistencePort`.
- External contract: a purpose-specific name such as `PaymentGateway`, `CarrierClient`, or `SafetyStockNotifier`.
- REST contracts: `*Request` and `*Response`.
- Application contracts: `*Command`, `*Query`, and `*Result`.
- JPA model, repository, and adapter: `*Entity`, `*Repository`, and `*PersistenceAdapter`.
- Mapper: `*DtoMapper` at the REST boundary and `*DomainMapper` at the persistence boundary.

Do not use arbitrary abbreviations or legacy use-case identifiers such as `UC8.1` in names, logs, Swagger text, or error messages.

## 2. Java and dependency injection

- Java 21 is the baseline. Prefer records for immutable DTOs and configuration.
- Use constructor injection. `@RequiredArgsConstructor` is accepted.
- Do not use field injection or `@Autowired` on a single constructor.
- Do not use `@Data` on JPA entities or rich domain models.
- Introduce a constant only when it represents a named, reused rule. Express simple HTTP validation with Bean Validation instead of controller-local clamp variables.
- Avoid comments or Javadoc that merely repeat obvious code. Comments are for invariants, non-obvious constraints, and architectural decisions.

## 3. REST contracts

- Controllers return `ApiResponse<XxxResponse>` or a collection/page of response objects. They do not serialize domain models or application results directly.
- Controllers do not construct commands or queries directly when the conversion belongs in a DTO mapper.
- Request bodies use `@Valid`; constraints live on request DTOs.
- Pagination query parameters use:
  - `page`: `@Min(0)`;
  - `size` or `limit`: `@Min(1)` and an endpoint-appropriate `@Max(...)`;
  - invalid values produce `400`; controllers do not silently clamp them.
- Pagination defaults are declared in `@RequestParam(defaultValue = "...")`.
- Swagger describes the public API behavior. It does not include internal identifiers, migration history, or “UC/use case” terminology.
- Authorization uses `@PreAuthorize` according to the role and ownership contract. Controllers do not parse access tokens themselves.

## 4. Mapping

- MapStruct mappers declare `@Mapper(componentModel = "spring")`.
- REST mappers own Request -> Command/Query and Result -> Response conversions.
- Persistence mappers own Domain <-> JPA Entity conversions.
- Convert value objects and enums to stable primitive/string representations at the boundary.
- A mapper default method may contain small deterministic conversion logic. Business rules belong in domain models, services, or policies.

## 5. Exceptions and logging

- Every module uses a consistent module exception and error-code model.
- REST exception handlers convert failures to the standard `ApiResponse` shape.
- Do not catch `Exception` merely to discard failures. For intentional best-effort behavior, log the operation or entity identifier and the reason.
- Never log secrets, tokens, OTPs, passwords, raw payment data, or unnecessary personal data.
- Use parameterized logging rather than string concatenation.

## 6. Time

- Machine timestamps use `Instant`; database columns use `TIMESTAMPTZ`.
- Obtain the current instant with `clock.instant()`, never `Instant.now()`, `Instant.now(clock)`, or `System.currentTimeMillis()`.
- Use `LocalDate` only for business calendar dates. Derive it with `clock.instant().atZone(zone).toLocalDate()`.
- Use `LocalDateTime` only for a genuine zone-less wall-clock value, never for a persisted machine timestamp.
- Add calendar years or months through a zoned value rather than a fixed number of days.
- Tests use `Clock.fixed(...)`; production receives `Clock` from configuration.

## 7. Configuration and secrets

- Module configuration uses immutable, validated `@ConfigurationProperties` with a unique prefix.
- Application code does not import properties classes. Values needed by application flows are exposed through an application policy or port.
- Environment-specific endpoints and secrets come from configuration, never hardcoded values.
- Shared business thresholds belong in a clearly named domain or application policy constant and are not duplicated across validators and providers.
