package com.eyarko.ecom.dto;

import java.time.Instant;
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
public class InventoryResponse {
    private Long productId;
    private Integer quantity;
    /** Display string for client: "Out of stock", "N unit(s) in stock", "Few units in stock", "In stock". */
    private String stockStatus;
    private Instant lastUpdated;
}


