-- Database table used by FeeService (Task 1.2). Run once in MySQL.
CREATE DATABASE IF NOT EXISTS college;
USE college;
CREATE TABLE IF NOT EXISTS fees (
    reg_no  VARCHAR(15)   NOT NULL,
    amount  DECIMAL(10,2) NOT NULL,
    paid_at TIMESTAMP     NOT NULL
);
