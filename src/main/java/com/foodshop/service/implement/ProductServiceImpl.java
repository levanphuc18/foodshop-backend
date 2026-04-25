package com.foodshop.service.implement;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.entity.Category;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Product;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.ProductMapper;
import com.foodshop.repository.CategoryRepository;
import com.foodshop.repository.DiscountRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.service.CloudinaryService;
import com.foodshop.entity.ProductImage;
import com.foodshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
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
            // Xóa ảnh cũ
            product.getImages().clear();
            productRepository.saveAndFlush(product); // Đảm bảo xóa ảnh cũ trước khi thêm mới
            
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
            productRepository.flush(); // Cưỡng bức flush để bắt lỗi database ngay lập tức
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new GlobalException(GlobalCode.PRODUCT_IN_USE);
        } catch (Exception e) {
            throw new GlobalException(GlobalCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void bulkAssignDiscount(com.foodshop.dto.request.BulkAssignDiscountRequest request) {
        Discount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));
            
            if (discount.getType() != com.foodshop.enums.DiscountType.PRODUCT) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID); // Only PRODUCT type discounts can be assigned
            }
        }

        List<Product> products = productRepository.findAllById(request.getProductIds());
        for (Product product : products) {
            product.setDiscount(discount);
        }
        productRepository.saveAll(products);
    }

    @Override
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        
        // Khách hàng chỉ được xem sản phẩm active
        if (product.getIsActive() == null || !product.getIsActive()) {
            throw new GlobalException(GlobalCode.PRODUCT_NOT_FOUND);
        }
        
        return productMapper.toProductResponse(product);
    }

    @Override
    public ProductResponse getProductByIdAdmin(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        return productMapper.toProductResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        // Luôn lọc sản phẩm active cho API lấy danh sách chung
        List<Product> products = productRepository.findAllActive();
        
        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> getAllProductsAdmin(Integer categoryId, int page, int size, boolean asc) {
        // Trả về tất cả cho admin
        Sort sort = asc ? Sort.by("productId").ascending() : Sort.by("productId").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAllAdmin(categoryId, pageable)
                .map(productMapper::toProductResponse);
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }
        
        // Luôn lọc sản phẩm active cho khách hàng xem theo danh mục
        List<Product> products = productRepository.findActiveByCategoryId(categoryId);

        return products.stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategoryAdmin(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }
        return productRepository.findByCategory_CategoryId(categoryId)
                .stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, Integer categoryId, int page, int size, boolean asc) {
        Sort sort = asc ? Sort.by("price").ascending() : Sort.by("price").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null) ? keyword : "";
        // Luôn lọc sản phẩm active khi tìm kiếm công khai
        Page<Product> productPage = productRepository.searchActiveProducts(kw, categoryId, pageable);

        return productPage.map(productMapper::toProductResponse);
    }

    @Override
    public Page<ProductResponse> searchProductsAdmin(String keyword, Integer categoryId, int page, int size, boolean asc) {
        Sort sort = asc ? Sort.by("price").ascending() : Sort.by("price").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null) ? keyword : "";
        Page<Product> productPage = productRepository.searchAdminProducts(kw, categoryId, pageable);

        return productPage.map(productMapper::toProductResponse);
    }
}
