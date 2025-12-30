package com.buyone.productservice.service;

import com.buyone.productservice.exception.BadRequestException;
import com.buyone.productservice.exception.ConflictException;
import com.buyone.productservice.exception.ForbiddenException;
import com.buyone.productservice.exception.ProductNotFoundException;
import com.buyone.productservice.model.Product;
import com.buyone.productservice.repository.ProductRepository;
import com.buyone.productservice.request.CreateProductRequest;
import com.buyone.productservice.request.UpdateProductRequest;
import com.buyone.productservice.response.ProductResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // relax strict stubbing for this class
class ProductServiceImplTests {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    // -------- createProduct --------
    
    @Test
    void createProduct_savesAndReturns_whenValid() {
        String sellerId = "seller-1";
        
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Prod A")
                .description("desc")
                .price(10.0)
                .quantity(5)
                .categoryId("cat-1")
                .images(List.of("img1", "img2"))
                .build();
        
        when(productRepository.findByUserId(sellerId)).thenReturn(List.of());
        Product saved = Product.builder()
                .id("p1")
                .name("Prod A")
                .description("desc")
                .price(10.0)
                .quantity(5)
                .userId(sellerId)
                .categoryId("cat-1")
                .images(List.of("img1", "img2"))
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        
        ProductResponse result = productService.createProduct(req, sellerId);
        
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product toSave = captor.getValue();
        
        assertThat(toSave.getUserId()).isEqualTo(sellerId);
        assertThat(toSave.getName()).isEqualTo("Prod A");
        assertThat(result.getId()).isEqualTo("p1");
        assertThat(result.getPrice()).isEqualTo(10.0);
    }
    
    @Test
    void createProduct_throwsBadRequest_whenNegativePrice() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Prod A")
                .price(-1.0)
                .quantity(1)
                .build();
        
