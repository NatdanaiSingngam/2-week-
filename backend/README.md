# Workmatch Backend (Spring Boot)

Backend ของระบบ Workmatch: Java 17 + Spring Boot 3.3 + Thymeleaf + Spring Security + Spring Data JPA + MySQL 8
รันที่พอร์ต **8080** และเรียก service NLP (Python/FastAPI) ที่พอร์ต **8000**

> **เริ่มเร็วที่สุด (หลังทำข้อ 1–3 ครั้งแรกแล้ว):** เปิด MySQL แล้วดับเบิลคลิก `start-demo.bat` ที่โฟลเดอร์บนสุดของ repo
> (macOS/Linux: `./start-demo.sh`) รอจนขึ้น `Started WorkmatchApplication` แล้วเปิด http://localhost:8080
> แล้ว login ด้วยบัญชีทดลองในหัวข้อ "ง) บัญชีทดลองสำหรับสาธิต"

## โครงสร้างโฟลเดอร์

```
2-week-/
├── start-demo.bat / start-demo.sh  # เปิด NLP + Spring Boot (พร้อมข้อมูลตัวอย่าง) ในคำสั่งเดียว
├── nlp-service/                    # Python FastAPI ของณัฐดนัย (/extract, /match, /health)
└── backend/
    ├── pom.xml, mvnw, mvnw.cmd, .mvn/   # Maven Wrapper (ไม่ต้องติดตั้ง Maven เอง)
    ├── db/create_database.sql           # สร้างฐานข้อมูลและผู้ใช้ MySQL
    └── src/
        ├── main/java/com/workmatch/
        │   ├── config/SecurityConfig.java      # form login + BCrypt + สิทธิ์ STUDENT/COMPANY
        │   ├── security/                       # โหลดผู้ใช้จากอีเมล, ส่งไปหน้าแรกตามบทบาท
        │   ├── controller/                     # Auth, Resume, Job, StudentDashboard, Home,
        │   │                                   # GlobalModelAdvice, GlobalExceptionHandler
        │   ├── service/                        # Registration, Resume, Job, Match, CurrentUser
        │   ├── nlp/                            # NlpClient เรียก /extract และ /match (timeout 2s/5s)
        │   ├── demo/DemoDataLoader.java        # ข้อมูลตัวอย่าง (เฉพาะ profile demo)
        │   ├── dto/  entity/  repository/  exception/
        ├── main/resources/
        │   ├── application.properties
        │   ├── demo/                           # ข้อความเรซูเม่ตัวอย่าง
        │   ├── templates/  static/             # หน้าเว็บของชญาณัตถพล + error.html, error/403, error/404
        └── test/                               # mvn test ใช้ H2 ในหน่วยความจำ ไม่ต้องเปิด MySQL
```

## 1. สิ่งที่ต้องติดตั้ง

- JDK 17 ขึ้นไป (ตรวจด้วย `java -version`)
- MySQL 8 (หรือ MariaDB 10.6+)
- Python 3.10 ขึ้นไป (สำหรับ service NLP)
- Git และ VS Code (Extension Pack for Java + Spring Boot Extension Pack) หรือ IntelliJ IDEA
- ไม่ต้องติดตั้ง Maven เพราะใช้ `mvnw` ที่มากับโปรเจกต์

## ก) ตัวแปรสภาพแวดล้อมที่ต้องตั้ง

| ตัวแปร | ใช้ทำอะไร | จำเป็น |
|---|---|---|
| `DB_USER` | ชื่อผู้ใช้ MySQL ที่สร้างในข้อ ข) | ต้องมี |
| `DB_PASSWORD` | รหัสผ่านของผู้ใช้นั้น | ต้องมี |
| `NLP_VENV` | ตำแหน่ง venv ของ Python ถ้าไม่อยากให้ `start-demo` สร้างไว้ใน `nlp-service/venv` | ไม่จำเป็น |

**ห้ามเขียนรหัสผ่านลงในไฟล์ใด ๆ ที่ขึ้น GitHub**

**Windows แบบถาวร** (ตั้งครั้งเดียว แล้วปิดเปิด VS Code/เทอร์มินัลใหม่)
```powershell
setx DB_USER "workmatch"
setx DB_PASSWORD "รหัสผ่านของคุณ"
```

