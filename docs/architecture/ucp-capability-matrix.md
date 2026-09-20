# UCP Capability Matrix

**Protocol baseline:** `2026-08-25` (`v2026-08-25`, commit `cd78fb38e819de77d9b527d110476eccb876f1bd`).  
Canonical schemas, REST service descriptions, scaffolds, license, and provenance are located in:
- Runtime resources: [`modules/ucp/src/main/resources/ucp/2026-08-25/`](../../modules/ucp/src/main/resources/ucp/2026-08-25/)
- Contract test bundle: [`modules/ucp/src/test/resources/ucp-contract/2026-08-25/`](../../modules/ucp/src/test/resources/ucp-contract/2026-08-25/)

---

## 1. Architectural Boundary

Universal Commerce Protocol (UCP) is implemented as a **protocol adapter module** (`modules/ucp`) within the Aionn Modular Monolith.
- **Role:** Translates canonical UCP request/response envelopes to internal commerce commands and queries.
- **Integration:** Calls exposed application ports in `shared-kernel` (e.g., `CartOperationsPort`, `OrderPlacementPort`, `CatalogQueryPort`, `OrderSnapshotQueryPort`).
- **Invariants:** Does not duplicate business aggregates, persistence tables, or business truth. Protocol-only checkout session and identity linking states are managed via dedicated adapter ports.
- **Discovery Endpoint:** `GET /.well-known/ucp`
- **REST Base Route:** `/ucp/v1`

---

## 2. Capability Matrix

In accordance with the UCP specification, the discovery document (`/.well-known/ucp`) advertises only capabilities that are fully implemented and enabled via configuration (`aionn.ucp.capabilities.*`). Incomplete or unverified extensions remain unadvertised (`advertised=false`).

### Core Capabilities (Implemented & Supported)

| Capability Identifier | Canonical Schema | REST Endpoints | Implementation & Evidence | Discovery Advertisement |
|---|---|---|---|---|
| **Cart**<br>`dev.ucp.shopping.cart` | `schemas/shopping/cart.json` | `POST /ucp/v1/carts`<br>`GET /ucp/v1/carts/{id}`<br>`PUT /ucp/v1/carts/{id}`<br>`POST /ucp/v1/carts/{id}/cancel` | `UcpCartController`<br>`UcpCartApplicationService`<br>`CartOperationsPort`<br>`OrderingCartAdapter`<br>Tests: `UcpCartControllerTest`, `UcpCartApplicationServiceTest` | Configurable<br>(`true` when `capabilities.cart=true`) |
| **Checkout**<br>`dev.ucp.shopping.checkout` | `schemas/shopping/checkout.json` | `POST /ucp/v1/checkout-sessions`<br>`GET /ucp/v1/checkout-sessions/{id}`<br>`PUT /ucp/v1/checkout-sessions/{id}`<br>`POST /ucp/v1/checkout-sessions/{id}/complete`<br>`POST /ucp/v1/checkout-sessions/{id}/cancel` | `UcpCheckoutController`<br>`UcpCheckoutApplicationService`<br>`OrderPlacementPort.placeHeadless`<br>`InMemoryUcpCheckoutSessionAdapter`<br>Tests: `UcpCheckoutControllerTest`, `UcpCheckoutApplicationServiceTest` | Configurable<br>(`true` when `capabilities.checkout=true`) |
| **Catalog Lookup**<br>`dev.ucp.shopping.catalog.lookup` | `schemas/shopping/catalog_lookup.json` | `POST /ucp/v1/catalog/lookup`<br>`POST /ucp/v1/catalog/product` | `UcpCatalogController`<br>`UcpCatalogApplicationService`<br>`CatalogQueryPort`<br>Tests: `UcpCatalogControllerTest`, `UcpCatalogApplicationServiceTest` | Configurable<br>(`true` when `capabilities.catalog=true`) |
| **Catalog Search**<br>`dev.ucp.shopping.catalog.search` | `schemas/shopping/catalog_search.json` | `POST /ucp/v1/catalog/search` | `UcpCatalogController`<br>`UcpCatalogApplicationService`<br>`CatalogQueryPort`<br>Tests: `UcpCatalogControllerTest`, `UcpCatalogApplicationServiceTest` | Configurable<br>(`true` when `capabilities.catalog=true`) |
| **Identity Linking**<br>`dev.ucp.common.identity_linking` | `schemas/common/identity_linking.json` | `POST /ucp/v1/identity/links`<br>`GET /ucp/v1/identity/links/{platformId}/{platformSubject}`<br>`DELETE /ucp/v1/identity/links/{platformId}/{platformSubject}` | `UcpIdentityController`<br>`UcpIdentityApplicationService`<br>`UcpIdentityLinkPort`<br>`InMemoryUcpIdentityLinkAdapter`<br>Tests: `UcpIdentityControllerTest`, `UcpIdentityApplicationServiceTest` | Configurable<br>(`true` when `capabilities.identity-linking=true`) |
| **Order**<br>`dev.ucp.shopping.order` | `schemas/shopping/order.json` | `GET /ucp/v1/orders/{id}`<br>Outbound order lifecycle webhooks | `UcpOrderController`<br>`UcpOrderApplicationService`<br>`OrderSnapshotQueryPort`<br>`UcpOrderEventListener`<br>`RestClientUcpWebhookDispatcher`<br>Tests: `UcpOrderControllerTest`, `UcpOrderEventListenerTest` | Configurable<br>(`true` when `capabilities.order=true`) |

