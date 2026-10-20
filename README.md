# 📱 MobiStock

**Inventory Management System for Mobile Phone & Accessories Shops**

MobiStock replaces manual register-based tracking with a digital system accessible from any device — phone, tablet, or computer.

## Features

- 📱 **Phone Inventory** — Track each phone individually by IMEI (packed & used)
- 🎧 **Accessories Inventory** — Track accessories by quantity per variant (color, model)
- 🛒 **Point of Sale** — Barcode/QR scanning with manual entry fallback
- 📊 **Dashboard** — Today's sales, profit, and low-stock alerts at a glance
- 📈 **Reports** — Profit reports by day/month/custom range, CSV export
- 🔔 **Low-Stock Alerts** — Per-variant configurable thresholds
- 🔐 **Access Control** — Admin and Seller roles with audit logging
- ☁️ **Backup** — Scheduled database backup to cloud storage

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2 |
| Frontend | Thymeleaf, Bootstrap 5, HTMX |
| Database | MySQL 8 |
| Security | Spring Security |
| Build | Maven |

## Prerequisites

1. **Java 17 (JDK)** — [Download Adoptium Temurin](https://adoptium.net/temurin/releases/?version=17)
2. **MySQL 8** — [Download MySQL](https://dev.mysql.com/downloads/installer/)

## Quick Start

1. **Set up MySQL:**
   ```sql
   -- MySQL creates the database automatically, just make sure MySQL is running
   -- Default credentials in application.properties: root/root
   ```

2. **Run the application:**
   ```bash
   # Windows
   mvnw.cmd spring-boot:run

   # Or with Maven installed
   mvn spring-boot:run
   ```

3. **Open in browser:**
   ```
   http://localhost:8080
   ```

4. **Default login:**
   - Username: `admin`
   - Password: `admin123`

## Project Structure

```
MobiStock/
├── src/main/java/com/mobistock/
│   ├── config/          # Security, app configuration
│   ├── controller/      # Web controllers (7 controllers)
│   ├── dto/             # Data transfer objects
│   ├── model/           # JPA entities (7 entities)
│   ├── repository/      # Spring Data repositories
│   └── service/         # Business logic services
├── src/main/resources/
│   ├── templates/       # Thymeleaf HTML templates
│   ├── static/          # CSS, JS, images
│   └── application.properties
└── pom.xml
```

## License

Private — For shop use only.