**Windows PowerShell (เฉพาะหน้าต่างนี้)**
```powershell
$env:DB_USER="workmatch"; $env:DB_PASSWORD="รหัสผ่านของคุณ"
```

**macOS / Linux**
```bash
export DB_USER=workmatch DB_PASSWORD='รหัสผ่านของคุณ'
```

**IntelliJ IDEA**: Run > Edit Configurations... > WorkmatchApplication > Environment variables
`DB_USER=workmatch;DB_PASSWORD=รหัสผ่านของคุณ`

## ข) ลำดับการสร้างฐานข้อมูล (ทำครั้งเดียว)

1. เปิด MySQL (Windows ที่ติดตั้งแบบ ZIP: เปิด `mysqld` ค้างไว้ หรือใช้ service ของ MySQL Installer)
2. เข้า MySQL ด้วยบัญชี root (`mysql -u root -p` หรือ MySQL Workbench)
3. รันไฟล์ `db/create_database.sql` **แก้ `change_me` เป็นรหัสผ่านของคุณก่อน** หรือพิมพ์เอง:

```sql
CREATE DATABASE workmatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'workmatch'@'localhost' IDENTIFIED BY 'รหัสผ่านของคุณ';
GRANT ALL PRIVILEGES ON workmatch.* TO 'workmatch'@'localhost';
FLUSH PRIVILEGES;
```

4. ไม่ต้องสร้างตาราง Spring Boot สร้างให้ทั้ง 6 ตารางตอนรันครั้งแรก (`ddl-auto=update`)
   ตรวจได้ด้วย `USE workmatch; SHOW TABLES;` ต้องเห็น jobs, job_skills, matches, resume_skills, resumes, users

## ค) ลำดับการรัน

**ต้องเปิดตามลำดับนี้ทุกครั้ง:** MySQL → NLP service → Spring Boot

### แบบคำสั่งเดียว (แนะนำสำหรับสาธิต)

เปิด MySQL ก่อน แล้ว
```bat
start-demo.bat          :: Windows (ดับเบิลคลิกได้)
```
```bash
./start-demo.sh         # macOS / Linux
```
สคริปต์จะ 1) เตรียม venv และติดตั้ง package ของ NLP (ครั้งแรกใช้เวลาหลายนาที) 2) เปิด NLP แล้วรอจน
`http://localhost:8000/health` ตอบ `ok` 3) เปิด Spring Boot ด้วย profile `demo` ซึ่งสร้างข้อมูลตัวอย่างให้ครั้งแรก

### แบบทีละขั้น

```powershell
# หน้าต่างที่ 1: NLP
cd nlp-service
python -m venv venv
venv\Scripts\activate                 # macOS/Linux: source venv/bin/activate
pip install -r requirements.txt
uvicorn nlp_service.api_server:app --port 8000

# หน้าต่างที่ 2: Spring Boot
cd backend
$env:MAVEN_OPTS="-Duser.language=en -Duser.country=US"   # Windows ภาษาไทยเท่านั้น (ดูปัญหาที่พบบ่อย)
.\mvnw spring-boot:run                                    # ไม่มีข้อมูลตัวอย่าง
.\mvnw spring-boot:run -Dspring-boot.run.profiles=demo    # พร้อมข้อมูลตัวอย่าง
```

รอจนเห็น `Started WorkmatchApplication` แล้วเปิด http://localhost:8080

## ง) บัญชีทดลองสำหรับสาธิต

สร้างอัตโนมัติเมื่อรันด้วย profile `demo` (`start-demo` ใช้ profile นี้) **ครั้งแรกเท่านั้น** ถ้ามีอยู่แล้วจะไม่สร้างซ้ำ
ต้องเปิด NLP ก่อน เพราะทักษะและคะแนนคำนวณผ่าน NLP เหมือนผู้ใช้จริง

รหัสผ่านทุกบัญชี: **`demo1234`** (เป็นรหัสสำหรับสาธิตบนเครื่องตัวเองเท่านั้น ไม่ใช่รหัสผ่านจริงของใคร)

