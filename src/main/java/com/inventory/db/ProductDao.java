package com.inventory.db;

import com.inventory.exception.InventoryException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.model.Product;
import com.inventory.model.ProductFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private Product map(ResultSet rs) throws SQLException {
        return ProductFactory.create(rs.getInt("id"), rs.getString("type"), rs.getString("name"),
            rs.getString("category"), rs.getDouble("price"), rs.getInt("quantity"), rs.getInt("reorder_level"));
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        try (Connection c = Database.get(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM products ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) { throw new InventoryException("Failed to load products", e); }
    }

    public Product findById(int id) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                throw new ProductNotFoundException(id);
            }
        } catch (SQLException e) { throw new InventoryException("Failed to load product", e); }
    }

    public int insert(Product p) {
        String sql = "INSERT INTO products(name,category,type,price,quantity,reorder_level) VALUES(?,?,?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName()); ps.setString(2, p.getCategory()); ps.setString(3, p.getType());
            ps.setDouble(4, p.getPrice()); ps.setInt(5, p.getQuantity()); ps.setInt(6, p.getReorderLevel());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        } catch (SQLException e) { throw new InventoryException("Failed to add product", e); }
    }

    public void updateQuantity(int id, int qty) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE products SET quantity=? WHERE id=?")) {
            ps.setInt(1, qty); ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new ProductNotFoundException(id);
        } catch (SQLException e) { throw new InventoryException("Failed to update stock", e); }
    }

    public void delete(int id) {
        try (Connection c = Database.get()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM sales WHERE product_id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id=?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) throw new ProductNotFoundException(id);
            }
        } catch (SQLException e) { throw new InventoryException("Failed to delete product", e); }
    }
}
