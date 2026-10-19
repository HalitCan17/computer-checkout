SET NAMES utf8mb4;
USE swe307pro2;

CREATE TABLE students (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(30) NOT NULL,
    last_name      VARCHAR(30) NOT NULL,
    department     VARCHAR(30) NOT NULL,
    student_number VARCHAR(50) NOT NULL,
    photo          VARCHAR(100) NULL
);

CREATE TABLE computers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand         VARCHAR(30)  NOT NULL,
    model         VARCHAR(30)  NOT NULL,
    serial_number VARCHAR(100) NOT NULL,
    is_available  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE reservations (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_date DATETIME NOT NULL,
    return_date      DATETIME NOT NULL,
    student_id       BIGINT   NOT NULL,
    computer_id      BIGINT   NOT NULL,
    FOREIGN KEY (student_id)  REFERENCES students(id),
    FOREIGN KEY (computer_id) REFERENCES computers(id)
);