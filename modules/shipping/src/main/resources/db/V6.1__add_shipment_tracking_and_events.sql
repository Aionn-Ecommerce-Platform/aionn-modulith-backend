ALTER TABLE shipments
    ADD COLUMN registered_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN in_transit_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN out_for_delivery_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE shipment_events (
    event_id VARCHAR(50) PRIMARY KEY,
    shipment_id VARCHAR(50) NOT NULL REFERENCES shipments (shipment_id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipment_events_shipment
    ON shipment_events (shipment_id, occurred_at);
