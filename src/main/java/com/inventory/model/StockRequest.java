package com.inventory.model;

/** A pending restock request waiting in the FIFO queue. */
public class StockRequest {
    private final int id;
    private final int productId;
    private final String productName;
    private final int quantity;
    private final String requestedAt;

    public StockRequest(int id, int productId, String productName, int quantity, String requestedAt) {
        this.id = id; this.productId = productId; this.productName = productName;
        this.quantity = quantity; this.requestedAt = requestedAt;
    }
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public String getRequestedAt() { return requestedAt; }
}
