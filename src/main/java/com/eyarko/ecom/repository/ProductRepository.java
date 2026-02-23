package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for products.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category"})
    @Query("select p from Product p")
    Page<Product> findAllProducts(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);


    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategory_IdAndNameContainingIgnoreCase(
        Long categoryId,
        String name,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"category"})
    @Query(
        "select p from Product p join p.category c "
            + "where lower(p.name) like lower(concat('%', :term, '%')) "
            + "or lower(c.name) like lower(concat('%', :term, '%'))"
    )
    Page<Product> searchByNameOrCategory(@Param("term") String term, Pageable pageable);

    boolean existsByCategory_Id(Long categoryId);
}


