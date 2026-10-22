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
    │   ├── config/SecurityConfig.java      # form login + BCrypt + สิทธิ์ STUDENT/COMPANY
    │   ├── security/                       # โหลดผู้ใช้จากอีเมล, ส่งไปหน้าแรกตามบทบาท
    │   ├── controller/                     # Auth, Resume, Job, StudentDashboard, Home, GlobalModelAdvice
    │   ├── service/                        # Registration, Resume, Job, Match, CurrentUser
    │   ├── nlp/                            # NlpClient เรียก /extract และ /match (timeout 2s/5s)
    │   ├── dto/                            # ข้อมูลที่ส่งให้หน้าเว็บ (ชื่อ field ตามข้อตกลง)
    │   ├── exception/                      # JobNotFoundException -> 404
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
จะถูกส่งไปหน้า `/login` สมัครสมาชิกได้ที่ `/register`

**ต้องรัน service NLP ก่อน** ไม่อย่างนั้นการบันทึก resume / ประกาศงานจะขึ้น
"ระบบวิเคราะห์ทักษะยังไม่พร้อม ลองใหม่อีกครั้ง" (ดูวิธีรันใน `../nlp-service/README.md`)

```powershell
cd nlp-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
uvicorn nlp_service.api_server:app --port 8000
```
เปิด http://localhost:8000/health ต้องได้ `{"status":"ok"}`

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
| บันทึก resume แล้วขึ้น "ระบบวิเคราะห์ทักษะยังไม่พร้อม" | service NLP ไม่ได้รันที่พอร์ต 8000 |
| กดส่งฟอร์มแล้วได้ 403 | ฟอร์มไม่ได้ใช้ `th:action` จึงไม่มี CSRF token |

## TODO การเชื่อม frontend กับ backend (ยังไม่ได้แก้)

ทดสอบจริงแล้ว 22 ต.ค. ผ่านหน้าเว็บจริง (สมัคร, login, resume, dashboard, ประกาศงาน, ผู้สมัคร) ใช้งานได้ครบ เหลือจุดเหล่านี้

| # | จุด | ใครแก้ | ไฟล์ |
|---|---|---|---|
| 1 | หน้า 403/404/500 เป็น Whitelabel ภาษาอังกฤษ (เรื่อง stack trace แก้แล้ว 22 ต.ค.) | เรา (A5) | `SecurityConfig.java`, `JobNotFoundException.java`, `application.properties` |
| 2 | หัวข้อหมวดทักษะในหน้า resume แสดงคีย์อังกฤษ (programming, database, soft ...) | เพื่อน | `templates/student/resume.html` |
| 3 | ส่วน "ไม่พบตำแหน่งงาน" (job == null) ใน job-detail ไม่เคยถูกใช้ | ตกลงร่วมกัน | `templates/company/job-detail.html` |
| 4 | templates/static มี 2 ชุด (`frontend-draft/` กับ `backend/`) เสี่ยงแก้ไม่ตรงกัน | เพื่อน | `frontend-draft/README.md` |
| 5 | `job.skills` เป็น `{name, category}` แต่เอกสารสั่งงานเขียนว่า list ข้อความ | แก้เอกสาร | `dto/JobDetail.java` |
| 6 | ~~`static/images/workmatch.svg` มี metadata เกินมาตอนคัดลอก~~ แก้แล้ว 22 ต.ค. (คัดลอกจาก `frontend-draft` ทับ) | เรา | `static/images/workmatch.svg` |

## หมายเหตุหน้าเว็บ

ไฟล์ใน `src/main/resources/templates` และ `static` คัดลอกมาจาก `frontend-draft/` ของชญาณัตถพลโดยไม่แก้ไข
(ตามขั้นตอนใน `frontend-draft/README.md`) ถ้าจะแก้หน้าเว็บให้แก้ที่นี่ แล้วลบ `frontend-draft` ทีหลัง

`job.skills` ในหน้า `company/job-detail` ส่งเป็น list ของ `{name, category}` ตามที่หน้าเว็บใช้ `skill.name`
(เอกสารสั่งงานเขียนว่า list ข้อความ)

## สถานะงาน

- [x] A1 โปรเจกต์ ต่อ MySQL และ entity/repository ครบ 6 ตาราง
- [x] A2 สมัครสมาชิก / login / logout แยกสิทธิ์ STUDENT/COMPANY รหัสผ่าน BCrypt
- [x] A3 เรซูเม่ ประกาศงาน และ /extract
- [x] A4 จับคู่ด้วย /match เก็บลง matches แล้วส่งให้ Dashboard / หน้าผู้สมัคร
- [ ] A5 ข้อมูลตัวอย่างและเตรียมเดโม

## การจับคู่ (A4)

- บันทึก resume -> คำนวณกับทุกประกาศงาน / สร้างประกาศงาน -> คำนวณกับทุก resume
- คะแนนมาจาก `/match` ของ NLP เท่านั้น (ไม่คำนวณใน Java) เก็บใน `matches` คู่ละ 1 แถว
- ประกาศที่ไม่มีทักษะจะถูกข้าม ถ้าคำนวณไม่สำเร็จ การบันทึก resume / ประกาศงานยังสำเร็จ และหน้า resume จะขึ้นข้อความเตือน
- หน้า company/job-detail แสดงเฉพาะผู้ที่ได้คะแนนมากกว่า 0 (ตรงกับ applicantCount)
