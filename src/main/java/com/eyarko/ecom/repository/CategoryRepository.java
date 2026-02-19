package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Category;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("select c from Category c")
    List<Category> findAllCategories(Sort sort);
}


