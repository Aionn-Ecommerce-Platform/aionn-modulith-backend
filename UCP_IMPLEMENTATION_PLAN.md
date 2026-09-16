# UCP Implementation Plan for the Modular Monolith

## 1. Decision Summary

UCP is a protocol boundary, not a new commerce domain and not a replacement for catalog, ordering, payment, or identity. The authoritative repository describes UCP as a transport-agnostic interoperability protocol: a business advertises capabilities through `/.well-known/ucp`, then exposes those capabilities through REST, MCP, A2A, or embedded transports. The REST binding discovers a business endpoint and defines capability operations below that endpoint.

Therefore the modulith should implement UCP as a **protocol adapter module** inside the existing application. It must call existing application ports in ordering, catalog, inventory, payment, shipping, promotion, and identity; it must not duplicate their aggregates or become a second source of business truth. Module boundaries serve the current modulith's ownership, maintainability, and testability.

The reference implementation in `aionn-microservices` is comparison material only, not a target deployment architecture. This plan covers implementation and operation within the existing modulith exclusively.

## 2. Source Baseline

Refresh and verify before implementation:

- `ucp` main: commit `8e600b05` after fast-forward refresh.
- `ucp-schema` main: commit `a0fc4fc` after fast-forward refresh.
- Canonical specifications are under `ucp/docs/specification/`; current paths include `common/identity-linking`, `shopping/cart`, `shopping/checkout`, `shopping/catalog`, `shopping/order`, payment, and extensions.
- Canonical schemas are under `ucp/source/schemas/` and service descriptions under `ucp/source/services/`.
- `ucp-schema` is the reference composition/resolution/validation pipeline. It supports self-describing payloads, dynamic `allOf` extension composition, operation-specific resolution, direction-aware annotations, authority binding, and conformance fixtures.

Implementation wire baseline: UCP release `2026-08-25`, tag `v2026-08-25`, commit `cd78fb38e819de77d9b527d110476eccb876f1bd`. The refreshed `main` is a draft, not the released wire contract. Discovery schema provenance and the upstream license are stored with the pinned runtime resources under `modules/ucp/src/main/resources/ucp/2026-08-25/`.

The remaining tasks below must preserve this baseline; upgrading the protocol requires an explicit contract change.

## 3. Target Boundary and Package Layout

Add a dedicated application module or bounded package named `ucp` (prefer a Gradle module only if the current dependency graph permits it without cycles). Suggested structure:

```text
modules/ucp/
  src/main/java/com/aionn/ucp/
    adapter/rest/controller/
    adapter/rest/dto/
    adapter/rest/mapper/
    application/profile/
    application/cart/
    application/checkout/
    application/catalog/
    application/order/
    application/identity/
    application/payment/
    application/validation/
    application/port/in/
    application/port/out/
    infrastructure/config/
    infrastructure/schema/
    infrastructure/security/
    infrastructure/signing/
    infrastructure/webhook/
```

The module depends on exposed input ports from existing modules, never on their persistence internals. If a required use case is not currently exposed, add a narrow cross-module port to the owning module. Keep UCP request/response objects separate from internal commands and results.

UCP runs within the existing `app`; the external route is `/ucp/v1` and discovery is `/.well-known/ucp`. Calls to commerce modules are in-process through their exposed application ports. This plan requires no separate gateway deployment, remote inter-module calls, or new distributed infrastructure.

## 4. Capability Scope and Delivery Order

### Phase 0: contract and inventory

1. [x] Import the canonical released `2026-08-25` contract bundle into `modules/ucp/src/test/resources/ucp-contract/2026-08-25/`, including schemas, REST descriptions, scaffolds, LICENSE, provenance, and SHA-256 manifest. The bundle is test-only: it is not registered with the discovery runtime validator and does not advertise or implement a commerce capability.
2. [x] Add the offline verifier `scripts/ucp/check-ucp-contract-bundle.py`; it checks the exact local upstream commit, provenance baseline, manifest files and hashes, JSON parseability, and declared roots without network access.
3. [x] Build the evidence-based capability matrix at `document/ucp/capability-matrix.md`. Cart, checkout, catalog lookup/search, identity linking, order, buyer consent, discount, and fulfillment are all `advertised=false`.
4. [x] Confirm current checkout/session support: ordering has an authenticated, user-keyed internal cart and a headless order-placement port, but no UCP checkout-session persistence, cart-to-checkout conversion, or incomplete-checkout reuse. Cart and Checkout remain separate capabilities and the normal lifecycle is `cart session -> checkout session -> order`.
5. [x] Add focused bundle tests for baseline/provenance, hashes, JSON parseability, declared roots, and offline schema-reference closure (including canonical absolute references and JSON Pointer fragments; instance-data keywords are not scanned as schemas). Existing discovery-only allow-list rejection tests remain unchanged.
6. [x] Run the reproducibility verifier with an explicit local UCP checkout. It verifies the pin, complete declared import scope, manifest duplicates/missing/untracked files, JSON parsing, hashes, and byte-for-byte equality to `git show` at the pin.
7. [x] Execute the pinned official `ucp-schema` validator through Docker: 361 upstream tests passed, including resolution, composition, CLI and conformance tests; 18 released cart/checkout/catalog/location/order scaffolds passed explicit-schema, direction-aware validation with networking disabled. Reproduce with `scripts/ucp/check-ucp-reference-fixtures.py --validator PATH` after building the clean pinned validator checkout with `cargo test --locked` in `rust:1.89`. This verifies the inventory fixtures, not application or full capability conformance.

