# Workmatch: รายงานสัปดาห์ที่ 1 (16–22 พ.ย.) นักศึกษาครบวงจรสมัครงาน

Nov 19, 2026 · @Natdanai Singngam

## สรุป

ทั้ง 7 ฟังก์ชันของสัปดาห์นี้**มีอยู่แล้วในโค้ด** (commit 818699c) และ**ผ่านเกณฑ์ตรวจตามแผนทุกข้อ** ไม่ต้องเขียนโค้ดเพิ่ม ทดสอบเมื่อ 19 พ.ย. 2569 บนระบบสำเนาทดสอบ (ฐานข้อมูล H2 + ข้อมูลเดโม) ผ่านหน้าเว็บจริง ไม่กระทบฐานข้อมูล MySQL

| วันที่ | ฟังก์ชัน | ผู้รับผิดชอบ | เกณฑ์ตามแผน | ผลทดสอบ |
| --- | --- | --- | --- | --- |
| 16 พ.ย. | สถานะเปิด/ปิดรับประกาศ | ธนวัฒน์ | บริษัทสลับสถานะได้ | ✅ ปิด Frontend → "ปิดรับสมัคร" → เปิดกลับ → "เปิดรับสมัคร" |
| 17 พ.ย. | หน้าค้นหางาน | ชญาณัตถพล | เห็นเฉพาะงานที่เปิดรับ | ✅ ปิด Data Analyst แล้วหายจากหน้าค้นหา เหลือ 5 งานที่เปิด |
| 18 พ.ย. | หน้ารายละเอียดงาน (% ทักษะตรง/ขาด) | ณัฐดนัย | งานที่ลบแล้วขึ้น 404 | ✅ Backend แสดง 60% · ทักษะตรง 5 จาก 7 · ขาด REST API, Docker · ลบ Data Analyst แล้วเปิดได้ 404 |
| 19 พ.ย. | บันทึกงาน (Bookmark) | ธนวัฒน์ | กดซ้ำคือยกเลิก | ✅ กด 3 ครั้ง: นำออก → บันทึก → นำออก |
| 20 พ.ย. | หน้างานที่บันทึกไว้ | ชญาณัตถพล | เห็นเฉพาะของตัวเอง | ✅ student1 เห็นของตัวเอง student2 เห็นรายการว่าง |
| 21 พ.ย. | สมัครงาน | ธนวัฒน์ | สมัครซ้ำไม่ได้, งานปิดรับสมัครไม่ได้ | ✅ ครั้งที่ 2 ขึ้น "คุณสมัครตำแหน่งนี้ไปแล้ว" · งานปิดขึ้น "ตำแหน่งนี้ปิดรับสมัครแล้ว" |
| 22 พ.ย. | ติดตามสถานะการสมัคร | ชญาณัตถพล | เห็น pending/reviewed/accepted/rejected | ✅ บริษัทเปลี่ยนสถานะ นิสิตเห็น รอพิจารณา / ตรวจสอบแล้ว / ผ่านการพิจารณา / ไม่ผ่านการพิจารณา |

