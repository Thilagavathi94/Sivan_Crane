-- ============================================================================
-- Sivan Crane Service - Database Schema
-- ============================================================================
-- This file is OPTIONAL. By default, the Spring Boot application will
-- automatically create/update this schema on startup because
-- application.properties has: spring.jpa.hibernate.ddl-auto=update
--
-- Use this file only if you prefer to create the database manually first
-- via phpMyAdmin (XAMPP) before running the application.
--
-- HOW TO USE (XAMPP):
--   1. Start Apache + MySQL in the XAMPP Control Panel.
--   2. Open http://localhost/phpmyadmin
--   3. Click "Import" (or "SQL" tab after creating an empty database
--      named crane_management_db) and run this file.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS crane_management_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE crane_management_db;

-- ---------------------------------------------------------------------------
-- Users (login)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  role VARCHAR(50) DEFAULT 'ADMIN',
  active BOOLEAN DEFAULT TRUE
);

-- ---------------------------------------------------------------------------
-- Customers
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  gst_number VARCHAR(50),
  address VARCHAR(500),
  status VARCHAR(50) DEFAULT 'Active',
  created_at DATETIME
);

-- ---------------------------------------------------------------------------
-- Cranes
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cranes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  crane_no VARCHAR(50) NOT NULL UNIQUE,
  registration_no VARCHAR(50),
  type VARCHAR(50),
  capacity VARCHAR(50),
  status VARCHAR(50) DEFAULT 'Available'
);

-- ---------------------------------------------------------------------------
-- Drivers
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS drivers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  license_no VARCHAR(50),
  assigned_crane_id BIGINT,
  status VARCHAR(50) DEFAULT 'Active',
  CONSTRAINT fk_driver_crane FOREIGN KEY (assigned_crane_id) REFERENCES cranes(id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_no VARCHAR(50) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  booking_date DATE,
  location VARCHAR(255),
  work_type VARCHAR(255),
  description VARCHAR(1000),
  preferred_crane_id BIGINT,
  driver_id BIGINT,
  status VARCHAR(50) DEFAULT 'Pending',
  converted_to_trip_sheet BOOLEAN DEFAULT FALSE,
  created_at DATETIME,
  CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_booking_crane FOREIGN KEY (preferred_crane_id) REFERENCES cranes(id),
  CONSTRAINT fk_booking_driver FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

-- ---------------------------------------------------------------------------
-- Trip Sheets
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trip_sheets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_sheet_no VARCHAR(50) NOT NULL UNIQUE,
  booking_id BIGINT,
  customer_id BIGINT NOT NULL,
  crane_id BIGINT NOT NULL,
  trip_date DATE,
  total_hours DECIMAL(10,2) DEFAULT 0,
  amount DECIMAL(12,2) DEFAULT 0,
  billing_type VARCHAR(50) DEFAULT 'Regular',
  converted_to_invoice BOOLEAN DEFAULT FALSE,
  created_at DATETIME,
  CONSTRAINT fk_trip_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
  CONSTRAINT fk_trip_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_trip_crane FOREIGN KEY (crane_id) REFERENCES cranes(id)
);

-- ---------------------------------------------------------------------------
-- Quotations
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quotations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  quotation_no VARCHAR(50) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  quotation_date DATE,
  subtotal DECIMAL(12,2) DEFAULT 0,
  gst_percent DECIMAL(5,2) DEFAULT 18,
  gst_amount DECIMAL(12,2) DEFAULT 0,
  total_amount DECIMAL(12,2) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'Pending',
  CONSTRAINT fk_quotation_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS quotation_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  quotation_id BIGINT NOT NULL,
  description VARCHAR(255),
  hours_or_units DECIMAL(10,2) DEFAULT 0,
  rate_per_hour DECIMAL(12,2) DEFAULT 0,
  additional_hours DECIMAL(10,2) DEFAULT 0,
  additional_rate DECIMAL(12,2) DEFAULT 0,
  additional_amount DECIMAL(12,2) DEFAULT 0,
  amount DECIMAL(12,2) DEFAULT 0,
  CONSTRAINT fk_qitem_quotation FOREIGN KEY (quotation_id) REFERENCES quotations(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Invoices (GST)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_no VARCHAR(50) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  trip_sheet_id BIGINT,
  booking_id BIGINT,
  invoice_date DATE,
  taxable_amount DECIMAL(12,2) DEFAULT 0,
  cgst_percent DECIMAL(5,2) DEFAULT 9,
  sgst_percent DECIMAL(5,2) DEFAULT 9,
  cgst_amount DECIMAL(12,2) DEFAULT 0,
  sgst_amount DECIMAL(12,2) DEFAULT 0,
  total_amount DECIMAL(12,2) DEFAULT 0,
  payment_status VARCHAR(50) DEFAULT 'Pending',
  received_amount DECIMAL(12,2) DEFAULT 0,
  balance_amount DECIMAL(12,2) DEFAULT 0,
  CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_invoice_tripsheet FOREIGN KEY (trip_sheet_id) REFERENCES trip_sheets(id),
  CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS invoice_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  description VARCHAR(255),
  hours_or_units DECIMAL(10,2) DEFAULT 0,
  rate DECIMAL(12,2) DEFAULT 0,
  amount DECIMAL(12,2) DEFAULT 0,
  CONSTRAINT fk_iitem_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Payments
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT,
  trip_sheet_id BIGINT,
  payment_date DATE,
  received_amount DECIMAL(12,2) DEFAULT 0,
  payment_mode VARCHAR(50) DEFAULT 'Cash',
  payment_type VARCHAR(50) DEFAULT 'Paid',
  notes VARCHAR(500),
  CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
  CONSTRAINT fk_payment_tripsheet FOREIGN KEY (trip_sheet_id) REFERENCES trip_sheets(id)
);

