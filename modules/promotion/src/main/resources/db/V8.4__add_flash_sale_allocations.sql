CREATE TABLE flash_sale_allocations (
    order_id        VARCHAR(50) NOT NULL,
    registration_id VARCHAR(50) NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at     TIMESTAMPTZ,
    PRIMARY KEY (order_id, registration_id),
    CONSTRAINT fk_flash_sale_allocation_registration
        FOREIGN KEY (registration_id) REFERENCES flash_sale_registrations(registration_id)
);

CREATE INDEX idx_flash_sale_allocations_active
    ON flash_sale_allocations(order_id)
    WHERE released_at IS NULL;