| บทบาท | อีเมล | ข้อมูล |
|---|---|---|
| นักศึกษา | `student1@demo.workmatch.test` | เรซูเม่สาย Backend (Java, Spring Boot, MySQL, Git) |
| นักศึกษา | `student2@demo.workmatch.test` | เรซูเม่สาย Frontend (HTML, CSS, JavaScript, React, Figma) |
| นักศึกษา | `student3@demo.workmatch.test` | เรซูเม่สาย Data (Python, SQL, Machine Learning, Excel) |
| บริษัท | `company1@demo.workmatch.test` | Backend / Frontend / Data Analyst |
| บริษัท | `company2@demo.workmatch.test` | Mobile App / DevOps-QA / UX-UI Designer (ส่งเอกสารแล้ว รออนุมัติ) |
| ผู้ดูแลระบบ | `admin@demo.workmatch.test` | หน้าผู้ดูแลระบบ `/admin` |

ข้อมูลตัวอย่างเพิ่มเติม (สร้างให้ทั้งฐานข้อมูลใหม่และฐานข้อมูลเดิมที่มีบัญชีตัวอย่างแล้ว): รหัสนิสิต/คณะ/สาขา, ข้อมูลบริษัท,
ใบสมัคร 6 ใบหลายสถานะ, งานที่ student1 บันทึกไว้ และคำขอยืนยันตัวตนของ company2 (ไฟล์ PDF ตัวอย่าง ไม่ใช่เอกสารจริง)

ลำดับสาธิตที่แนะนำ: login `student1` → เห็น Dashboard มีคะแนนทันที → ค้นหางาน → เปิดรายละเอียดงาน → บันทึก/สมัครงาน →
ดูประวัติการสมัคร → logout → login `company1` → เปิดตำแหน่ง Backend Developer → ดูผู้สมัคร → อัปเดตสถานะ →
logout → login `admin` → อนุมัติ company2 → แก้พจนานุกรมทักษะ/คำพ้อง

### ฟีเจอร์ตามเอกสารบทที่ 3 (User Interface รูปที่ 32-52)

| บทบาท | หน้า | URL |
|---|---|---|
| ทุกคน | หน้าแรก, เข้าสู่ระบบ, ลงทะเบียนนักศึกษา (รหัสนิสิต 8 หลัก) / สถานประกอบการ (เลขผู้เสียภาษี 13 หลัก) | `/`, `/login`, `/register` |
| นักศึกษา | กระดานสรุปผล, จัดการเรซูเม่ (อัปโหลด PDF / ลบ-เพิ่มทักษะ), ค้นหางาน, รายละเอียดงาน + บันทึก/สมัคร, งานที่บันทึก, ประวัติการสมัคร, ข้อมูลส่วนตัว | `/student/dashboard`, `/student/resume`, `/student/jobs`, `/student/jobs/{id}`, `/student/bookmarks`, `/student/applications`, `/student/profile` |
| สถานประกอบการ | ภาพรวม, จัดการประกาศ (แก้ไข/ปิดรับ/ลบ/แบบร่าง), ผู้สมัครแยกตามสถานะ + CSV, รายละเอียดผู้สมัคร + อัปเดตสถานะ, ผู้สมัครทั้งหมด, โปรไฟล์ + ยืนยันตัวตน (ภ.พ.20) | `/company/jobs`, `/company/postings`, `/company/jobs/{id}`, `/company/applications/{id}`, `/company/applications`, `/company/profile` |
| ผู้ดูแลระบบ | ภาพรวมระบบ, อนุมัติสถานประกอบการ, พจนานุกรมทักษะ, คำพ้องความหมาย, บันทึกข้อผิดพลาด, จัดการบัญชีผู้ใช้ (ระงับ/เปิดใช้งาน) | `/admin`, `/admin/companies`, `/admin/skills`, `/admin/synonyms`, `/admin/logs`, `/admin/users` |

- **พจนานุกรมทักษะ:** ตาราง `skill_dictionary` / `skill_synonyms` ในฐานข้อมูลเป็นต้นฉบับ เปิดระบบครั้งแรกนำเข้าจาก NLP (`GET /dictionary`)
  ผู้ดูแลแก้ไขแล้วระบบส่งชุดใหม่ให้ NLP (`PUT /dictionary`) ซึ่งเขียน `skill_dictionary.csv` และใช้ชุดใหม่ทันที
