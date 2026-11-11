package com.aionn.sharedkernel.domain.model;

import com.aionn.sharedkernel.domain.id.BaseId;

import java.util.Objects;

public abstract class Entity<I extends BaseId> {

    protected final I id;

    protected Entity(I id) {
        this.id = Objects.requireNonNull(id, "Entity ID must not be null");
    }

    public I getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Entity<?> entity))
            return false;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
