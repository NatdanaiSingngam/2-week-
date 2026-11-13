# Workmatch: แฟ้มนำเสนอ

ทุกไฟล์ที่ใช้ตอบคำถามอยู่ใน repo นี้แล้ว (ไม่มีสำเนาโค้ดแยก เปิดจากตำแหน่งจริงตามตารางด้านล่าง)

## โครงสร้างโฟลเดอร์

```
2-week-/
├── docs/presentation/         ← แฟ้มนำเสนอ (ไฟล์นี้)
│   ├── slide-links.txt        ลิงก์สไลด์เดโม 9 แผ่น + สไลด์เช็คลิสต์ 13 แผ่น
│   └── screenshots/           ภาพหน้าจอสำรอง ถ้าเปิดระบบไม่ขึ้น
├── nlp-service/
│   ├── nlp_service/           โค้ด NLP: skill_extractor.py, match_scorer.py, api_server.py, skill_dictionary.csv
│   ├── evaluation/            วัดความแม่นยำ: run_eval.py, ผลก่อน/หลังปรับพจนานุกรม, ตัวอย่างการตัดคำ
│   ├── tests/                 pytest
│   └── api-contract.md        ข้อตกลง API ระหว่าง Backend กับ NLP
├── sample-data/               เรซูเม่จำลอง 10 ฉบับ งาน 10 ตำแหน่ง และเฉลย (expected.csv)
├── backend/                   Spring Boot (README.md, db/create_database.sql, src/)
└── start-demo.bat / .sh       เปิดระบบก่อนนำเสนอ
```

## ใช้ตอนนำเสนอ
- **สไลด์:** `docs/presentation/slide-links.txt` (ลิงก์ส่วนตัว ต้องกดแชร์ก่อนให้คนอื่นเปิด)
- **เปิดระบบ:** `start-demo.bat` ที่โฟลเดอร์บนสุด (backend :8080, NLP :8000) ต้องตั้ง DB_USER / DB_PASSWORD และเปิด MySQL ก่อน
- **บัญชีทดลอง** (รหัสผ่าน `demo1234`): `student1@demo.workmatch.test`, `company1@demo.workmatch.test`, `admin@demo.workmatch.test`
- **ข้อความเรซูเม่สำหรับวางตอนเดโม:** `sample-data/resumes/`
- **ภาพสำรอง:** `docs/presentation/screenshots/` (ถ่ายจากหน้าตาเวอร์ชันแรก ธีมเขียว ก่อนปรับตามเอกสารบทที่ 3)

## ถ้าอาจารย์ถามว่า... ให้เปิดไฟล์นี้
| คำถาม | เปิดไฟล์ |
|---|---|
| ทำครบตามแผน 2 สัปดาห์ไหม | สไลด์เช็คลิสต์ (ลิงก์ใน `slide-links.txt`) + เอกสารแผนงาน (PDF เก็บนอก repo ดูหัวข้อ "ไม่ได้รวมไว้") |
| สกัดทักษะอย่างไร ตัดคำอย่างไร | `nlp-service/nlp_service/skill_extractor.py`, `nlp-service/evaluation/tokenize_samples/` |
| พจนานุกรมทักษะมีอะไรบ้าง | `nlp-service/nlp_service/skill_dictionary.csv` (74 รายการ) หรือหน้าผู้ดูแลระบบ `/admin/skills` |
| คะแนนคำนวณอย่างไร | `nlp-service/nlp_service/match_scorer.py` (TF-IDF + Cosine Similarity ตามเอกสาร 2.3.3) |
| วัดความแม่นยำอย่างไร ตัวเลขมาจากไหน | `nlp-service/evaluation/run_eval.py`, `results_after_dictionary_update.md`, `sample-data/expected.csv` |
| ก่อนปรับพจนานุกรมผลเป็นอย่างไร | `nlp-service/evaluation/results_baseline.md` |
| เรซูเม่ภาษาอังกฤษ / ไม่มีทักษะ / ว่าง | `results_after_dictionary_update.md` หัวข้อ "กรณีพิเศษ" และ `nlp-service/tests/test_extract_and_match.py` |
| Backend เรียก NLP อย่างไร | `nlp-service/api-contract.md`, `backend/src/main/java/com/workmatch/nlp/NlpClient.java` |
| ถ้า NLP ล่มระหว่างบันทึก | `backend/src/main/java/com/workmatch/service/ResumeService.java` (เรียก NLP ก่อนแตะฐานข้อมูล) + `WebFlowTests` |
| แก้ไข/ลบประกาศงาน | `backend/src/main/java/com/workmatch/controller/JobController.java`, `service/JobPostingService.java` |
| โครงสร้างฐานข้อมูล | `backend/db/create_database.sql`, `backend/README.md`, `backend/src/main/java/com/workmatch/entity/` |
| ทดสอบอะไรไปบ้าง | ด้านล่าง "ผลทดสอบ" |

## ผลทดสอบ (ณ 6 พ.ย. 2569)
| ชุดเทส | คำสั่ง | ผล |
|---|---|---|
| NLP | `cd nlp-service` แล้ว `venv\Scripts\python -m pytest` | 15 passed (12 ของทีม NLP + 3 ของ `/dictionary`) |
| วัดความแม่นยำ | `venv\Scripts\python evaluation\run_eval.py` | Precision 98.1%, Recall 99.3%, อันดับ 1 ตรง 10/10, กรณีพิเศษผ่านทั้งหมด |
| Backend | `cd backend` แล้ว `.\mvnw test` | 45 tests, BUILD SUCCESS |

## ตัวเลขหลัก
Precision 98.1% · Recall 99.3% · อันดับ 1 ตรงเฉลย 10/10 (เรซูเม่จำลอง 10 ฉบับ งาน 10 ตำแหน่ง)

## สิ่งที่ทำแล้ว / ยังไม่ทำ (เทียบรายงาน A04)
- **ทำแล้ว:** หน้าจอครบ 21 หน้าตามบทที่ 3 (อัปโหลด PDF, ค้นหา/บันทึก/สมัครงาน, สถานะผู้สมัคร + CSV, ยืนยันตัวตนบริษัท, หน้าผู้ดูแลระบบ พจนานุกรม/คำพ้อง/บันทึกข้อผิดพลาด)
- **ยังไม่ทำ:** ดู `docs/changes-v2.md` หัวข้อ "สิ่งที่ยังทำไม่ได้"

## ไม่ได้รวมไว้ใน repo
- **เอกสารแผนงาน PDF** (Plan-2weeks-all-members, Plan-Backend-Thanawat, Plan-Frontend-Chayanatthapon): มีชื่อและรหัสนิสิตของสมาชิก
  และ repo นี้เป็นสาธารณะ จึงเก็บไว้ในเครื่องเท่านั้น (ถ้าวางไว้ที่ `docs/presentation/plans/` git จะไม่นำขึ้น เพราะอยู่ใน `.gitignore`)
- **รายงาน A04 ฉบับเต็ม** (.docx/.pdf)
