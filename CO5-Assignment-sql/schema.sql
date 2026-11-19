-- Local TEST database for Assignment 5 only. Not a production database.
DROP DATABASE IF EXISTS transport_test;
CREATE DATABASE transport_test;
USE transport_test;

CREATE TABLE users (
    id   VARCHAR(30) PRIMARY KEY,
    pwd  VARCHAR(30) NOT NULL
);
INSERT INTO users (id, pwd) VALUES ('admin', 'secret123'), ('student1', 'pass456');

CREATE TABLE bus_pass (
    pass_id   VARCHAR(10)  PRIMARY KEY,
    reg_no    VARCHAR(20)  NOT NULL,
    name      VARCHAR(100) NOT NULL,
    route_no  INT          NOT NULL,
    amount    DECIMAL(10,2) NOT NULL
);
