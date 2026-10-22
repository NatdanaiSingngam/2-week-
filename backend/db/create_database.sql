-- สร้างฐานข้อมูลและผู้ใช้สำหรับ Workmatch (รันครั้งเดียวด้วยบัญชี root ของ MySQL)
-- ตารางทั้ง 6 จะถูกสร้างอัตโนมัติตอนรัน Spring Boot ครั้งแรก (ddl-auto=update)
CREATE DATABASE IF NOT EXISTS workmatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- เปลี่ยน 'change_me' เป็นรหัสผ่านของคุณเอง แล้วใช้ค่าเดียวกันใน DB_PASSWORD
CREATE USER IF NOT EXISTS 'workmatch'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON workmatch.* TO 'workmatch'@'localhost';
FLUSH PRIVILEGES;
