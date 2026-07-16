CREATE TABLE notifications (
    id                UUID PRIMARY KEY,
    restaurant_id     UUID NOT NULL REFERENCES restaurants(id),
    guest_session_id  UUID REFERENCES guest_sessions(id),
    audience          VARCHAR(10) NOT NULL,
    type              VARCHAR(30) NOT NULL,
    title             VARCHAR(150) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    read              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_notifications_staff ON notifications (restaurant_id, audience, created_at DESC);
CREATE INDEX idx_notifications_guest ON notifications (restaurant_id, guest_session_id, created_at DESC);

ALTER TABLE menu_items
    ADD COLUMN stock_quantity INTEGER,
    ADD COLUMN low_stock_threshold INTEGER;
