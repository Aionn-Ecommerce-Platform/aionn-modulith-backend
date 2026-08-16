# Aionn Operational Readiness Plan — Local Working File

This checklist is local-only. Never commit it. Remove completed items and delete the file when it is empty.

## Completed local baseline

The repository now has a trustworthy nine-module E2E runner using an isolated, migrated PostgreSQL database; a pinned, non-root runtime image; liveness/readiness probes; graceful shutdown; image validation in CI; and repeatable Makefile entry points. Continue below only when the AWS deployment target is selected.

## P0 — AWS release and data safety

### O2 — Publish and verify the immutable image

- Select ECR and publish the image once per green commit, tagged with the commit SHA and deployed by digest.
- Generate an SBOM, scan dependencies and the image for high/critical vulnerabilities, and decide whether signing/attestation is required.
- Validate read-only runtime filesystem and intentional writable temporary paths on the chosen AWS service.

### O3 — Staging, promotion, and rollback

- Choose the runtime: prefer ECS/Fargate unless measured requirements justify EKS.
- Create isolated staging and production configuration with protected GitHub environments and production approval.
- Deploy automatically to staging, run post-deploy smoke/E2E checks, and promote the exact same digest without rebuilding.
- Serialize deployments and implement rollback to the previous known-good digest.
- Rehearse a failed health check and rollback, including database backward-compatibility constraints.

### O4 — Backup, restore, and migration rehearsal

- Define RPO/RTO and classify PostgreSQL as authoritative; explicitly classify Redis and OpenSearch as rebuildable or durable.
- Enable encrypted RDS backups and point-in-time recovery.
- Restore a recent snapshot into isolation, run Flyway preflight, start the app, and verify money/order/inventory invariants.
- Document forward/backward-compatible schema rules and reconciliation/reindex steps after restore.

### O5 — Secrets management

- Store production secrets in AWS Secrets Manager or SSM Parameter Store with least-privilege IAM.
- Separate staging and production credentials; keep secrets out of images, logs, workflow output, and command arguments.
- Document and rehearse rotation for JWT, database, Redis, webhooks, payment providers, Cloudinary, and deployment credentials.
- Add secret scanning and production configuration validation to release gates.

## P1 — Operability and resilience

### O6 — Observability and alerting

- Send structured, redacted logs to CloudWatch and preserve correlation/request/order/payment/event identifiers.
- Scrape or export metrics and provision dashboards for API health, checkout, providers, outbox/dead letters, compensation, refund/settlement, inventory, DB pools, and JVM health.
- Add useful OpenTelemetry traces across HTTP, jobs, outbox, providers, and database operations.
- Define initial SLIs/SLOs and alerts based on sustained customer/business impact.

### O7 — Incident runbooks

- Deployment failure and rollback.
- RDS restore and Flyway failure.
- Outbox replay, ordering compensation, refund/settlement mismatch, and payout suspension.
- Inventory drift/contention, Redis/OpenSearch/provider outage, and credential compromise.
- Include symptoms, queries/dashboards, safe commands, decision points, escalation, and post-incident verification.

### O8 — Multi-replica runtime validation

- Set task CPU/memory and JVM container memory from measurements.
- Verify ShedLock jobs, outbox claiming, provider idempotency, and WebSocket/STOMP routing with multiple replicas.
- Configure rolling deployment, termination grace, disruption behavior, and any justified session affinity.
- Terminate an instance during checkout/outbox work and verify no lost event or duplicate economic effect.

### O9 — Load and failure testing

- Build k6/Gatling scenarios for auth, search/fallback, checkout, stock contention, payment/refund, outbox/notification, and STOMP.
- Measure p50/p95/p99, throughput, errors, locks, DB pools, CPU, memory, GC, and queue age with production-like cardinality.
- Use `EXPLAIN (ANALYZE, BUFFERS)` before adding indexes.
- Test provider latency, Redis/OpenSearch loss, PostgreSQL restart/failover, and retry storms; publish capacity limits and scaling triggers.

## P2 — AWS pipeline and operational polish

### O10 — Complete release gates

- Add vulnerability scan, SBOM retention, deployment-manifest validation, and artifact evidence retention.
- Add ephemeral dependency services to CI where they provide useful integration confidence.
- Add scheduled dependency/security checks without restoring noisy routine update PRs.

### O11 — Repeatable cloud operations

- Add migration preflight, backup/restore, reindex, deploy smoke, and rollback entry points once AWS resources exist.
- Require explicit environment/target confirmation for destructive production operations.
- Define log/data retention, disk/capacity alerts, release checklist, ownership/escalation contacts, and deployed-digest change history.

## Launch gate

- Exact image digest passes CI, security policy, migration preflight, and staging E2E.
- Backup restoration and rollback are rehearsed.
- Critical dashboards, alerts, and runbooks are live and owned.
- Capacity and SLO thresholds come from measurements.
- No unresolved P0 item remains.
