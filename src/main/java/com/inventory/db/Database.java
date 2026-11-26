package com.inventory.db;

import com.inventory.exception.InventoryException;
import java.sql.*;

/** JDBC connection + schema creation (H2 embedded database stored in ./data). */
public final class Database {
    private static final String URL = "jdbc:h2:./data/inventory";
    private Database() {}

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, "sa", "");
    }

    public static void init() {
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS products (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, category VARCHAR(60), " +
                "type VARCHAR(20) NOT NULL, price DOUBLE NOT NULL, quantity INT NOT NULL, reorder_level INT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS sales (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, quantity INT NOT NULL, " +
                "total DOUBLE NOT NULL, sale_date DATE NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(200) NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                "token_hash CHAR(64) PRIMARY KEY, user_id INT NOT NULL, expires_at TIMESTAMP NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            throw new InventoryException("Could not initialise database", e);
        }
    }
}