**Next-phase gate (not part of Phase 0):** Phase 1 must add runtime operation resolution, request/response validation, authority binding, extension composition, protocol security/error behavior, and conformance tooling before any commerce capability is enabled or advertised.

### Phase 1: remaining shared protocol foundation

Implement:

- Populate discovery service transport metadata, capability versions, schema/spec URLs, REST endpoint, and payment handlers only as their complete implementations become available. The discovery-only profile intentionally has empty registries.
- Stable business identity and endpoint configuration.
- Request correlation (`X-Request-Id`), UCP agent metadata, content type enforcement, HTTPS deployment requirements, and structured protocol errors.
- Extend schema validation beyond discovery to commerce requests/responses using pinned canonical resources. Verify composition and operation-resolution behavior against `ucp-schema` through build/test tooling rather than silently approximating those rules in Java. No runtime sidecar or separate validation service is required.
- Authority-binding and schema URL allow-list checks; never dereference arbitrary schemas from an untrusted request.

Discovery must return only capabilities enabled by configuration and backed by a complete implementation. Malformed profile/configuration is a deployment failure; upstream discovery dependency failures must map according to the REST specification rather than being hidden as generic 500 responses.

### Phase 2: Cart

Expose the REST cart binding under the discovered endpoint:

- `POST /carts`
- `GET /carts/{id}`
- `PUT /carts/{id}`
- `POST /carts/{id}/cancel`

Map these to ordering cart input ports and existing cart persistence. Preserve UCP IDs through a stable external-ID mapping or use a documented prefix/namespace. Resolve product/item identity through catalog ports and reprice using the owning pricing/promotion rules. Never trust client prices, totals, inventory, or fulfillment selections.

Implement optimistic concurrency/version checks, ownership and token checks, expiration/cancellation states, and idempotency for mutating operations. Cart-to-checkout conversion must reuse an incomplete checkout for the same cart instead of creating conflicting sessions.

### Phase 3: Checkout

Expose the canonical checkout operations from the current UCP REST spec, including create/read/update/complete and any cancel/recover operation required by the pinned version. Map them to ordering, inventory, shipping, promotion, identity, and payment ports.

The checkout adapter owns protocol-session coordination and mapping only. Final completion must delegate to ordering's existing headless placement flow through an exposed port (inspect the existing `OrderPlacementPort` before adding another). Ordering remains authoritative for pricing, stock, promotion, shipping, payment initiation, and compensation. Neither boundary may hold an open database transaction while calling payment providers, carriers, email/SMS, or other external systems. Completion must have a stable operation key, deterministic replay behavior, and a clear state machine for pending, requires action, completed, failed, and cancelled outcomes.

For payment handlers:

- Advertise only configured, actually usable handlers.
- Keep credentials/tokens out of logs and persisted protocol envelopes.
- Map payment authorization, redirect, 3DS, asynchronous confirmation, and provider failure into canonical checkout messages/errors.
- Use provider idempotency keys derived from the stable UCP operation/event identity.

### Phase 4: Catalog search/lookup and fulfillment extensions

Implement catalog search first if the existing catalog read ports can satisfy the canonical search schema. Add lookup/get-product only when their semantics and authorization are clear. Map pagination, filters, prices, availability, and item identity without leaking internal persistence models.

Add fulfillment, discounts, buyer consent, payment terms, location, loyalty, and other extensions incrementally. Extensions must be composed and validated through canonical schemas; they must not be accepted merely because Jackson can deserialize them.

### Phase 5: Identity linking and orders

Implement identity linking as a separate capability using the existing identity module and OAuth-compatible authorization boundary. Bind linked platform identity to the business user subject, scopes, consent, expiry, revocation, and audit records. Do not invent a UCP-specific user database.

Implement order lifecycle exposure/webhooks only after order event contracts are stable. Publish order/shipping/refund transitions through transactional outbox events; webhook delivery is at-least-once and consumers must tolerate replay.

## 5. Validation and DTO Rules

Each controller pipeline should be:

1. Authenticate the platform/agent and establish request identity.
2. Resolve the requested operation and capability version.
3. Validate the request against the canonical resolved request schema, including profile/capability negotiation and extensions.
4. Map to an internal command/query.
5. Execute through an owning module input port.
6. Map the result to a self-describing UCP response.
7. Validate the response against the canonical response schema before sending it.
8. Emit correlation, latency, capability, operation, outcome, and replay metrics without sensitive payloads.

