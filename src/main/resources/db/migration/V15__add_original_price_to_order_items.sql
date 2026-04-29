ALTER TABLE order_items
    ADD COLUMN original_price DECIMAL(10,2) NULL AFTER price;

UPDATE order_items
SET original_price = price
WHERE original_price IS NULL;

ALTER TABLE order_items
    MODIFY COLUMN original_price DECIMAL(10,2) NOT NULL;
