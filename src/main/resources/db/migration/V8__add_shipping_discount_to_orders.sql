-- Add shipping_fee and shipping_discount columns to orders table
ALTER TABLE orders 
ADD COLUMN shipping_fee DECIMAL(10, 2) DEFAULT 0.00 AFTER discount_amount,
ADD COLUMN shipping_discount DECIMAL(10, 2) DEFAULT 0.00 AFTER shipping_fee;
