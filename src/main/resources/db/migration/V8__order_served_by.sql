ALTER TABLE orders
    ADD COLUMN served_by_user_id UUID REFERENCES app_users(id);
