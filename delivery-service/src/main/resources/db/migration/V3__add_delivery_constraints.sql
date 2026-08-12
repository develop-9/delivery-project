CREATE UNIQUE INDEX uq_hub_delivery_sequence
    ON delivery_schema.p_delivery_managers (delivery_sequence)
    WHERE type = 'HUB_DELIVERY'
      AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_company_delivery_sequence
    ON delivery_schema.p_delivery_managers (hub_id, delivery_sequence)
    WHERE type = 'COMPANY_DELIVERY'
      AND deleted_at IS NULL;

ALTER TABLE delivery_schema.p_delivery_routes
    ADD CONSTRAINT chk_delivery_route_sequence_positive
        CHECK (sequence > 0);