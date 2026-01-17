package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.mapper.CategoryMapper;
import com.eyarko.ecom.repository.CategoryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = Category.builder()
            .name(request.getName())
            .build();
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        category.setName(request.getName());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return CategoryMapper.toResponse(category);
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
            .map(CategoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }
}

