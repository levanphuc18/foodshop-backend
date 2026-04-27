DROP TABLE IF EXISTS test_flyway;

CREATE TABLE IF NOT EXISTS users (
                                     user_id INT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(40) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address VARCHAR(500) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) NOT NULL
    );

CREATE TABLE IF NOT EXISTS categories (
                                          category_id INT PRIMARY KEY AUTO_INCREMENT,
                                          name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS discounts (
                                         discount_id INT PRIMARY KEY AUTO_INCREMENT,
                                         code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) NULL,
    max_discount DECIMAL(10,2) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL
    );

CREATE TABLE IF NOT EXISTS products (
                                        product_id INT PRIMARY KEY AUTO_INCREMENT,
                                        name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    discount_id INT NULL,
    category_id INT NOT NULL,
    CONSTRAINT fk_products_discount FOREIGN KEY (discount_id) REFERENCES discounts(discount_id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id)
    );

CREATE TABLE IF NOT EXISTS product_images (
                                              id INT PRIMARY KEY AUTO_INCREMENT,
                                              image_url VARCHAR(255) NOT NULL,
    product_id INT NOT NULL,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(product_id)
    );

CREATE TABLE IF NOT EXISTS cart_item (
                                         user_id INT NOT NULL,
                                         product_id INT NOT NULL,
                                         quantity INT NOT NULL,
                                         PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_cart_item_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES products(product_id)
    );

CREATE TABLE IF NOT EXISTS orders (
                                      order_id INT PRIMARY KEY AUTO_INCREMENT,
                                      shipping_address VARCHAR(255) NOT NULL,
    shipping_note VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    final_amount DECIMAL(10,2) NOT NULL,
    user_id INT NOT NULL,
    discount_id INT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_orders_discount FOREIGN KEY (discount_id) REFERENCES discounts(discount_id)
    );

ALTER TABLE orders
    MODIFY COLUMN discount_id INT NULL;

CREATE TABLE IF NOT EXISTS order_items (
                                           order_item_id INT PRIMARY KEY AUTO_INCREMENT,
                                           order_id INT NOT NULL,
                                           product_id INT NOT NULL,
                                           quantity INT NOT NULL,
                                           price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id)
    );

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              refresh_token VARCHAR(512) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );

CREATE TABLE IF NOT EXISTS messages (
                                        message_id INT PRIMARY KEY AUTO_INCREMENT,
                                        sender_id INT NOT NULL,
                                        receiver_id INT NOT NULL,
                                        content TEXT NOT NULL,
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_messages_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id)
    );

CREATE TEMPORARY TABLE tmp_digits (
    n INT PRIMARY KEY
);

