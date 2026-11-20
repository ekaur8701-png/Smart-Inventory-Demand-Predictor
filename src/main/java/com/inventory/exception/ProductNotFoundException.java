package com.inventory.exception;

public class ProductNotFoundException extends InventoryException {
    public ProductNotFoundException(int id) { super("Product not found: id " + id); }
}
