# Aionn Time & Clock Conventions

This document defines the strict rules and guidelines for handling time, timezones, and clocks in the Aionn Modulith Backend.

---

## 1. Machine Timestamps: Standardize on `java.time.Instant`

Always use `java.time.Instant` for every machine timestamp (e.g., `createdAt`, `updatedAt`, `expiresAt`, `verifiedAt`, event times, cache TTL anchors, etc.). **Never introduce `java.time.LocalDateTime` for database-persisted timestamps.**

### Rationale:
*   `LocalDateTime` carries no timezone information. Hibernate maps it to `TIMESTAMP WITHOUT TIME ZONE` in PostgreSQL, causing the driver to interpret values against the local JVM/session timezone. This silently skews data across hosts running in different timezones.
*   `Instant` represents a single point on the timeline (UTC), which is the standard industry norm (e.g., Stripe, GitHub, AWS SDK, OpenTelemetry). Jackson serializes it cleanly as `"2026-01-01T00:00:00Z"`.

---

## 2. Core Rules

*   **Database Columns**: Use `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`) for all database tables. Flyway migrations must define columns as `TIMESTAMPTZ` instead of plain `TIMESTAMP`.
*   **Module Layers**: From domain aggregates, services, DTOs, entities, ports, and repositories, all timestamps must be declared as `Instant`.
*   **LocalDate for Unanchored Calendar Days**: Use `java.time.LocalDate` only where "a calendar day unanchored to a zone" is the intended business meaning (e.g., `dateOfBirth`, date-based analytics buckets, calendar cursors). Convert to `Instant` at module boundaries using `date.atStartOfDay(ZoneOffset.UTC).toInstant()`.
*   **LocalDateTime is Forbidden**: `LocalDateTime` should never be used, except for rare wall-clock inputs that have no zone context (e.g., a form field representing "meeting at 09:00 at the venue's local time"). Question the design thoroughly before using it.

---

## 3. Idioms & Coding Patterns

### Pattern A: Clock Parameter + Overloaded Defaults
To write testable, deterministic business logic, domain models and services must allow the current time to be injected:
1.  **Primary Method**: Accepts a `java.time.Clock clock` parameter. It retrieves the current time via `clock.instant()`.
2.  **Overloaded Method (Default)**: Takes no parameters and delegates to the primary method, passing `Clock.systemUTC()` (for production runtime).

*Example:*
```java
public void submit() {
    submit(Clock.systemUTC());
}

public void submit(Clock clock) {
    this.submittedAt = clock.instant();
}
```

### Pattern B: Time Operations
*   **Retrieving Current Time**: Always use `Instant.now(clock)` or `clock.instant()`. Do not call `Instant.now()` without passing the clock in domain aggregates or services.
*   **Adding Calendar Amounts**: `Instant` does not support adding calendar periods like years or months directly. Use `instant.atZone(ZoneOffset.UTC).plusYears(n).toInstant()` to ensure leap-year safety. Do not use `Duration.ofDays(365)` as a substitute.
*   **Epoch Seconds**: Use `instant.getEpochSecond()` / `Instant.ofEpochSecond(seconds)`.
*   **Formatting**: Use `DateTimeFormatter.ISO_INSTANT`. Never format timestamps using local time formatters.

---

## 4. Anti-Patterns to Avoid

*   **Do not** chain `.toInstant(ZoneOffset.UTC)` on values that are already `Instant`.
*   **Do not** call `LocalDateTime.now(Clock.systemUTC())`. Zone information is stripped upon database storage, hiding timezone drift bugs.
*   **Do not** implement converter helpers between `Instant` and `LocalDateTime` at boundaries. Standardize on `Instant` everywhere.
*   **Do not** write naked `Instant.now()` in tests. Always use `Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)` to mock time deterministically.
