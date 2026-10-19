SET NAMES utf8mb4;
USE swe307pro2;

INSERT INTO students (first_name, last_name, department, student_number) VALUES
('Halit',   'Can',   'Yazılım Mühendisliği', 'B250000001'),
('Aytek', 'Aksu',    'Yazılım Mühendisliği',    'B250000002'),
('Berkan', 'Akten',  'Bilgisayar Mühendisliği', 'B250000003'),
('Eren',   'Can',  'Elektrik-Elektronik',     'B250000004'),
('Ata',   'Zaptiye',   'Kaldırım Mühendisliği',   'B250000005'),
('Burak',  'Çelik',   'Yazılım Mühendisliği',    'B250000006'),
('Selin',  'Aydın',   'Makine Mühendisliği',     'B250000007'),
('Can',    'Öztürk',  'Bilgisayar Mühendisliği', 'B250000008'),
('Deniz',  'Koç',     'Elektrik-Elektronik',     'B250000009'),
('Ece',    'Polat',   'Yazılım Mühendisliği',    'B250000010');

INSERT INTO computers (brand, model, serial_number, is_available) VALUES
('Dell',   'Latitude 5440',   'SN-DL-0001', TRUE),
('Lenovo', 'ThinkPad T14',    'SN-LN-0002', TRUE),
('HP',     'ProBook 450',     'SN-HP-0003', TRUE),
('Asus',   'VivoBook 15',     'SN-AS-0004', TRUE),
('Apple',  'MacBook Air M2',  'SN-AP-0005', TRUE),
('MSI',    'Modern 14',       'SN-MS-0006', FALSE);

