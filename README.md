# Coal India Integrated Employee, Welfare, Vendor and Coal Booking Management System

This repository contains a complete CIVWBMS implementation with a Spring Boot 3 backend, React/Vite frontend, MySQL schema, JWT authentication, role dashboards, approval workflows, coal booking, notifications, charts, PDF export, Excel export, and Swagger documentation.

## Stack

- Frontend: React, Vite, Bootstrap 5, Chart.js, Axios
- Backend: Java 21, Spring Boot 3, Spring Security, JWT, JDBC, Swagger/OpenAPI
- Database: MySQL 8

## Setup

1. Create the database by importing `database/database.sql` in MySQL.
2. Update `backend/src/main/resources/application.properties` if your MySQL username or password differs from XAMPP defaults.
3. Start the backend:

```bash
cd backend
mvn spring-boot:run
```

4. Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

5. Open the Vite URL, usually `http://localhost:5173`.

## Seed Login

All seeded accounts use password `Password@123`.

- Employee: `EMP001`, subsidiary `ECL`
- Admin: `ADM001`, subsidiary `ECL`
- HQ: `HQ001`, subsidiary `ECL`
- Client: `CLI001`

## API Documentation

Swagger UI is available after the backend starts:

`http://localhost:8080/swagger-ui.html`

## Reports

HQ users can export:

- Approved welfare report as PDF or Excel
- Inventory report as PDF or Excel
- Coal orders report as PDF or Excel

## Folder Structure

```text
backend/
frontend/
database/database.sql
README.md
```

## ER Summary

`subsidiaries` is referenced by `employees`, `admins`, `headquarters`, `coal_inventory`, and `coal_orders`.
Employee welfare modules reference `employees`.
Coal orders reference `clients`, `coal_inventory`, and `subsidiaries`.
Order tracking references `coal_orders`.
Notifications store recipient role and recipient id for employees, clients, admins, and HQ users.
