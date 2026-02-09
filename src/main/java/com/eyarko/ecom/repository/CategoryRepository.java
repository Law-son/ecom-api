package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}


