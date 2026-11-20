package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.model.Suggestion;

/** Rule-based demand estimate. No machine learning: only averages and thresholds. */
public class DemandEstimator {
    public static final int WINDOW_DAYS = 30;
    public static final int BUFFER_DAYS = 7;   // safety stock on top of lead time

    public Suggestion estimate(Product p, int unitsSoldInWindow) {
        double avg = unitsSoldInWindow / (double) WINDOW_DAYS;
        double daysLeft = avg == 0 ? 999 : p.getQuantity() / avg;

        // Rule 1: target stock = demand during (lead time + buffer)
        int target = (int) Math.ceil(avg * (p.leadTimeDays() + BUFFER_DAYS));
        int order = Math.max(0, Math.max(target, p.getReorderLevel() * 2) - p.getQuantity());

        // Rule 2: status thresholds
        String status;
        if (p.getQuantity() == 0) status = "OUT";
        else if (p.getQuantity() <= p.getReorderLevel() || daysLeft < p.leadTimeDays()) status = "LOW";
        else status = "OK";
        if (status.equals("OK")) order = 0;

        return new Suggestion(p.getId(), p.getName(), p.getQuantity(), p.getReorderLevel(),
            Math.round(avg * 100) / 100.0, Math.round(daysLeft * 10) / 10.0, order, status);
    }
}
