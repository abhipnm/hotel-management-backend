-- Seed demo data so a freshly deployed instance (e.g. on a free-tier Postgres
-- such as Neon, Supabase, Render or Railway) has something to show immediately:
-- one restaurant, an admin + a staff login, tables with QR tokens, a full menu,
-- and a couple of completed/paid orders so the analytics dashboard isn't empty.
--
-- Demo staff logins (password for both: password123):
--   Admin  ->  admin@spicegarden.demo
--   Staff  ->  staff@spicegarden.demo
--
-- All rows use fixed UUIDs and ON CONFLICT DO NOTHING so this is safe to apply
-- to an empty database and won't clobber a real restaurant that reuses an email.
-- Remove this migration (and never let it reach a real production tenant) if you
-- don't want demo data seeded.

-- ---------------------------------------------------------------------------
-- Restaurant
-- ---------------------------------------------------------------------------
INSERT INTO restaurants (id, name, slug, address, phone, active, veg_only, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Spice Garden',
    'spice-garden',
    '42 Marine Drive, Mumbai 400020',
    '+91 22 4000 1234',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (slug) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Staff logins (BCrypt hash of "password123")
-- ---------------------------------------------------------------------------
INSERT INTO app_users (id, restaurant_id, name, email, password_hash, role, active, created_at, updated_at)
VALUES
    ('a1111111-0000-0000-0000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'Demo Admin', 'admin@spicegarden.demo',
     '$2a$10$KdXEvxUELeIgWkwhy1a8h.sDnqq/rF06zJJq.0D97sUFAfy0U4S42',
     'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a1111111-0000-0000-0000-000000000002',
     '11111111-1111-1111-1111-111111111111',
     'Demo Waiter', 'staff@spicegarden.demo',
     '$2a$10$KdXEvxUELeIgWkwhy1a8h.sDnqq/rF06zJJq.0D97sUFAfy0U4S42',
     'STAFF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Tables (qr_token is what the guest QR code deep-links with: ?qr=<token>)
-- ---------------------------------------------------------------------------
INSERT INTO restaurant_tables (id, restaurant_id, table_number, qr_token, active, created_at, updated_at)
VALUES
    ('b1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'T1', 'demo-qr-spice-garden-t1-9f2a1c', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'T2', 'demo-qr-spice-garden-t2-7b4d8e', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1111111-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'T3', 'demo-qr-spice-garden-t3-3c6f0a', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1111111-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'T4', 'demo-qr-spice-garden-t4-1e9a5b', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1111111-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'T5', 'demo-qr-spice-garden-t5-8d2c7f', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1111111-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'T6', 'demo-qr-spice-garden-t6-4a0e63', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (qr_token) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Menu categories
-- ---------------------------------------------------------------------------
INSERT INTO menu_categories (id, restaurant_id, name, display_order, active, created_at, updated_at)
VALUES
    ('c1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Starters',     1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Main Course',  2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1111111-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Breads & Rice',3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1111111-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'Desserts',     4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1111111-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'Beverages',    5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Menu items (food_type is one of VEG / EGG / NON_VEG)
-- ---------------------------------------------------------------------------
INSERT INTO menu_items (id, restaurant_id, category_id, name, description, price, image_url, food_type, available, display_order, created_at, updated_at)
VALUES
    -- Starters
    ('e1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000001', 'Paneer Tikka',       'Char-grilled cottage cheese marinated in spiced yoghurt', 289.00, NULL, 'VEG',     TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000001', 'Chicken 65',         'Crispy fried chicken tossed with curry leaves and chilli', 329.00, NULL, 'NON_VEG', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000001', 'Masala Papad',       'Crisp papad topped with onion, tomato and chaat masala',    99.00, NULL, 'VEG',     TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Main Course
    ('e1111111-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000002', 'Butter Chicken',     'Tandoori chicken in a rich tomato and butter gravy',       379.00, NULL, 'NON_VEG', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000002', 'Paneer Butter Masala','Cottage cheese simmered in a creamy makhani gravy',        319.00, NULL, 'VEG',     TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000002', 'Dal Makhani',        'Slow-cooked black lentils finished with cream',            249.00, NULL, 'VEG',     TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000002', 'Egg Curry',          'Boiled eggs in an onion-tomato masala',                    229.00, NULL, 'EGG',     TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Breads & Rice
    ('e1111111-0000-0000-0000-000000000008', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000003', 'Butter Naan',        'Leavened flatbread brushed with butter',                    59.00, NULL, 'VEG',     TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-000000000009', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000003', 'Garlic Naan',        'Naan topped with fresh garlic and coriander',               79.00, NULL, 'VEG',     TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-00000000000a', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000003', 'Jeera Rice',         'Basmati rice tempered with cumin',                         159.00, NULL, 'VEG',     TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Desserts
    ('e1111111-0000-0000-0000-00000000000b', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000004', 'Gulab Jamun',        'Warm milk-solid dumplings in cardamom syrup (2 pcs)',       119.00, NULL, 'VEG',     TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-00000000000c', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000004', 'Gelato',             'Vanilla bean gelato with a chocolate drizzle',              149.00, NULL, 'EGG',     TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Beverages
    ('e1111111-0000-0000-0000-00000000000d', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000005', 'Sweet Lassi',        'Chilled sweetened yoghurt drink',                            99.00, NULL, 'VEG',     TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-00000000000e', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000005', 'Masala Chai',        'Spiced Indian tea',                                          49.00, NULL, 'VEG',     TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('e1111111-0000-0000-0000-00000000000f', '11111111-1111-1111-1111-111111111111', 'c1111111-0000-0000-0000-000000000005', 'Fresh Lime Soda',    'Lime, soda and a choice of sweet or salted',                 79.00, NULL, 'VEG',     TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Two closed/paid guest sessions with served orders, so the analytics
-- dashboard has real sales data on first login.
-- ---------------------------------------------------------------------------
INSERT INTO guest_sessions (id, restaurant_id, table_id, guest_name, status, bill_requested, paid_at, expires_at, created_at, updated_at)
VALUES
    ('f1111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'b1111111-0000-0000-0000-000000000001', 'Riya',   'CLOSED', TRUE, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP + INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    ('f1111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'b1111111-0000-0000-0000-000000000003', 'Aarav',  'CLOSED', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 hours', CURRENT_TIMESTAMP + INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '90 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hours')
ON CONFLICT (id) DO NOTHING;

-- Order 1: Butter Chicken x1 (379) + Butter Naan x2 (118) + Sweet Lassi x1 (99) = 596.00
INSERT INTO orders (id, restaurant_id, table_id, guest_session_id, status, total_amount, notes, created_at, updated_at)
VALUES
    ('01111111-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'b1111111-0000-0000-0000-000000000001', 'f1111111-0000-0000-0000-000000000001', 'SERVED', 596.00, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours 10 minutes'),
-- Order 2: Paneer Butter Masala x1 (319) + Garlic Naan x2 (158) + Gulab Jamun x1 (119) = 596.00
    ('01111111-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'b1111111-0000-0000-0000-000000000003', 'f1111111-0000-0000-0000-000000000002', 'SERVED', 596.00, NULL, CURRENT_TIMESTAMP - INTERVAL '80 minutes', CURRENT_TIMESTAMP - INTERVAL '65 minutes')
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, menu_item_id, item_name_snapshot, item_price_snapshot, quantity, subtotal, notes, created_at, updated_at)
VALUES
    -- Order 1
    ('00111111-0000-0000-0000-000000000001', '01111111-0000-0000-0000-000000000001', 'e1111111-0000-0000-0000-000000000004', 'Butter Chicken', 379.00, 1, 379.00, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes'),
    ('00111111-0000-0000-0000-000000000002', '01111111-0000-0000-0000-000000000001', 'e1111111-0000-0000-0000-000000000008', 'Butter Naan',     59.00, 2, 118.00, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes'),
    ('00111111-0000-0000-0000-000000000003', '01111111-0000-0000-0000-000000000001', 'e1111111-0000-0000-0000-00000000000d', 'Sweet Lassi',     99.00, 1,  99.00, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes', CURRENT_TIMESTAMP - INTERVAL '2 hours 40 minutes'),
    -- Order 2
    ('00111111-0000-0000-0000-000000000004', '01111111-0000-0000-0000-000000000002', 'e1111111-0000-0000-0000-000000000005', 'Paneer Butter Masala', 319.00, 1, 319.00, NULL, CURRENT_TIMESTAMP - INTERVAL '80 minutes', CURRENT_TIMESTAMP - INTERVAL '80 minutes'),
    ('00111111-0000-0000-0000-000000000005', '01111111-0000-0000-0000-000000000002', 'e1111111-0000-0000-0000-000000000009', 'Garlic Naan',           79.00, 2, 158.00, NULL, CURRENT_TIMESTAMP - INTERVAL '80 minutes', CURRENT_TIMESTAMP - INTERVAL '80 minutes'),
    ('00111111-0000-0000-0000-000000000006', '01111111-0000-0000-0000-000000000002', 'e1111111-0000-0000-0000-00000000000b', 'Gulab Jamun',          119.00, 1, 119.00, NULL, CURRENT_TIMESTAMP - INTERVAL '80 minutes', CURRENT_TIMESTAMP - INTERVAL '80 minutes')
ON CONFLICT (id) DO NOTHING;
