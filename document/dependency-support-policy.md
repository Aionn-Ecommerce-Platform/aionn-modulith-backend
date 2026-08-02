# Dependency support and vulnerability policy

The supported framework baseline is Java 21, Spring Boot 4.1.x, and Spring Cloud 2025.1.2 or newer. Patch releases may be
adopted directly through a reviewed pull request. Minor or major upgrades require the full Gradle test suite and the
module E2E suite because they can change public APIs, serialization, or provider behavior.

## Explicitly pinned integrations

| Integration | Baseline | Decision |
| --- | --- | --- |
| Resilience4j | 2.4.0, Spring Boot 4 starter | Keep aligned with the Boot generation; verify circuit-breaker auto-configuration in integration tests. |
| Stripe Java | 25.7.0 | Pin to avoid unreviewed payment API changes; upgrade in a dedicated payment PR. |
| Twilio Java | 10.8.0 | Pin to avoid unreviewed messaging behavior changes; upgrade in a dedicated notification PR. |
| OpenSearch clients | REST HLRC 2.11.1 and Java client 2.13.0 | Retain while the compatibility adapter uses both APIs; remove HLRC in a dedicated search migration. |
| JJWT | 0.12.6 | Keep API, implementation, and Jackson modules on the same version; security fixes have priority. |
| Cloudinary HTTP 5 | 2.0.0 | Pin provider behavior; validate upload and deletion flows before upgrading. |

Springdoc is kept on its Boot 4-compatible 3.x line. Testcontainers remains on the maintained 1.21.x line to preserve
the existing artifact names and container packages; moving to 2.x is a separate test-infrastructure migration.

## Monitoring and response

Dependabot checks Gradle dependencies weekly and GitHub Actions monthly. GitHub security alerts must remain enabled
for the repository.

- Critical or actively exploited vulnerabilities: triage within one business day and release a fix as soon as tests pass.
- High-severity vulnerabilities: triage within two business days and target the next patch release.
- Medium and low severity vulnerabilities: review during the monthly dependency maintenance window.
- A suppression requires an owner, evidence that the vulnerable path is not reachable, and an expiry date.
- Every dependency update must pass `gradlew test` and `scripts/run-e2e-suite.ps1 -Module all` before merge.
