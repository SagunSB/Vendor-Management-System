# Vendor Management System

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
```

