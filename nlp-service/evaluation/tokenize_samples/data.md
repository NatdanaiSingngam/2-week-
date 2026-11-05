# ผลตัดคำ: sample_resume_data.txt

## ข้อความเดิม

มีพื้นฐานการวิเคราะห์ข้อมูลด้วย Python (pandas, scikit-learn) และ SQL เคยทำโปรเจกต์ Machine Learning ใช้ Excel ได้ สื่อสารภาษาอังกฤษได้ดี

## Token ที่ newmm ตัดได้ (19 token)

มี | พื้นฐาน | การวิเคราะห์ข้อมูล | ด้วย | python | pandas | scikit-learn | และ | sql | เคย | ทำ | โปรเจกต์ | machine learning | ใช้ | excel | ได้ | สื่อสาร | ภาษาอังกฤษ | ได้ดี

## ทักษะที่จับได้ (9 ทักษะ)

- Data Analysis (data)
- Python (programming)
- pandas (data)
- scikit-learn (data)
- SQL (database)
- Machine Learning (data)
- Excel (office)
- การสื่อสาร (soft)
- ภาษาอังกฤษ (language)

## Token ที่ไม่ใช่ทักษะ (ถูกข้ามเพราะไม่อยู่ในพจนานุกรม)

มี | พื้นฐาน | ด้วย | และ | เคย | ทำ | โปรเจกต์ | ใช้ | ได้ | ได้ดี

## ข้อสังเกต

- token ที่ตรงพจนานุกรม 9 จาก 19 ตัว ส่วนที่เหลือเป็นคำทั่วไปหรือเครื่องหมาย ถูกข้ามโดยอัตโนมัติ
- ชื่อทักษะที่มีจุดหรือขีด (Node.js, scikit-learn) ต้องเพิ่มเข้า Trie ไม่เช่นนั้น newmm ตัดแยก จึงเพิ่ม surface form ทุกคำจากพจนานุกรมเข้า Trie
- เครื่องหมายวงเล็บและจุลภาคถูกลบก่อนตัดคำ ไม่เช่นนั้นจะติดกับชื่อทักษะ เช่น (pandas,
