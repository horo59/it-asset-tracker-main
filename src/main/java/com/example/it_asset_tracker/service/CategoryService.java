
package com.example.it_asset_tracker.service;
import com.example.it_asset_tracker.entity.Category;
import com.example.it_asset_tracker.repository.CategoryRepository;


import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.it_asset_tracker.dto.category.CategoryRequest;
import com.example.it_asset_tracker.dto.category.CategoryResponse;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName()))
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();

        boolean exists = categoryRepository.existsByNameIgnoreCase(name);

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A category with this name already exists");
        }

        Category category = new Category(name);
        Category savedCategory = categoryRepository.save(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName());
    }
}
