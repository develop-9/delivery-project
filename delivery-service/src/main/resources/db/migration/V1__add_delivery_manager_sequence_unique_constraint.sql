CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_manager_sequence_scope
    ON delivery_schema.p_delivery_manager_sequences
    (manager_type, hub_id)
    NULLS NOT DISTINCT;