-- Thêm cột discount_unit vào bảng discounts
ALTER TABLE discounts ADD COLUMN discount_unit VARCHAR(20) DEFAULT 'AMOUNT' AFTER type;

-- Cập nhật mô tả hoặc kiểm tra nếu cần (tùy thuộc vào việc bạn có dùng CHECK constraint không)
-- Ở đây chúng ta chỉ cần thêm cột để lưu Enum mới.
