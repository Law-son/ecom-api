package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.InventoryAdjustRequest;
import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.service.InventoryService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory endpoints for product stock.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Adjusts inventory quantity for a product.
     *
     * @param request inventory adjustment payload
     * @return updated inventory
     */
    @PostMapping("/adjust")
    public ApiResponse<InventoryResponse> adjustInventory(@Valid @RequestBody InventoryAdjustRequest request) {
        return ResponseUtil.success("Inventory updated", inventoryService.adjustInventory(request));
    }

    /**
     * Retrieves inventory details for a product.
     *
     * @param productId product id
     * @return inventory details
     */
    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ResponseUtil.success("Inventory retrieved", inventoryService.getInventoryByProduct(productId));
    }
}

