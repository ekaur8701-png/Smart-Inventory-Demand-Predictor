package com.inventory.exception;

/** Base custom exception for all business-rule and database errors. */
public class InventoryException extends RuntimeException {
    public InventoryException(String message) { super(message); }
    public InventoryException(String message, Throwable cause) { super(message, cause); }
}