---

### Optional Extensions (Currently Deferred / Unadvertised)

The following extensions exist in upstream UCP specifications but are not advertised by Aionn Modulith (`advertised=false`) because dedicated canonical composition, validation, and semantic mapping are reserved for subsequent milestone hardening:

| Extension / Pinned Artifact | Upstream Schema | Internal Candidate State | Status |
|---|---|---|---|
| **Buyer Consent** | `schemas/shopping/buyer_consent.json` | Identity module has internal consent storage (`UserConsent.java`, `ConsentService.java`), but does not expose canonical UCP extension composition. | `advertised=false` |
| **Discount** | `schemas/shopping/discount.json` | Promotion module owns voucher CRUD and redemption, but does not implement canonical UCP discount line item composition or validation. | `advertised=false` |
| **Fulfillment** | `schemas/shopping/fulfillment.json` | Shipping module owns shipment CRUD and rate calculation, but does not expose canonical UCP fulfillment option selection in checkout. | `advertised=false` |
| **Payment Terms** | `schemas/common/payment_terms.json` | Payment module handles payment methods and processing, but does not implement UCP payment-terms negotiation or action binding. | `advertised=false` |
| **Location Lookup & Search** | `schemas/common/location_lookup.json`<br>`schemas/common/location_search.json` | Geography reference data exists in Identity module, but does not implement canonical UCP location lookup/search endpoints. | `advertised=false` |
| **Loyalty** | `schemas/common/loyalty.json` | No loyalty domain model currently exists in the platform. | `advertised=false` |

---

## 3. Protocol Implementation Details

### Cart (`dev.ucp.shopping.cart`)
- Implements full lifecycle: create cart, read cart by ID, update line item quantities, and cancel/clear cart.
- Enforces strict IDOR (Insecure Direct Object Reference) protection: caller identity is resolved from the security context and verified against cart ownership.
- Reprices items through catalog and pricing ports, rejects mixed currencies, and guards against quantity overflow.
- All request and response envelopes are validated offline against `schemas/shopping/cart.json`.

### Checkout (`dev.ucp.shopping.checkout`)
- Manages ephemeral checkout sessions (`UcpCheckoutSession`) via `UcpCheckoutSessionPort`.
- Supports direct checkout from line items or conversion from an existing cart ID, preventing duplicate incomplete checkout sessions.
- Final order completion delegates to ordering's headless flow via `OrderPlacementPort.placeHeadless(...)`, ensuring that stock deduction, payment authorization, and event publishing remain authoritative in the ordering aggregate.

### Catalog (`dev.ucp.shopping.catalog.search` & `dev.ucp.shopping.catalog.lookup`)
- Exposes text search, price range filtering (with minor unit conversions), and pagination clamping without leaking internal database entities.
- Supports batch SKU lookup and single product retrieval with variant availability mapping.

### Identity Linking (`dev.ucp.common.identity_linking`)
- Binds external platform identifiers and subjects (`platformId` / `platformSubject`) to authenticated Aionn user accounts.
- Supports active and revoked lifecycle states with fail-closed authorization.

### Order & Webhooks (`dev.ucp.shopping.order`)
- Exposes canonical order lookup by order ID with fail-closed ownership verification.
- Listens to internal order domain events (`OrderPlaced`, `OrderApproved`, `OrderShipped`, `OrderCompleted`, `OrderCancelled`) via `UcpOrderEventListener`.
- Outbound webhooks are dispatched asynchronously to the client's configured webhook URL with request correlation and SSRF protection.

---

## 4. Conformance & Verification

### Automated Test Suite
Run unit, integration, and architecture tests for the UCP module:

```powershell
.\gradlew.bat :modules:ucp:test
```

### Reference Contract & Fixture Verification
To verify the pinned contract bundle against upstream UCP schemas and the reference validator CLI:

```powershell
# 1. Verify byte-for-byte provenance of the imported bundle against an upstream UCP checkout
python scripts/ucp/check-ucp-contract-bundle.py --upstream <path-to-ucp-repo>

# 2. Build official reference schema validator (requires Docker & clean ucp-schema checkout)
docker run --rm -v "<path-to-ucp-schema-repo>:/work" -w /work rust:1.89 cargo test --locked

# 3. Validate released reference scaffolds against canonical schemas (offline, no network)
python scripts/ucp/check-ucp-reference-fixtures.py --validator <path-to-ucp-schema-repo>
```
