-- Restaurant Manager: initial schema
-- IDs are UUIDs generated application-side by Hibernate, so no DB-side
-- default/generator function is required on the id columns.

CREATE TABLE restaurants (
    id          UUID PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(80)  NOT NULL,
    address     VARCHAR(250),
    phone       VARCHAR(30),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_restaurants_slug UNIQUE (slug)
);

CREATE TABLE app_users (
    id             UUID PRIMARY KEY,
    restaurant_id  UUID          NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name           VARCHAR(100)  NOT NULL,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    role           VARCHAR(20)   NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    CONSTRAINT uq_app_users_email UNIQUE (email)
);
CREATE INDEX idx_app_users_restaurant_id ON app_users(restaurant_id);

CREATE TABLE restaurant_tables (
    id             UUID PRIMARY KEY,
    restaurant_id  UUID         NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_number   VARCHAR(20)  NOT NULL,
    qr_token       VARCHAR(64)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uq_restaurant_tables_qr_token UNIQUE (qr_token),
    CONSTRAINT uq_restaurant_tables_number UNIQUE (restaurant_id, table_number)
);
CREATE INDEX idx_restaurant_tables_restaurant_id ON restaurant_tables(restaurant_id);

CREATE TABLE menu_categories (
    id             UUID          PRIMARY KEY,
    restaurant_id  UUID          NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name           VARCHAR(100)  NOT NULL,
    display_order  INTEGER       NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_menu_categories_restaurant_id ON menu_categories(restaurant_id);

CREATE TABLE menu_items (
    id             UUID           PRIMARY KEY,
    restaurant_id  UUID           NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    category_id    UUID           NOT NULL REFERENCES menu_categories(id) ON DELETE CASCADE,
    name           VARCHAR(150)   NOT NULL,
    description    VARCHAR(1000),
    price          NUMERIC(10,2)  NOT NULL,
    image_url      VARCHAR(500),
    vegetarian     BOOLEAN        NOT NULL DEFAULT FALSE,
    available      BOOLEAN        NOT NULL DEFAULT TRUE,
    display_order  INTEGER        NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL
);
CREATE INDEX idx_menu_items_restaurant_id ON menu_items(restaurant_id);
CREATE INDEX idx_menu_items_category_id ON menu_items(category_id);

CREATE TABLE guest_sessions (
    id             UUID          PRIMARY KEY,
    restaurant_id  UUID          NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_id       UUID          NOT NULL REFERENCES restaurant_tables(id) ON DELETE CASCADE,
    guest_name     VARCHAR(100)  NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    expires_at     TIMESTAMP     NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_guest_sessions_restaurant_id ON guest_sessions(restaurant_id);
CREATE INDEX idx_guest_sessions_table_id ON guest_sessions(table_id);

CREATE TABLE orders (
    id                UUID           PRIMARY KEY,
    restaurant_id     UUID           NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_id          UUID           NOT NULL REFERENCES restaurant_tables(id) ON DELETE CASCADE,
    guest_session_id  UUID           NOT NULL REFERENCES guest_sessions(id) ON DELETE CASCADE,
    status            VARCHAR(20)    NOT NULL,
    total_amount      NUMERIC(10,2)  NOT NULL,
    notes             VARCHAR(500),
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP      NOT NULL
);
CREATE INDEX idx_orders_restaurant_id ON orders(restaurant_id);
CREATE INDEX idx_orders_restaurant_id_status ON orders(restaurant_id, status);
CREATE INDEX idx_orders_guest_session_id ON orders(guest_session_id);

CREATE TABLE order_items (
    id                    UUID           PRIMARY KEY,
    order_id              UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id          UUID           NOT NULL REFERENCES menu_items(id),
    item_name_snapshot    VARCHAR(150)   NOT NULL,
    item_price_snapshot   NUMERIC(10,2)  NOT NULL,
    quantity              INTEGER        NOT NULL,
    subtotal              NUMERIC(10,2)  NOT NULL,
    notes                 VARCHAR(300),
    created_at            TIMESTAMP      NOT NULL,
    updated_at            TIMESTAMP      NOT NULL
);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
