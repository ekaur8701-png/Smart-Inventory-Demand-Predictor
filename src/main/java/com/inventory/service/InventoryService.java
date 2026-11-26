package com.inventory.service;

import com.inventory.db.ProductDao;
import com.inventory.db.SaleDao;
import com.inventory.exception.InventoryException;
import com.inventory.exception.OutOfStockException;
import com.inventory.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Business logic. Uses List, Map, Queue and Comparator from the Collections framework. */
public class InventoryService {
    private final ProductDao productDao = new ProductDao();
    private final SaleDao saleDao = new SaleDao();
    private final DemandEstimator estimator = new DemandEstimator();
    private final Queue<StockRequest> pending = new LinkedList<>();   // FIFO queue of restock requests
    private int requestSeq = 1;

    private static final Map<String, Comparator<Product>> SORTS = new HashMap<>();
    static {
        SORTS.put("name", Comparator.comparing(p -> p.getName().toLowerCase()));
        SORTS.put("quantity", Comparator.comparingInt(Product::getQuantity));
        SORTS.put("price", Comparator.comparingDouble(Product::getPrice));
    }

    public List<Product> listProducts(String query, String sort) {
        List<Product> list = productDao.findAll();
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase(Locale.ROOT);
            list = list.stream().filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(q)
                    || (p.getCategory() != null && p.getCategory().toLowerCase(Locale.ROOT).contains(q)))
                    .collect(Collectors.toList());
        }
        Comparator<Product> cmp = SORTS.get(sort == null ? "" : sort);
        if (cmp != null) list.sort(cmp);
        return list;
    }

    public Product addProduct(String type, String name, String category, double price, int qty, int reorder) {
        if (name == null || name.isBlank()) throw new InventoryException("Product name is required");
        if (!Double.isFinite(price) || price < 0 || qty < 0 || reorder < 0) {
            throw new InventoryException("Price must be finite and non-negative; quantity and reorder level cannot be negative");
        }
        Product p = ProductFactory.create(0, type, name.trim(), category == null ? "" : category.trim(), price, qty, reorder);
        int id = productDao.insert(p);
        return productDao.findById(id);
    }

    public void deleteProduct(int id) { productDao.delete(id); }

    public synchronized Product stockIn(int id, int qty) {
        if (qty <= 0) throw new InventoryException("Quantity must be positive");
        Product p = productDao.findById(id);
        if (qty > Integer.MAX_VALUE - p.getQuantity()) {
            throw new InventoryException("Stock quantity exceeds the maximum supported value");
        }
        productDao.updateQuantity(id, p.getQuantity() + qty);
        return productDao.findById(id);
    }

    public synchronized Sale sell(int id, int qty) {
        if (qty <= 0) throw new InventoryException("Quantity must be positive");
        Product p = productDao.findById(id);
        if (p.getQuantity() < qty) throw new OutOfStockException(p.getName(), p.getQuantity(), qty);
        productDao.updateQuantity(id, p.getQuantity() - qty);
        double total = qty * p.getPrice();
        saleDao.insert(id, qty, total, LocalDate.now());
        return new Sale(0, id, p.getName(), qty, total, LocalDate.now().toString());
    }

    public List<Sale> recentSales() { return saleDao.recent(50); }

    public List<Suggestion> suggestions() {
        LocalDate since = LocalDate.now().minusDays(DemandEstimator.WINDOW_DAYS);
        List<Suggestion> out = new ArrayList<>();
        Map<Integer, Integer> unitsSold = saleDao.unitsSoldByProductSince(since);
        for (Product p : productDao.findAll()) {
            out.add(estimator.estimate(p, unitsSold.getOrDefault(p.getId(), 0)));
        }
        Map<String, Integer> rank = Map.of("OUT", 0, "LOW", 1, "OK", 2);
        out.sort(Comparator.comparing((Suggestion s) -> rank.get(s.status())).thenComparingDouble(Suggestion::daysLeft));
        return out;
    }

    public synchronized StockRequest queueRequest(int productId, int qty) {
        if (qty <= 0) throw new InventoryException("Quantity must be positive");
        Product p = productDao.findById(productId);
        StockRequest r = new StockRequest(requestSeq++, productId, p.getName(), qty,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM HH:mm")));
        pending.offer(r);
        return r;
    }

    public synchronized List<StockRequest> pendingRequests() { return new ArrayList<>(pending); }

    /** Receives the oldest request (FIFO) and adds the stock. */
    public synchronized StockRequest fulfilNext() {
        StockRequest r = pending.peek();
        if (r == null) throw new InventoryException("No pending requests");
        stockIn(r.getProductId(), r.getQuantity());
        pending.remove();
        return r;
    }

    public Map<String, Object> report() {
        List<Product> all = productDao.findAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalProducts", all.size());
        m.put("stockValue", all.stream().mapToDouble(Product::stockValue).sum());
        m.put("lowStock", suggestions().stream().filter(s -> !s.status().equals("OK")).count());
        m.put("revenue", saleDao.totalRevenue());
        m.put("unitsSold", (int) saleDao.totalUnits());
        m.put("topSellers", saleDao.topSellers(5));
        Map<String, Long> byCat = all.stream().collect(Collectors.groupingBy(
            p -> p.getCategory().isBlank() ? "Uncategorised" : p.getCategory(), TreeMap::new, Collectors.counting()));
        m.put("byCategory", byCat);
        return m;
    }
}
