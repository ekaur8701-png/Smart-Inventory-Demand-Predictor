package com.inventory.model;

/** Food/medicine style items: short supplier lead time, must be restocked often. */
public class PerishableProduct extends Product {
    public PerishableProduct(int id, String name, String category, double price, int quantity, int reorderLevel) {
        super(id, name, category, "PERISHABLE", price, quantity, reorderLevel);
    }
    @Override public int leadTimeDays() { return 3; }
}
