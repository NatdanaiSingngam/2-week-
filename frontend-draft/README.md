# Workmatch Frontend — C1–C4

หน้าเว็บภาษาไทยด้วย HTML5, CSS และ JavaScript ธรรมดา พร้อม Thymeleaf templates สำหรับ Spring Boot 3 ไม่มี npm หรือ framework หน้าเว็บ

## เปิดดูใน VS Code / เบราว์เซอร์

1. เปิดโฟลเดอร์ `D:\2-week-` ใน VS Code
2. เปิด `frontend-draft/login.html` ด้วยเบราว์เซอร์ได้ทันที หรือใช้ Live Server ของ VS Code
3. เปิด `dashboard.html` ดูฝั่งนักศึกษา และ `jobs.html` ดูฝั่งบริษัท
4. เมนูสถานะด้านบนใช้ลองข้อมูลว่าง/ไม่มี resume/ข้อผิดพลาด ข้อมูลทั้งหมดสมมติ ฟอร์มไม่ส่งข้อมูลไปภายนอก ไม่ได้ยืนยันตัวตนหรือวิเคราะห์ทักษะจริง

ตัวอย่างหน้าครบ 7 หน้า: login, register, resume, dashboard, jobs, job-form, job-detail

## ส่งต่อให้ฝั่ง backend

ยังไม่มี backend ใน repository จึงวางทุกไฟล์ไว้ใน frontend-draft ตามแผน ห้ามนำหน้าสาธิตที่ root ไปแทน templates

เมื่อโครง Spring Boot พร้อม ให้คัดลอก **เนื้อหาภายใน** `frontend-draft/templates/` ไป `backend/src/main/resources/templates/` และ **เนื้อหาภายใน** `frontend-draft/static/` ไป `backend/src/main/resources/static/` อย่าคัดลอก `fixtures.js` หรือไฟล์ HTML ตัวอย่างไป production

ทุกหน้าใช้ `fragments/layout :: head(title)`, `navbar`, `footer` ร่วมกัน CSS มีไฟล์เดียวคือ `static/css/style.css` ฟอร์มจริงใช้ `th:action` เพื่อให้ Spring Security แนบ CSRF token

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

ทุกหน้าหลังเข้าสู่ระบบรับ `displayName`, `role` (STUDENT/COMPANY) เพิ่มเติม Backend ต้องจัดการ auth, authorization, validation, password hash, CSRF และเรียก NLP เอง หน้าเว็บไม่เรียก FastAPI โดยตรง

รายละเอียดเพิ่มเติม:
- `job.skills` ใช้รายการ object `{name,category}` เช่นเดียวกับผล `/extract` (PDF ไม่ระบุชนิดไว้ชัดเจน ต้องยืนยันตอนต่อ controller)
- `matched`/`missing` เป็นรายการข้อความ คะแนน `score` และ `cosine` เป็นเลข 0–100 แสดงหนึ่งตำแหน่งทศนิยม
- PDF กำหนดให้แสดง cosine เป็นค่าประกอบ แม้ api-contract เดิมบอกว่ายังไม่ต้องแสดง จึงยึด PDF สำหรับ UI
- Dashboard และผู้สมัครคงลำดับจาก backend Backend ต้องเรียงคะแนนมากไปน้อย
- สมัครสมาชิกเมื่อ validation ไม่ผ่าน ให้ return `register` ใน POST request เดิมเพื่อให้ param คงค่าชื่อ/อีเมล/บทบาท รหัสผ่านไม่เติมกลับ ถ้าใช้ redirect ให้เพิ่ม flash attributes และปรับการเติมค่าร่วมกัน
- Login สำเร็จ: STUDENT → /student/dashboard, COMPANY → /company/jobs กำหนดใน Spring Security success handler
- ต้องอนุญาต `/css/**`, `/js/**`, `/images/**`, `/login`, `/register` ก่อน login
- ทุก form POST ต้องทดสอบ CSRF ผ่าน Spring Security จริงหลังต่อ backend; เปิด HTML ตรง ๆ ไม่ได้ทดสอบ Thymeleaf

## รายการตรวจ

- Login: error/logout/registered; register role 2 แบบและ password อย่างน้อย 8 ตัว
- Resume: ว่าง/มีทักษะ/ข้อความ error, ตัวนับ 20,000 ตัว, ป้องกันกดซ้ำและคืนสถานะเมื่อ Back
- Dashboard: ไม่มี resume / ไม่มี matches / หลาย matches, สีเกณฑ์ 70/40, ขยายการ์ด, ไม่มี missing
- Company: jobs ว่าง, error ฟอร์ม, job ว่าง, applicants ว่าง, อันดับ 1–3, ไม่แสดงอีเมล
- ตรวจ 390px และ desktop, keyboard focus, ภาษาไทย และข้อความผู้ใช้ที่มี HTML (ต้องเป็นข้อความ)

C5 ข้อมูลประเมิน NLP 10 resume/10 jobs และ C6 รายงาน/สไลด์แยกจากงานหน้าเว็บนี้ ยังไม่ได้จัดทำ ไม่มีผลทดสอบระบบจริงหรือคะแนนความแม่นยำที่อ้างว่าทดสอบแล้ว
