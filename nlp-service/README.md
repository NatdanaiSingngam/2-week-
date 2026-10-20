# nlp-service — ฝั่ง NLP ของระบบ Workmatch

Python service (FastAPI) ที่ Spring Boot เรียกใช้ มี 2 หน้าที่
1. `/extract` สกัดทักษะจากข้อความเรซูเม่หรือประกาศงาน
2. `/match` คำนวณคะแนนจับคู่ระหว่างทักษะผู้สมัครกับทักษะที่งานต้องการ

รายละเอียดรูปแบบ JSON อยู่ใน [api-contract.md](api-contract.md)

## ไฟล์แต่ละตัวคืออะไร

| ไฟล์ | หน้าที่ |
|---|---|
| `nlp_service/api_server.py` | กำหนด endpoint `/extract`, `/match`, `/health` |
| `nlp_service/skill_extractor.py` | ตัดคำไทย/อังกฤษด้วย PyThaiNLP แล้วหาทักษะที่อยู่ใน dictionary |
| `nlp_service/match_scorer.py` | คำนวณคะแนน (ตรง ÷ ที่ต้องการ × 100) และค่า Cosine เทียบ |
| `nlp_service/skill_dictionary.csv` | รายการทักษะ + ชื่อเรียกอื่น + หมวด (แก้ไฟล์นี้เพื่อเพิ่มทักษะ ไม่ต้องแก้โค้ด) |
| `tests/test_extract_and_match.py` | ชุดทดสอบ 10 ข้อ |
| `tests/sample_resume_backend.txt` | เรซูเม่ตัวอย่างสาย Backend (Java, Spring Boot, MySQL) |
| `tests/sample_resume_frontend.txt` | เรซูเม่ตัวอย่างสาย Frontend (HTML, CSS, React, Figma) |
| `tests/sample_resume_data.txt` | เรซูเม่ตัวอย่างสาย Data (Python, pandas, Machine Learning) |
| `api-contract.md` | สัญญา API ที่ตกลงกับฝั่ง Spring Boot |
| `requirements.txt` | รายการ package ที่ต้องติดตั้ง |

## วิธีใช้

ติดตั้งครั้งแรก:

    python -m venv venv
    venv\Scripts\activate          (Windows)   หรือ   source venv/bin/activate   (Mac/Linux)
    pip install -r requirements.txt

รัน service:

    uvicorn nlp_service.api_server:app --port 8000

เปิดหน้าเอกสาร API อัตโนมัติที่ http://localhost:8000/docs

รันชุดทดสอบ:

    python -m pytest -q

## หมายเหตุ
- `skill_dictionary.csv` เป็นร่างแรก ควรให้ผู้รับผิดชอบข้อมูลช่วยตรวจและเพิ่มทักษะตามตำแหน่งที่ใช้เดโมจริง
- เรซูเม่ใน `tests/` เป็นข้อมูลตัวอย่างที่เขียนขึ้นเพื่อทดสอบ ไม่ใช่ข้อมูลจริง
