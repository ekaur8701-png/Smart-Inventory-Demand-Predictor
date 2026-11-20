# Smart Inventory (Programming in Java – 25CSE0202)

Inventory management web app: tracks products, stock levels and sales, and suggests what to reorder using
**simple rules** (30-day average sales + supplier lead time). No machine learning.

## Tech
- Java 17, JDK built-in `HttpServer` (REST API), JDBC + H2 embedded database, Gson
- Frontend: single HTML/CSS/JS page served by the app (`src/main/resources/static/index.html`)

## Run
```
mvn compile exec:java
```
Open http://localhost:8080. Demo data is created on first run (stored in `./data`).

## Structure
| Package | Purpose |
|---|---|
| `model` | `Product` + `PerishableProduct` / `ElectronicProduct` (inheritance), `Sale`, `StockRequest`, `Suggestion` |
| `db` | JDBC: `Database`, `ProductDao`, `SaleDao` (aggregate SQL: SUM, GROUP BY) |
| `service` | `InventoryService` (List/Map/Queue/Comparator), `DemandEstimator` (rules), `SeedData` |
| `exception` | `InventoryException`, `ProductNotFoundException`, `OutOfStockException` |
| `web` | `WebServer` REST endpoints |

## REST API
`GET/POST /api/products`, `DELETE /api/products/{id}`, `POST /api/products/{id}/stock`,
`GET/POST /api/sales`, `GET /api/suggestions`, `GET/POST /api/requests`, `POST /api/requests/fulfil`, `GET /api/report`

## Reorder rules
- Target stock = average daily sales × (lead time + 7 buffer days); lead time: perishable 3, general 7, electronic 14 days
- Status: **OUT** if 0 stock; **LOW** if stock ≤ reorder level or days-left < lead time; else **OK**

## Status / pending work
- [x] Product, sales, stock-in/out, search + sort
- [x] Rule-based reorder suggestions, FIFO pending request queue
- [x] JDBC persistence, custom exceptions, web UI
- [ ] Persist the pending-request queue in the database
- [ ] Edit product, CSV export, login

## Team
(Add names + roll numbers here)
