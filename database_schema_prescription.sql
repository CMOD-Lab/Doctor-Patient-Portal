-- SQL script to create prescription table for Hospital Management System
-- Execute this script manually in MySQL database

CREATE TABLE prescription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointmentId INT NOT NULL,
    doctorId INT NOT NULL,
    userId INT NOT NULL,
    medicineName VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    instructions TEXT,
    prescriptionDate VARCHAR(50) NOT NULL
);

-- Note: No foreign key constraints are added per existing application pattern
-- Relationships are enforced at the application layer
