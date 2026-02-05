package com.eyarko.ecom.util;

public final class InventoryStatusUtil {
    private InventoryStatusUtil() {
    }

    public static String resolveStatus(Integer quantity) {
        int qty = quantity == null ? 0 : quantity;
        if (qty < 0) {
            return "Out of stock";
        }
        if (qty <= 5) {
            return qty == 1 ? "1 unit left" : qty + " units left";
        }
        if (qty <= 10) {
            return "Few units in stock";
        }
        return "In stock";
    }
}

