# ผลตรวจ Frontend C1–C4

ตรวจวันที่ 20 ตุลาคม 2026

## ผ่านแล้ว

- Preview ทั้ง 7 หน้าที่ขนาด desktop และ 390px: มีหัวข้อหลัก, โลโก้โหลดสำเร็จ, ไม่มีเนื้อหาล้นหน้าจอ (ตารางผู้สมัครเลื่อนแนวนอนภายในกรอบ)
- Dashboard: ไม่มี resume / ไม่มี matches / มี 4 matches, เปิด-ปิดรายละเอียด, แถบสี 85, 66.7, 40, 20 ตามเกณฑ์ 70/40
- Resume: ตัวนับ, ทักษะจัดหมวด, ไม่มีทักษะ, error, บันทึกตัวอย่างโดยไม่ส่งข้อความออกไป
- ฟอร์มงาน: แสดงบันทึกสำเร็จในโหมดสาธิต แล้วกลับไปแสดง error ได้
- JavaScript syntax ผ่านทั้ง app.js และ fixtures.js
- app.js: valid/invalid/default-prevented submit, ป้องกันกดซ้ำ, สถานะกำลังวิเคราะห์, คืนสถานะเมื่อ pageshow, ตัวนับอักษร
- SpringTemplateEngine (Thymeleaf 3.1.3 + Spring 6.1.14): เรนเดอร์ 7 templates x 6 สถานะ = 42 ครั้ง ได้แก่ model ขาด, list ว่าง, มีข้อมูล, field เป็น null, ทักษะครบ, ข้อผิดพลาด
- การเรนเดอร์รวม fragment, grouping ทักษะ, safe navigation, คะแนนทศนิยม, th:text escape ข้อความ HTML ที่ใช้ทดสอบ

## ยังต้องทดสอบร่วมกับ backend

repository ยังไม่มี Spring Boot backend จึงยังไม่ใช่ end-to-end test

- การสมัคร/เข้าสู่ระบบจริง, BCrypt, สิทธิ์ตาม role, การ redirect
- การเติม CSRF token ของ Spring Security และการ submit POST จริง (smoke test ใช้ test URL resolver และไม่ได้รัน servlet/security filter)
- การบันทึกฐานข้อมูล, การเรียก NLP ผ่าน Spring Boot, ผลคะแนนจริง, ลำดับที่ controller ส่ง
- ยืนยัน job.skills เป็นรายการ object ที่มี name/category ตามที่ templates ใช้

ข้อมูลและคะแนนใน preview ทั้งหมดสมมติ ไม่ใช่ผลความแม่นยำ NLP หรือผลประเมินระบบจริง ไม่มีการ push หรือส่งข้อความหาทีมจากงานนี้
