package com.eyarko.ecom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByNameContainingIgnoreCase_returnsMatchingProducts() {
        Category category = categoryRepository.save(Category.builder().name("Electronics").build());
        productRepository.save(
            Product.builder()
                .category(category)
                .name("Laptop Pro")
                .price(new BigDecimal("1299.99"))
                .build()
        );

        Page<Product> results = productRepository.findByNameContainingIgnoreCase(
            "laptop",
            PageRequest.of(0, 10)
        );

        assertThat(results.getTotalElements()).isEqualTo(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Laptop Pro");
    }
}

