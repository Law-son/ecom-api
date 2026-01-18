package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(
        String name,
        String categoryName,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(
        Long categoryId,
        String name,
        Pageable pageable
    );
}


