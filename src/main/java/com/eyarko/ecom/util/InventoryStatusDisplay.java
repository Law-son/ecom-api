package com.eyarko.ecom.util;

/**
 * Derives the inventory status display string from quantity for client rendering.
 * <ul>
 *   <li>0 → "Out of stock"</li>
 *   <li>1 → "1 unit in stock"</li>
 *   <li>2–10 → "N units in stock"</li>
 *   <li>11–15 → "Few units in stock"</li>
 *   <li>16+ → "In stock"</li>
 * </ul>
 */
public final class InventoryStatusDisplay {

    private static final int FEW_UNITS_MAX = 15;
    private static final int UNITS_IN_STOCK_MAX = 10;

    private InventoryStatusDisplay() {
    }

    public static String fromQuantity(int quantity) {
        if (quantity <= 0) {
            return "Out of stock";
        }
        if (quantity == 1) {
            return "1 unit in stock";
        }
        if (quantity <= UNITS_IN_STOCK_MAX) {
            return quantity + " units in stock";
        }
        if (quantity <= FEW_UNITS_MAX) {
            return "Few units in stock";
        }
        return "In stock";
    }
}
