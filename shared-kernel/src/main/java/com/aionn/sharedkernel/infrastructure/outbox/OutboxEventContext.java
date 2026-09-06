package com.aionn.sharedkernel.infrastructure.outbox;

public final class OutboxEventContext {

    private static final ThreadLocal<String> CURRENT_EVENT_ID = new ThreadLocal<>();

    private OutboxEventContext() {
    }

    public static Scope open(String eventId) {
        CURRENT_EVENT_ID.set(eventId);
        return new Scope();
    }

    public static String currentEventId() {
        return CURRENT_EVENT_ID.get();
    }

    public static final class Scope implements AutoCloseable {
        private Scope() {
        }

        @Override
        public void close() {
            CURRENT_EVENT_ID.remove();
        }
    }
}
