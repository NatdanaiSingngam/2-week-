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
  "job_skills":    ["Java", "Spring Boot", "Git", "Docker"] }
```
ตอบกลับ:
```json
{ "score": 50.0, "cosine": 50.0,
  "matched": ["Java", "Spring Boot"],
  "missing": ["Git", "Docker"] }
```
- score = ทักษะที่ตรง / ทักษะที่งานต้องการ x 100 (ใช้แสดงผลหลัก)
- cosine = ค่า Cosine Similarity x 100 (ไว้เทียบสูตร ยังไม่ต้องแสดงบนหน้าเว็บ)
- matched / missing เรียงตามลำดับของ job_skills ใช้แสดง "ทักษะที่ขาด" บน Dashboard
- ไม่สนตัวพิมพ์เล็กใหญ่ และตัดชื่อซ้ำให้
- job_skills ว่างได้ score 0

## 3) GET /health
ตอบ `{ "status": "ok" }` ใช้เช็กว่า service รันอยู่

## ข้อตกลงที่ต้องยืนยันกับธนวัฒน์
1. ชื่อ field ตามด้านบน (text, skills, name, category, resume_skills, job_skills, score, matched, missing) โอเคไหม
2. Spring Boot เรียก /match ครั้งละ 1 คู่ (ผู้สมัคร 1 คน กับงาน 1 ตำแหน่ง) หากต้องการจับคู่หลายคู่ในครั้งเดียว บอกมาจะเพิ่ม endpoint ให้
