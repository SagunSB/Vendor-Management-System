DROP DATABASE IF EXISTS civwbms;
CREATE DATABASE civwbms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE civwbms;

CREATE TABLE subsidiaries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL
);

CREATE TABLE employees (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_code VARCHAR(40) NOT NULL UNIQUE,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  mobile VARCHAR(20) NOT NULL,
  subsidiary_id BIGINT NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (subsidiary_id) REFERENCES subsidiaries(id)
);

CREATE TABLE admins (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  mobile VARCHAR(20) NOT NULL,
  subsidiary_id BIGINT NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (subsidiary_id) REFERENCES subsidiaries(id)
);

CREATE TABLE headquarters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  hq_code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  mobile VARCHAR(20) NOT NULL,
  subsidiary_id BIGINT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (subsidiary_id) REFERENCES subsidiaries(id)
);

CREATE TABLE clients (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  client_code VARCHAR(40) NOT NULL UNIQUE,
  company_name VARCHAR(160) NOT NULL,
  gst_number VARCHAR(30) NOT NULL UNIQUE,
  contact_person VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  mobile VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attendance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  in_time TIME NOT NULL,
  out_time TIME NOT NULL,
  status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE leave_applications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  leave_type VARCHAR(60) NOT NULL,
  from_date DATE NOT NULL,
  to_date DATE NOT NULL,
  reason TEXT NOT NULL,
  document_path VARCHAR(255),
  status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE scholarships (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  student_name VARCHAR(120) NOT NULL,
  relationship VARCHAR(60) NOT NULL,
  school_college VARCHAR(160) NOT NULL,
  class_name VARCHAR(40) NOT NULL,
  marks DECIMAL(5,2) NOT NULL,
  annual_income DECIMAL(12,2) NOT NULL,
  document_path VARCHAR(255),
  status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE bus_cards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  student_name VARCHAR(120) NOT NULL,
  school_name VARCHAR(160) NOT NULL,
  route VARCHAR(120) NOT NULL,
  address TEXT NOT NULL,
  photo_path VARCHAR(255),
  status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE pensions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  retirement_date DATE NOT NULL,
  service_years INT NOT NULL,
  last_designation VARCHAR(120) NOT NULL,
  document_path VARCHAR(255),
  status ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE coal_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subsidiary_id BIGINT NOT NULL,
  area VARCHAR(120) NOT NULL,
  colliery VARCHAR(120) NOT NULL,
  coal_grade VARCHAR(30) NOT NULL,
  coal_quality ENUM('Premium','Good','Medium','Low') NOT NULL,
  available_quantity DECIMAL(12,2) NOT NULL,
  price_per_tonne DECIMAL(12,2) NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (subsidiary_id) REFERENCES subsidiaries(id)
);

CREATE TABLE coal_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  client_id BIGINT NOT NULL,
  inventory_id BIGINT NOT NULL,
  subsidiary_id BIGINT NOT NULL,
  area VARCHAR(120) NOT NULL,
  colliery VARCHAR(120) NOT NULL,
  coal_grade VARCHAR(30) NOT NULL,
  quantity_required DECIMAL(12,2) NOT NULL,
  delivery_address TEXT NOT NULL,
  gst_number VARCHAR(30) NOT NULL,
  price_per_tonne DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(14,2) NOT NULL,
  status ENUM('Pending','Approved','Processing','Dispatched','Delivered','Cancelled') NOT NULL DEFAULT 'Pending',
  admin_remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES clients(id),
  FOREIGN KEY (inventory_id) REFERENCES coal_inventory(id),
  FOREIGN KEY (subsidiary_id) REFERENCES subsidiaries(id)
);

CREATE TABLE order_tracking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL,
  remarks VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES coal_orders(id) ON DELETE CASCADE
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recipient_role ENUM('EMPLOYEE','CLIENT','ADMIN','HQ') NOT NULL,
  recipient_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  message VARCHAR(255) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO subsidiaries(code, name) VALUES
('ECL','Eastern Coalfields Limited'),
('BCCL','Bharat Coking Coal Limited'),
('CCL','Central Coalfields Limited'),
('WCL','Western Coalfields Limited'),
('SECL','South Eastern Coalfields Limited'),
('NCL','Northern Coalfields Limited'),
('MCL','Mahanadi Coalfields Limited'),
('CMPDI','Central Mine Planning & Design Institute Limited');

