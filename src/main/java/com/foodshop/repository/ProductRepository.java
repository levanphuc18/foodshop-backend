package com.foodshop.repository;

import com.foodshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.productId = :id")
    Optional<Product> findById(@Param("id") Integer id);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.category.categoryId = :categoryId")
    List<Product> findByCategory_CategoryId(@Param("categoryId") Integer categoryId);

    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT count(p) FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.images
            WHERE (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """,
           countQuery = """
            SELECT count(p)
            FROM Product p
            WHERE (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """)
    Page<Product> findAllAdmin(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.images
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """,
           countQuery = """
            SELECT count(p)
            FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """)
    Page<Product> searchAdminProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images")
    List<Product> findAll();

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.isActive = true")
    List<Product> findAllActive();

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.category.categoryId = :categoryId AND p.isActive = true")
    List<Product> findActiveByCategoryId(@Param("categoryId") Integer categoryId);

    @Query(value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true",
           countQuery = "SELECT count(p) FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    Page<Product> searchActiveProducts(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.images
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND p.isActive = true
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """,
           countQuery = """
            SELECT count(p)
            FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND p.isActive = true
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """)
    Page<Product> searchActiveProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );
}
