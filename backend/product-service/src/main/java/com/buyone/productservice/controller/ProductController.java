package com.buyone.productservice.controller;

import com.buyone.productservice.request.CreateProductRequest;
import com.buyone.productservice.request.UpdateProductRequest;
import com.buyone.productservice.response.ProductResponse;
import com.buyone.productservice.response.ApiResponse;
import com.buyone.productservice.exception.ForbiddenException;
import com.buyone.productservice.service.ProductService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/products")
@Validated
public class ProductController {
    
    private static final String SELLER_ROLE = "SELLER";
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    // GET /products (public)
    // @GetMapping
    // public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
    //     List<ProductResponse> products = productService.getAllProducts();
    //     return ResponseEntity.ok(okResponse("Products fetched successfully", products));
    // }

    
    // GET /products (public) or GET /products?sellerId=... (public)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) String sellerId) {

        List<ProductResponse> products;

        if (sellerId != null) {
            products = productService.getProductsBySeller(sellerId);
        } else {
            products = productService.getAllProducts();
        }

        return ResponseEntity.ok(okResponse("Products fetched successfully", products));
    }

    
    // GET /products/{id} (public)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(okResponse("Product fetched successfully", product));
    }
    
    // POST /products (seller only)
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-USER-ID") String sellerId,
            @RequestHeader("X-USER-ROLE") String role
    ) {
        if (!SELLER_ROLE.equals(role)) {
            throw new ForbiddenException("Only sellers can create products.");
        }
        
        ProductResponse product = productService.createProduct(request, sellerId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(okResponse("Product created successfully", product));
    }
    
    // PUT /products/{id} (seller only & must own)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request,
            @RequestHeader("X-USER-ID") String sellerId,
            @RequestHeader("X-USER-ROLE") String role
    ) {
        if (!SELLER_ROLE.equals(role)) {
            throw new ForbiddenException("Only sellers can update products.");
        }
        
        ProductResponse product = productService.updateProduct(id, request, sellerId);
        return ResponseEntity.ok(okResponse("Product updated successfully", product));
    }
    
    // DELETE /products/{id} (seller only & must own)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-USER-ID") String sellerId,
            @RequestHeader("X-USER-ROLE") String role
    ) {
        if (!SELLER_ROLE.equals(role)) {
            throw new ForbiddenException("Only sellers can delete products.");
        }
        
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.ok(okResponse("Product deleted successfully", null));
    }
    
    // Helper to build ApiResponse consistently
    private <T> ApiResponse<T> okResponse(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}
