package com.inventory.web;

import com.google.gson.Gson;
import com.inventory.exception.AuthenticationException;
import com.inventory.exception.InventoryException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.service.AuthService;
import com.inventory.service.InventoryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** REST API + static page server using the JDK built-in HttpServer (no framework needed). */
public class WebServer {
    private static final Map<String, String> IMAGE_TYPES = Map.of(
        "/images/pen.jpg", "image/jpeg",
        "/images/notebook.jpg", "image/jpeg",
        "/images/books.jpg", "image/jpeg",
        "/images/mouse.jpg", "image/jpeg",
        "/images/keyboard.jpg", "image/jpeg",
        "/images/headphones.jpg", "image/jpeg",
        "/images/calculator.jpg", "image/jpeg",
        "/images/cable.png", "image/png"
    );

    record ProductReq(String name, String category, String type, double price, int quantity, int reorderLevel) {}
    record QtyReq(int productId, int quantity) {}
    record AuthReq(String email, String password) {}

    private final InventoryService svc = new InventoryService();
    private final AuthService auth = new AuthService();
    private final Gson gson = new Gson();

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::api);
        server.createContext("/", this::page);
        server.start();
        System.out.println("Smart Inventory running at http://localhost:" + port);
    }

    private void page(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String resource = "/static/index.html";
        String contentType = "text/html; charset=utf-8";
        if (!path.equals("/")) {
            contentType = IMAGE_TYPES.get(path);
            if (contentType == null) {
                ex.sendResponseHeaders(404, -1);
                ex.close();
                return;
            }
            resource = "/static" + path;
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        }
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                ex.sendResponseHeaders(404, -1);
                ex.close();
                return;
            }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", contentType);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
        } finally { ex.close(); }
    }

    private void api(HttpExchange ex) throws IOException {
        int status = 200; Object result;
        try {
            if (!ex.getRequestURI().getPath().startsWith("/api/auth/")) {
                auth.authenticate(sessionToken(ex));
            }
            result = route(ex);
        } catch (AuthenticationException e) { status = e.getStatus(); result = Map.of("error", e.getMessage());
        } catch (ProductNotFoundException e) { status = 404; result = Map.of("error", e.getMessage());
        } catch (InventoryException e) { status = 400; result = Map.of("error", e.getMessage());
        } catch (Exception e) { e.printStackTrace(); status = 500; result = Map.of("error", "Server error: " + e.getMessage()); }
        byte[] out = gson.toJson(result).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    private Object route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] seg = ex.getRequestURI().getPath().substring(1).split("/");   // api, products, 3, stock
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String res = seg.length > 1 ? seg[1] : "";

        switch (res) {
            case "auth":
                if (seg.length == 3 && seg[2].equals("signup") && method.equals("POST")) {
                    AuthReq request = gson.fromJson(body, AuthReq.class);
                    return startSession(ex, auth.createAccount(request.email(), request.password()));
                }
                if (seg.length == 3 && seg[2].equals("login") && method.equals("POST")) {
                    AuthReq request = gson.fromJson(body, AuthReq.class);
                    return startSession(ex, auth.login(request.email(), request.password()));
                }
                if (seg.length == 3 && seg[2].equals("me") && method.equals("GET")) {
                    return auth.authenticate(sessionToken(ex));
                }
                if (seg.length == 3 && seg[2].equals("logout") && method.equals("POST")) {
                    auth.logout(sessionToken(ex));
                    ex.getResponseHeaders().add("Set-Cookie", "SI_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
                    return Map.of("ok", true);
                }
                break;
            case "products":
                if (seg.length == 2 && method.equals("GET")) return svc.listProducts(q.get("q"), q.get("sort"));
                if (seg.length == 2 && method.equals("POST")) {
                    ProductReq r = gson.fromJson(body, ProductReq.class);
                    return svc.addProduct(r.type(), r.name(), r.category(), r.price(), r.quantity(), r.reorderLevel());
                }
                if (seg.length == 3 && method.equals("DELETE")) { svc.deleteProduct(Integer.parseInt(seg[2])); return Map.of("ok", true); }
                if (seg.length == 4 && seg[3].equals("stock") && method.equals("POST"))
                    return svc.stockIn(Integer.parseInt(seg[2]), gson.fromJson(body, QtyReq.class).quantity());
                break;
            case "sales":
                if (method.equals("GET")) return svc.recentSales();
                if (method.equals("POST")) { QtyReq r = gson.fromJson(body, QtyReq.class); return svc.sell(r.productId(), r.quantity()); }
                break;
            case "suggestions":
                if (method.equals("GET")) return svc.suggestions();
                break;
            case "requests":
                if (seg.length == 2 && method.equals("GET")) return svc.pendingRequests();
                if (seg.length == 2 && method.equals("POST")) { QtyReq r = gson.fromJson(body, QtyReq.class); return svc.queueRequest(r.productId(), r.quantity()); }
                if (seg.length == 3 && seg[2].equals("fulfil") && method.equals("POST")) return svc.fulfilNext();
                break;
            case "report":
                if (method.equals("GET")) return svc.report();
                break;
        }
        throw new InventoryException("Unknown endpoint: " + method + " " + ex.getRequestURI().getPath());
    }

    private Object startSession(HttpExchange ex, AuthService.Session session) {
        ex.getResponseHeaders().add("Set-Cookie", "SI_SESSION=" + session.token() +
            "; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax");
        return session.user();
    }

    private String sessionToken(HttpExchange ex) {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && parts[0].equals("SI_SESSION")) return parts[1];
        }
        return null;
    }

    private Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }
}
