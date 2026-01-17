package com.eyarko.ecom.dto;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private String id;
    private Long userId;
    private Long productId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Map<String, Object> metadata;
}

