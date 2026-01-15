# Pet & Microchip Management System


Backend system developed in Java focused on data integrity, layered architecture, and transactional consistency.

The application manages pets and their associated microchips, implementing full CRUD operations, validations, SQL JOINs, and a real database transaction with rollback using JDBC and MySQL.

The project is intentionally designed to demonstrate backend fundamentals such as persistence, business rules, and transaction management.

---

## Key Concepts Demonstrated

- Layered architecture (DAO / Service / Model)
- JDBC persistence with MySQL
- Full CRUD operations
- SQL JOINs
- Transactions and rollback
- Data validation
- Soft delete strategy
- Separation of concerns
- Business rule enforcement

---

## Domain Overview

### Pets
- Name (required)
- Species (required)
- Breed (optional)
- Birth date (optional)
- Owner (required)

### Microchips
- Unique code (required)
- Implant date (optional)
- Veterinary clinic (optional)
- Notes (optional)

Relationship:
One-to-one (1 Pet ↔ 1 Microchip)

---

## Tech Stack

- Java 17+
- JDBC
- MySQL 8+
- SQL
- Apache NetBeans

---

## Database Setup

The project includes SQL scripts for database creation and test data:

- script_creacion.sql
- script_datos_test.sql

Database configuration is defined in:

src/Config/DatabaseConnection.java

URL, user and password must be configured according to the local MySQL setup.

---

## Running the Project

1. Import the project into NetBeans.
2. Create the database by running script_creacion.sql.
3. (Optional) Load test data using script_datos_test.sql.
4. Run the application from:

src/Main/AppMenu.java

The application runs as a console-based menu system.

---

## Transaction & Rollback Example

The system includes a specific operation that demonstrates a real database transaction with rollback.

Flow:

1. A microchip is inserted within a transaction.
2. The pet is inserted within the same transaction.
3. If the pet insertion fails due to validation constraints:
   - The pet is not persisted
   - The microchip insertion is rolled back automatically

This ensures:
- No orphan records
- Database consistency
- Proper transactional behavior

---

## Project Structure

src/
 ├── Config/
 │     ├── DatabaseConnection.java
 │     └── TransactionManager.java
 │
 ├── Dao/
 │     ├── GenericDAO.java
 │     ├── MascotaDAO.java
 │     └── MicrochipDAO.java
 │
 ├── Models/
 │     ├── Base.java
 │     ├── Mascota.java
 │     └── Microchip.java
 │
 ├── Service/
 │     ├── GenericService.java
 │     ├── MascotaServiceImpl.java
 │     └── MicrochipServiceImpl.java
 │
 └── Main/
       ├── AppMenu.java
       ├── MenuHandler.java
       └── MenuDisplay.java

---

## Layer Responsibilities

### Model
- Plain entities
- No business logic

### DAO
- Direct interaction with MySQL using JDBC
- CRUD operations
- SQL JOINs
- Soft delete implementation

### Service
- Business rules
- Data validation
- Transaction coordination
- Error handling and transformation

### Presentation
- Console-based menu
- User interaction flow
- Operation handling

---

## Project Status

Completed.

This project focuses exclusively on backend fundamentals by design:
architecture, persistence, transactions, and business logic.
No frontend was included.