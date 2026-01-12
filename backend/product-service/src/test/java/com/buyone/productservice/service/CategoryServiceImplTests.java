package com.buyone.productservice.service;

import com.buyone.productservice.exception.ResourceNotFoundException;
import com.buyone.productservice.model.Category;
import com.buyone.productservice.repository.CategoryRepository;
import com.buyone.productservice.request.UpdateCategoryRequest;
import com.buyone.productservice.response.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTests {
        
    private static final String SLUG_1 = "slug-1";
    private static final String CAT_1 = "Cat 1";

    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private CategoryServiceImpl categoryService;
    
    @Test
    void getAllCategories_mapsEntitiesToResponses() {
        Category c1 = Category.builder()
                .id("c1").slug(SLUG_1).name(CAT_1)
                .icon("icon1").description("desc1")
                .build();
        Category c2 = Category.builder()
                .id("c2").slug("slug-2").name("Cat 2")
                .icon("icon2").description("desc2")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        
        List<CategoryResponse> result = categoryService.getAllCategories();
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::id)
                .containsExactlyInAnyOrder("c1", "c2");
    }
    
    @Test
    void getCategoryById_returnsResponse_whenFound() {
        Category c = Category.builder()
                .id("c1").slug(SLUG_1).name(CAT_1)
                .icon("icon1").description("desc1")
                .build();
        when(categoryRepository.findById("c1")).thenReturn(Optional.of(c));
        
        CategoryResponse result = categoryService.getCategoryById("c1");
        
        assertThat(result.id()).isEqualTo("c1");
        assertThat(result.name()).isEqualTo(CAT_1);
    }
    
    @Test
    void getCategoryById_throwsNotFound_whenMissing() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> categoryService.getCategoryById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }
    
    @Test
    void deleteCategory_deletes_whenExists() {
        when(categoryRepository.existsById("c1")).thenReturn(true);
        
        categoryService.deleteCategory("c1");
        
        verify(categoryRepository).deleteById("c1");
    }
    
    @Test
    void deleteCategory_throwsNotFound_whenMissing() {
        when(categoryRepository.existsById("c1")).thenReturn(false);
        
        assertThatThrownBy(() -> categoryService.deleteCategory("c1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }
    
    @Test
    void updateCategory_updatesFieldsAndSaves() {
        Category existing = Category.builder()
                .id("c1").slug(SLUG_1).name("Old")
                .icon("old-icon").description("old-desc")
                .build();
        
        UpdateCategoryRequest req = new UpdateCategoryRequest(
                "New",
                "new-icon",
                "new-desc"
        );
        
        when(categoryRepository.findById("c1")).thenReturn(Optional.of(existing));
        
        Category saved = Category.builder()
                .id("c1").slug(SLUG_1).name("New")
                .icon("new-icon").description("new-desc")
                .build();
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        
        CategoryResponse result = categoryService.updateCategory("c1", req);
        
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category toSave = captor.getValue();
        
        assertThat(toSave.getName()).isEqualTo("New");
        assertThat(toSave.getIcon()).isEqualTo("new-icon");
        assertThat(result.description()).isEqualTo("new-desc");
    }
    
    @Test
    void updateCategory_throwsNotFound_whenMissing() {
        UpdateCategoryRequest req = new UpdateCategoryRequest("New", "icon", "desc");
        when(categoryRepository.findById("c1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> categoryService.updateCategory("c1", req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }
}
