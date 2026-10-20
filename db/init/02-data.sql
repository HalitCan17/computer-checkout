SET NAMES utf8mb4;
USE swe307pro2;

INSERT INTO students (first_name, last_name, department, student_number) VALUES
('Halit', 'Can', 'Yazılım Mühendisliği', 'B231202070'),
('Aytek', 'Aksu', 'Yazılım Mühendisliği', 'B250000002'),
('Berkan', 'Akten', 'Bilgisayar Mühendisliği', 'B250000003'),
('Eren  Yusuf', 'Can', 'Elektrik-Elektronik', 'B250000004'),
('Ata', 'Zaptiye', 'Kaldırım Mühendisliği', 'B250000005'),
('Burak', 'Çelik', 'Yazılım Mühendisliği', 'B250000006'),
('Selin', 'Aydın', 'Makine Mühendisliği', 'B250000007'),
('Can', 'Öztürk', 'Bilgisayar Mühendisliği', 'B250000008'),
('Deniz', 'Koç', 'Elektrik-Elektronik', 'B250000009'),
('Ece', 'Polat', 'Yazılım Mühendisliği', 'B250000010');

INSERT INTO computers (brand, model, serial_number, is_available) VALUES
('Dell', 'Latitude 5440', 'SN-DL-0001', TRUE),
('Lenovo', 'ThinkPad T14', 'SN-LN-0002', TRUE),
('HP', 'ProBook 450', 'SN-HP-0003', TRUE),
('Asus', 'VivoBook 15', 'SN-AS-0004', TRUE),
('Apple', 'MacBook Air M2', 'SN-AP-0005', TRUE),
('MSI', 'Modern 14', 'SN-MS-0006', FALSE),
('Asus', 'TUF A15', 'SN-AS-0007', TRUE);

INSERT INTO reservations (student_id, computer_id, reservation_date, return_date) VALUES
(1, 1, '2026-10-19 09:00:00', '2026-10-19 11:00:00'),
(2, 1, '2026-10-19 11:00:00', '2026-10-19 13:00:00'),
(3, 2, '2026-10-19 09:00:00', '2026-10-19 12:00:00'),
(4, 3, '2026-10-19 10:00:00', '2026-10-19 14:00:00'),
(5, 3, '2026-10-20 09:00:00', '2026-10-20 11:30:00'),
(6, 4, '2026-10-20 13:00:00', '2026-10-20 16:00:00'),
(7, 5, '2026-10-20 10:00:00', '2026-10-20 12:00:00'),
(8, 1, '2026-10-20 14:00:00', '2026-10-20 17:00:00'),
(9, 2, '2026-10-21 09:00:00', '2026-10-21 10:30:00');