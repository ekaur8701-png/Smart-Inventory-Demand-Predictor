package com.inventory.model;

/** Electronics: long import lead time, so reorder earlier. */
public class ElectronicProduct extends Product {
    public ElectronicProduct(int id, String name, String category, double price, int quantity, int reorderLevel) {
        super(id, name, category, "ELECTRONIC", price, quantity, reorderLevel);
    }
    @Override public int leadTimeDays() { return 14; }
}
