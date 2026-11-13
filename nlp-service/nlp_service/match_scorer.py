# ==== Workmatch · NLP · คิดคะแนนจับคู่ ====
# หน้าที่: เทียบทักษะเรซูเม่กับทักษะงาน คืน score (TF-IDF + Cosine Similarity × 100), cosine (แบบ 0/1 ไว้เทียบ), matched, missing
# ถูกเรียกโดย: api_server.py (POST /match) <- backend MatchService (ส่งสถิติคลังเอกสาร doc_freq / total_docs มาด้วย)
# ผลถูกเก็บที่: ตาราง matches ในฐานข้อมูล (โดย backend) แล้วแสดงในหน้า dashboard / job-detail
# ====
"""คำนวณค่าร้อยละความสอดคล้องระหว่างทักษะของผู้สมัครกับทักษะที่ตำแหน่งงานต้องการ

ตามเอกสารโครงงาน (หัวข้อ 1.3.2, 1.4.4.4, 2.1, 2.3.3):
1. แปลงชุดทักษะมาตรฐานของเรซูเม่และของประกาศงานเป็นเวกเตอร์ด้วย TF-IDF
2. วัดความคล้ายคลึงด้วย Cosine Similarity = (a · b) / (‖a‖ × ‖b‖)
3. ผลลัพธ์ × 100 คือ "ค่าร้อยละความสอดคล้อง" ใช้จัดอันดับ

รายละเอียดการคำนวณ
- เอกสาร (document) 1 ฉบับ = ชุดทักษะของเรซูเม่ 1 ฉบับ หรือประกาศงาน 1 ประกาศ
- TF = 1 เมื่อมีทักษะนั้น (ระบบเก็บทักษะแบบไม่ซ้ำต่อเอกสาร จึงไม่มีความถี่มากกว่า 1)
- IDF = ln((1 + N) / (1 + n_j)) + 1 โดย N = จำนวนเอกสารทั้งหมด, n_j = จำนวนเอกสารที่มีทักษะ j
  (เพิ่ม +1 จากสูตร log(N / n_j) ในเอกสาร เพื่อไม่ให้ทักษะที่มีในทุกเอกสารได้น้ำหนัก 0
  ซึ่งจะทำให้คะแนนเป็น 0 ทั้งที่ทักษะตรงกัน แนวทางเดียวกับ smooth_idf ของ scikit-learn)
- ถ้าไม่ได้ส่งสถิติคลังเอกสารมา ทุกทักษะได้น้ำหนักเท่ากัน (IDF = 1) ผลเท่ากับ Cosine แบบ 0/1
"""
import math


def _norm(skills):
    """ตัดซ้ำ/ช่องว่าง/ตัวพิมพ์ แต่เก็บชื่อเดิมไว้แสดงผล"""
    out = {}
    for s in skills:
        key = s.strip().lower()
        if key and key not in out:
            out[key] = s.strip()
    return out


def idf_weights(doc_freq: dict[str, int] | None, total_docs: int | None) -> dict[str, float]:
    """IDF ของแต่ละทักษะ (คีย์ตัวพิมพ์เล็ก) จากสถิติคลังเอกสาร"""
    if not doc_freq or not total_docs:
        return {}
    n = max(int(total_docs), 0)
    return {k.strip().lower(): math.log((1 + n) / (1 + max(int(v), 0))) + 1 for k, v in doc_freq.items() if k.strip()}


def _cosine(resume: dict, job: dict, weight) -> float:
    """Cosine Similarity ของเวกเตอร์ TF-IDF (TF = 1) คืนค่า 0-1"""
    if not resume or not job:
        return 0.0
    dot = sum(weight(k) ** 2 for k in job if k in resume)
    norm_r = math.sqrt(sum(weight(k) ** 2 for k in resume))
    norm_j = math.sqrt(sum(weight(k) ** 2 for k in job))
    return dot / (norm_r * norm_j) if norm_r and norm_j else 0.0


def match_skills(resume_skills: list[str], job_skills: list[str],
                 doc_freq: dict[str, int] | None = None, total_docs: int | None = None) -> dict:
    resume, job = _norm(resume_skills), _norm(job_skills)
    matched_keys = [k for k in job if k in resume]
    missing_keys = [k for k in job if k not in resume]

    idf = idf_weights(doc_freq, total_docs)
    # ทักษะที่ไม่อยู่ในสถิติ (เช่น เพิ่งเพิ่ม) ถือว่าพบใน 1 เอกสาร
    default_idf = math.log((1 + total_docs) / 2) + 1 if idf and total_docs else 1.0

    # สูตรหลักตามเอกสาร: TF-IDF + Cosine Similarity
    score = _cosine(resume, job, lambda k: idf.get(k, default_idf) if idf else 1.0)
    # สูตรเทียบ: Cosine แบบ 0/1 (ทุกทักษะน้ำหนักเท่ากัน) เก็บไว้ใช้ในรายงาน ไม่แสดงบนหน้าเว็บ
    cosine01 = _cosine(resume, job, lambda k: 1.0)

    return {
        "score": round(score * 100, 1),
        "cosine": round(cosine01 * 100, 1),
        "matched": [job[k] for k in matched_keys],
        "missing": [job[k] for k in missing_keys],
    }
