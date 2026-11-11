CREATE TABLE order_return_items (
    return_id     VARCHAR(50) NOT NULL,
    sku_id        VARCHAR(50) NOT NULL,
    qty           INT         NOT NULL,
    unit_price    NUMERIC(18,2) NOT NULL,
    refund_amount NUMERIC(18,2) NOT NULL,
    PRIMARY KEY (return_id, sku_id),
    CONSTRAINT fk_return_items_return FOREIGN KEY (return_id)
        REFERENCES order_returns(return_id) ON DELETE CASCADE,
    CONSTRAINT chk_return_items_qty CHECK (qty > 0)
);