ตำแหน่งไฟล์ทุกจุดเขียนเต็มนับจากโฟลเดอร์ราก repo (โฟลเดอร์ app/ คือ https://github.com/NatdanaiSingngam/2-week-) ตามด้วยเลขบรรทัด และชื่อเมธอดที่บรรทัดนั้นอยู่ เช่น `backend/src/main/java/com/workmatch/service/JobPostingService.java:121` ใน `setStatus()` คือไฟล์ JobPostingService บรรทัด 121 ในเมธอด setStatus เลขบรรทัดอ้างอิงตาม commit 818699c (โค้ดยังไม่เปลี่ยนจนถึงวันที่เขียนรายงาน) ส่วนเลข "บรรทัด xx" ในวงเล็บที่ไม่มีชื่อไฟล์ หมายถึงไฟล์เดียวกับในช่องนั้น

## 16 พ.ย. สถานะเปิด/ปิดรับประกาศงาน

บริษัทกดปุ่ม "ปิดรับ" / "เปิดรับ" ในหน้าจัดการประกาศ สถานะมี 3 ค่า: OPEN เปิดรับสมัคร, CLOSED ปิดรับสมัคร, DRAFT แบบร่าง (`backend/src/main/java/com/workmatch/entity/JobStatus.java:11–13`)

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `frontend/templates/company/postings.html:35` | ฟอร์มปุ่ม "ปิดรับ" แสดงเมื่อสถานะเป็น OPEN (บรรทัด 36 คือปุ่ม "เปิดรับ" ตอนสถานะอื่น) | POST /company/jobs/{id}/status พร้อม status=CLOSED หรือ OPEN |
| 2 | `backend/src/main/java/com/workmatch/controller/JobController.java:146` ใน `status()` | รับคำขอ แปลงข้อความเป็น JobStatus (บรรทัด 148) | เรียก JobPostingService.setStatus() (บรรทัด 149) |
| 3 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:121` ใน `setStatus()` | owned(): ตรวจว่าประกาศเป็นของบริษัทนี้ ไม่ใช่ → 404 | บริษัทอื่นแก้สถานะประกาศคนอื่นไม่ได้ |
| 4 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:124–125` ใน `setStatus()` | job.setStatus() แล้ว jobs.save() | อัปเดตคอลัมน์ status ในตาราง jobs |
| 5 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:126` ใน `setStatus()` | ถ้าไม่ใช่ OPEN → matches.deleteByJobId() | ลบคะแนนจับคู่ของประกาศนี้ งานหายจากหน้าหลักนิสิต |
| 6 | `backend/src/main/java/com/workmatch/controller/JobController.java:150` ใน `status()` | ถ้าเปิดกลับเป็น OPEN → recalculate() | MatchService.recalculateForJob() คำนวณคะแนนกับทุกเรซูเม่ใหม่ |
| 7 | `backend/src/main/java/com/workmatch/controller/JobController.java:151–152` ใน `status()` | ใส่ข้อความแจ้ง แล้วกลับหน้า /company/postings | บริษัทเห็น "เปลี่ยนสถานะเป็น … แล้ว" |

ผลต่อฝั่งนิสิต: งานปิดรับไม่ขึ้นในหน้าค้นหา (หัวข้อ 17 พ.ย.) และสมัครไม่ได้ (หัวข้อ 21 พ.ย.) แต่ยังเปิดหน้ารายละเอียดได้ เทสอัตโนมัติ: `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:212` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()` (ปิดประกาศแล้วงานหายจากหน้าหลักนิสิต)

## 17 พ.ย. หน้าค้นหารายการประกาศงาน

หน้า /student/jobs แสดงเฉพาะงานที่เปิดรับ เพราะคำสั่งดึงข้อมูลกรองสถานะตั้งแต่ฐานข้อมูล ค้นได้ด้วยชื่อตำแหน่ง ชื่อบริษัท หรือทักษะ และกรองประเภทกิจการ

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `frontend/templates/student/jobs.html:19` | ช่องค้นหา name="q" (ฟอร์ม GET) | GET /student/jobs?q=...&type=... |
| 2 | `backend/src/main/java/com/workmatch/controller/StudentController.java:52` ใน `explore()` | รับคำขอ หาตัวนิสิตที่ล็อกอิน | เรียก JobPostingService.explore() (บรรทัด 55) |
| 3 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:174` ใน `explore()` | เตรียมคำค้น (ตัวพิมพ์เล็ก), รายการที่นิสิตบันทึกไว้, เรซูเม่ของนิสิต | ใช้ในขั้นถัดไป |
| 4 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:183` ใน `explore()` → `backend/src/main/java/com/workmatch/repository/JobRepository.java:20–22` ใน `findOpenJobs()` | **jobs.findOpenJobs()**: SQL where status is null or status = OPEN เรียงใหม่สุดก่อน | **ตรงนี้คือเกณฑ์ "เห็นเฉพาะงานที่เปิดรับ"** แบบร่างและปิดรับไม่ถูกดึงมาเลย |
| 5 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:186–190` ใน `explore()` | กรองประเภทกิจการ และเทียบคำค้นกับชื่อตำแหน่ง / ชื่อบริษัท / ทักษะ | ไม่ตรง → ข้ามงานนั้น |
| 6 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:192–193` ใน `explore()` | ดึงคะแนนจับคู่ของนิสิตกับงานจากตาราง matches | กลายเป็นป้าย "Match xx%" บนการ์ด |
| 7 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:195–197` ใน `explore()` | สร้าง ExploreItem (ชื่อ, บริษัท, ป้ายยืนยันแล้ว, ทักษะ, ประกาศกี่วัน, คะแนน, บันทึกแล้วหรือยัง) | ส่งกลับให้ controller ใส่ลง Model ชื่อ jobs |
| 8 | `frontend/templates/student/jobs.html:37` | วนแสดงการ์ดทีละงาน (ปุ่มดาวบันทึกที่บรรทัด 48) | นิสิตเห็นรายการงาน |

ทดสอบ: ปิด Data Analyst แล้วหน้าค้นหาเหลือ 5 งาน (UX/UI, DevOps/QA, Mobile, Frontend, Backend) ไม่มี Data Analyst

## 18 พ.ย. หน้ารายละเอียดงาน (% ทักษะตรง/ขาด)

หน้า /student/jobs/{id} แสดงค่าร้อยละความสอดคล้อง ทักษะที่มี และทักษะที่ควรเตรียมเพิ่ม งานที่ไม่มีแล้วหรือเป็นแบบร่างตอบ 404

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `backend/src/main/java/com/workmatch/controller/StudentController.java:62–64` ใน `job()` | รับ GET /student/jobs/{id} | เรียก JobPostingService.studentView() |
| 2 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:211` ใน `studentView()` | jobs.findById(jobId) **ไม่เจอ → โยน JobNotFoundException** | **ตรงนี้คือเกณฑ์ "งานที่ลบแล้วขึ้น 404"** |
| 3 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:212` ใน `studentView()` | เป็นแบบร่าง (DRAFT) → โยน JobNotFoundException | นิสิตเปิดดูประกาศที่ยังไม่เผยแพร่ไม่ได้ |
| 4 | `backend/src/main/java/com/workmatch/exception/JobNotFoundException.java:18` | @ResponseStatus(NOT\_FOUND) | Spring ตอบ 404 และแสดง `frontend/templates/error/404.html` (`backend/src/main/java/com/workmatch/controller/GlobalExceptionHandler.java:70` ใน `other()` ปล่อยผ่าน ไม่บันทึกเป็น error) |
| 5 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:214–215` ใน `studentView()` | ดึงทักษะที่งานต้องการจากตาราง job\_skills | แสดงในส่วน "ทักษะที่ต้องการ" |
| 6 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:216` ใน `studentView()` → `backend/src/main/java/com/workmatch/service/MatchService.java:162` ใน `matchFor()` | matchFor(): หาเรซูเม่ของนิสิต แล้วดึงแถวคะแนนจากตาราง matches | ได้ score, matched (ทักษะที่ตรง), missing (ทักษะที่ขาด) |
| 7 | `backend/src/main/java/com/workmatch/service/JobPostingService.java:217` ใน `studentView()` | ดูว่านิสิตสมัครงานนี้แล้วหรือยัง | ใช้ซ่อน/แสดงปุ่มสมัคร |
| 8 | `frontend/templates/student/job.html:15` | คำนวณ s = คะแนน และ lv = @fmt.level(s) | ได้ป้ายระดับ (เกณฑ์ที่ `backend/src/main/java/com/workmatch/service/MatchLevels.java`) |
| 9 | `frontend/templates/student/job.html:42–43` | วงกลมคะแนน + ป้ายระดับ + "ทักษะตรง x จาก y" | นิสิตเห็นเปอร์เซ็นต์ |
| 10 | `frontend/templates/student/job.html:46–47` | วนแสดง matched (สีเขียว "ทักษะที่คุณมี") และ missing ("ควรเตรียมเพิ่ม") | นิสิตรู้ว่าต้องเตรียมทักษะอะไร |

ทดสอบ: student1 เปิด Backend Developer ได้ 60% · ทักษะตรง 5 จาก 7 · มี Java, Spring Boot, MySQL, Git, การทำงานเป็นทีม · ควรเตรียมเพิ่ม REST API, Docker · บริษัทลบ Data Analyst แล้วเปิด /student/jobs/3 ได้ 404 · เทสอัตโนมัติ `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:160` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()` (แบบร่างตอบ 404)

## 19 พ.ย. บันทึกงานที่สนใจ (Bookmark)

ปุ่มเดียวทำทั้งบันทึกและยกเลิก: ถ้ามีแถวอยู่แล้วลบ ถ้าไม่มีสร้างใหม่

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `frontend/templates/student/jobs.html:48`, `frontend/templates/student/job.html:64` | ปุ่มดาว (หน้าค้นหา) / ปุ่ม "บันทึก" (หน้ารายละเอียด) | POST /student/jobs/{id}/bookmark |
| 2 | `backend/src/main/java/com/workmatch/controller/StudentController.java:69` ใน `toggleBookmark()` | รับคำขอ | เรียก BookmarkService.toggle() (บรรทัด 72) |
| 3 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:41` ใน `toggle()` → `backend/src/main/java/com/workmatch/repository/BookmarkRepository.java:18` ใน `findByStudentIdAndJobId()` | findByStudentIdAndJobId(): หาแถวเดิมของนิสิตคนนี้กับงานนี้ | แยก 2 ทาง |
| 4 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:42–43` ใน `toggle()` | **เจอ → bookmarks.delete() คืน false** | **ตรงนี้คือเกณฑ์ "กดซ้ำคือยกเลิก"** |
| 5 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:45–46` ใน `toggle()` | ไม่เจอ → ตรวจว่างานมีจริงและไม่ใช่แบบร่าง | ไม่ผ่าน → 404 |
| 6 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:47–51` ใน `toggle()` | สร้าง Bookmark ใหม่ บันทึก คืน true | เพิ่มแถวในตาราง bookmarks |
| 7 | `backend/src/main/java/com/workmatch/entity/Bookmark.java:19` | Unique constraint (student\_id, job\_id) | ฐานข้อมูลกันไม่ให้บันทึกซ้ำอีกชั้น |
| 8 | `backend/src/main/java/com/workmatch/controller/StudentController.java:73–74` ใน `toggleBookmark()` | ข้อความแจ้งตามผล true/false แล้วกลับหน้าที่กดมา (safeBack กัน redirect ไปเว็บอื่น) | เห็น "บันทึกงานนี้แล้ว" หรือ "นำออกจากรายการที่บันทึกแล้ว" |

ทดสอบ: student1 กดบันทึก Frontend Developer 3 ครั้งติดกัน (เดิมบันทึกไว้ในข้อมูลเดโม) ได้ นำออก → บันทึก → นำออก รายการในหน้างานที่บันทึกเปลี่ยนตามทุกครั้ง · เทสอัตโนมัติ `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:166` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()`

## 20 พ.ย. หน้ารายการงานที่บันทึกไว้

หน้า /student/bookmarks ดึงเฉพาะแถวที่ student\_id เป็นของนิสิตที่ล็อกอิน นิสิตคนอื่นจึงเห็นไม่ได้

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `backend/src/main/java/com/workmatch/controller/StudentController.java:90–92` ใน `bookmarks()` | รับ GET /student/bookmarks ส่ง currentUserService.getCurrentUser() เข้า service | ตัวผู้ใช้มาจาก session ที่ล็อกอิน ไม่ใช่จาก URL จึงปลอมเป็นคนอื่นไม่ได้ |
| 2 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:61–62` ใน `list()` | bookmarks.findForStudent(student.getId()) | ดึงรายการของนิสิตคนนี้ |
| 3 | `backend/src/main/java/com/workmatch/repository/BookmarkRepository.java:21–22` ใน `findForStudent()` | **SQL where b.student.id = ?1 order by createdAt desc** | **ตรงนี้คือเกณฑ์ "เห็นเฉพาะของตัวเอง"** ใหม่สุดอยู่บน |
| 4 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:63` ใน `list()` | ตัดประกาศที่กลายเป็นแบบร่างออก | ไม่แสดงงานที่บริษัทดึงกลับไปแก้ |
| 5 | `backend/src/main/java/com/workmatch/service/BookmarkService.java:64–66` ใน `list()` | สร้าง BookmarkItem: ชื่องาน, บริษัท, วันที่บันทึก, ยังเปิดรับไหม, สมัครแล้วหรือยัง | ส่งให้หน้าเว็บ |
| 6 | `frontend/templates/student/bookmarks.html:25` | วนแสดงทีละรายการ | นิสิตเห็นงานที่บันทึก |
| 7 | `frontend/templates/student/bookmarks.html:34` → `backend/src/main/java/com/workmatch/controller/StudentController.java:97` ใน `removeBookmark()` → `backend/src/main/java/com/workmatch/service/BookmarkService.java:56–58` ใน `remove()` | ปุ่มนำออก → ลบเฉพาะแถวของนิสิตคนนี้ | รายการหายจากหน้า |

ทดสอบ: student1 มีรายการที่บันทึก (Mobile App Developer) ส่วน student2 เปิดหน้าเดียวกันได้รายการว่าง ไม่เห็นของ student1

## 21 พ.ย. สมัครงาน

การสมัครผ่านการตรวจ 3 ชั้นก่อนบันทึก: งานต้องเปิดรับ, นิสิตต้องมีเรซูเม่, และห้ามสมัครซ้ำ หน้าเว็บยังซ่อนปุ่มสมัครให้อีกชั้น

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `frontend/templates/student/job.html:59, 65` | แสดงฟอร์มและปุ่มสมัคร**เฉพาะ**ตอนยังไม่สมัคร และงานเป็น OPEN | POST /student/jobs/{id}/apply (พร้อมข้อความแนะนำตัว coverLetter) |
| 2 | `frontend/templates/student/job.html:56–57` | สมัครแล้ว → แสดง "สมัครตำแหน่งนี้แล้วเมื่อ … · สถานะ …" แทนปุ่ม | นิสิตไม่เห็นปุ่มสมัครซ้ำ |
| 3 | `backend/src/main/java/com/workmatch/controller/StudentController.java:77–82` ใน `apply()` | รับคำขอ เรียก ApplicationService.apply() | สำเร็จ → "ส่งใบสมัครแล้ว…" / ผิดเงื่อนไข → จับ IllegalArgumentException แสดงข้อความ error (บรรทัด 83–85) |
| 4 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:80` ใน `apply()` | หางาน ไม่เจอ → 404 | — |
| 5 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:81` ใน `apply()` | **งานไม่เปิดรับ → "ตำแหน่งนี้ปิดรับสมัครแล้ว"** | **เกณฑ์ "งานปิดรับสมัครไม่ได้"** (กันได้แม้ยิงคำขอตรงโดยไม่ผ่านปุ่ม) |
| 6 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:82–84` ใน `apply()` | ไม่มีเรซูเม่ → "กรุณาบันทึกเรซูเม่ก่อนสมัครงาน" | บริษัทจะได้เห็นทักษะของผู้สมัครเสมอ |
| 7 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:85–87` ใน `apply()` | **สมัครแล้ว (existsByStudentIdAndJobId) → "คุณสมัครตำแหน่งนี้ไปแล้ว"** | **เกณฑ์ "สมัครซ้ำไม่ได้"** |
| 8 | `backend/src/main/java/com/workmatch/entity/JobApplication.java:22` | Unique constraint (student\_id, job\_id) | ฐานข้อมูลกันสมัครซ้ำอีกชั้น กรณีกดพร้อมกัน 2 ครั้ง |
| 9 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:88–95` ใน `apply()` | ตรวจความยาวข้อความ (≤ 3,000) สร้าง JobApplication สถานะ PENDING แล้วบันทึก | เพิ่มแถวในตาราง job\_applications บริษัทเห็นทันที |

ทดสอบ: student1 สมัคร Frontend Developer ครั้งแรกสำเร็จ · ครั้งที่ 2 ขึ้น "คุณสมัครตำแหน่งนี้ไปแล้ว" · สมัคร Data Analyst ที่ปิดรับ ขึ้น "ตำแหน่งนี้ปิดรับสมัครแล้ว" (ยิงคำขอตรง เพราะหน้าเว็บซ่อนปุ่มแล้ว) · เทสอัตโนมัติ `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:173–177` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()`

## 22 พ.ย. ติดตามสถานะการสมัคร

สถานะมี 4 ค่าตาม Data Dictionary (`backend/src/main/java/com/workmatch/entity/ApplicationStatus.java`): PENDING รอพิจารณา, REVIEWED ตรวจสอบแล้ว, ACCEPTED ผ่านการพิจารณา, REJECTED ไม่ผ่านการพิจารณา บริษัทเปลี่ยน → นิสิตเห็นทันทีเมื่อเปิดหน้า

**ฝั่งบริษัทเปลี่ยนสถานะ**

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `frontend/templates/company/candidate.html:60–62` | ปุ่ม ไม่ผ่าน / ตรวจสอบแล้ว / ผ่านการพิจารณา | POST /company/applications/{id}/status |
| 2 | `backend/src/main/java/com/workmatch/controller/CompanyController.java:74` ใน `updateStatus()` | รับคำขอ แปลงเป็น ApplicationStatus | เรียก ApplicationService.updateStatus() |
| 3 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:159` ใน `updateStatus()` | ตรวจว่าใบสมัครเป็นของประกาศบริษัทนี้ แล้วบันทึกสถานะใหม่ | อัปเดตคอลัมน์ status ในตาราง job\_applications |

**ฝั่งนิสิตดูสถานะ**

| # | บรรทัด | ทำอะไร | ส่งต่อไป / ผลลัพธ์ |
| --- | --- | --- | --- |
| 1 | `backend/src/main/java/com/workmatch/controller/StudentController.java:104–106` ใน `applications()` | รับ GET /student/applications | เรียก ApplicationService.forStudent() |
| 2 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:99–100` ใน `forStudent()` → `backend/src/main/java/com/workmatch/repository/JobApplicationRepository.java:23–24` ใน `findForStudent()` | SQL where a.student.id = ?1 order by appliedAt desc | เห็นเฉพาะใบสมัครของตัวเอง ใหม่สุดอยู่บน |
| 3 | `backend/src/main/java/com/workmatch/service/ApplicationService.java:101–102` ใน `forStudent()` | สร้าง StudentApplicationItem (งาน, บริษัท, วันที่สมัคร, **status**) | ส่งให้หน้าเว็บ |
| 4 | `frontend/templates/student/applications.html:29, 32` | วนแต่ละแถว แสดง a.status().label พร้อม class CSS ตามชื่อสถานะ | **เกณฑ์ "เห็น pending/reviewed/accepted/rejected"** สีป้ายอยู่ที่ `frontend/static/css/style.css:359–362` |

ทดสอบ: บริษัทตั้งใบสมัครของ student1 เป็น ตรวจสอบแล้ว (Frontend), ผ่านการพิจารณา (Backend), ไม่ผ่านการพิจารณา (DevOps/QA ของบริษัทที่ 2) หน้าประวัติการสมัครของ student1 แสดงครบทั้ง 3 สถานะ ส่วน "รอพิจารณา" เห็นตอนสมัครใหม่ · เทสอัตโนมัติ `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:199–202` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()`

## ข้อสังเกตที่พบระหว่างทดสอบ

ทั้ง 7 ฟังก์ชันผ่านเกณฑ์ "ตรวจว่าเสร็จเมื่อ" ตามแผน โดยไม่ต้องแก้โค้ด (ทดสอบ 19 พ.ย. บนสำเนา H2 โปรไฟล์ demo พอร์ต 8081 ไม่ได้แตะ MySQL จริง) สิ่งที่ควรรู้ก่อนเดโม:

1. **งานที่ปิดรับแล้วยังเปิดหน้ารายละเอียดได้** (`frontend/templates/student/job.html:59` ซ่อนปุ่มสมัคร) ส่วนงาน DRAFT ขึ้น 404 ถ้าอาจารย์ถาม ตอบได้ว่าตั้งใจให้คนที่บันทึกงานไว้ยังเปิดดูได้ แต่สมัครไม่ได้ (`backend/src/main/java/com/workmatch/service/ApplicationService.java:81` ใน `apply()` กันไว้อีกชั้น)
2. **บริษัทที่ยังไม่ยืนยันตัวตนยังจัดการผู้สมัครได้** (company2) อยู่ในรายการรอพัฒนา B5 ของ `docs/backlog.md` และตรงกับแผนสัปดาห์ 3 (1 ธ.ค. อนุมัติ/ปฏิเสธบริษัท)
3. **ข้อมูลเดโมมี bookmark อยู่แล้ว** (student1 บันทึกงาน Frontend ไว้) ถ้าเดโมกดดาวแล้วดาวหายไป แปลว่ายกเลิก ไม่ใช่บั๊ก
4. **สีป้ายสถานะ** อยู่ที่ `frontend/static/css/style.css:359–362` (PENDING เหลือง, REVIEWED น้ำเงิน, ACCEPTED เขียว, REJECTED เทา)

**เทสอัตโนมัติที่ยังขาด** (มีแค่ `backend/src/test/java/com/workmatch/DocumentFeaturesTest.java:147` ใน `studentExploresBookmarksAppliesAndCompanyManagesApplicants()` ที่ทดสอบเส้นทางหลัก)

| เกณฑ์ในแผน | มีเทสแล้ว | ยังขาด |
| --- | --- | --- |
| 18 พ.ย. งานที่ลบแล้วขึ้น 404 | งาน DRAFT ขึ้น 404 (บรรทัด 160) | id ที่ไม่มีอยู่จริง / ลบแล้ว |
| 19 พ.ย. กดซ้ำคือยกเลิก | กดบันทึกครั้งแรก (บรรทัด 166) | กดครั้งที่สองแล้วหาย |
| 20 พ.ย. เห็นเฉพาะของตัวเอง | - | นักศึกษา B ไม่เห็น bookmark ของ A |
| 21 พ.ย. งานปิดรับสมัครไม่ได้ | สมัครซ้ำไม่ได้ (บรรทัด 173–177) | สมัครงาน CLOSED แล้วถูกปฏิเสธ |
| 22 พ.ย. เห็นสถานะ | REVIEWED (บรรทัด 201) | ACCEPTED / REJECTED ฝั่งหน้านักศึกษา |

เทสพวกนี้เพิ่มได้ในไฟล์เดียวกันภายหลัง ยังไม่ได้เพิ่มตามที่ตกลงไว้ว่ายังไม่พัฒนาต่อ