-- ---------------------------------------------------------------------------
-- Expenses
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expenses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  expense_date DATE,
  category VARCHAR(100),
  crane_id BIGINT,
  amount DECIMAL(12,2) DEFAULT 0,
  description VARCHAR(500),
  CONSTRAINT fk_expense_crane FOREIGN KEY (crane_id) REFERENCES cranes(id)
);

-- ---------------------------------------------------------------------------
-- Seed data (matches DataInitializer.java so both approaches stay in sync)
-- Default login: admin / admin123  (BCrypt hash below)
-- ---------------------------------------------------------------------------
INSERT INTO users (username, password, full_name, role, active)
SELECT 'admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5nba1V7t8sNZ.b3B7EprYRc0Fy0y.', 'Administrator', 'ADMIN', TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO cranes (crane_no, registration_no, type, capacity, status)
SELECT * FROM (SELECT 'KCN-01' AS a, 'TN38CT7504' AS b, 'Hydra' AS c, '12 Ton' AS d, 'Available' AS e) t
WHERE NOT EXISTS (SELECT 1 FROM cranes WHERE crane_no = 'KCN-01');

INSERT INTO cranes (crane_no, registration_no, type, capacity, status)
SELECT * FROM (SELECT 'KCN-02', 'TN38DS9893', 'Hydra', '17 Ton', 'Working') t
WHERE NOT EXISTS (SELECT 1 FROM cranes WHERE crane_no = 'KCN-02');

INSERT INTO cranes (crane_no, registration_no, type, capacity, status)
SELECT * FROM (SELECT 'KCN-03', 'TN38CR1648', 'Hydra', '12 Ton', 'Working') t
WHERE NOT EXISTS (SELECT 1 FROM cranes WHERE crane_no = 'KCN-03');

INSERT INTO cranes (crane_no, registration_no, type, capacity, status)
SELECT * FROM (SELECT 'KCN-04', 'TN38CT0931', 'Hydra', '16 Ton', 'Available') t
WHERE NOT EXISTS (SELECT 1 FROM cranes WHERE crane_no = 'KCN-04');

INSERT INTO cranes (crane_no, registration_no, type, capacity, status)
SELECT * FROM (SELECT 'KCN-05', 'TN38DF6791', 'Hydra', '13 Ton', 'Service') t
WHERE NOT EXISTS (SELECT 1 FROM cranes WHERE crane_no = 'KCN-05');

INSERT INTO customers (name, phone, gst_number, address, status, created_at)
SELECT * FROM (SELECT 'Siva Construction', '9787654321', '33ABCCD1234F1Z5', 'Coimbatore', 'Active', NOW()) t
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE name = 'Siva Construction');

INSERT INTO customers (name, phone, gst_number, address, status, created_at)
SELECT * FROM (SELECT 'Raj Builders', '9798765432', '33ABQFG5678H1Z2', 'Tirupur', 'Active', NOW()) t
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE name = 'Raj Builders');

INSERT INTO customers (name, phone, gst_number, address, status, created_at)
SELECT * FROM (SELECT 'Kumar Transport', '9845678901', '33AACVB1111L1Z1', 'Erode', 'Active', NOW()) t
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE name = 'Kumar Transport');

INSERT INTO customers (name, phone, gst_number, address, status, created_at)
SELECT * FROM (SELECT 'Vetri Infra', '9889879697', '33AACVV2222R1Z3', 'Chennai', 'Active', NOW()) t
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE name = 'Vetri Infra');

INSERT INTO drivers (name, phone, license_no, status)
SELECT * FROM (SELECT 'Murugan', '9876543210', 'TN-DL-00123', 'Active') t
WHERE NOT EXISTS (SELECT 1 FROM drivers WHERE name = 'Murugan');

INSERT INTO drivers (name, phone, license_no, status)
SELECT * FROM (SELECT 'Selvam', '9876543211', 'TN-DL-00456', 'Active') t
WHERE NOT EXISTS (SELECT 1 FROM drivers WHERE name = 'Selvam');

INSERT INTO drivers (name, phone, license_no, status)
SELECT * FROM (SELECT 'Karthik', '9876543212', 'TN-DL-00789', 'Active') t
WHERE NOT EXISTS (SELECT 1 FROM drivers WHERE name = 'Karthik');