SET @hash = 'Password@123';

INSERT INTO employees(employee_code, full_name, email, mobile, subsidiary_id, password_hash)
SELECT CONCAT('EMP', LPAD(n,3,'0')), CONCAT('Employee ', n), CONCAT('employee', n, '@cil.test'), CONCAT('9000000', LPAD(n,3,'0')), ((n - 1) MOD 8) + 1, @hash
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 100) SELECT n FROM seq) s;

INSERT INTO admins(employee_code, name, email, mobile, subsidiary_id, password_hash)
SELECT CONCAT('ADM', LPAD(n,3,'0')), CONCAT('Admin ', n), CONCAT('admin', n, '@cil.test'), CONCAT('9100000', LPAD(n,3,'0')), ((n - 1) MOD 8) + 1, @hash
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 10) SELECT n FROM seq) s;

INSERT INTO headquarters(hq_code, name, email, mobile, subsidiary_id, password_hash)
SELECT CONCAT('HQ', LPAD(n,3,'0')), CONCAT('HQ User ', n), CONCAT('hq', n, '@cil.test'), CONCAT('9200000', LPAD(n,3,'0')), ((n - 1) MOD 8) + 1, @hash
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 5) SELECT n FROM seq) s;

INSERT INTO clients(client_code, company_name, gst_number, contact_person, email, mobile, password_hash)
SELECT CONCAT('CLI', LPAD(n,3,'0')), CONCAT('Client Company ', n), CONCAT('GSTCIL', LPAD(n,8,'0')), CONCAT('Buyer ', n), CONCAT('client', n, '@buyer.test'), CONCAT('9300000', LPAD(n,3,'0')), @hash
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 20) SELECT n FROM seq) s;

INSERT INTO coal_inventory(subsidiary_id, area, colliery, coal_grade, coal_quality, available_quantity, price_per_tonne)
SELECT ((n - 1) MOD 8) + 1, CONCAT('Area ', ((n - 1) MOD 12) + 1), CONCAT('Colliery ', n), CONCAT('G', ((n - 1) MOD 10) + 1),
ELT(((n - 1) MOD 4) + 1, 'Premium','Good','Medium','Low'), 1000 + (n * 75), 1800 + (n * 35)
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 50) SELECT n FROM seq) s;

INSERT INTO attendance(employee_id, attendance_date, in_time, out_time, status)
SELECT n, DATE_SUB(CURDATE(), INTERVAL (n MOD 20) DAY), '09:00:00', '17:30:00', ELT((n MOD 3)+1, 'Pending','Approved','Rejected')
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 60) SELECT n FROM seq) s;

INSERT INTO leave_applications(employee_id, leave_type, from_date, to_date, reason, document_path, status)
SELECT n, 'Casual Leave', DATE_ADD(CURDATE(), INTERVAL (n MOD 12) DAY), DATE_ADD(CURDATE(), INTERVAL (n MOD 12)+1 DAY), 'Family work', 'uploads/leave.pdf', ELT((n MOD 3)+1, 'Pending','Approved','Rejected')
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 40) SELECT n FROM seq) s;

INSERT INTO coal_orders(client_id, inventory_id, subsidiary_id, area, colliery, coal_grade, quantity_required, delivery_address, gst_number, price_per_tonne, total_amount, status)
SELECT ((n - 1) MOD 20) + 1, ((n - 1) MOD 50) + 1, ci.subsidiary_id, ci.area, ci.colliery, ci.coal_grade, 25 + n, CONCAT('Industrial Yard ', n), CONCAT('GSTCIL', LPAD(((n - 1) MOD 20) + 1,8,'0')), ci.price_per_tonne, ci.price_per_tonne * (25 + n), ELT((n MOD 6)+1, 'Pending','Approved','Processing','Dispatched','Delivered','Cancelled')
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n < 50) SELECT n FROM seq) s
JOIN coal_inventory ci ON ci.id = ((n - 1) MOD 50) + 1;

INSERT INTO order_tracking(order_id, status, remarks)
SELECT id, status, CONCAT('Order currently ', status) FROM coal_orders;

INSERT INTO notifications(recipient_role, recipient_id, title, message)
VALUES ('EMPLOYEE',1,'Welcome','Your employee dashboard is active.'),
('CLIENT',1,'Welcome','Your client dashboard is active.');
