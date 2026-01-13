package com.buyone.productservice.service;

import com.buyone.productservice.exception.ResourceNotFoundException;
import com.buyone.productservice.model.Category;
import com.buyone.productservice.repository.CategoryRepository;
import com.buyone.productservice.request.UpdateCategoryRequest;
import com.buyone.productservice.response.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    @Override
    public CategoryResponse getCategoryById(String id) {
        var category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return toResponse(category);
    }
    
    @Override
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }
    
    @Override
    public CategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        cat.setName(request.getName());
        cat.setIcon(request.getIcon());
        cat.setDescription(request.getDescription());
        Category saved = categoryRepository.save(cat);
        return toResponse(saved);
    }
    
    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getSlug(),
                c.getName(),
                c.getIcon(),
                c.getDescription()
        );
    }
}
