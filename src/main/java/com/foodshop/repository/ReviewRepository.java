package com.foodshop.repository;

import com.foodshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    /** Kiem tra xem danh gia da ton tai cho user va orderItem nay chua */
    boolean existsByUser_UserIdAndOrderItem_OrderItemId(Integer userId, Integer orderItemId);

    /** Tim danh gia dua vao user va orderItem */
    Optional<Review> findByUser_UserIdAndOrderItem_OrderItemId(Integer userId, Integer orderItemId);

    /** Tat ca danh gia cua mot san pham - co ho tro phan trang */
    Page<Review> findByProduct_ProductId(Integer productId, Pageable pageable);

    /** Tat ca danh gia cua mot san pham duoc loc theo so sao */
    Page<Review> findByProduct_ProductIdAndRating(Integer productId, Integer rating, Pageable pageable);

    /** Danh gia cua san pham co it nhat mot hinh anh */
    @Query("SELECT r FROM Review r WHERE r.product.productId = :productId AND SIZE(r.images) > 0")
    Page<Review> findByProductIdWithImages(@Param("productId") Integer productId, Pageable pageable);

    /** Danh gia cua san pham duoc loc theo so sao VA co hinh anh */
    @Query("SELECT r FROM Review r WHERE r.product.productId = :productId AND r.rating = :rating AND SIZE(r.images) > 0")
    Page<Review> findByProductIdAndRatingWithImages(@Param("productId") Integer productId, @Param("rating") Integer rating, Pageable pageable);

    /** Tat ca danh gia thuoc mot don hang (dung de kiem tra trang thai danh gia) */
    List<Review> findByOrder_OrderId(Integer orderId);

    /** Tinh diem danh gia trung binh cho san pham */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.productId = :productId")
    Double computeAverageRating(@Param("productId") Integer productId);

    /** Dem tong so danh gia cua mot san pham */
    long countByProduct_ProductId(Integer productId);

    /** Thong ke so luong danh gia theo tung muc sao (1-5) cua san pham */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.productId = :productId GROUP BY r.rating")
    List<Object[]> countByProductGroupByRating(@Param("productId") Integer productId);
}
