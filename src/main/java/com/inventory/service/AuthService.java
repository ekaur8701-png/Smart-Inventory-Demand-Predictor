package com.inventory.service;

import com.inventory.db.AuthDao;
import com.inventory.exception.AuthenticationException;
import com.inventory.exception.InventoryException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final int PASSWORD_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final Duration SESSION_LIFETIME = Duration.ofDays(7);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthDao dao = new AuthDao();

    public Session createAccount(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        dao.createUser(normalizedEmail, hashPassword(password));
        return createSession(normalizedEmail);
    }

    public Session login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isEmpty()) {
            throw invalidCredentials();
        }
        Optional<String> storedHash = dao.findPasswordHash(normalizedEmail);
        if (storedHash.isEmpty() || !verifyPassword(password, storedHash.get())) {
            throw invalidCredentials();
        }
        return createSession(normalizedEmail);
    }

    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException(401, "Please log in to continue");
        }
        return dao.findEmailBySession(hashToken(token), Instant.now())
            .map(User::new)
            .orElseThrow(() -> new AuthenticationException(401, "Your session has expired. Please log in again"));
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            dao.deleteSession(hashToken(token));
        }
    }

    private Session createSession(String email) {
        Instant now = Instant.now();
        dao.deleteExpiredSessions(now);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        dao.createSession(hashToken(token), email, now.plus(SESSION_LIFETIME));
        return new Session(new User(email), token);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : Normalizer.normalize(email.trim(), Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new AuthenticationException(400, "Enter a valid email address");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new AuthenticationException(400, "Password must be between 8 and 128 characters");
        }
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, PASSWORD_ITERATIONS);
        return PASSWORD_ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
            Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String password, String encoded) {
        try {
            String[] parts = encoded.split(":", 3);
            if (parts.length != 3) {
                throw new InventoryException("Stored account credentials are invalid");
            }
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            throw new InventoryException("Stored account credentials are invalid", e);
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new InventoryException("Could not process account credentials", e);
        } finally {
            spec.clearPassword();
        }
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new InventoryException("Could not process account session", e);
        }
    }

    private AuthenticationException invalidCredentials() {
        return new AuthenticationException(401, "Email or password is incorrect");
    }

    public record User(String email) {}
    public record Session(User user, String token) {}
}
