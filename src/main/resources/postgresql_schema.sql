-- PostgreSQL 16 Schema Initialization Script
-- Doctor-Patient Portal - Hospital Management System
-- Migrated from MySQL to PostgreSQL 16
--
-- Usage: psql -U postgres -d hospital -f postgresql_schema.sql
-- Prerequisites: CREATE DATABASE hospital; (run as superuser)

-- ============================================================
-- Create tables with PostgreSQL-compatible syntax
-- Note: All column names use snake_case (PostgreSQL best practice)
--       No quoted identifiers needed with snake_case naming
-- ============================================================

-- Drop tables if they exist (for clean re-initialization)
DROP TABLE IF EXISTS appointment CASCADE;
DROP TABLE IF EXISTS doctor CASCADE;
DROP TABLE IF EXISTS user_details CASCADE;
DROP TABLE IF EXISTS specialist CASCADE;

-- ============================================================
-- Table: specialist
-- ============================================================
CREATE TABLE specialist (
    id               SERIAL PRIMARY KEY,
    specialist_name  VARCHAR(255) NOT NULL
);

-- ============================================================
-- Table: user_details
-- ============================================================
CREATE TABLE user_details (
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL
);

-- ============================================================
-- Table: doctor
-- Note: snake_case column names for PostgreSQL compatibility
-- ============================================================
CREATE TABLE doctor (
    id              SERIAL PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    date_of_birth   VARCHAR(50),
    qualification   VARCHAR(255),
    specialist      VARCHAR(255),
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(50),
    password        VARCHAR(255) NOT NULL
);

-- ============================================================
-- Table: appointment
-- Note: snake_case column names for PostgreSQL compatibility
-- ============================================================
CREATE TABLE appointment (
    id               SERIAL PRIMARY KEY,
    user_id          INTEGER NOT NULL REFERENCES user_details(id) ON DELETE CASCADE,
    full_name        VARCHAR(255) NOT NULL,
    gender           VARCHAR(20),
    age              VARCHAR(20),
    appointment_date VARCHAR(50),
    email            VARCHAR(255),
    phone            VARCHAR(50),
    diseases         VARCHAR(500),
    doctor_id        INTEGER REFERENCES doctor(id) ON DELETE SET NULL,
    address          VARCHAR(500),
    status           VARCHAR(500) DEFAULT 'Pending'
);

-- ============================================================
-- Indexes for performance
-- ============================================================
CREATE INDEX idx_appointment_userid   ON appointment(user_id);
CREATE INDEX idx_appointment_doctorid ON appointment(doctor_id);
CREATE INDEX idx_doctor_email         ON doctor(email);
CREATE INDEX idx_user_email           ON user_details(email);

-- ============================================================
-- Sample data (optional - for testing)
-- ============================================================
-- INSERT INTO specialist (specialist_name) VALUES ('Cardiology'), ('Neurology'), ('Orthopedics');
