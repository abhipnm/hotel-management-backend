ALTER TABLE menu_items ADD COLUMN food_type VARCHAR(10) NOT NULL DEFAULT 'NON_VEG';

UPDATE menu_items SET food_type = CASE WHEN vegetarian THEN 'VEG' ELSE 'NON_VEG' END;

ALTER TABLE menu_items ALTER COLUMN food_type DROP DEFAULT;
ALTER TABLE menu_items DROP COLUMN vegetarian;
