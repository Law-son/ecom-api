package com.eyarko.ecom.dto;

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
public class ReviewCreateRequest {
    private Long userId;
    private Long productId;
    private Integer rating;
    private String comment;
    private Map<String, Object> metadata;
}

