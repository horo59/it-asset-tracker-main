
package com.example.it_asset_tracker.repository;
import com.example.it_asset_tracker.entity.Category;


import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);
}
