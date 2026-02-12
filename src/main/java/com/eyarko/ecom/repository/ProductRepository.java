package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Product;
import java.util.List;
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
    List<Product> findAll();

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategory_NameIgnoreCase(String categoryName, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByPriceBetween(java.math.BigDecimal min, java.math.BigDecimal max, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(
        String name,
        String categoryName,
        Pageable pageable
    );

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

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findByNameIgnoreCase(String name);

    void deleteByCategory_Id(Long categoryId);
}


