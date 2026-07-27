# Sivan Crane Service - Management & GST Billing Software

A full-stack Java Spring Boot application implementing the crane rental
business workflow: **Booking → Trip Sheet → GST Invoice → Payment**, plus
Quotation and Expense tracking, backed by MySQL (works with XAMPP).

---

## 1. Tech Stack

| Layer          | Technology                                   |
|----------------|-----------------------------------------------|
| Backend        | Java 17, Spring Boot 3.3.2                    |
| Web/Frontend   | Thymeleaf + Bootstrap 5 (server-rendered HTML)|
| Database       | MySQL (via XAMPP)                             |
| ORM            | Spring Data JPA / Hibernate                   |
| Security       | Spring Security (form login)                  |
| Build Tool     | Maven                                         |

This is a traditional **server-rendered** Spring Boot app (not a separate
React/Angular frontend + REST API). Every page is a Thymeleaf template
served directly by the Spring MVC controllers — simplest possible setup
for a first-time computer user's business, and easiest to deploy.

---

## 2. Project Structure

```
crane-management/
├── pom.xml
├── database/
│   └── schema.sql                 <- optional manual DB setup for phpMyAdmin
├── src/main/java/com/sivan/cranemanagement/
│   ├── CraneManagementApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java     <- login/logout, password encoding
│   │   ├── WebConfig.java          <- form dropdown -> entity binding
│   │   └── DataInitializer.java    <- seeds admin user + sample data
│   ├── model/                      <- JPA entities (Customer, Crane, Driver,
│   │                                   Booking, TripSheet, Quotation, Invoice,
│   │                                   Payment, Expense, User, ...)
│   ├── repository/                 <- Spring Data JPA repositories
│   ├── service/                    <- business logic incl. document numbering
│   │                                   and the Booking->TripSheet->Invoice
│   │                                   "carry forward" conversion logic
│   └── controller/                 <- Spring MVC controllers (one per module)
└── src/main/resources/
    ├── application.properties      <- XAMPP MySQL connection settings
    ├── templates/                  <- Thymeleaf HTML pages
    └── static/css/style.css
```

---

## 3. Prerequisites

1. **JDK 17+** installed (`java -version` to check)
2. **Maven 3.8+** installed (`mvn -version` to check) — or use an IDE
   (IntelliJ IDEA / Eclipse / VS Code) which bundles Maven support
3. **XAMPP** installed with **Apache** and **MySQL** modules

---

## 4. Setup Instructions (XAMPP + MySQL)

### Step 1 — Start XAMPP
Open the XAMPP Control Panel and click **Start** next to both:
- Apache
- MySQL

### Step 2 — Database
You have two options; pick ONE:

**Option A (recommended — zero setup):**
Do nothing. The app's `application.properties` already points to
`jdbc:mysql://localhost:3306/crane_management_db?createDatabaseIfNotExist=true`
with `spring.jpa.hibernate.ddl-auto=update`, so Spring Boot will
automatically create the database and every table the first time it runs.

**Option B (manual, via phpMyAdmin):**
1. Go to `http://localhost/phpmyadmin`
2. Click the **SQL** tab (or Import) and run the script in
   `database/schema.sql`
   This creates all tables and inserts the same sample data as Option A.

> If your MySQL root user has a password set (not the XAMPP default),
> update `spring.datasource.password` in
> `src/main/resources/application.properties` accordingly.

### Step 3 — Run the application

From the project folder:

```bash
mvn spring-boot:run
```

Or build a runnable jar:

```bash
mvn clean package
java -jar target/crane-management.jar
```

Or simply open the project folder in **IntelliJ IDEA** (File → Open →
select the folder with `pom.xml`) and run
`CraneManagementApplication.java` directly.

### Step 4 — Open the app

Go to: **http://localhost:8080**

You'll be redirected to the login page.

**Default login:**
- Username: `admin`
- Password: `admin123`

(Change this password later from a real "Users/Settings" module if you
extend the project — the current build focuses on the core operational
workflow you asked for.)

---

## 5. The Core Workflow (matches your process diagram)

```
LOGIN → DASHBOARD
  → New Booking (pick/add Customer, date, location, work type,
                 preferred crane, driver)
  → "→ Trip Sheet" button on the booking row
       (Customer / Crane / Driver / Date carry forward automatically —
        nothing is re-typed)
  → Fill in Start/End time, hours, work details → Save
  → "→ Invoice" button on the trip sheet row
       (Customer + a crane-hours line item are pre-filled automatically)
  → Adjust rate/hours, GST %, save → GST Invoice is generated
       (INV-2026-00001 style numbering, CGST + SGST auto-calculated)
  → Record Payment against that invoice
       - Paid in full  -> invoice marked "Paid"
       - Partial/Credit -> balance tracked automatically,
                            shows up in "Pending Payments"
```

This means: **Booking BK-00001 → Trip Sheet TS-00001 → Invoice
INV-2026-00001 → Payment**, exactly as you described, with each step
pre-filling the next so a first-time computer user never re-types the
same information three times.

### Quotation workflow (separate, as requested)
`Quotations` page lets you price a job before it's confirmed. Add line
items + GST %, save to generate a quotation. When the customer accepts,
click **"Accept → Booking"** and a new Booking is created automatically
from that quotation (status marked `Converted`).

### Expense workflow
`Expenses` page: pick a date, category (Diesel / Driver Advance / Repair /
Maintenance / Food / Tyre / Other), optionally link a crane, enter the
amount. These roll up into the Dashboard's "Total Expenses" card and the
Reports page.

### Dashboard
Shows: total cranes, today's bookings, today's income, pending payments,
a booking-status breakdown, recent bookings, crane status list, and a
"Today Summary" panel computing **Income − Expenses**.

---

## 6. Page-by-Page Build Order (for reference / if extending)

Login → Dashboard → Customers → Cranes → Drivers → Bookings → Trip Sheet
→ Quotation → GST Invoice → Payment → Expenses → Reports

Master data pages (Customers, Cranes, Drivers) are simple CRUD screens.
The operational pages (Bookings, Trip Sheet, Invoice) carry data forward
via the "Convert" links described above.

---

## 7. Notes & Next Steps

- **Numbering:** Booking (`BK-00001`), Trip Sheet (`TS-00001`), Quotation
  (`QUO-00001`), Invoice (`INV-2026-00001`) are auto-generated sequentially
  by `NumberGeneratorService`.
- **Crane status** automatically flips to "Working" when a crane is
  assigned to an in-progress booking, so the dashboard/crane list stays
  accurate without manual updates.
- **This build covers the full operational workflow** you asked for.
  Not yet wired up (left as straightforward extensions since they weren't
  part of the core workflow): PDF generation / WhatsApp send buttons on
  quotations & invoices (currently these are print-ready HTML views —
  add a PDF library like OpenPDF/iText and a `/print` route per module
  if you need downloadable PDFs), full role-based Settings/User
  Management screens, and a Backup/Restore UI (the data already lives
  in a normal MySQL database, so any standard MySQL backup tool —
  phpMyAdmin export, `mysqldump`, etc. — works today).
- **Security:** all pages require login except `/login` and static
  assets. Passwords are BCrypt-hashed.

---

## 8. Default Login Recap

| Field    | Value      |
|----------|------------|
| Username | admin      |
| Password | admin123   |
| URL      | http://localhost:8080 |