        assertThatThrownBy(() -> productService.createProduct(req, "seller"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Price must be non-negative");
    }
    
    @Test
    void createProduct_throwsBadRequest_whenNegativeQuantity() {
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Prod A")
                .price(1.0)
                .quantity(-1)
                .build();
        
        assertThatThrownBy(() -> productService.createProduct(req, "seller"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Quantity must be zero or greater");
    }
    
    @Test
    void createProduct_throwsConflict_whenDuplicateNameForSeller() {
        String sellerId = "seller-1";
        
        CreateProductRequest req = CreateProductRequest.builder()
                .name("Prod A")
                .price(1.0)
                .quantity(1)
                .build();
        
        Product existing = Product.builder()
                .id("p1").name("prod a").userId(sellerId)
                .build();
        when(productRepository.findByUserId(sellerId)).thenReturn(List.of(existing));
        
        assertThatThrownBy(() -> productService.createProduct(req, sellerId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Product with name already exists for seller");
    }
    
    // -------- getProductById / getAllProducts --------
    
    @Test
    void getProductById_returnsResponse_whenFound() {
        Product p = Product.builder()
                .id("p1").name("Prod A").price(10.0)
                .userId("seller-1")
                .build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(p));
        
        ProductResponse result = productService.getProductById("p1");
        
        assertThat(result.getId()).isEqualTo("p1");
        assertThat(result.getName()).isEqualTo("Prod A");
    }
    
    @Test
    void getProductById_throwsNotFound_whenMissing() {
        when(productRepository.findById("p1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> productService.getProductById("p1"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with ID");
    }
    
    @Test
    void getAllProducts_returnsList_whenExists() {
        Product p1 = Product.builder().id("p1").name("A").build();
        Product p2 = Product.builder().id("p2").name("B").build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        
        List<ProductResponse> result = productService.getAllProducts();
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProductResponse::getId)
                .containsExactlyInAnyOrder("p1", "p2");
    }
    
    @Test
    void getAllProducts_throwsNotFound_whenEmpty() {
        when(productRepository.findAll()).thenReturn(List.of());
        
        assertThatThrownBy(productService::getAllProducts)
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("No products found");
    }
    
    // -------- updateProduct --------
    
    @Test
    void updateProduct_updatesFields_whenOwnerAndValid() {
        String sellerId = "seller-1";
        
        Product existing = Product.builder()
                .id("p1")
                .name("Old")
                .description("old")
                .price(10.0)
                .quantity(5)
                .userId(sellerId)
                .categoryId("cat-1")
                .images(List.of("old-img"))
                .build();
        
        UpdateProductRequest req = UpdateProductRequest.builder()
                .name("New")
                .description("new")
                .price(20.0)
                .quantity(10)
                .categoryId("cat-2")
                .images(List.of("new-img"))
                .build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        when(productRepository.findByUserId(sellerId)).thenReturn(List.of(existing));
        Product saved = Product.builder()
                .id("p1")
                .name("New")
                .description("new")
                .price(20.0)
                .quantity(10)
                .userId(sellerId)
                .categoryId("cat-2")
                .images(List.of("new-img"))
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        
        ProductResponse result = productService.updateProduct("p1", req, sellerId);
        
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getPrice()).isEqualTo(20.0);
        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getCategoryId()).isEqualTo("cat-2");
    }
    
    @Test
    void updateProduct_throwsNotFound_whenMissing() {
        UpdateProductRequest req = UpdateProductRequest.builder().build();
        when(productRepository.findById("p1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> productService.updateProduct("p1", req, "seller"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Cannot update — Product not found with ID");
    }
    
    @Test
    void updateProduct_throwsForbidden_whenNotOwner() {
        Product existing = Product.builder()
                .id("p1")
                .userId("owner-1")
                .build();
        
        UpdateProductRequest req = UpdateProductRequest.builder().build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> productService.updateProduct("p1", req, "seller-2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not own this product");
    }
    
    @Test
    void updateProduct_throwsBadRequest_whenNegativePrice() {
        Product existing = Product.builder()
                .id("p1")
                .userId("seller-1")
                .build();
        
        UpdateProductRequest req = UpdateProductRequest.builder()
                .price(-1.0)
                .build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> productService.updateProduct("p1", req, "seller-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Price must be non-negative");
    }
    
    @Test
    void updateProduct_throwsBadRequest_whenNegativeQuantity() {
        Product existing = Product.builder()
                .id("p1")
                .userId("seller-1")
                .build();
        
        UpdateProductRequest req = UpdateProductRequest.builder()
                .quantity(-1)
                .build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> productService.updateProduct("p1", req, "seller-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Quantity must be zero or greater");
    }
    
    @Test
    void updateProduct_throwsConflict_whenNewNameAlreadyExistsForSeller() {
        String sellerId = "seller-1";
        
        Product existing = Product.builder()
                .id("p1")
                .name("Old")
                .userId(sellerId)
                .build();
        
        Product other = Product.builder()
                .id("p2")
                .name("NewName")
                .userId(sellerId)
                .build();
        
        UpdateProductRequest req = UpdateProductRequest.builder()
                .name("NewName")
                .build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        when(productRepository.findByUserId(sellerId)).thenReturn(List.of(existing, other));
        
        assertThatThrownBy(() -> productService.updateProduct("p1", req, sellerId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Product with name already exists for seller");
    }
    
    // -------- deleteProduct --------
//
//    @Test
//    void deleteProduct_deletes_whenOwner() {
//        String sellerId = "seller-1";
//
//        Product existing = Product.builder()
//                .id("p1")
//                .userId(sellerId)
//                .build();
//
//        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
//
//        // Lenient, generic Kafka stub so Mockito ignores argument mismatch
//        lenient().when(kafkaTemplate.send(anyString(), any()))
//                .thenReturn(CompletableFuture.completedFuture(null));
//
//        productService.deleteProduct("p1", sellerId);
//
//        verify(productRepository).deleteById("p1");
//    }
    
    @Test
    void deleteProduct_throwsNotFound_whenMissing() {
        when(productRepository.findById("p1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> productService.deleteProduct("p1", "seller"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Cannot delete — Product not found with ID");
    }
    
    @Test
    void deleteProduct_throwsForbidden_whenNotOwner() {
        Product existing = Product.builder()
                .id("p1")
                .userId("owner-1")
                .build();
        
        when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> productService.deleteProduct("p1", "seller-2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not own this product");
    }
    
    // -------- getProductsBySeller --------
    
    @Test
    void getProductsBySeller_returnsList_evenWhenEmpty() {
        when(productRepository.findByUserId("seller-1")).thenReturn(List.of());
        
        List<ProductResponse> result = productService.getProductsBySeller("seller-1");
        
        assertThat(result).isEmpty();
    }
}
