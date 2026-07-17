# Aionn Code Style Specification

This document defines the coding styles, naming conventions, and programming practices for all Java source code in the Aionn Modulith Backend.

---

## 1. Comment & Documentation Policy (Strict)

### No Inline Comments
Do not add inline comments, section-divider comments, or Javadoc to code files you create or modify unless:
*   The user explicitly requests them.
*   The file already has a comment style established and you are matching an adjacent pattern.
*   The comment is a required standard annotation (e.g., `@Override`, `@Deprecated`).

Prefer expressing business intent through descriptive naming, small focused methods, and clean architecture. Avoid:
*   Section headers (e.g., `// ---- Queries ----` or `// User actions`).
*   Method-level inline comments explaining how the code works.
*   Javadoc on domain models, services, controllers, or port methods.

*Note: SQL migration files may keep a single-line header identifying the migration purpose.*

---

## 2. Naming Conventions

### Classes & Interfaces
*   **Domain Aggregates & Models**: Plain nouns representing the domain entity (e.g., `User`, `Address`).
*   **Application Services**: Appended with `Service` (e.g., `AddressService`).
*   **UseCase Components**: Active verb phrases appended with `UseCase` (e.g., `CreateAddressUseCase`).
*   **Input Ports**: Active verb phrases appended with `InputPort` or `QueryPort` (e.g., `CreateAddressInputPort`, `ListAddressesQueryPort`).
*   **Output Ports**: Appended with `PersistencePort` or `Notifier` (e.g., `UserPersistencePort`, `OutboundOrderNotifier`).
*   **REST Controllers**: Appended with `Controller` (e.g., `AddressController`).
*   **DTOs**:
    *   Command DTOs: Appended with `Command` (e.g., `CreateAddressCommand`).
    *   Query DTOs: Appended with `Query` (e.g., `ListAddressesQuery`).
    *   Result DTOs: Appended with `Result` (e.g., `AddressResult`).
    *   Request DTOs (REST): Appended with `Request` (e.g., `CreateAddressRequest`).
    *   Response DTOs (REST): Appended with `Response` (e.g., `AddressResponse`).
*   **Persistence Entities**: Appended with `Entity` (e.g., `AddressEntity`).
*   **Persistence Adapters**: Appended with `PersistenceAdapter` (e.g., `AddressPersistenceAdapter`).
*   **Mappers**:
    *   REST DTO Mappers: Appended with `DtoMapper` (e.g., `AddressDtoMapper`).
    *   Persistence/Domain Mappers: Appended with `DomainMapper` (e.g., `AddressDomainMapper`).

### Methods & Variables
*   Use camelCase for all method and variable names.
*   Boolean getters must be prefixed with `is` (e.g., `isDefault()`, `isEnabled()`).
*   Avoid arbitrary abbreviations; prioritize readability (e.g., use `quantity` instead of `qty`, unless matching an existing pattern like `qty` in the database).

---

## 3. Exception Handling

Every business module must define a unified exception hierarchy:
*   **Custom Runtime Exception**: Extends `RuntimeException` (e.g., `IdentityException`). It must accept a modular `ErrorCode` and an optional custom message.
*   **ErrorCode Enum**: Implements a shared interface or defines an enum specifying error code strings and HTTP statuses (e.g., `IdentityErrorCode` containing `USER_NOT_FOUND("USER_NOT_FOUND")`).
*   **Global Handling**: Caught at the adapter layer by the global controller exception handler, formatting the error into the standardized `ApiResponse` structure.

---

## 4. Lombok & MapStruct Practices

### Clean Lombok Usage
*   Use `@RequiredArgsConstructor` for constructor-based dependency injection on classes.
*   Use `@Getter` at the class level for domain models/entities.
*   **Do not use `@Data`** on JPA Entities or rich Domain Models; it generates arbitrary `toString()`, `equals()`, and `hashCode()` methods that cause cyclic reference exceptions in database relationships.
*   Always implement explicit `equals()` and `hashCode()` based on a unique database identifier (e.g., `userId` or `addressId`) rather than relying on `@EqualsAndHashCode`.

### MapStruct Mappers
*   Annotate all mappers with `@Mapper(componentModel = "spring")`.
*   Always handle conversion of nested custom value objects or enums to primitive types (such as `String`) explicitly inside the mapper configuration to avoid type leak.
