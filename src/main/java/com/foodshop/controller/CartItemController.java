package com.foodshop.controller;

import com.foodshop.dto.request.CartItemRequest;
import com.foodshop.dto.response.CartItemResponse;
import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.CartItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;

    @GetMapping("/ping")
    public String ping(){
        return "server ok";
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(@Valid @RequestBody CartItemRequest request) {
        CartItemResponse response = cartItemService.addToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(GlobalCode.SUCCESS, "Item added to cart successfully.", response)
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCartItemsByUser(@PathVariable Integer userId) {
        List<CartItemResponse> responses = cartItemService.getCartItemsByUser(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Cart items retrieved successfully.", responses)
        );
    }

    @PutMapping("/{userId}/{productId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @PathVariable Integer userId,
            @PathVariable Integer productId,
            @RequestParam Integer quantity) {
        CartItemResponse response = cartItemService.updateQuantity(userId, productId, quantity);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Quantity updated successfully.", response)
        );
    }

    @DeleteMapping("/{userId}/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable Integer userId, @PathVariable Integer productId) {
        cartItemService.removeCartItem(userId, productId);
        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(GlobalCode.SUCCESS, "Cart item removed successfully.", null)
        );
    }

    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Integer userId) {
        cartItemService.clearCart(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Cart cleared successfully.", null)
        );
    }

    @DeleteMapping("/remove-selected")
    public ResponseEntity<ApiResponse<Void>> removeSelectedItems(
            @RequestParam Integer userId,
            @RequestBody List<Integer> productIds) {
        cartItemService.removeSelectedItems(userId, productIds);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Selected products removed successfully.", null)
        );
    }
}