- **ไฟล์ PDF** (เรซูเม่ / ภ.พ.20) เก็บในตาราง `stored_files` ขนาดไม่เกิน 5MB เปิดดูได้เฉพาะเจ้าของ บริษัทที่ได้รับใบสมัคร และผู้ดูแลระบบ
- **บัญชีผู้ดูแลระบบนอก profile demo:** ตั้ง `WORKMATCH_ADMIN_EMAIL` และ `WORKMATCH_ADMIN_PASSWORD` (เฉพาะเซสชัน) แล้วเปิดระบบ จะสร้างให้ถ้ายังไม่มี ADMIN
- **ยังไม่ทำ:** ลืมรหัสผ่าน (ต้องมีระบบส่งอีเมล), อัปโหลดรูปโปรไฟล์, การลบบัญชีผู้ใช้โดยผู้ดูแล (ใช้ระงับบัญชีแทน)

ข้อความเรซูเม่ตัวอย่างอยู่ใน `src/main/resources/demo/` (คัดลอกจาก `nlp-service/tests/`) ประกาศงานอยู่ใน `DemoDataLoader.java`

## จ) ปัญหาที่พบบ่อยและวิธีแก้

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| `Could not resolve placeholder 'DB_USER'` | ยังไม่ได้ตั้ง DB_USER / DB_PASSWORD ในเทอร์มินัลที่รัน (ถ้าใช้ `setx` ต้องปิดเปิดเทอร์มินัลใหม่) |
| `Access denied for user` | DB_PASSWORD ไม่ตรงกับที่สร้างใน MySQL |
| `Communications link failure` | MySQL ยังไม่เปิด หรือไม่ได้อยู่ที่พอร์ต 3306 |
| `Unknown database 'workmatch'` | ยังไม่ได้ทำข้อ ข) |
| `Port 8080 was already in use` | มีแอปรันค้างอยู่ ปิดหน้าต่างเดิมก่อน |
| บันทึก resume / ประกาศงานแล้วขึ้น "ระบบวิเคราะห์ทักษะยังไม่พร้อม" | NLP ไม่ได้รันที่พอร์ต 8000 เปิด http://localhost:8000/health ดู |
| Dashboard ไม่มีคะแนน / บัญชีทดลองไม่มีข้อมูล | ตอนเปิด profile demo NLP ยังไม่พร้อม ข้อมูลตัวอย่างจึงไม่ถูกสร้าง เปิด NLP แล้วรันใหม่ (ดูบรรทัด `[demo]` ใน log) |
| ทักษะที่ดึงมาไม่ครบ | คำนั้นไม่อยู่ใน `nlp-service/nlp_service/skill_dictionary.csv` แจ้งณัฐดนัย |
| กดส่งฟอร์มแล้วได้หน้า "คุณไม่มีสิทธิ์เข้าถึงหน้านี้" (403) | เปิดหน้าของบทบาทอื่น หรือฟอร์มไม่ได้ใช้ `th:action` จึงไม่มี CSRF token |
| Windows ภาษาไทย: Maven ล้มด้วย `Negative time` | ตั้ง `MAVEN_OPTS=-Duser.language=en -Duser.country=US` ก่อนรัน (`start-demo.bat` ตั้งให้แล้ว) |
| ไดรฟ์ C: เต็ม | ตั้ง `NLP_VENV` ไปไว้ไดรฟ์อื่น และย้าย Maven repo ด้วย `MAVEN_OPTS=-Dmaven.repo.local=D:\m2repo` |

## รันชุดทดสอบ

```powershell
cd backend
.\mvnw test
```
ใช้ฐานข้อมูล H2 ในหน่วยความจำ และ NLP จำลอง จึงไม่ต้องเปิด MySQL หรือ NLP
มีเทสต์เส้นทางหลัก `MainFlowIntegrationTest` (สมัคร → login → วาง resume → บริษัทสร้างตำแหน่ง → นักศึกษาเห็นคะแนน)
และ `DocumentFeaturesTest` (ฟีเจอร์ตามเอกสารบทที่ 3: ลงทะเบียนแบบเต็ม, ค้นหา/บันทึก/สมัครงาน, ผู้สมัคร + CSV, อัปโหลด PDF, ยืนยันตัวตน, หน้าผู้ดูแลระบบ)

ชุดทดสอบของ NLP (รวม `GET/PUT /dictionary`): `cd nlp-service` แล้ว `venv\Scripts\python -m pytest tests`