Return canonical protocol errors with stable machine-readable codes, field paths, recoverability, and correlation ID. Map internal exceptions through an explicit translator; never expose stack traces or persistence errors.

## 6. Security

Extend `ApiSecurityConfig` deliberately for discovery, UCP REST routes, OAuth/identity-linking callbacks, and webhooks. Keep discovery public only where the spec requires it; protect user/cart/checkout/order operations with platform and user authorization.

Use TLS at the edge, strict content types, body/request limits, replay protection, nonce/timestamp checks where signing requires them, and rate limits. Treat `UCP-Agent`, profile URLs, schema URLs, redirect URLs, and payment metadata as untrusted input. Use an allow-list for remote profile/schema fetches and SSRF-safe clients.

Reuse the reference service's signing/JWK ideas only after confirming the pinned specification requires them for the selected transport. Isolate key material behind a signing port, rotate keys, publish only public JWK material, and test verification/rotation/failure paths.

## 7. Persistence, Transactions, and Events

Do not create a second UCP business database in the modulith. Persist only protocol-specific state that cannot belong to an existing module: external subject/link records, protocol operation/replay records if the shared idempotency store is insufficient, webhook delivery attempts, and key metadata.

Use existing Redis/database idempotency infrastructure for request replay. Store request fingerprint, operation, principal, status, response metadata/body reference, and expiry. A repeated key with a different payload must be rejected.

For state changes, commit domain mutation and outbox event atomically. Use stable event IDs, aggregate ordering keys, backward-compatible event payloads, inbox/idempotent consumers, leases, retry/backoff, and dead-letter handling already defined by `document/architecture.md`. Never perform provider calls inside the transaction. Webhook dispatch should be asynchronous and retryable.

## 8. Testing Strategy

### Contract tests

- Validate every request and response fixture through the pinned `ucp-schema` pipeline.
- Test capability negotiation, unknown capability rejection, extension composition, authority binding, version mismatch, and strict/forward-compatible fields.
- Import official examples/conformance fixtures where compatible with the selected version.

### Module integration tests

- Cart create/read/update/cancel and ownership.
- Cart-to-checkout conversion and duplicate incomplete checkout behavior.
- Checkout recalculation, inventory reservation, promotion, shipping, payment action, completion, cancellation, and failure recovery.
- Order events and webhook retries.
- Identity linking, scope enforcement, revocation, and consent.
- Idempotency replay, conflicting reuse, concurrent completion, and outbox redelivery.

### Web/security tests

- `/.well-known/ucp` and all `/ucp/v1` routes through MockMvc/WebTestClient.
- Authentication/authorization matrix, CSRF/CORS policy where relevant, request limits, SSRF protections, signature verification, key rotation, and sensitive-log assertions.
- Provider calls mocked outside transactions and verified with transaction-boundary tests.

### End-to-end/conformance

Run the canonical schema/conformance tooling in CI, then exercise a local platform/agent client against discovery, cart, checkout, payment action, completion, and order webhook flows. Record unsupported capabilities rather than weakening validation.

## 9. Observability and Operations

Add an OpenAPI/API group for UCP without exposing internal module details. Emit metrics by capability, operation, status, validation result, auth result, replay, provider action, and webhook outcome. Propagate request ID and trace context into outbox records and provider calls. Redact payment credentials, tokens, addresses where policy requires, and full payloads by default.

Provide health/readiness checks for schema resources, signing keys, idempotency store, and required provider configuration. Add admin visibility for protocol operation failures and webhook dead letters using existing protected operational patterns.

## 10. Milestones and Acceptance Gates

1. **Contract baseline:** pinned version, schemas, profile fixture, validation harness, capability matrix.
2. **Foundation:** discovery, protocol errors, validation, auth/correlation/idempotency skeleton.
3. **Cart:** all cart REST operations with contract and integration tests.
4. **Checkout:** lifecycle, conversion, payment actions, completion, replay and failure handling.
5. **Catalog/extensions:** search and selected extensions with composed schema tests.
6. **Identity/order:** linking, authorization, lifecycle events, webhooks.
7. **Hardening:** security, observability, conformance, load/replay testing, rollout controls.

Each milestone requires passing focused module tests, contract validation, security tests, and an explicit review of advertised capabilities. No capability is enabled in production until its end-to-end flow and failure/replay semantics are verified.

## 11. Explicit Non-Goals

- No microservices migration, Kafka integration, service extraction, or separate UCP deployment is in scope.
- Do not copy the reference repository's deployment topology.
- Do not duplicate cart, checkout, order, payment, catalog, inventory, shipping, promotion, or identity aggregates.
- Do not accept arbitrary schemas/extensions from the network.
- Do not call providers inside database transactions.
- Do not advertise incomplete capabilities.
- Do not start implementation before this plan and the pinned protocol scope are reviewed.
