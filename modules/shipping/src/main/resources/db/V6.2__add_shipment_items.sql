CREATE TABLE shipment_items (
    shipment_id VARCHAR(50) NOT NULL REFERENCES shipments (shipment_id) ON DELETE CASCADE,
    order_id VARCHAR(50) NOT NULL,
    sku_id VARCHAR(50) NOT NULL,
    qty INT NOT NULL CHECK (qty > 0),
    PRIMARY KEY (shipment_id, sku_id)
);

CREATE INDEX idx_shipment_items_order_sku
    ON shipment_items (order_id, sku_id);
