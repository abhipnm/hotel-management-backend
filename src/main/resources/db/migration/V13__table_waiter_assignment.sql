ALTER TABLE restaurant_tables
    ADD COLUMN assigned_waiter_id UUID REFERENCES app_users(id) ON DELETE SET NULL;

CREATE INDEX idx_restaurant_tables_assigned_waiter ON restaurant_tables(assigned_waiter_id);
