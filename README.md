# Workmatch

ระบบจับคู่นักศึกษาสหกิจศึกษากับตำแหน่งงาน โดยดูจากทักษะในเรซูเม่

## โครงสร้างโปรเจกต์

| โฟลเดอร์ | ส่วน | ภาษา / เทคโนโลยี | รันที่ |
|---|---|---|---|
| [`frontend/`](frontend/) | หน้าเว็บทั้งหมด (HTML, CSS, JS, รูป) | Thymeleaf | ไม่ได้รันแยก Spring Boot เป็นตัวสร้างหน้าเว็บ |
| [`backend/`](backend/) | ตรรกะหลัก, ฐานข้อมูล, ล็อกอิน | Java 17, Spring Boot | http://localhost:8080 |
| [`nlp-service/`](nlp-service/) | สกัดทักษะจากข้อความ, คิดคะแนน | Python, FastAPI, PyThaiNLP | http://localhost:8000 |
| [`sample-data/`](sample-data/) | เรซูเม่จำลอง, งานจำลอง, เฉลย สำหรับวัดผล NLP | ข้อความ / CSV | — |
| [`docs/presentation/`](docs/presentation/) | แฟ้มนำเสนอ | — | — |
| `start-demo.bat` / `.sh` | เปิด NLP + backend พร้อมข้อมูลสาธิตในคำสั่งเดียว | — | — |

```
app/
├── frontend/
│   ├── templates/            หน้าเว็บ แยกตามผู้ใช้
│   │   ├── fragments/        ชิ้นส่วนร่วม (head, แถบเมนู, footer)
│   │   ├── student/          หน้านักศึกษา
│   │   ├── company/          หน้าสถานประกอบการ
│   │   ├── admin/            หน้าผู้ดูแลระบบ
│   │   ├── error/            หน้า 403 / 404
│   │   └── landing / login / register / error .html
│   └── static/               css/style.css, js/app.js, images/
├── backend/
│   ├── pom.xml               ตั้งค่า build (ดึง frontend/ เข้า classpath ตรงนี้)
│   ├── db/                   สคริปต์สร้างฐานข้อมูล
│   └── src/main/java/com/workmatch/
│       ├── controller/       รับคำขอจากเบราว์เซอร์ -> เรียก service -> ส่งข้อมูลให้หน้าเว็บ
│       ├── service/          ตรรกะหลัก (บันทึกเรซูเม่ ประกาศงาน จับคู่ สมัครงาน ฯลฯ)
│       ├── nlp/              ตัวเรียก NLP service (NlpClient)
│       ├── repository/       อ่าน/เขียนฐานข้อมูล
│       ├── entity/           โครงสร้างตารางฐานข้อมูล
│       ├── dto/              ข้อมูลที่ส่งให้หน้าเว็บ
│       ├── config/ security/ ตั้งค่าระบบ / ล็อกอินและสิทธิ์
│       ├── exception/        ข้อผิดพลาดเฉพาะของระบบ
│       └── demo/             ข้อมูลสาธิต (profile demo)
└── nlp-service/
    ├── nlp_service/          api_server.py, skill_extractor.py, match_scorer.py, skill_dictionary.csv
    ├── evaluation/           สคริปต์วัดผลและผลลัพธ์
    ├── tests/                pytest
    └── api-contract.md       ข้อตกลงรูปแบบข้อมูลระหว่าง backend กับ NLP
```

## ข้อมูลไหลอย่างไร

```
เบราว์เซอร์ ──(คลิก / ส่งฟอร์ม)──> backend: controller ──> service ──> repository ──> MySQL
     ▲                                  │                    │
     │                                  │                    └──(NlpClient: HTTP JSON)──> nlp-service
     └──── หน้า HTML จาก frontend/templates ◄── controller ใส่ข้อมูล (Model) ◄──────────── ผลทักษะ / คะแนน
```

ตัวอย่าง: นักศึกษาบันทึกเรซูเม่
1. ฟอร์มใน `frontend/templates/student/resume.html` ส่ง `POST /student/resume`
2. `controller/ResumeController` รับคำขอ แล้วเรียก `service/ResumeService`
3. `ResumeService` ส่งข้อความผ่าน `nlp/NlpClient` ไปที่ NLP `POST /extract`
4. `nlp-service/nlp_service/api_server.py` เรียก `skill_extractor.py` แล้วตอบทักษะกลับมา
5. `ResumeService` บันทึกเรซูเม่และทักษะลงตาราง `resumes` / `resume_skills` ผ่าน repository
6. `service/MatchService` ส่งทักษะเรซูเม่กับทักษะของทุกประกาศงานไปที่ NLP `POST /match` แล้วเก็บคะแนนลงตาราง `matches`
7. หน้า `student/dashboard.html` อ่านคะแนนจากตาราง `matches` มาแสดง

## หาไฟล์ที่ต้องการ

- **ทุกไฟล์มีคอมเมนต์หัวไฟล์** ที่ขึ้นต้นด้วย `==== Workmatch` บอกว่าไฟล์นั้นทำอะไร รับข้อมูลจากไหน ส่งไปที่ไหน และไฟล์ไหนเรียกใช้
- **เริ่มจาก URL:** ค้นหา URL (เช่น `/student/resume`) ในโฟลเดอร์ `backend/` จะเจอ controller ที่รับ และหัวไฟล์ controller บอกว่าแสดงผลที่ template ไหน
- **เริ่มจากหน้าเว็บ:** เปิด template แล้วดูหัวไฟล์ จะเห็นว่า controller ไหนแสดงหน้านี้ ได้รับตัวแปรอะไร และฟอร์มส่งไปที่ URL ไหน
- **เริ่มจากตาราง:** ค้นหาชื่อตาราง (เช่น `matches`) ใน `entity/` แล้วดูหัวไฟล์ของ repository ที่คู่กัน ว่า service ไหนใช้

## วิธีรัน

ดูรายละเอียดใน [`backend/README.md`](backend/README.md) โดยย่อ:
1. เปิด MySQL และตั้งตัวแปร `DB_USER` / `DB_PASSWORD`
2. ดับเบิลคลิก `start-demo.bat` (เปิด NLP ที่ :8000 แล้วเปิด backend ที่ :8080)
3. เปิด http://localhost:8080 บัญชีทดลองดูได้ใน [`docs/presentation/README.md`](docs/presentation/README.md)
