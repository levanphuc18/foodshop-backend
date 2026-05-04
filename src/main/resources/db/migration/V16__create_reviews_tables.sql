-- V16: Create reviews & review_images tables + add rating cache to products

CREATE TABLE IF NOT EXISTS reviews (
    review_id    INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL,
    product_id   INT          NOT NULL,
    order_id     INT          NOT NULL,
    order_item_id INT         NOT NULL,
    rating       TINYINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uq_user_orderitem (user_id, order_item_id),
    KEY idx_product_rating (product_id, rating),
    KEY idx_order (order_id),
    CONSTRAINT fk_review_user      FOREIGN KEY (user_id)       REFERENCES users(user_id),
    CONSTRAINT fk_review_product   FOREIGN KEY (product_id)    REFERENCES products(product_id),
    CONSTRAINT fk_review_order     FOREIGN KEY (order_id)      REFERENCES orders(order_id),
    CONSTRAINT fk_review_orderitem FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_images (
    review_image_id INT          NOT NULL AUTO_INCREMENT,
    review_id       INT          NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    PRIMARY KEY (review_image_id),
    CONSTRAINT fk_reviewimg_review FOREIGN KEY (review_id) REFERENCES reviews(review_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- We handle the products table columns in FlywayConfig.java directly to avoid MySQL Add Column IF NOT EXISTS syntax issues.
