package com.inventory.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.inventory.db.Database;
import com.inventory.db.ProductDao;
import com.inventory.model.Product;
import com.inventory.model.ProductFactory;

/** Replaces the demo catalog once with the curated student-supply inventory. */
public final class SeedData {
    private SeedData() {}

    public static void loadIfEmpty() {
        ProductDao pd = new ProductDao();
        if (catalogMigrationApplied()) return;
        replaceCatalog(pd);
        markCatalogMigration();
    }

    private static boolean catalogMigrationApplied() {
        try (Connection c = Database.get(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS catalog_migrations (" +
                "name VARCHAR(100) PRIMARY KEY)");
            try (ResultSet rs = s.executeQuery(
                "SELECT 1 FROM catalog_migrations WHERE name='student_catalog_v1'")) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not check the product catalog migration", e);
        }
    }

    private static void replaceCatalog(ProductDao pd) {
        Object[][] rows = {
            {"GENERAL", "Notebook", "Stationery", 60.0, 50, 10},
            {"GENERAL", "Blue Pen", "Stationery", 10.0, 100, 20},
            {"GENERAL", "Black Pen", "Stationery", 10.0, 15, 20},
            {"GENERAL", "Pencil", "Stationery", 5.0, 80, 20},
            {"GENERAL", "Eraser", "Stationery", 5.0, 8, 15},
            {"GENERAL", "Geometry Box", "Stationery", 120.0, 25, 5},
            {"GENERAL", "Highlighter", "Stationery", 30.0, 12, 5},
            {"GENERAL", "A4 Paper Pack", "Stationery", 250.0, 40, 8},
            {"ELECTRONIC", "Wireless Mouse", "Electronics", 450.0, 30, 6},
            {"ELECTRONIC", "Keyboard", "Electronics", 700.0, 20, 5},
            {"ELECTRONIC", "USB Cable", "Electronics", 150.0, 60, 10},
            {"ELECTRONIC", "Pendrive 32GB", "Electronics", 500.0, 7, 5},
            {"ELECTRONIC", "Headphones", "Electronics", 899.0, 18, 5},
            {"ELECTRONIC", "Laptop Charger", "Electronics", 1200.0, 4, 3},
            {"ELECTRONIC", "USB Hub", "Electronics", 650.0, 25, 5},
            {"GENERAL", "Java Programming Book", "Books", 450.0, 20, 5},
            {"GENERAL", "Python Programming Book", "Books", 550.0, 15, 5},
            {"GENERAL", "Database Management Book", "Books", 400.0, 30, 5},
            {"GENERAL", "Data Structures Book", "Books", 500.0, 6, 5},
            {"GENERAL", "Computer Networks Book", "Books", 350.0, 12, 5},
            {"GENERAL", "Stapler", "Office Supplies", 80.0, 25, 5},
            {"GENERAL", "Stapler Pins", "Office Supplies", 20.0, 100, 20},
            {"GENERAL", "Scissors", "Office Supplies", 60.0, 30, 5},
            {"GENERAL", "Glue Stick", "Office Supplies", 25.0, 9, 5},
            {"GENERAL", "Calculator", "Office Supplies", 350.0, 18, 5}
        };
        try (Connection c = Database.get(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM sales");
            s.execute("DELETE FROM products");
        } catch (Exception e) {
            throw new RuntimeException("Could not clear the previous product catalog", e);
        }
        for (Object[] r : rows) {
            Product p = ProductFactory.create(0, (String) r[0], (String) r[1], (String) r[2],
                (double) r[3], (int) r[4], (int) r[5]);
            pd.insert(p);
        }
    }

    private static void markCatalogMigration() {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO catalog_migrations(name) VALUES(?)")) {
            ps.setString(1, "student_catalog_v1");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Could not save the product catalog migration", e);
        }
    }
}
