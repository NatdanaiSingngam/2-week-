# sample-data: ข้อมูลจำลองสำหรับทดสอบ Workmatch

ข้อมูลทั้งหมดในโฟลเดอร์นี้ **สร้างขึ้นเอง (สมมติ)** ไม่ใช่ resume หรือประกาศงานของคนหรือบริษัทจริง ไม่มีชื่อ อีเมล หรือเบอร์โทรจริง

| ไฟล์ | เนื้อหา |
| --- | --- |
| `resumes/resume_01.txt` ถึง `resume_10.txt` | resume ภาษาไทย 10 ฉบับ คนละสายงาน |
| `jobs.csv` | ตำแหน่งงาน 10 ตำแหน่ง (company, title, description) |
| `expected.csv` | ทักษะที่คนอ่านแล้วควรได้จากแต่ละ resume และตำแหน่งงาน (type, id, expected_skills คั่นด้วย `|`) |
| `pairs.csv` | 10 คู่ resume กับตำแหน่งงานที่ใช้วัดคะแนนจับคู่ |

## ข้อจำกัดที่ต้องบอกในรายงาน

- ผู้ตั้งคำตอบที่ถูกต้อง (`expected.csv`) กับผู้เขียนข้อความเป็นฝ่ายเดียวกัน ผลที่ได้จึงเป็นการทดสอบเบื้องต้นกับข้อมูลจำลอง ไม่ใช่การประเมินกับ resume จริง
- มีกับดักตั้งใจ 3 จุด: Java Island (resume_02), Taylor Swift (resume_03), excel ที่เป็นคำกริยา (resume_10) ซึ่งไม่ใช่ทักษะ
- มีทักษะที่ไม่อยู่ในพจนานุกรมตั้งใจ เช่น AWS, Cypress, Jira, Cisco, Shell Script, Jupyter Notebook

วัดด้วย `nlp-service/evaluation/run_eval.py` (ดูวิธีรันใน `nlp-service/README.md`)
