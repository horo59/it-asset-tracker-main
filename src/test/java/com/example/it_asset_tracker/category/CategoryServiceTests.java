
package com.example.it_asset_tracker.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.it_asset_tracker.entity.Category;
import com.example.it_asset_tracker.repository.CategoryRepository;
import com.example.it_asset_tracker.service.CategoryService;
import com.example.it_asset_tracker.dto.category.CategoryRequest;
import com.example.it_asset_tracker.dto.category.CategoryResponse;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Laptop");
    }

    @Test
    void getAllCategoriesReturnsCategoryResponses() {
        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        List<CategoryResponse> result =
                categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).name());

        verify(categoryRepository).findAll();
    }

    @Test
    void createCategorySavesNewCategory() {
        CategoryRequest request = new CategoryRequest("Laptop");

        when(categoryRepository.existsByNameIgnoreCase("Laptop"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result =
                categoryService.createCategory(request);

        assertEquals("Laptop", result.name());

        verify(categoryRepository)
                .existsByNameIgnoreCase("Laptop");

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategoryTrimsSurroundingWhitespace() {
        CategoryRequest request =
                new CategoryRequest("  Laptop  ");

        when(categoryRepository.existsByNameIgnoreCase("Laptop"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result =
                categoryService.createCategory(request);

        assertEquals("Laptop", result.name());

        verify(categoryRepository)
                .existsByNameIgnoreCase("Laptop");
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        CategoryRequest request =
                new CategoryRequest("Laptop");

        when(categoryRepository.existsByNameIgnoreCase("Laptop"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.createCategory(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }
}
