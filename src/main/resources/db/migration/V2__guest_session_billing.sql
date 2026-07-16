ALTER TABLE guest_sessions
    ADD COLUMN bill_requested BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN paid_at        TIMESTAMP;
