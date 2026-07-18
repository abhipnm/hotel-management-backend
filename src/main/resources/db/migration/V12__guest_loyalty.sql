ALTER TABLE guest_sessions
    ADD COLUMN guest_phone VARCHAR(20);

CREATE INDEX idx_guest_sessions_restaurant_phone ON guest_sessions(restaurant_id, guest_phone);
