package com.eyarko.ecom.document;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reviews")
@CompoundIndexes({
    @CompoundIndex(name = "idx_product_created", def = "{'productId': 1, 'createdAt': -1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private Long productId;

    private Integer rating;

    private String comment;

    @Indexed
    private Instant createdAt;

    private Map<String, Object> metadata;
}


