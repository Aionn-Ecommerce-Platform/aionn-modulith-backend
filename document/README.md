# Aionn Modulith Backend Documentation

This directory is the long-term source of truth for the system's engineering conventions. It describes the architecture after completion of the modular-monolith migration. Migration checklists, historical notes, and temporary plans do not belong here.

## System scope

The application consists of `app`, `shared-kernel`, and these business modules:

- `identity`
- `catalog`
- `inventory`
- `ordering`
- `payment`
- `shipping`
- `notification`
- `promotion`
- `chat`

`app` is the composition root. It starts Spring Boot, wires the modules, and owns application-wide configuration. Business logic must not be placed in `app`.

`shared-kernel` contains only contracts and primitives that are genuinely shared. It must not become a dumping ground for convenience utilities or module-specific business logic.

## Documents

- [architecture.md](architecture.md): module boundaries, dependencies, transactions, events, and schedulers.
- [coding-conventions.md](coding-conventions.md): Java, REST, validation, mapping, configuration, and time conventions.
- [testing.md](testing.md): test strategy, coverage, and quality gates.
- [operations-and-data.md](operations-and-data.md): runtime configuration, databases, outbox operations, dependencies, and local commands.

## Maintenance rules

- Documentation describes current rules, not completed work.
- When code and documentation disagree, determine the intended design and update both the enforcement mechanism and documentation together.
- Important rules should be enforced by ArchUnit, tests, or CI whenever practical.
- Do not copy rapidly changing versions, coverage percentages, or current migration numbers into documentation. The build and CI reports are the source of truth for those values.
