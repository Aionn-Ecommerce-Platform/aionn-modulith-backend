# Database migration data policy

Production Flyway migrations under `classpath:db` contain schema changes and approved reference data only.
Development and test fixtures live under `classpath:db-demo`, which is enabled only by the `dev` and `test`
Spring profiles.

Approved production reference data currently consists of:

- Vietnam geography (`countries`, `provinces`, `districts`, and `wards`)
- notification templates
- catalog settings
- shipping rates

Business records such as users, credentials, merchants, products, inventory, orders, payments, promotions,
notifications, and chat messages must never be added to `classpath:db`.

## Existing databases

The baseline migrations were rewritten to remove legacy fixtures, so their Flyway checksums changed. Recreate local
and test databases before applying this change. Do not run `flyway repair` against a production database without a
reviewed migration plan: repair only accepts the new checksums and does not remove demo rows that were already loaded.
