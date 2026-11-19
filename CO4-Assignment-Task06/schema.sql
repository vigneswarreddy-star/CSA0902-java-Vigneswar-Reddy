-- Campus Event Registration: database script (Task 6)
CREATE DATABASE IF NOT EXISTS campus_events;
USE campus_events;

CREATE TABLE IF NOT EXISTS events (
    event_id    INT AUTO_INCREMENT PRIMARY KEY,
    event_name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS registrations (
    registration_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id        INT NOT NULL,
    student_id      VARCHAR(20)  NOT NULL,
    student_name    VARCHAR(100) NOT NULL,
    email           VARCHAR(120) NOT NULL,
    registered_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES events (event_id),
    -- Database-level guarantee: one student, one registration per event (duplicate rule)
    CONSTRAINT uq_student_per_event UNIQUE (event_id, student_id)
);

INSERT IGNORE INTO events (event_name) VALUES
    ('Music Night'), ('Dance Battle'), ('Art Fair'), ('Tech Talk');
