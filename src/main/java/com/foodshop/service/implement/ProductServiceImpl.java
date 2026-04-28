package com.foodshop.service.implement;

import com.foodshop.dto.request.BulkAssignDiscountRequest;
import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.entity.Category;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Product;
import com.foodshop.entity.ProductImage;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.ProductMapper;
import com.foodshop.repository.CategoryRepository;
import com.foodshop.repository.DiscountRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.service.CloudinaryService;
import com.foodshop.service.ProductService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DiscountRepository discountRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new GlobalException(GlobalCode.CATEGORY_NOT_FOUND));

        Discount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));
        }

        Product product = productMapper.toProduct(request);
        product.setCategory(category);
        product.setDiscount(discount);
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            for (MultipartFile file : request.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    ProductImage productImage = ProductImage.builder()
                            .imageUrl(url)
                            .product(product)
                            .build();
                    product.getImages().add(productImage);
                }
            }
        }

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new GlobalException(GlobalCode.CATEGORY_NOT_FOUND));

        Discount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));
        }

        productMapper.updateProductFromRequest(request, product);
        product.setCategory(category);
        product.setDiscount(discount);
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            product.getImages().clear();
            productRepository.saveAndFlush(product);

            for (MultipartFile file : request.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    ProductImage productImage = ProductImage.builder()
                            .imageUrl(url)
                            .product(product)
                            .build();
                    product.getImages().add(productImage);
                }
            }
        }

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new GlobalException(GlobalCode.PRODUCT_IN_USE);
        } catch (Exception e) {
            throw new GlobalException(GlobalCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void bulkAssignDiscount(BulkAssignDiscountRequest request) {
        Set<Integer> requestedIds = new LinkedHashSet<>(request.getProductIds());
        if (requestedIds.isEmpty()) {
            throw new GlobalException(GlobalCode.BAD_REQUEST);
        }

        Discount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

            if (discount.getType() != DiscountType.PRODUCT) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID);
            }

            if (discount.getStatus() != DiscountStatus.ACTIVE || discount.getEndDate().isBefore(LocalDate.now())) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID);
            }
        }

        List<Product> products = productRepository.findAllById(requestedIds);
        if (products.size() != requestedIds.size()) {
            throw new GlobalException(GlobalCode.PRODUCT_NOT_FOUND);
        }

        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExisting());
        Discount selectedDiscount = discount;
        if (selectedDiscount != null && !replaceExisting) {
            boolean hasConflict = products.stream()
                    .anyMatch(product -> product.getDiscount() != null
                            && !product.getDiscount().getDiscountId().equals(selectedDiscount.getDiscountId()));
            if (hasConflict) {
                throw new GlobalException(GlobalCode.PRODUCT_DISCOUNT_CONFLICT);
            }
        }

        for (Product product : products) {
            if (selectedDiscount == null || replaceExisting || product.getDiscount() == null
                    || product.getDiscount().getDiscountId().equals(selectedDiscount.getDiscountId())) {
                product.setDiscount(selectedDiscount);
            }
        }
        productRepository.saveAll(products);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));

        if (product.getIsActive() == null || !product.getIsActive()) {
            throw new GlobalException(GlobalCode.PRODUCT_NOT_FOUND);
        }

        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByIdAdmin(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAllActive();
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            String search,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        return findProducts(search, categoryId, null, true, minPrice, maxPrice, page, size, sortBy, sortDir);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProductsAdmin(
            String search,
            Integer categoryId,
            String status,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        return findProducts(search, categoryId, status, isActive, null, null, page, size, sortBy, sortDir);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }

        List<Product> products = productRepository.findActiveByCategoryId(categoryId);

        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategoryAdmin(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }
        return productRepository.findByCategory_CategoryId(categoryId)
                .stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    private Page<ProductResponse> findProducts(
            String search,
            Integer categoryId,
            String status,
            Boolean isActive,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveProductSort(sortBy, sortDir));
        Specification<Product> specification = buildProductSpecification(search, categoryId, status, isActive, minPrice, maxPrice);
        Page<Product> productPage = productRepository.findAll(specification, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return new PageImpl<>(content, pageable, productPage.getTotalElements());
    }

    private Specification<Product> buildProductSpecification(
            String search,
            Integer categoryId,
            String status,
            Boolean isActive,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        String normalizedStatus = status == null ? null : status.trim().toUpperCase();

        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("images", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (!normalizedSearch.isBlank()) {
                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(cb.like(cb.lower(root.get("name")), "%" + normalizedSearch + "%"));
                searchPredicates.add(cb.like(cb.lower(root.get("description")), "%" + normalizedSearch + "%"));
                if (normalizedSearch.chars().allMatch(Character::isDigit)) {
                    searchPredicates.add(cb.equal(root.get("productId"), Integer.valueOf(normalizedSearch)));
                }
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (normalizedStatus != null && !normalizedStatus.isBlank()) {
                Predicate statusPredicate = switch (normalizedStatus) {
                    case "IN_STOCK" -> cb.greaterThan(root.get("quantity"), 10);
                    case "LOW_STOCK" -> cb.and(
                            cb.greaterThan(root.get("quantity"), 0),
                            cb.lessThanOrEqualTo(root.get("quantity"), 10)
                    );
                    case "OUT_OF_STOCK" -> cb.lessThanOrEqualTo(root.get("quantity"), 0);
                    default -> null;
                };

                if (statusPredicate != null) {
                    predicates.add(statusPredicate);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveProductSort(String sortBy, String sortDir) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "name" -> "name";
            case "price" -> "price";
            case "quantity" -> "quantity";
            case "createdAt" -> "createdAt";
            default -> "productId";
        };

        boolean ascending = "ASC".equalsIgnoreCase(sortDir);
        return ascending ? Sort.by(property).ascending() : Sort.by(property).descending();
    }
}
