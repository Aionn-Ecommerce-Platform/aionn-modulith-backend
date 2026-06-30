package com.aionn.sharedkernel.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionn.sharedkernel.domain.id.BaseId;
import org.junit.jupiter.api.Test;

class EntitySupportTest {

    @Test
    void entityRequiresIdAndUsesIdForEqualityAndHashCode() {
        TestEntity first = new TestEntity(new TestEntityId("01J00000000000000000000001"));
        TestEntity sameId = new TestEntity(new TestEntityId("01J00000000000000000000001"));
        TestEntity different = new TestEntity(new TestEntityId("01J00000000000000000000002"));

        assertEquals(first, sameId);
        assertEquals(first.hashCode(), sameId.hashCode());
        assertNotEquals(first, different);
        assertNotEquals(first, "not-an-entity");
        assertThrows(NullPointerException.class, () -> new TestEntity(null));
    }

    static final class TestEntity extends Entity<TestEntityId> {
        TestEntity(TestEntityId id) {
            super(id);
        }
    }

    static final class TestEntityId extends BaseId {
        TestEntityId(String value) {
            super(value);
        }
    }
}
