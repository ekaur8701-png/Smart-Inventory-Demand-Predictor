package com.inventory.model;

/** One reorder suggestion produced by the rule-based DemandEstimator. */
public record Suggestion(int productId, String name, int quantity, int reorderLevel,
                         double avgDailySales, double daysLeft, int suggestedOrder, String status) {}
