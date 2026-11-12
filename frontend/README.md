# frontend: หน้าเว็บ Workmatch

หน้าเว็บทั้งหมดของระบบ เขียนด้วย Thymeleaf (HTML ที่มีคำสั่ง `th:*`) โฟลเดอร์นี้ไม่ได้รันแยกเอง

## ทำงานกับ backend อย่างไร

- ตอน build `backend/pom.xml` คัดลอก `templates/` และ `static/` เข้า classpath ของ Spring Boot
- ผู้ใช้เปิด URL แล้ว controller ใน `backend/src/main/java/com/workmatch/controller/` ใส่ข้อมูลลง Model และบอกชื่อ template (เช่น `"student/resume"` คือ `templates/student/resume.html`)
- Thymeleaf นำข้อมูลมาเติมใน template แล้วส่ง HTML ให้เบราว์เซอร์
- ฟอร์มส่งกลับไปที่ URL ของ backend (`th:action`) ซึ่ง Spring Security แนบ CSRF token ให้เอง
- หน้าเว็บไม่เรียก NLP service ตรงๆ ทุกอย่างผ่าน backend

## โครงสร้าง

| โฟลเดอร์ / ไฟล์ | ใช้ทำอะไร |
|---|---|
| `templates/fragments/layout.html` | ชิ้นส่วนที่ทุกหน้าใช้ร่วมกัน: `head(title)`, แถบเมนู, แถบด้านซ้ายของ admin, footer, scripts |
| `templates/student/` | หน้านักศึกษา: dashboard, resume, jobs, job, bookmarks, applications, profile |
| `templates/company/` | หน้าสถานประกอบการ: jobs (ภาพรวม), postings, job-form, job-detail, applications, candidate, profile |
| `templates/admin/` | หน้าผู้ดูแลระบบ: overview, companies, skills, synonyms, logs, users |
| `templates/error/`, `error.html` | หน้าข้อผิดพลาดภาษาไทย (403, 404, อื่นๆ) |
| `templates/landing.html`, `login.html`, `register.html` | หน้าก่อนล็อกอิน |
| `static/css/style.css` | สไตล์ทั้งหมด (ไฟล์เดียว) มีคำอธิบายสีและฟอนต์ที่หัวไฟล์ |
| `static/js/app.js` | ตัวช่วยฟอร์ม (กันกดซ้ำ, ตัวนับตัวอักษร) ปิด JavaScript แล้วฟอร์มยังใช้ได้ |
| `static/images/` | โลโก้ |

## หัวไฟล์ของ template

ทุก template มีคอมเมนต์ `<!--/* ... */-->` อยู่บนสุด Thymeleaf ตัดคอมเมนต์นี้ออกก่อนส่งให้เบราว์เซอร์ ในคอมเมนต์บอก:
- **หน้าที่:** หน้านี้ใช้ทำอะไร
- **แสดงโดย:** controller และ URL ที่แสดงหน้านี้
- **ข้อมูลที่ได้รับจาก Controller:** ชื่อตัวแปรที่ใช้ในหน้า
- **ส่งฟอร์มไปที่:** URL ที่ฟอร์มส่งข้อมูลไป

## แก้หน้าเว็บระหว่างที่ server รันอยู่

Spring Boot อ่าน template จากไฟล์ที่ build ไว้ ไม่ได้อ่านจากโฟลเดอร์นี้ตรงๆ แก้เสร็จแล้วให้รันคำสั่งนี้ในโฟลเดอร์ `backend` แล้วรีเฟรชหน้า (หรือรีสตาร์ต server)

```bash
mvnw process-resources
```