## การจับคู่ (A4)

- บันทึก resume -> คำนวณกับทุกประกาศงาน / สร้างประกาศงาน -> คำนวณกับทุก resume
- คะแนนมาจาก `/match` ของ NLP เท่านั้น (ไม่คำนวณใน Java) เก็บใน `matches` คู่ละ 1 แถว
- ประกาศที่ไม่มีทักษะจะถูกข้าม ถ้าคำนวณไม่สำเร็จ การบันทึก resume / ประกาศงานยังสำเร็จ และหน้า resume จะขึ้นข้อความเตือน
- หน้า company/job-detail แสดงเฉพาะผู้ที่ได้คะแนนมากกว่า 0 (ตรงกับ applicantCount)

## หน้า error (A5)

- 403 / 404 ใช้ `templates/error/403.html`, `error/404.html` ส่วนข้อผิดพลาดอื่นใช้ `templates/error.html` (ภาษาไทยทั้งหมด)
- `GlobalExceptionHandler` จับ exception ที่หลุดจาก controller (NLP ล่ม, ฐานข้อมูลล่ม, อื่น ๆ) แสดงหน้าภาษาไทย และเก็บรายละเอียดไว้ใน log
- ไม่ส่ง stack trace ให้ผู้ใช้ (`server.error.include-stacktrace=never`)

## TODO การเชื่อม frontend กับ backend

ทดสอบจริงแล้ว 22 ต.ค. ผ่านหน้าเว็บจริง (สมัคร, login, resume, dashboard, ประกาศงาน, ผู้สมัคร) ใช้งานได้ครบ

| # | จุด | ใครแก้ | สถานะ |
|---|---|---|---|
| 1 | หน้า 403/404/500 เป็น Whitelabel ภาษาอังกฤษ / JSON error มี stack trace | เรา | **แก้แล้ว** (stack trace 22 ต.ค., หน้า error ภาษาไทย 24 ต.ค. งาน A5) |
| 2 | หัวข้อหมวดทักษะในหน้า resume แสดงคีย์อังกฤษ (programming, database, soft ...) | เพื่อน | **แก้แล้ว** 27 ต.ค. (แสดงชื่อไทย) — `templates/student/resume.html` |
| 3 | ส่วน "ไม่พบตำแหน่งงาน" (job == null) ใน job-detail ไม่เคยถูกใช้ | ตกลงร่วมกัน | **แก้แล้ว** 27 ต.ค. (แบบที่ 2: ตอบ 404 แต่แสดงส่วน "ไม่พบตำแหน่งงาน" + ปุ่มกลับรายการตำแหน่ง) — `JobController.java` |
| 4 | templates/static มี 2 ชุด (`frontend-draft/` กับ `backend/`) เสี่ยงแก้ไม่ตรงกัน | เพื่อน | **แก้แล้ว** 27 ต.ค. (เหลือชุดเดียวใน `backend/`) และ 5 พ.ย. ลบหน้า preview ข้อมูลสมมติใน `frontend-draft/` ออก เหลือเฉพาะหน้าเว็บจริง |
| 5 | `job.skills` เป็น `{name, category}` แต่เอกสารสั่งงานเขียนว่า list ข้อความ | แก้เอกสาร | ยังไม่แก้ — `dto/JobDetail.java` |
| 6 | `static/images/workmatch.svg` มี metadata เกินมาตอนคัดลอก | เรา | **แก้แล้ว** 22 ต.ค. |

## หมายเหตุหน้าเว็บ

ไฟล์ใน `src/main/resources/templates` และ `static` เป็นงานของชญาณัตถพล (ย้ายมาจาก `frontend-draft/` เดิม และเป็นชุดเดียวที่ใช้จริง)
ยกเว้น `error.html`, `error/403.html`, `error/404.html` ที่ธนวัฒน์เพิ่มในงาน A5 (ใช้ layout และ CSS ของชญาณัตถพล)

5 พ.ย.: ออกแบบหน้าเว็บใหม่ทั้งหมดเป็นธีม "Professional Blue" (ระบบดีไซน์จาก skill ui-ux-pro-max) รายละเอียดสี ฟอนต์ และกฎอยู่ที่หัวไฟล์ `static/css/style.css`
หน้า preview ข้อมูลสมมติใน `frontend-draft/` ถูกลบออกแล้ว ดูหน้าเว็บได้ที่ http://localhost:8080 เท่านั้น (ต้องรัน backend)

