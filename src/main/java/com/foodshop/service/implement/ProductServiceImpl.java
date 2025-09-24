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
import com.foodshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        String imageUrl = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            imageUrl = cloudinaryService.uploadFile(request.getImageFile());
        }

        Product product = productMapper.toProduct(request);
        product.setCategory(category);
        product.setDiscount(discount);
        product.setImageUrl(imageUrl);

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

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            product.setImageUrl(cloudinaryService.uploadFile(request.getImageFile()));
        }

        productMapper.updateProductFromRequest(request, product);
        product.setCategory(category);
        product.setDiscount(discount);

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    @Override
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        productRepository.delete(product);
    }

    @Override
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));
        return productMapper.toProductResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }
        return productRepository.findByCategory_CategoryId(categoryId)
                .stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, int page, int size, boolean asc) {
        Sort sort = asc ? Sort.by("price").ascending() : Sort.by("price").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCase(
                keyword != null ? keyword : "", pageable
        );

        return productPage.map(productMapper::toProductResponse);
    }
}