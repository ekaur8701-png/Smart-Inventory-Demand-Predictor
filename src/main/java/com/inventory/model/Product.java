package com.inventory.model;

/** Base class for all products. Subclasses override lead time (inheritance + polymorphism). */
public class Product {
    private int id;
    private String name;
    private String category;
    private String type;
    private double price;
    private int quantity;
    private int reorderLevel;

    public Product(int id, String name, String category, String type, double price, int quantity, int reorderLevel) {
        this.id = id; this.name = name; this.category = category; this.type = type;
        this.price = price; this.quantity = quantity; this.reorderLevel = reorderLevel;
    }

    /** Days a supplier needs to deliver. Overridden by subclasses. */
    public int leadTimeDays() { return 7; }

    public double stockValue() { return price * quantity; }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
