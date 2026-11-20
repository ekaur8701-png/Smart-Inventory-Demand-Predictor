package com.inventory.db;

import com.inventory.exception.InventoryException;
import com.inventory.model.Sale;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaleDao {
    public void insert(int productId, int qty, double total, LocalDate date) {
        String sql = "INSERT INTO sales(product_id,quantity,total,sale_date) VALUES(?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId); ps.setInt(2, qty); ps.setDouble(3, total); ps.setDate(4, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) { throw new InventoryException("Failed to record sale", e); }
    }

    public List<Sale> recent(int limit) {
        String sql = "SELECT s.id, s.product_id, p.name, s.quantity, s.total, s.sale_date FROM sales s " +
                     "JOIN products p ON p.id = s.product_id ORDER BY s.id DESC LIMIT ?";
        List<Sale> list = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Sale(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getInt(4), rs.getDouble(5), rs.getDate(6).toString()));
            }
            return list;
        } catch (SQLException e) { throw new InventoryException("Failed to load sales", e); }
    }

    /** Aggregate SQL: units sold for a product since a given date. */
    public int unitsSoldSince(int productId, LocalDate since) {
        String sql = "SELECT COALESCE(SUM(quantity),0) FROM sales WHERE product_id=? AND sale_date>=?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId); ps.setDate(2, Date.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new InventoryException("Failed to aggregate sales", e); }
    }

    public double totalRevenue() { return scalar("SELECT COALESCE(SUM(total),0) FROM sales"); }
    public double totalUnits()   { return scalar("SELECT COALESCE(SUM(quantity),0) FROM sales"); }

    private double scalar(String sql) {
        try (Connection c = Database.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next(); return rs.getDouble(1);
        } catch (SQLException e) { throw new InventoryException("Query failed", e); }
    }

    /** Aggregate SQL with GROUP BY: top sellers. */
    public List<Map<String, Object>> topSellers(int limit) {
        String sql = "SELECT p.name, SUM(s.quantity) AS units, SUM(s.total) AS revenue FROM sales s " +
                     "JOIN products p ON p.id = s.product_id GROUP BY p.name ORDER BY units DESC LIMIT ?";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", rs.getString(1)); m.put("units", rs.getInt(2)); m.put("revenue", rs.getDouble(3));
                    out.add(m);
                }
            }
            return out;
        } catch (SQLException e) { throw new InventoryException("Failed to load top sellers", e); }
    }
}
