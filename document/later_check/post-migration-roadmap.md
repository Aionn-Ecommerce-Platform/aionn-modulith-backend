# Post-Migration Optimization Roadmap (Tech Debts & Improvements)

This document tracks verified concurrency risks, architectural improvements, and integration enhancements to be addressed AFTER migrating all business modules to the modulith repository.

---

## 1. Concurrency & Locking Enhancements

### Inventory Module

- **Race Condition in Reserve → Commit**: Implement pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) on `StockReservationRepository.findById` to prevent concurrent modifications/auto-releases from double-processing the same reservation.
- **Reserve Race (Negative Stock)**: Introduce optimistic locking (`@Version`) on `InventoryItemEntity` to guarantee that concurrent reservation requests atomically decrement the `availableQty` and cannot bypass the `Guard.require` check.
- **Warehouse Selector Race**: Guard the picking logic in `selectWarehouseForSku()` against concurrent inventory depletion.

### Catalog Module

- **Concurrent Stock/Inventory Sync**: Address potential race conditions where simultaneous updates to active products or prices cause out-of-order indexing updates.

### Identity Module

- **Registration Race Condition**: Introduce distributed locking or database unique constraints verification on the phone number/email verification session to prevent concurrent `initiate()` calls from generating multiple sessions for the same phone/email.
- **MFA Backup Code Verification Race**: Prevent potential replay attacks or concurrent verify calls where a backup code could be double-submitted before its state is marked as used.

---

## 2. External Calls Inside Transaction Boundaries

Application Services carry `@Transactional` at class level and several of them call third-party systems from inside those methods. A transaction pins its pooled DB connection and any row locks it holds until commit, so every one of these sites carries three risks: connection-pool exhaustion under a slow provider, non-atomic dual writes (remote side effect cannot roll back), and availability coupling (an auxiliary system outage blocking core operations).

The fix is the orchestrator pattern documented in `document/architecture/module-structure.md` section 4.A: move the "read → call external system → write" flow into a class with no `@Transactional` that calls the transactional Service as a separate bean. `ShipmentCarrierOrchestrator` in `shipping` is the reference implementation and the only site already fixed.

Outstanding sites, highest risk first:

| Module    | Service                                                                                                | External call                                   | Why it ranks here                                                                                                               |
| :-------- | :----------------------------------------------------------------------------------------------------- | :---------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------ |
| ordering  | `OrderService.placeFromLines`                                                                          | payment gateway, shipping gateway               | Fans out to two external systems in one transaction, and holds inventory row locks taken by `reserveAll` for the whole duration |
| payment   | `PaymentService`                                                                                       | `PaymentProviderRouter` → Stripe / VNPay        | Slow provider on the highest-traffic path; a failed commit after a successful charge debits the customer with no record         |
| ordering  | `OrderReturnService.adminApprove`                                                                      | refund via payment gateway                      | Same dual-write exposure on the refund path                                                                                     |
| identity  | `RegistrationService`, `AuthService`, `KycService`, `PasswordResetService`, `AccountManagementService` | captcha, SMS/email, social token verify, Sumsub | Many sites but mostly fire-and-forget; several can simply move to an `AFTER_COMMIT` listener instead of an orchestrator         |
| catalog   | `ProductService`                                                                                       | OpenSearch index                                | Index outage blocks product publication; a rollback after a successful index leaves search out of sync                          |
| inventory | `InventoryItemService`                                                                                 | `SafetyStockNotifier`                           | Low traffic, low severity                                                                                                       |
| shipping  | `ShipmentService.quote`                                                                                | GHN, under `readOnly = true`                    | Remaining site in an otherwise fixed module                                                                                     |

Cheap mitigations worth doing before the refactor, since they are configuration only:

- Configure Hikari explicitly. The project currently sets no pool properties at all, so it runs on the default `maximumPoolSize = 10`.
- Tighten provider timeouts. `GhnCarrierClient` allows 20s per request, which is the worst-case duration a transaction can be held open.
- Add metrics for pool utilisation and transaction duration, so the refactor can be prioritised on measured latency rather than assumption.

Note that the resilience4j wrappers (`ResilientPaymentProviderRouter`, `ResilientCarrierClient`, `ResilientPaymentAdapter`) already limit how long a broken provider can stall a call, which reduces the first risk but does nothing for the other two.

---

## 3. Cross-Module Transactional Outbox Pattern

Currently, all modules publish integration events directly inside the service transaction boundary (`eventPublisher.publish(...)`). If event publishing or inter-module dispatch fails, the main transaction is committed but the event is lost.

- **Goal**: Implement a unified **Transactional Outbox Pattern** in `shared-kernel`.
- **Apply to**:
  - **Identity**: Registration and profile updates.
  - **Catalog**: Product modifications, brand creations.
  - **Inventory**: Stock reservations, adjustments, and safety stock breaches.

---

## 4. Integration Lifecycle & Event Cleanups

- **Catalog → Inventory Cleanup**: Listen to `ProductDeletedIntegrationEvent` inside the `inventory` module to automatically clean up orphaned inventory items associated with the deleted product.
- **Catalog Category Soft-Delete Cascade**: Verify if a Category has subcategories or active products before soft-deleting, preventing orphaned child categories.
- **Inventory → Ordering Expiry Notification**: When the `ReservationAutoReleaseScheduler` auto-releases expired reservations, publish a `ReservationExpiredIntegrationEvent` to notify the `ordering` module to automatically cancel stuck orders.

---

## 5. Cross-Module Data Consistency & Event Handling Risks

- **Spring Event Listener Failures**: Currently, cross-module synchronization (e.g., `MerchantLifecycleListener` in `inventory` listening to `MerchantRegistered` from `catalog`) runs inside `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. If the listener fails, the originating transaction is already committed, causing silent data inconsistency between modules.
  - _Recommendation_: Implement a retry mechanism, an asynchronous compensation worker, or migrate to a persistent Outbox queue.
- **Verification of Database Locks**: Ensure that `InventoryItemPersistencePort.lockByKey` is backed by a pessimistic database lock (e.g., `SELECT ... FOR UPDATE`) to prevent concurrent updates to stock levels from causing race conditions.
- **JWT Blacklist Fail-Safe**: The token blacklist (e.g., in `identity` logout) is stored in Redis. In the event of a Redis outage or flush, blacklisted/revoked tokens temporarily become valid again.
  - _Recommendation_: Implement a fallback database check for active sessions during authentication.
