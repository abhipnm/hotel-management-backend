CREATE TABLE activity_log (
    id             UUID           PRIMARY KEY,
    restaurant_id  UUID           NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    actor_name     VARCHAR(100)   NOT NULL,
    action         VARCHAR(50)    NOT NULL,
    description    VARCHAR(500)   NOT NULL,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);
CREATE INDEX idx_activity_log_restaurant_created ON activity_log(restaurant_id, created_at DESC);
