-- Increase length of type and status columns in discounts table to prevent truncation errors
-- and ensure compatibility with newer Enum values like 'SHIPPING'
ALTER TABLE discounts MODIFY COLUMN type VARCHAR(50) NOT NULL;
ALTER TABLE discounts MODIFY COLUMN status VARCHAR(50) NOT NULL;

-- Also ensure discount_unit has enough space just in case
ALTER TABLE discounts MODIFY COLUMN discount_unit VARCHAR(50) DEFAULT 'AMOUNT';