INSERT INTO tmp_digits (n) VALUES
                               (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TEMPORARY TABLE tmp_digits_b AS
SELECT n FROM tmp_digits;

CREATE TEMPORARY TABLE tmp_digits_c AS
SELECT n FROM tmp_digits;

CREATE TEMPORARY TABLE tmp_seq_50 AS
SELECT ones.n + tens.n * 10 AS n
FROM tmp_digits ones
         CROSS JOIN tmp_digits_b tens
WHERE ones.n + tens.n * 10 <= 50;

CREATE TEMPORARY TABLE tmp_seq_300 AS
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1 AS n
FROM tmp_digits ones
         CROSS JOIN tmp_digits_b tens
         CROSS JOIN tmp_digits_c hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 < 300;

CREATE TEMPORARY TABLE tmp_seq_500 AS
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1 AS n
FROM tmp_digits ones
         CROSS JOIN tmp_digits_b tens
         CROSS JOIN tmp_digits_c hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 < 500;

DELETE FROM messages;
DELETE FROM refresh_tokens;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM cart_item;
DELETE FROM product_images;
DELETE FROM products;
DELETE FROM discounts;
DELETE FROM categories;
DELETE FROM users;

ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;
ALTER TABLE discounts AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE product_images AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE refresh_tokens AUTO_INCREMENT = 1;
ALTER TABLE messages AUTO_INCREMENT = 1;

INSERT INTO users (
    user_id, username, password, email, full_name, phone_number, address, created_at, role
) VALUES (
             1,
             'admin',
             '$2a$10$e6osB6vOjot8IjiObQQmUu..fzXWmLWP1nCauCft5f2HfaHHbGeX.',
             'admin@foodshop.local',
             'System Admin',
             '0900000000',
             '1 Admin Street, District 1, Ho Chi Minh City',
             '2026-01-01 08:00:00',
             'ADMIN'
         );

INSERT INTO users (
    username, password, email, full_name, phone_number, address, created_at, role
)
SELECT
    CASE WHEN n = 0 THEN 'user' ELSE CONCAT('user', n) END,
    '$2a$10$e6osB6vOjot8IjiObQQmUu..fzXWmLWP1nCauCft5f2HfaHHbGeX.',
    CASE WHEN n = 0 THEN 'user@foodshop.local' ELSE CONCAT('user', n, '@foodshop.local') END,
    CASE WHEN n = 0 THEN 'Demo Customer' ELSE CONCAT('Demo Customer ', n) END,
    CONCAT('09', LPAD(10000000 + n, 8, '0')),
    CONCAT(10 + n, ' Seed Street, Ward ', MOD(n, 9) + 1, ', District ', MOD(n, 12) + 1, ', Ho Chi Minh City'),
    DATE_ADD('2026-01-02 08:00:00', INTERVAL n DAY),
    'CUSTOMER'
FROM tmp_seq_50
ORDER BY n;

INSERT INTO categories (category_id, name, description, image_url, created_at) VALUES
                                                                                   (1, 'Rau la', 'Danh muc rau la va rau xanh tuoi moi ngay.', 'https://picsum.photos/seed/category-1/800/600', '2026-01-05 08:00:00'),
                                                                                   (2, 'Cu qua', 'Danh muc cu qua thong dung cho bua an gia dinh.', 'https://picsum.photos/seed/category-2/800/600', '2026-01-05 08:05:00'),
                                                                                   (3, 'Trai cay', 'Trai cay noi dia va nhap khau theo mua.', 'https://picsum.photos/seed/category-3/800/600', '2026-01-05 08:10:00'),
                                                                                   (4, 'Do uong', 'Nuoc ep, nuoc giai khat va do uong dong chai.', 'https://picsum.photos/seed/category-4/800/600', '2026-01-05 08:15:00'),
                                                                                   (5, 'Sua va trung', 'Sua tuoi, sua hat, sua chua va cac loai trung.', 'https://picsum.photos/seed/category-5/800/600', '2026-01-05 08:20:00'),
                                                                                   (6, 'Do kho', 'Ngu coc, hat va thuc pham dong goi kho.', 'https://picsum.photos/seed/category-6/800/600', '2026-01-05 08:25:00'),
                                                                                   (7, 'Gia vi', 'Gia vi nau an va nuoc cham co ban.', 'https://picsum.photos/seed/category-7/800/600', '2026-01-05 08:30:00'),
                                                                                   (8, 'Thit tuoi', 'Thit heo, bo, ga dong goi an toan.', 'https://picsum.photos/seed/category-8/800/600', '2026-01-05 08:35:00'),
                                                                                   (9, 'Hai san', 'Ca, tom, muc va hai san dong lanh.', 'https://picsum.photos/seed/category-9/800/600', '2026-01-05 08:40:00'),
                                                                                   (10, 'Dong lanh', 'Thuc pham dong lanh va do an tien loi.', 'https://picsum.photos/seed/category-10/800/600', '2026-01-05 08:45:00'),
                                                                                   (11, 'Banh ngot', 'Banh ngot, snack va mon an vat dong goi.', 'https://picsum.photos/seed/category-11/800/600', '2026-01-05 08:50:00'),
                                                                                   (12, 'Mi pho', 'Mi goi, nui, bun va thuc pham an lien.', 'https://picsum.photos/seed/category-12/800/600', '2026-01-05 08:55:00'),
                                                                                   (13, 'Thuc pham huu co', 'Nhom san pham huu co cho khach hang cao cap.', 'https://picsum.photos/seed/category-13/800/600', '2026-01-05 09:00:00'),
                                                                                   (14, 'Do hop', 'Ca hop, thit hop, rau cu dong hop.', 'https://picsum.photos/seed/category-14/800/600', '2026-01-05 09:05:00'),
                                                                                   (15, 'Do an sang', 'Ngu coc, granola va cac mon an sang nhanh.', 'https://picsum.photos/seed/category-15/800/600', '2026-01-05 09:10:00');

INSERT INTO discounts (
    discount_id, code, type, value, min_order_amount, max_discount, start_date, end_date, status
) VALUES
      (1, 'ORDER10', 'ORDER', 10.00, 150000.00, 80000.00, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (2, 'ORDER15', 'ORDER', 15.00, 300000.00, 120000.00, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (3, 'ORDER20', 'ORDER', 20.00, 500000.00, 150000.00, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (4, 'PRODUCT05', 'PRODUCT', 5.00, NULL, NULL, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (5, 'PRODUCT10', 'PRODUCT', 10.00, NULL, NULL, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (6, 'PRODUCT15', 'PRODUCT', 15.00, NULL, NULL, '2026-01-01', '2026-12-31', 'ACTIVE'),
      (7, 'SPRING25', 'ORDER', 25.00, 700000.00, 200000.00, '2025-01-01', '2025-12-31', 'EXPIRED'),
      (8, 'CLEARANCE20', 'PRODUCT', 20.00, NULL, NULL, '2025-01-01', '2025-12-31', 'EXPIRED');

INSERT INTO products (
    name, description, price, quantity, created_at, updated_at, discount_id, category_id
)
SELECT
    CONCAT(c.name, ' Product ', LPAD(s.n, 3, '0')),
    CONCAT('San pham seed ', LPAD(s.n, 3, '0'), ' thuoc danh muc ', c.name, '.'),
    ROUND(12000 + s.n * 1750 + c.category_id * 2500, 2),
    20 + MOD(s.n * 3, 180),
    DATE_ADD('2026-02-01 08:00:00', INTERVAL s.n HOUR),
    DATE_ADD('2026-02-05 08:00:00', INTERVAL s.n HOUR),
    CASE
        WHEN MOD(s.n, 10) IN (1, 2) THEN 4
        WHEN MOD(s.n, 10) IN (3, 4) THEN 5
        WHEN MOD(s.n, 10) IN (5, 6) THEN 6
        ELSE NULL
        END,
    c.category_id
FROM tmp_seq_300 s
         JOIN categories c ON c.category_id = MOD(s.n - 1, 15) + 1
ORDER BY s.n;

INSERT INTO product_images (image_url, product_id)
SELECT
    CONCAT('https://picsum.photos/seed/product-', product_id, '/900/700'),
    product_id
FROM products
ORDER BY product_id;

INSERT INTO product_images (image_url, product_id)
SELECT
    CONCAT('https://picsum.photos/seed/product-', product_id, '-2/900/700'),
    product_id
FROM products
ORDER BY product_id;

INSERT INTO product_images (image_url, product_id)
SELECT
    CONCAT('https://picsum.photos/seed/product-', product_id, '-3/900/700'),
    product_id
FROM products
ORDER BY product_id;

INSERT INTO product_images (image_url, product_id)
SELECT
    CONCAT('https://picsum.photos/seed/product-', product_id, '-4/900/700'),
    product_id
FROM products
ORDER BY product_id;

INSERT INTO cart_item (user_id, product_id, quantity)
SELECT
    u.user_id,
    p.product_id,
    MOD(u.user_id + p.product_id, 5) + 1
FROM users u
         JOIN products p ON MOD(u.user_id * 1000 + p.product_id, 7) = 0
WHERE u.role = 'CUSTOMER'
ORDER BY u.user_id, p.product_id
    LIMIT 500;

INSERT INTO orders (
    shipping_address, shipping_note, status, created_at, total_amount, discount_amount, final_amount, user_id, discount_id
)
SELECT
    u.address,
    CONCAT('Seed order ', LPAD(s.n, 3, '0')),
    CASE MOD(s.n, 5)
        WHEN 1 THEN 'PENDING'
        WHEN 2 THEN 'CONFIRMED'
        WHEN 3 THEN 'SHIPPED'
        WHEN 4 THEN 'COMPLETED'
        ELSE 'CANCELLED'
        END,
    DATE_ADD('2026-03-01 09:00:00', INTERVAL s.n HOUR),
    0.01,
    0.00,
    0.01,
    u.user_id,
    CASE
        WHEN MOD(s.n, 10) = 0 THEN 3
        WHEN MOD(s.n, 5) = 0 THEN 2
        WHEN MOD(s.n, 3) = 0 THEN 1
        ELSE 1
        END
FROM tmp_seq_300 s
         JOIN users u ON u.user_id = 2 + MOD(s.n - 1, 50)
ORDER BY s.n;

INSERT INTO order_items (order_id, product_id, quantity, price, subtotal)
SELECT
    o.order_id,
    p.product_id,
    1 + MOD(o.order_id + line_no.line_no, 3),
    p.price,
    ROUND(p.price * (1 + MOD(o.order_id + line_no.line_no, 3)), 2)
FROM orders o
         JOIN (
    SELECT 1 AS line_no
    UNION ALL
    SELECT 2
    UNION ALL
    SELECT 3
) line_no
         JOIN products p ON p.product_id = MOD(o.order_id * 11 + line_no.line_no * 7 - 1, 300) + 1
ORDER BY o.order_id, line_no.line_no;

UPDATE orders o
    JOIN (
    SELECT order_id, ROUND(SUM(subtotal), 2) AS total_amount
    FROM order_items
    GROUP BY order_id
    ) totals ON totals.order_id = o.order_id
    LEFT JOIN discounts d ON d.discount_id = o.discount_id
    SET
        o.total_amount = totals.total_amount,
        o.discount_amount = CASE
        WHEN d.discount_id IS NULL THEN 0.00
        WHEN d.type <> 'ORDER' THEN 0.00
        WHEN d.min_order_amount IS NOT NULL AND totals.total_amount < d.min_order_amount THEN 0.00
        WHEN d.max_discount IS NOT NULL THEN LEAST(ROUND(totals.total_amount * d.value / 100, 2), d.max_discount)
        ELSE ROUND(totals.total_amount * d.value / 100, 2)
END,
    o.final_amount = totals.total_amount - CASE
        WHEN d.discount_id IS NULL THEN 0.00
        WHEN d.type <> 'ORDER' THEN 0.00
        WHEN d.min_order_amount IS NOT NULL AND totals.total_amount < d.min_order_amount THEN 0.00
        WHEN d.max_discount IS NOT NULL THEN LEAST(ROUND(totals.total_amount * d.value / 100, 2), d.max_discount)
        ELSE ROUND(totals.total_amount * d.value / 100, 2)
END;

INSERT INTO refresh_tokens (refresh_token, expiry_date, user_id)
SELECT
    CONCAT('seed-refresh-token-', user_id),
    '2026-12-31 23:59:59',
    user_id
FROM users
WHERE role = 'CUSTOMER'
ORDER BY user_id;

INSERT INTO messages (sender_id, receiver_id, content, created_at)
SELECT
    CASE WHEN MOD(s.n, 2) = 1 THEN 2 + MOD(s.n - 1, 50) ELSE 1 END,
    CASE WHEN MOD(s.n, 2) = 1 THEN 1 ELSE 2 + MOD(s.n - 1, 50) END,
    CONCAT('Seed message ', LPAD(s.n, 3, '0'), ' generated for demo chat history.'),
    DATE_ADD('2026-03-15 08:00:00', INTERVAL s.n MINUTE)
FROM tmp_seq_500 s
ORDER BY s.n;

DROP TEMPORARY TABLE IF EXISTS tmp_seq_500;
DROP TEMPORARY TABLE IF EXISTS tmp_seq_300;
DROP TEMPORARY TABLE IF EXISTS tmp_seq_50;
DROP TEMPORARY TABLE IF EXISTS tmp_digits_c;
DROP TEMPORARY TABLE IF EXISTS tmp_digits_b;
DROP TEMPORARY TABLE IF EXISTS tmp_digits;