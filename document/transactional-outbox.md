# Transactional outbox

Domain and integration events are stored in `outbox_events`. When a publisher is called inside a database
transaction, the aggregate mutation and event insert commit or roll back together. Events produced while handling a
durable event are also stored before the source event is acknowledged, so a dispatcher failure cannot silently lose
the follow-up event.

`OutboxDispatcher` atomically claims rows with `FOR UPDATE SKIP LOCKED`, a processing lease, and one active event per
ordering key. Successful synchronous Spring event-bus delivery writes an `event_inbox` receipt and marks the outbox
row published. A receipt lets a recovered worker acknowledge an event whose event-bus delivery already completed.

Failures use exponential backoff. After the configured maximum attempts, the row moves to `DEAD_LETTER` and retains
the latest error for operations review. Defaults can be overridden with:

- `aionn.outbox.batch-size` (default `50`)
- `aionn.outbox.max-attempts` (default `10`)
- `aionn.outbox.poll-delay-ms` (default `1000`)

Delivery is at-least-once around process termination: an external provider may complete immediately before the JVM
stops and before its inbox receipt is committed. Provider-facing consumers must therefore continue to use provider
idempotency keys based on the stable `eventId`.
