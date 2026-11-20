package com.inventory.exception;

public class OutOfStockException extends InventoryException {
    public OutOfStockException(String name, int available, int requested) {
        super("Not enough stock for '" + name + "': available " + available + ", requested " + requested);
    }
}
