# UCP Capability Matrix

**Protocol baseline:** `2026-08-25` (`v2026-08-25`, commit `cd78fb38e819de77d9b527d110476eccb876f1bd`).  
Pinned schemas, REST service descriptions, and contract verification bundles are located in:

- Runtime schemas: [`modules/ucp/src/main/resources/ucp/2026-08-25/`](../../modules/ucp/src/main/resources/ucp/2026-08-25/)
- Contract test bundle: [`modules/ucp/src/test/resources/ucp-contract/2026-08-25/`](../../modules/ucp/src/test/resources/ucp-contract/2026-08-25/)

---

## 1. Architectural Role

The Universal Commerce Protocol (UCP) module (`modules/ucp`) acts as an **interoperability protocol adapter** inside the Aionn Modular Monolith:

- **Discovery Endpoint:** `GET /.well-known/ucp`
- **REST Base Path:** `/ucp/v1`
- **Integration Boundary:** Calls application ports in `shared-kernel` (`CartOperationsPort`, `OrderPlacementPort`, `CatalogQueryPort`, `OrderSnapshotQueryPort`).
- **Data Ownership:** Does not create duplicate commerce database tables. Checkout session state and external identity bindings are managed via dedicated adapter ports (`UcpCheckoutSessionPort`, `UcpIdentityLinkPort`).

---

## 2. Capability Matrix

UCP discovery (`/.well-known/ucp`) advertises only capabilities that are fully implemented and enabled via configuration (`aionn.ucp.capabilities.*`).

### Core Capabilities

| Capability | Canonical Schema | REST Endpoints | Implemented By | Implementation | Discovery Advertisement |
|---|---|---|---|---|---|
| **Cart**<br>`dev.ucp.shopping.cart` | `schemas/shopping/cart.json` | `POST /ucp/v1/carts`<br>`GET /ucp/v1/carts/{id}`<br>`PUT /ucp/v1/carts/{id}`<br>`POST /ucp/v1/carts/{id}/cancel` | `UcpCartController`<br>`UcpCartApplicationService`<br>`CartOperationsPort` | `Supported` | `true` (when `aionn.ucp.capabilities.cart=true`) |
| **Checkout**<br>`dev.ucp.shopping.checkout` | `schemas/shopping/checkout.json` | `POST /ucp/v1/checkout-sessions`<br>`GET /ucp/v1/checkout-sessions/{id}`<br>`PUT /ucp/v1/checkout-sessions/{id}`<br>`POST /ucp/v1/checkout-sessions/{id}/complete`<br>`POST /ucp/v1/checkout-sessions/{id}/cancel` | `UcpCheckoutController`<br>`UcpCheckoutApplicationService`<br>`OrderPlacementPort.placeHeadless` | `Supported` | `true` (when `aionn.ucp.capabilities.checkout=true`) |
| **Catalog Lookup**<br>`dev.ucp.shopping.catalog.lookup` | `schemas/shopping/catalog_lookup.json` | `POST /ucp/v1/catalog/lookup`<br>`POST /ucp/v1/catalog/product` | `UcpCatalogController`<br>`UcpCatalogApplicationService`<br>`CatalogQueryPort` | `Supported` | `true` (when `aionn.ucp.capabilities.catalog=true`) |
| **Catalog Search**<br>`dev.ucp.shopping.catalog.search` | `schemas/shopping/catalog_search.json` | `POST /ucp/v1/catalog/search` | `UcpCatalogController`<br>`UcpCatalogApplicationService`<br>`CatalogQueryPort` | `Supported` | `true` (when `aionn.ucp.capabilities.catalog=true`) |
| **Identity Linking**<br>`dev.ucp.common.identity_linking` | `schemas/common/identity_linking.json` | `POST /ucp/v1/identity/links`<br>`GET /ucp/v1/identity/links/{platformId}/{platformSubject}`<br>`DELETE /ucp/v1/identity/links/{platformId}/{platformSubject}` | `UcpIdentityController`<br>`UcpIdentityApplicationService`<br>`UcpIdentityLinkPort` | `Supported` | `true` (when `aionn.ucp.capabilities.identity-linking=true`) |
| **Order**<br>`dev.ucp.shopping.order` | `schemas/shopping/order.json` | `GET /ucp/v1/orders/{id}`<br>Outbound order lifecycle webhooks | `UcpOrderController`<br>`UcpOrderApplicationService`<br>`UcpOrderEventListener`<br>`RestClientUcpWebhookDispatcher` | `Supported` | `true` (when `aionn.ucp.capabilities.order=true`) |

### Optional Extensions (Unadvertised)

The following extensions exist in UCP specifications but are not advertised by Aionn (`advertised=false`):

| Extension | Canonical Schema | Implementation | Discovery Advertisement | Note |
|---|---|---|---|---|
| **Buyer Consent** | `schemas/shopping/buyer_consent.json` | `Internal only` | `false` | Handled internally by Identity module; not exposed via UCP extension. |
| **Discount** | `schemas/shopping/discount.json` | `Internal only` | `false` | Promotion vouchers are applied through cart/checkout rather than UCP discount extension. |
| **Fulfillment** | `schemas/shopping/fulfillment.json` | `Internal only` | `false` | Shipping module manages logistics internally; not exposed as UCP selection. |
| **Payment Terms** | `schemas/common/payment_terms.json` | `Internal only` | `false` | Internal payment flows only; not exposed as UCP payment terms. |
| **Location Lookup & Search** | `schemas/common/location_lookup.json`<br>`schemas/common/location_search.json` | `Internal only` | `false` | Standard address management used instead. |
| **Loyalty** | `schemas/common/loyalty.json` | `Unimplemented` | `false` | No loyalty system in platform. |

---

## 3. Implementation Highlights

- **Cart:** Full CRUD lifecycle, strict IDOR ownership checks, repricing via catalog/pricing ports, minor-unit conversions, offline schema validation.
- **Checkout:** Ephemeral session coordination (`UcpCheckoutSession`), incomplete session reuse, delegation to `OrderPlacementPort.placeHeadless(...)` for inventory reservation and payment processing.
- **Catalog:** Filtered pagination, minor-unit price range queries, batch SKU lookup, variant availability mapping.
- **Identity Linking:** External `platformId` + `platformSubject` composite key binding with active/revoked lifecycle.
- **Order & Webhooks:** Canonical order snapshot lookup, asynchronous webhook dispatching upon order lifecycle events (`OrderPlaced`, `OrderShipped`, etc.) with SSRF protection.
- **Observability:** OpenAPI documentation group `"UCP"`, Micrometer metrics (`ucp.requests.total`, `ucp.requests.duration`, `ucp.webhooks.total`).

---

## 4. Verification

```powershell
# Run UCP module tests
.\gradlew.bat :modules:ucp:test

# Verify imported contract bundle provenance
python scripts/ucp/check-ucp-contract-bundle.py --upstream <path-to-ucp-repo>

# Build reference schema validator (clean ucp-schema checkout)
docker run --rm -v "<path-to-ucp-schema-repo>:/work" -w /work rust:1.89 cargo test --locked

# Validate released scaffolds against canonical schemas (offline)
python scripts/ucp/check-ucp-reference-fixtures.py --validator <path-to-ucp-schema-repo>
```
