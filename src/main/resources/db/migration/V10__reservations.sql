CREATE TABLE reservations (
    id                 UUID           PRIMARY KEY,
    restaurant_id      UUID           NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_id           UUID           REFERENCES restaurant_tables(id) ON DELETE SET NULL,
    guest_name         VARCHAR(100)   NOT NULL,
    guest_phone        VARCHAR(20)    NOT NULL,
    party_size         INTEGER        NOT NULL,
    reservation_time   TIMESTAMP      NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    notes              VARCHAR(500),
    created_at         TIMESTAMP      NOT NULL,
    updated_at         TIMESTAMP      NOT NULL
);
CREATE INDEX idx_reservations_restaurant_time ON reservations(restaurant_id, reservation_time);
CREATE INDEX idx_reservations_restaurant_status ON reservations(restaurant_id, status);
