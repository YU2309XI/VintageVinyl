# Vintage Vinyl

![CI](https://github.com/Han1230c/VintageVinyl/actions/workflows/ci.yml/badge.svg)
![CodeQL](https://github.com/Han1230c/VintageVinyl/actions/workflows/codeql.yml/badge.svg)


A Spring Boot 3 (Java 21) e-commerce web app for a small record store. It supports browsing records, viewing details, user authentication, shopping cart, checkout & orders, wishlist, admin inventory management, and CSV bulk import. Server-side rendering is done with **Thymeleaf**; data access via **Spring Data JPA**; database is **MySQL**.

> ⚠️ Credentials in `application.properties` should be moved to environment variables for security before deploying.

---

## Features

### Customer
- **Browse records** with pagination & keyword search.
- **Record detail** page with metadata (artist, genre, price, stock).
- **Authentication & Registration** (Spring Security 6, BCrypt).
- **Shopping Cart**: add/remove items, update quantity, cart badge via `ControllerAdvice`.
- **Checkout & Orders**: place orders, see order history & order details.
- **Wishlist**: save records to wishlist for later.

### Admin
- **Inventory Management** (secured for `ADMIN`):
  - Create / Edit / Delete records.
  - Update stock & price.
- **CSV Import** (using Apache Commons CSV):
  - Bulk import records from a CSV file (e.g., initial catalog seed).
- **Dashboard** views for quick admin ops.

### Other
- **CSRF enabled** with custom request attribute handler.
- **JPA pagination** via `PageRequest`.
- **Validation** with Hibernate Validator annotations.
- **Tests** with Spring Boot Test, JUnit 5, Mockito.

---

## Tech Stack

- **Language**: Java 21  
- **Frameworks**: Spring Boot 3.2.x, Spring MVC, Spring Data JPA, Spring Security 6, Thymeleaf  
- **Database**: MySQL (Driver: `com.mysql.cj.jdbc.Driver`)  
- **Build**: Maven  
- **Libraries**: Lombok, Apache Commons CSV, Hibernate Validator, JUnit 5, Mockito  
- **Templates**: Thymeleaf (`src/main/resources/templates/*.html`)

---

## Project Structure

```text
vintage-vinyl/
  ├── pom.xml
  ├── src/
  │   ├── main/java/com/vintagevinyl/
  │   │   ├── VintageVinylApplication.java
  │   │   ├── config/SecurityConfig.java
  │   │   ├── controller/
  │   │   │   ├── AuthController.java
  │   │   │   ├── RecordController.java
  │   │   │   ├── ShoppingCartController.java
  │   │   │   ├── OrderController.java
  │   │   │   └── AdminController.java
  │   │   ├── model/
  │   │   │   ├── User.java
  │   │   │   ├── Record.java
  │   │   │   ├── ShoppingCart.java
  │   │   │   ├── CartItem.java
  │   │   │   ├── Order.java
  │   │   │   ├── OrderItem.java
  │   │   │   └── Wishlist.java
  │   │   ├── repository/*.java
  │   │   └── service/*.java
  │   ├── main/resources/
  │   │   ├── application.properties
  │   │   └── templates/*.html
  │   └── test/java/com/vintagevinyl/... (JUnit, Mockito tests)
```

---

## Data Model (JPA Entities)

- **User**: `id`, `username`, `passwordHash` (BCrypt), `roles` (`USER`, `ADMIN`), relation to `ShoppingCart`, `Order`, `Wishlist`.  
- **Record**: `id`, `title`, `artist`, `genre`, `price`, `stock`, `coverImageUrl`, etc.  
- **ShoppingCart** with `@OneToOne` `User`, and `CartItem` as line items.  
- **Order** / **OrderItem**: totals, status, timestamp, line items referencing `Record`.  
- **Wishlist**: `User` ↔ `Record` mapping.

> Pagination via `Page<Record>` in `RecordController` using `PageRequest`.

---

## Security

- Spring Security 6 `SecurityFilterChain` with `BCryptPasswordEncoder`.  
- Public endpoints: `/`, `/records`, `/login`, `/register`, static `/css/**`, `/js/**`, `/images/**`.  
- Auth-required: cart, checkout, orders, wishlist.  
- Admin-only: `/inventory/**`, `/api/inventory/**`, `/record-import`, related admin pages.  
- CSRF enabled with custom `_csrf` request attribute via `CsrfTokenRequestAttributeHandler`.

---

## CSV Import

- **`CSVImportService`** uses Apache Commons CSV to parse files and create/update `Record` entries.  
- Admin UI page `record-import.html` lets you upload a CSV.  
- Useful for initial catalog seeding and bulk updates.

---

## Prerequisites

- **Java 21** (`java --version`)  
- **Maven 3.9+** (`mvn -v`)  
- **MySQL 8.x** running locally

---

## Configuration

Project reads DB config from `src/main/resources/application.properties`. Replace credentials with your own:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vintage_vinyl_db?useSSL=false
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Recommended security**: Do not commit real passwords. Externalize using env vars or Maven profiles.

```sql
CREATE DATABASE vintage_vinyl_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

---

## Run Locally

```bash
mvn clean spring-boot:run
# or
mvn clean package
java -jar target/vintage-vinyl-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080

- Register a new user at `/register`, then login `/login`.
- To access admin features, bootstrap an admin user (manual DB update or DataLoader).

---

## Selected Endpoints / Pages

- **Public**: `/records`, `/record/{id}`, `/login`, `/register`  
- **User-only**: `/cart`, `/checkout`, `/orders`, `/wishlist`  
- **Admin-only**: `/inventory`, `/record-import` (CSV upload)

---

## Testing

```bash
mvn test
```

- **Data JPA tests** for `RecordRepository` with paging/search.  
- **Service/Controller** tests with JUnit 5 & Mockito.  
- Consider adding integration tests with Testcontainers for MySQL.

---

## Development Notes & Limitations

- Bootstrap an `ADMIN` manually or via seeder.  
- Thymeleaf templates server-rendered; add REST API + SPA if needed.  
- CSV import trusts column names — validate further in production.  
- Move secrets to env vars & configure profiles.  
- Consider adding image upload, payment integration, audit logs.

---

## Portfolio Highlights

- End-to-end commerce flow: catalog → cart → checkout → order tracking.  
- Secure auth with roles, CSRF, BCrypt; admin inventory portal.  
- Scalable data layer: Spring Data JPA, pagination, validation.  
- DevOps-ready: Maven build, externalized config, CI-friendly tests.  
- Bulk CSV import for rapid catalog onboarding.

---

## License

MIT (or your preferred license)
