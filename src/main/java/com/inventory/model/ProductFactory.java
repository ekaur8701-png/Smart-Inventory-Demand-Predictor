package com.inventory.model;

import com.inventory.exception.InventoryException;

public final class ProductFactory {
    private ProductFactory() {}

    public static Product create(int id, String type, String name, String category,
                                 double price, int qty, int reorderLevel) {
        String t = type == null ? "GENERAL" : type.toUpperCase();
        switch (t) {
            case "PERISHABLE": return new PerishableProduct(id, name, category, price, qty, reorderLevel);
            case "ELECTRONIC": return new ElectronicProduct(id, name, category, price, qty, reorderLevel);
            case "GENERAL":    return new Product(id, name, category, "GENERAL", price, qty, reorderLevel);
            default: throw new InventoryException("Unknown product type: " + type);
        }
    }
}
