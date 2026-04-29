ALTER TABLE discounts
    DROP COLUMN stackable,
    DROP COLUMN priority;

ALTER TABLE order_applied_discounts
    DROP COLUMN stackable,
    DROP COLUMN priority;
