package com.aionn.sharedkernel.domain.model;

public class TestAggregate extends AggregateRoot {

    private static final String FIXED_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Override
    protected String aggregateId() {
        return FIXED_ID;
    }

    public void recordEvent(DomainEvent event) {
        registerEvent(event);
    }

    public String fixedAggregateId() {
        return FIXED_ID;
    }

    public String exposedAggregateType() {
        return aggregateType();
    }
}
