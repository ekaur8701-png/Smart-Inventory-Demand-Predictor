package com.inventory.web;

import com.google.gson.Gson;
import com.inventory.exception.InventoryException;
import com.inventory.exception.ProductNotFoundException;
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
    record ProductReq(String name, String category, String type, double price, int quantity, int reorderLevel) {}
    record QtyReq(int productId, int quantity) {}

    private final InventoryService svc = new InventoryService();
    private final Gson gson = new Gson();

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::api);
        server.createContext("/", this::page);
        server.start();
        System.out.println("Smart Inventory running at http://localhost:" + port);
    }

    private void page(HttpExchange ex) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/static/index.html")) {
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
        } finally { ex.close(); }
    }

    private void api(HttpExchange ex) throws IOException {
        int status = 200; Object result;
        try {
            result = route(ex);
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
