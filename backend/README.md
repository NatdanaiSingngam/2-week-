# Workmatch Backend (Spring Boot)

Backend ของระบบ Workmatch: Java 17 + Spring Boot 3.3 + Thymeleaf + Spring Security + Spring Data JPA + MySQL 8
รันที่พอร์ต **8080**

## โครงสร้างโฟลเดอร์

```
backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/          # Maven Wrapper (ไม่ต้องติดตั้ง Maven เอง)
├── db/create_database.sql         # สร้างฐานข้อมูลและผู้ใช้ MySQL
└── src/
    ├── main/java/com/workmatch/
    │   ├── WorkmatchApplication.java
    │   ├── config/SecurityConfig.java      # ชั่วคราว: เปิดทุก URL (A2 จะเปลี่ยน)
    │   ├── controller/HomeController.java  # GET / -> redirect:/login
    │   ├── entity/                         # User, Role, Resume, ResumeSkill, Job, JobSkill, Match
    │   └── repository/                     # Repository ของทุก entity
    ├── main/resources/
    │   ├── application.properties
    │   ├── templates/   <- ชญาณัตถพลใส่ไฟล์ HTML ที่นี่
    │   └── static/      <- ชญาณัตถพลใส่ css/js/รูป ที่นี่
    └── test/                               # mvn test ใช้ H2 ในหน่วยความจำ ไม่ต้องเปิด MySQL
```

## 1. สิ่งที่ต้องติดตั้ง

- JDK 17 ขึ้นไป (ตรวจด้วย `java -version`)
- MySQL 8 (หรือ MariaDB 10.6+)
- Git, VS Code (ติดตั้ง Extension Pack for Java และ Spring Boot Extension Pack) หรือ IntelliJ IDEA
- ไม่ต้องติดตั้ง Maven เพราะใช้ `mvnw` ที่มากับโปรเจกต์

## 2. สร้างฐานข้อมูล (ทำครั้งเดียว)

เปิด MySQL ด้วยบัญชี root (MySQL Workbench หรือ `mysql -u root -p`) แล้วรันไฟล์ `db/create_database.sql`
**แก้ `change_me` เป็นรหัสผ่านของคุณเองก่อนรัน** หรือพิมพ์เองดังนี้

```sql
CREATE DATABASE workmatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'workmatch'@'localhost' IDENTIFIED BY 'รหัสผ่านของคุณ';
GRANT ALL PRIVILEGES ON workmatch.* TO 'workmatch'@'localhost';
FLUSH PRIVILEGES;
```

(จะใช้ root แทนก็ได้ แค่ตั้ง DB_USER=root)
ไม่ต้องสร้างตารางเอง Spring Boot สร้างให้ทั้ง 6 ตารางตอนรันครั้งแรก

## 3. ตั้งตัวแปรสภาพแวดล้อม DB_USER และ DB_PASSWORD

ห้ามเขียนรหัสผ่านลงในไฟล์ที่ขึ้น GitHub

**Windows PowerShell (เฉพาะหน้าต่างนี้)**
```powershell
$env:DB_USER="workmatch"
$env:DB_PASSWORD="รหัสผ่านของคุณ"
```

**Windows แบบถาวร** (ตั้งครั้งเดียว แล้วปิดเปิด VS Code/เทอร์มินัลใหม่)
```powershell
setx DB_USER "workmatch"
setx DB_PASSWORD "รหัสผ่านของคุณ"
```

**Windows cmd**
```bat
set DB_USER=workmatch
set DB_PASSWORD=รหัสผ่านของคุณ
```

**IntelliJ IDEA**: Run > Edit Configurations... > เลือก WorkmatchApplication > Environment variables ใส่
`DB_USER=workmatch;DB_PASSWORD=รหัสผ่านของคุณ`

**VS Code (ปุ่ม Run ของ Spring Boot Dashboard)**: ใช้แบบ `setx` ด้านบน แล้วปิดเปิด VS Code ใหม่

## 4. รันโปรเจกต์

```powershell
cd backend
.\mvnw spring-boot:run        # Windows
./mvnw spring-boot:run        # macOS / Linux
```

รอจนเห็น `Started WorkmatchApplication` แล้วเปิด http://localhost:8080
จะถูกส่งไป `/login` (ถ้ายังไม่มี `login.html` จะขึ้นหน้า error 404 ถือว่าปกติ)

## 5. ตรวจว่าตารางครบ

```sql
USE workmatch;
SHOW TABLES;
-- ต้องเห็น 6 ตาราง: jobs, job_skills, matches, resume_skills, resumes, users
```

## 6. รันชุดทดสอบ

```powershell
.\mvnw test
```
ใช้ฐานข้อมูล H2 ในหน่วยความจำ จึงไม่ต้องเปิด MySQL

## ปัญหาที่พบบ่อย

| อาการ | วิธีแก้ |
|---|---|
| `Could not resolve placeholder 'DB_USER'` | ยังไม่ได้ตั้ง DB_USER / DB_PASSWORD ในเทอร์มินัลที่รัน |
| `Access denied for user` | รหัสผ่านใน DB_PASSWORD ไม่ตรงกับที่สร้างใน MySQL |
| `Communications link failure` | MySQL ยังไม่เปิด หรือไม่ได้อยู่ที่พอร์ต 3306 |
| `Unknown database 'workmatch'` | ยังไม่ได้รันขั้นตอนที่ 2 |
| `Port 8080 was already in use` | ปิดโปรแกรมอื่นที่ใช้พอร์ต 8080 หรือปิดแอปที่รันค้างไว้ |

## สถานะงาน

- [x] A1 โปรเจกต์ ต่อ MySQL และ entity/repository ครบ 6 ตาราง
- [ ] A2 สมัครสมาชิก / login (SecurityConfig ตอนนี้เปิดทุก URL ชั่วคราว)
- [ ] A3 เรซูเม่ ประกาศงาน และ /extract
- [ ] A4 จับคู่ด้วย /match
- [ ] A5 ข้อมูลตัวอย่างและเตรียมเดโม
