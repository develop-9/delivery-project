CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_manager_sequences (
    id UUID PRIMARY KEY,
    manager_type VARCHAR(30) NOT NULL,
    hub_id UUID,
    next_sequence INTEGER NOT NULL,
    last_assigned_sequence INTEGER NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_delivery_manager_sequence_scope
    ON delivery_schema.p_delivery_manager_sequences
    (manager_type, hub_id)
    NULLS NOT DISTINCT;