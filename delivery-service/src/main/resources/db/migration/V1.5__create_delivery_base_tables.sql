CREATE TABLE IF NOT EXISTS delivery_schema.p_deliveries (
                                                            id UUID PRIMARY KEY,
                                                            order_id UUID NOT NULL,
                                                            departure_hub_id UUID NOT NULL,
                                                            destination_hub_id UUID NOT NULL,
                                                            delivery_address VARCHAR(255) NOT NULL,
    receiver_name VARCHAR(100) NOT NULL,
    receiver_slack_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    company_delivery_manager_id UUID,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
    );

CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_routes (
                                                                 id UUID PRIMARY KEY,
                                                                 delivery_id UUID NOT NULL,
                                                                 sequence INTEGER NOT NULL,
                                                                 departure_hub_id UUID NOT NULL,
                                                                 arrival_hub_id UUID NOT NULL,
                                                                 estimated_distance_km NUMERIC(10, 2) NOT NULL,
    estimated_duration_min INTEGER NOT NULL,
    actual_distance_km NUMERIC(10, 2),
    actual_duration_min INTEGER,
    status VARCHAR(30) NOT NULL,
    delivery_manager_id UUID,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
    );

CREATE TABLE IF NOT EXISTS delivery_schema.p_delivery_managers (
                                                                   id UUID PRIMARY KEY,
                                                                   user_id UUID NOT NULL UNIQUE,
                                                                   hub_id UUID,
                                                                   type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    delivery_sequence INTEGER NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
    );