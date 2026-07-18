CREATE TABLE coupons (
    id                  UUID           PRIMARY KEY,
    restaurant_id       UUID           NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    code                VARCHAR(30)    NOT NULL,
    discount_type       VARCHAR(20)    NOT NULL,
    discount_value      NUMERIC(10,2)  NOT NULL,
    min_order_amount    NUMERIC(10,2),
    max_uses            INTEGER,
    used_count          INTEGER        NOT NULL DEFAULT 0,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL,
    updated_at          TIMESTAMP      NOT NULL,
    CONSTRAINT uq_coupons_restaurant_code UNIQUE (restaurant_id, code)
);
CREATE INDEX idx_coupons_restaurant_id ON coupons(restaurant_id);

ALTER TABLE orders
    ADD COLUMN discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN coupon_code VARCHAR(30);
