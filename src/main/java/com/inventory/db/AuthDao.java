package com.inventory.db;

import com.inventory.exception.AuthenticationException;
import com.inventory.exception.InventoryException;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class AuthDao {
    public void createUser(String email, String passwordHash) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(email, password_hash) VALUES(?, ?)")) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new AuthenticationException(409, "An account with this email already exists");
            }
            throw new InventoryException("Failed to create account", e);
        }
    }

    public Optional<String> findPasswordHash(String email) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("password_hash")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new InventoryException("Failed to load account", e);
        }
    }

    public void createSession(String tokenHash, String email, Instant expiresAt) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO sessions(token_hash, user_id, expires_at) " +
                 "SELECT ?, id, ? FROM users WHERE email = ?")) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(expiresAt));
            ps.setString(3, email);
            if (ps.executeUpdate() != 1) {
                throw new InventoryException("Could not create account session");
            }
        } catch (SQLException e) {
            throw new InventoryException("Could not create account session", e);
        }
    }

    public Optional<String> findEmailBySession(String tokenHash, Instant now) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT u.email FROM sessions s JOIN users u ON u.id = s.user_id " +
                 "WHERE s.token_hash = ? AND s.expires_at > ?")) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("email")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new InventoryException("Failed to validate account session", e);
        }
    }

    public void deleteSession(String tokenHash) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sessions WHERE token_hash = ?")) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new InventoryException("Could not end account session", e);
        }
    }

    public void deleteExpiredSessions(Instant now) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sessions WHERE expires_at <= ?")) {
            ps.setTimestamp(1, Timestamp.from(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new InventoryException("Could not clean expired sessions", e);
        }
    }
}
