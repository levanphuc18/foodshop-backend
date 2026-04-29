ALTER TABLE discounts
    ADD COLUMN usage_limit INT NULL,
    ADD COLUMN used_count INT NOT NULL DEFAULT 0,
    ADD COLUMN per_user_limit INT NULL,
    ADD COLUMN stackable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN priority INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS order_applied_discounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    discount_id INT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_unit VARCHAR(20) NULL,
    value DECIMAL(10,2) NOT NULL,
    applied_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_applied_discounts_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_order_applied_discounts_discount FOREIGN KEY (discount_id) REFERENCES discounts(discount_id)
);

CREATE INDEX idx_order_applied_discounts_order_id ON order_applied_discounts(order_id);
CREATE INDEX idx_order_applied_discounts_discount_id ON order_applied_discounts(discount_id);
