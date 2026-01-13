package com.buyone.productservice.service;

import com.buyone.productservice.model.Product;
import com.buyone.productservice.repository.ProductRepository;
import com.buyone.productservice.request.CreateProductRequest;
import com.buyone.productservice.request.UpdateProductRequest;
import com.buyone.productservice.response.ProductResponse;
import com.buyone.productservice.exception.ProductNotFoundException;
import com.buyone.productservice.exception.BadRequestException;
import com.buyone.productservice.exception.ConflictException;
import com.buyone.productservice.exception.ForbiddenException;
// import com.buyone.productservice.event.ProductCreatedEvent;
// import com.buyone.productservice.event.ProductUpdatedEvent;
import com.buyone.productservice.event.ProductDeletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    
    
    @Value("${app.kafka.topic.product-created}")
    private String productCreatedTopic;
    @Value("${app.kafka.topic.product-updated}")
    private String productUpdatedTopic;
    @Value("${app.kafka.topic.product-deleted}")
    private String productDeletedTopic;
    
    public ProductServiceImpl(ProductRepository productRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    // Create Product (seller only, enforce at controller)
    @Override
    public ProductResponse createProduct(CreateProductRequest request, String sellerId) {
        // BUSINESS RULE:
        // Only seller can create - enforced at controller/gateway using JWT.
        // Example for business validation:
        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new BadRequestException("Price must be non-negative.");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new BadRequestException("Quantity must be zero or greater.");
        }
        
        // Example conflict check (duplicate product name for seller)
        List<Product> existing = productRepository.findByUserId(sellerId)
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName()))
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            throw new ConflictException("Product with name already exists for seller.");
        }
        
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .userId(sellerId)
                .categoryId(request.getCategoryId())
                .images(request.getImages())
                .build();
        
        Product savedProduct = productRepository.save(product);
        // ProductCreatedEvent event = ProductCreatedEvent.builder()
        //         .productId(savedProduct.getId())
        //         .sellerId(sellerId)
        //         .name(savedProduct.getName())
        //         .price(savedProduct.getPrice())
        //         .build();
        // Publish event
        // kafkaTemplate.send(productCreatedTopic, event)
        //         .whenComplete((result, ex) -> {
        //             if (ex != null) {
        //                 log.error("Failed to publish event", ex);
        //             } else {
        //                 log.info("Event published: " + event);
        //             }
        //         });
        return toProductResponse(savedProduct);
    }
    
    // Get single product by ID
    @Override
    public ProductResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toProductResponse)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with ID: " + id)
                );
    }
    
    // Get all products (consider pagination for production)
    // consider Pagination. (page,size) to reduce a massive call as this scales.
    @Override
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new ProductNotFoundException("No products found.");
        }
        return products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }
    
    // Update product (seller only)
    @Override
    public ProductResponse updateProduct(String id, UpdateProductRequest request, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Cannot update — Product not found with ID: " + id));
        // BUSINESS RULE:
        // Only owner (seller) can update this product
        if (!product.getUserId().equals(sellerId)) {
            throw new ForbiddenException("Unauthorized: You do not own this product");
        }
        
        // Validate business logic on incoming changes
        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new BadRequestException("Price must be non-negative.");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new BadRequestException("Quantity must be zero or greater.");
        }
        
        // Prevent changing to a name that already exists for same seller (conflict)
        if (request.getName() != null && !request.getName().equals(product.getName())) {
            List<Product> existing = productRepository.findByUserId(sellerId)
                    .stream()
                    .filter(p -> p.getName().equalsIgnoreCase(request.getName()) && !p.getId().equals(product.getId()))
                    .collect(Collectors.toList());
            if (!existing.isEmpty()) {
                throw new ConflictException("Product with name already exists for seller.");
            }
        }
        
        // Update fields if provided
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getQuantity() != null) product.setQuantity(request.getQuantity());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getImages() != null) product.setImages(request.getImages());

        Product updatedProduct = productRepository.save(product);
        // ProductUpdatedEvent event = ProductUpdatedEvent.builder()
        //         .productId(updatedProduct.getId())
        //         .sellerId(sellerId)
        //         .name(updatedProduct.getName())
        //         .price(updatedProduct.getPrice())
        //         .build();
        // kafkaTemplate.send(productUpdatedTopic, event)
        //         .whenComplete((result, ex) -> {
        //             if (ex != null) {
        //                 log.error("Failed to publish event", ex);
        //             } else {
        //                 log.info("Event published: " + event);
        //             }
        //         });
        return toProductResponse(updatedProduct);
    }
    
    // Delete product (seller only)
    @Override
    @Transactional
    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Cannot delete — Product not found with ID: " + id));
        // BUSINESS RULE:
        // Only owner (seller) can delete this product
        if (!product.getUserId().equals(sellerId)) {
            throw new ForbiddenException("Unauthorized: You do not own this product");
        }
        productRepository.deleteById(id);
        
        ProductDeletedEvent event = ProductDeletedEvent.builder()
                .productId(product.getId())
                .sellerId(sellerId)
                .build();
        kafkaTemplate.send(productDeletedTopic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event", ex);
                    } else {
                        log.info("Event published: " + event);
                    }
                });

    }
    
    // Get all products by seller (for seller dashboard)
    @Override
    public List<ProductResponse> getProductsBySeller(String sellerId) {
        List<Product> products = productRepository.findByUserId(sellerId);
        // Do NOT throw on empty; just map to DTOs
        // if (products.isEmpty()) {
        //     throw new ProductNotFoundException("No products found for seller: " + sellerId);
        // }
        return products.stream().map(this::toProductResponse).collect(Collectors.toList());
    }
    
    // Helper: Map Product entity to ProductResponse DTO
    private ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .images(product.getImages())
                .quantity(product.getQuantity())
                .userId(product.getUserId()) // Correct getter
                .categoryId(product.getCategoryId())
                .build();
    }
}