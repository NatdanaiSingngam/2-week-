# API ฝั่ง NLP (Python / FastAPI) สำหรับ Spring Boot เรียก

ดูวิธีรันและโครงไฟล์ได้ที่ [README.md](README.md)

ที่อยู่ตอนพัฒนา: http://localhost:8000 (รัน `uvicorn nlp_service.api_server:app --port 8000`)
หน้าเอกสาร API อัตโนมัติ: http://localhost:8000/docs

## 1) POST /extract — สกัดทักษะจากข้อความ
เรียกเมื่อบันทึกเรซูเม่หรือประกาศงาน

ส่งมา:
```json
{ "text": "มีทักษะ Java และ Spring Boot เคยใช้ MySQL ทำงานเป็นทีมได้ดี" }
```
ตอบกลับ:
```json
{ "skills": [
  { "name": "Java", "category": "programming" },
  { "name": "Spring Boot", "category": "backend" },
  { "name": "MySQL", "category": "database" },
  { "name": "การทำงานเป็นทีม", "category": "soft" }
] }
```
- name เป็นชื่อมาตรฐานจาก skill_dictionary.csv ให้เก็บค่านี้ลงฐานข้อมูล
- ไม่เจอทักษะเลยจะได้ `{ "skills": [] }` (status 200)
- ไม่ส่ง text มาจะได้ status 422

## 2) POST /match — คำนวณคะแนนจับคู่
เรียกเมื่อจะจับคู่ผู้สมัครกับตำแหน่งงาน ส่งรายชื่อทักษะ (name) ที่เก็บไว้แล้ว

ส่งมา:
```json
{ "resume_skills": ["Java", "Spring Boot", "MySQL", "การทำงานเป็นทีม"],
  "job_skills":    ["Java", "Spring Boot", "Git", "Docker"],
  "doc_freq":      {"java": 6, "spring boot": 3, "mysql": 5, "git": 8, "docker": 3, "การทำงานเป็นทีม": 7},
  "total_docs":    10 }
```
ตอบกลับ:
```json
{ "score": 55.7, "cosine": 50.0,
  "matched": ["Java", "Spring Boot"],
  "missing": ["Git", "Docker"] }
```
- score = ค่าร้อยละความสอดคล้อง = Cosine Similarity ของเวกเตอร์ TF-IDF x 100 (เอกสาร 1.4.4.4, 2.3.3) ใช้แสดงผลหลักและจัดอันดับ
  - เอกสาร 1 ฉบับ = ชุดทักษะของเรซูเม่ 1 ฉบับ หรือประกาศงานที่เปิดรับ 1 ประกาศ (backend นับให้แล้วส่งมาใน doc_freq / total_docs)
  - TF = 1 (มีทักษะ), IDF = ln((1 + N) / (1 + n_j)) + 1 ทักษะที่ไม่อยู่ใน doc_freq ถือว่าพบ 1 เอกสาร
  - ไม่ส่ง doc_freq / total_docs มา = ทุกทักษะน้ำหนักเท่ากัน (ผลเท่ากับ cosine)
- cosine = Cosine แบบ 0/1 x 100 (ทุกทักษะน้ำหนักเท่ากัน) เก็บไว้เทียบในรายงาน ไม่แสดงบนหน้าเว็บ
- matched / missing เรียงตามลำดับของ job_skills ใช้แสดง "ทักษะที่ขาด" บน Dashboard
- ไม่สนตัวพิมพ์เล็กใหญ่ และตัดชื่อซ้ำให้
- job_skills ว่างได้ score 0

## 3) GET /health
ตอบ `{ "status": "ok" }` ใช้เช็กว่า service รันอยู่

## ข้อตกลงที่ต้องยืนยันกับธนวัฒน์
1. ชื่อ field ตามด้านบน (text, skills, name, category, resume_skills, job_skills, score, matched, missing) โอเคไหม
2. Spring Boot เรียก /match ครั้งละ 1 คู่ (ผู้สมัคร 1 คน กับงาน 1 ตำแหน่ง) หากต้องการจับคู่หลายคู่ในครั้งเดียว บอกมาจะเพิ่ม endpoint ให้
