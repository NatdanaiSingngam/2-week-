"""คำนวณคะแนนความเหมาะสมระหว่างทักษะของผู้สมัครกับทักษะที่ตำแหน่งงานต้องการ"""
import math


def _norm(skills):
    """ตัดซ้ำ/ช่องว่าง/ตัวพิมพ์ แต่เก็บชื่อเดิมไว้แสดงผล"""
    out = {}
    for s in skills:
        key = s.strip().lower()
        if key and key not in out:
            out[key] = s.strip()
    return out


def match_skills(resume_skills: list[str], job_skills: list[str]) -> dict:
    resume, job = _norm(resume_skills), _norm(job_skills)
    matched_keys = [k for k in job if k in resume]
    missing_keys = [k for k in job if k not in resume]

    # สูตรหลัก: ทักษะที่ตรง / ทักษะที่งานต้องการ x 100
    score = round(len(matched_keys) / len(job) * 100, 1) if job else 0.0

    # สูตรเทียบ: Cosine Similarity ของเวกเตอร์ 0/1 (มี/ไม่มีทักษะ)
    if resume and job:
        cosine = len(matched_keys) / math.sqrt(len(resume) * len(job))
    else:
        cosine = 0.0

    return {
        "score": score,
        "cosine": round(cosine * 100, 1),
        "matched": [job[k] for k in matched_keys],
        "missing": [job[k] for k in missing_keys],
    }
