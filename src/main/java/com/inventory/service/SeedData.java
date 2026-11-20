package com.inventory.service;

import com.inventory.db.ProductDao;
import com.inventory.db.SaleDao;
import com.inventory.model.Product;
import com.inventory.model.ProductFactory;
import java.time.LocalDate;
import java.util.Random;

/** Inserts demo products and 30 days of sales the first time the app runs. */
public final class SeedData {
    private SeedData() {}

    public static void loadIfEmpty() {
        ProductDao pd = new ProductDao();
        if (!pd.findAll().isEmpty()) return;
        SaleDao sd = new SaleDao();
        Object[][] rows = {
            {"PERISHABLE", "Fresh Milk 1L", "Dairy", 60.0, 40, 20, 9},
            {"PERISHABLE", "Brown Bread", "Bakery", 45.0, 12, 15, 7},
            {"ELECTRONIC", "USB-C Cable", "Accessories", 299.0, 25, 10, 2},
            {"ELECTRONIC", "Wireless Mouse", "Accessories", 799.0, 6, 8, 1},
            {"GENERAL", "A4 Notebook", "Stationery", 70.0, 90, 30, 4},
            {"GENERAL", "Gel Pen (Blue)", "Stationery", 20.0, 150, 40, 12},
            {"GENERAL", "Hand Sanitizer", "Health", 120.0, 0, 15, 3},
            {"PERISHABLE", "Packed Juice", "Beverages", 35.0, 55, 25, 6},
        };
        Random rnd = new Random(42);
        for (Object[] r : rows) {
            Product p = ProductFactory.create(0, (String) r[0], (String) r[1], (String) r[2],
                (double) r[3], (int) r[4], (int) r[5]);
            int id = pd.insert(p);
            int avg = (int) r[6];
            for (int d = 29; d >= 0; d--) {
                int units = Math.max(0, avg + rnd.nextInt(5) - 2);
                if (units > 0) sd.insert(id, units, units * p.getPrice(), LocalDate.now().minusDays(d));
            }
        }
    }
}
