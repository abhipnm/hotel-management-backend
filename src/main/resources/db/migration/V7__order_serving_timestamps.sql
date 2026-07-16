-- Tracks when an order moved to ACCEPTED and SERVED, so average serving time
-- can be computed. Nullable: unset for orders that haven't reached that stage
-- (or were placed before this migration).
ALTER TABLE orders
    ADD COLUMN accepted_at TIMESTAMP,
    ADD COLUMN served_at   TIMESTAMP;
