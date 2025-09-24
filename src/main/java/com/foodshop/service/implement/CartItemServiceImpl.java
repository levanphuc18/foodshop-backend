package com.foodshop.service.implement;

import com.foodshop.dto.request.CartItemRequest;
import com.foodshop.dto.response.CartItemResponse;
import com.foodshop.entity.*;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.CartItemMapper;
import com.foodshop.repository.CartItemRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.repository.UserRepository;
import com.foodshop.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemMapper cartItemMapper;

    @Override
    public CartItemResponse addToCart(CartItemRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));

        CartItemId id = new CartItemId(user.getUserId(), product.getProductId());
        CartItem cartItem = cartItemRepository.findById(id).orElse(null);

        if (cartItem == null) {
            cartItem = new CartItem(id, user, product, request.getQuantity());
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        }
        cartItemRepository.save(cartItem);

        return cartItemMapper.toCartItemResponse(cartItem);
    }

    @Override
    public List<CartItemResponse> getCartItemsByUser(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findByUser_UserId(userId);
        if (items.isEmpty()) {
            throw new GlobalException(GlobalCode.CART_NOT_FOUND);
        }

        return items.stream()
                .map(cartItemMapper::toCartItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CartItemResponse updateQuantity(Integer userId, Integer productId, Integer quantity) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        productRepository.findById(productId)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findById(new CartItemId(userId, productId))
                .orElseThrow(() -> new GlobalException(GlobalCode.CART_NOT_FOUND));

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return cartItemMapper.toCartItemResponse(cartItem);
    }

    @Override
    public void removeCartItem(Integer userId, Integer productId) {
        if (!cartItemRepository.existsById(new CartItemId(userId, productId))) {
            throw new GlobalException(GlobalCode.CART_NOT_FOUND);
        }
        cartItemRepository.deleteById(new CartItemId(userId, productId));
    }

    @Override
    public void clearCart(Integer userId) {
        List<CartItem> items = cartItemRepository.findByUser_UserId(userId);
        if (items.isEmpty()) {
            throw new GlobalException(GlobalCode.CART_NOT_ITEM);
        }
        cartItemRepository.deleteAll(items);
    }

    @Override
    @Transactional
    public void removeSelectedItems(Integer userId, List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new GlobalException(GlobalCode.CART_NOT_ITEM);
        }
        int deleted = cartItemRepository.deleteSelectedItems(userId, productIds);
        if (deleted == 0) {
            throw new GlobalException(GlobalCode.CART_NO_MATCHING_ITEMS);
        }
    }
}