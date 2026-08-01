# Distributed scheduler locks

Business schedulers use ShedLock with the PostgreSQL `shedlock` table so only one application instance runs a given
job at a time. The JDBC provider uses database time, avoiding correctness problems caused by clock drift between
application nodes. A competing node skips the invocation instead of waiting.

Every singleton business `@Scheduled` method must also declare a unique `@SchedulerLock` name. `lockAtMostFor` is a
crash-recovery safety limit and must remain longer than the measured worst-case runtime. `lockAtLeastFor` prevents
near-simultaneous scheduler ticks on different nodes from executing the same sweep sequentially.

The transactional outbox dispatcher is intentionally not protected by a singleton lock. It coordinates workers at
row level with `FOR UPDATE SKIP LOCKED`, processing leases, ordering keys, and lease-owner checks, so multiple outbox
workers can safely make progress concurrently.
