package com.inventory.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.inventory.db.Database;

/** Replaces the demo catalog once with the curated student-supply inventory. */
public final class SeedData {
    private SeedData() {}

    public static void loadIfEmpty() {
        if (catalogMigrationApplied()) return;
        replaceCatalog();
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

    private static void replaceCatalog() {
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
            insertProducts(c, rows);
        } catch (Exception e) {
            throw new RuntimeException("Could not replace the previous product catalog", e);
        }
    }

    private static void insertProducts(Connection c, Object[][] rows) throws Exception {
        List<String> columns = new ArrayList<>();
        List<Integer> fields = new ArrayList<>();
        try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM products WHERE 1=0")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                int field = productField(md.getColumnLabel(i));
                if (field >= 0) {
                    columns.add(md.getColumnLabel(i));
                    fields.add(field);
                }
            }
        }
        if (columns.isEmpty()) throw new IllegalStateException("No recognized product columns found");

        StringBuilder sql = new StringBuilder("INSERT INTO products (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append('?');
        }
        sql.append(')');

        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (Object[] row : rows) {
                for (int i = 0; i < fields.size(); i++) ps.setObject(i + 1, row[fields.get(i)]);
                ps.executeUpdate();
            }
        }
    }

    private static int productField(String column) {
        String name = column.toLowerCase().replaceAll("[^a-z0-9]", "");
        switch (name) {
            case "type": case "producttype": return 0;
            case "name": case "productname": return 1;
            case "category": case "productcategory": return 2;
            case "price": case "unitprice": return 3;
            case "stock": case "quantity": case "stockquantity": return 4;
            case "threshold": case "reorderlevel": case "lowstockthreshold": return 5;
            default: return -1;
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