### URL, template และข้อมูลที่ส่งให้หน้าเว็บ

(ย้ายมาจาก `frontend-draft/README.md` เดิม)

| Method / URL | Template | Model / Fields |
| --- | --- | --- |
| GET /login, POST /login | login | param.error/logout/registered; username, password |
| GET/POST /register | register | error; displayName, email, password, role |
| POST /logout | Spring Security | CSRF token |
| GET/POST /student/resume | student/resume | resumeText, skills[{name,category}], message, error; text |
| GET /student/dashboard | student/dashboard | hasResume, matches[{jobId,jobTitle,companyName,score,cosine,matched,missing}] |
| GET /company/jobs | company/jobs | jobs[{id,title,skillCount,applicantCount}] |
| GET /company/jobs/new | company/job-form | job{title,description}, error |
| POST /company/jobs | redirect /company/jobs/{id} | title, description |
| GET /company/jobs/{id} | company/job-detail | job{id,title,description,skills}, applicants[{studentName,score,cosine,matched,missing}] |

- ทุกหน้าหลังเข้าสู่ระบบรับ `displayName`, `role` (STUDENT/COMPANY) เพิ่มเติม หน้าเว็บไม่เรียก FastAPI โดยตรง
- ทุกหน้าใช้ `fragments/layout :: head(title)`, `navbar`, `footer` (หน้า login/สมัครสมาชิกใช้ `authPanel` เพิ่ม) CSS มีไฟล์เดียวคือ `static/css/style.css` ฟอร์มใช้ `th:action` เพื่อให้ Spring Security แนบ CSRF token
- `matched`/`missing` เป็นรายการข้อความ คะแนน `score` และ `cosine` เป็นเลข 0–100 แสดงหนึ่งตำแหน่งทศนิยม ระดับ: เหมาะมาก ≥70, ปานกลาง ≥40, ต่ำกว่านั้นควรพัฒนาเพิ่ม
- Dashboard และรายชื่อผู้สมัครคงลำดับตามที่ backend ส่งมา (เรียงคะแนนมากไปน้อย)
- สมัครสมาชิกเมื่อ validation ไม่ผ่าน ให้ return `register` ใน POST request เดิม เพื่อให้ param คงค่าชื่อ/อีเมล/บทบาท (รหัสผ่านไม่เติมกลับ)
- Login สำเร็จ: STUDENT → /student/dashboard, COMPANY → /company/jobs
- ต้องอนุญาต `/css/**`, `/js/**`, `/images/**`, `/login`, `/register` ก่อน login

### รายการตรวจหน้าเว็บ

- Login: error/logout/registered; register role 2 แบบและ password อย่างน้อย 8 ตัว
- Resume: ว่าง/มีทักษะ/ข้อความ error, ตัวนับ 20,000 ตัว, ป้องกันกดซ้ำและคืนสถานะเมื่อ Back
- Dashboard: ไม่มี resume / ไม่มี matches / หลาย matches, ระดับ 70/40, ขยายการ์ด, ไม่มี missing
- Company: jobs ว่าง, error ฟอร์ม, job ว่าง, applicants ว่าง, อันดับ 1–3, ไม่แสดงอีเมล
- ตรวจที่ 375px, 768px และ desktop, keyboard focus, ภาษาไทย และข้อความผู้ใช้ที่มี HTML (ต้องแสดงเป็นข้อความ)

## สถานะงาน

- [x] A1 โปรเจกต์ ต่อ MySQL และ entity/repository ครบ 6 ตาราง
- [x] A2 สมัครสมาชิก / login / logout แยกสิทธิ์ STUDENT/COMPANY รหัสผ่าน BCrypt
- [x] A3 เรซูเม่ ประกาศงาน และ /extract
- [x] A4 จับคู่ด้วย /match เก็บลง matches แล้วส่งให้ Dashboard / หน้าผู้สมัคร
- [x] A5 ข้อมูลตัวอย่าง (profile demo), หน้า error ภาษาไทย, start-demo, เทสต์เส้นทางหลัก
