# ==== Workmatch · NLP · ทดสอบ (pytest) ====
# หน้าที่: ทดสอบการสกัดทักษะ การคิดคะแนน และ API /extract /match /health
# ดึงข้อมูลจาก: tests/sample_resume_*.txt
# รัน: cd nlp-service แล้ว venv\Scripts\python -m pytest
# ====
from pathlib import Path

from fastapi.testclient import TestClient

from nlp_service.skill_extractor import extract_skills
from nlp_service.api_server import app
from nlp_service.match_scorer import match_skills

HERE = Path(__file__).parent
client = TestClient(app)


def names(text):
    return {s["name"] for s in extract_skills(text)}


def read(name):
    return (HERE / name).read_text(encoding="utf-8")


def test_extract_backend_resume():
    assert names(read("sample_resume_backend.txt")) >= {
        "Java", "Python", "Spring Boot", "MySQL", "Git", "การสื่อสาร", "การแก้ปัญหา"}


def test_extract_frontend_resume():
    assert names(read("sample_resume_frontend.txt")) >= {
        "HTML", "CSS", "JavaScript", "React", "Figma", "การทำงานเป็นทีม", "ความรับผิดชอบ"}


def test_extract_data_resume():
    assert names(read("sample_resume_data.txt")) >= {
        "Python", "pandas", "scikit-learn", "SQL", "Machine Learning", "Excel", "ภาษาอังกฤษ"}


def test_no_false_positive_java_in_javascript():
    assert names("ใช้ JavaScript ได้") == {"JavaScript"}


def test_no_skills_text():
    assert extract_skills("สวัสดีครับ ยินดีที่ได้รู้จัก") == []


def test_match_basic():
    # ไม่ส่งสถิติคลังเอกสาร: ทุกทักษะน้ำหนักเท่ากัน = Cosine แบบ 0/1 -> 2 / sqrt(3 x 4) = 57.7
    r = match_skills(["Java", "Python", "Git"], ["Java", "Spring Boot", "Git", "MySQL"])
    assert r["score"] == 57.7 and r["cosine"] == 57.7
    assert r["matched"] == ["Java", "Git"]
    assert r["missing"] == ["Spring Boot", "MySQL"]


def test_match_tfidf_weights_rare_skills_higher():
    # TF-IDF + Cosine ตามเอกสาร 2.3.3: ทักษะหายาก (พบในเอกสารน้อย) มีน้ำหนักมากกว่าทักษะที่พบบ่อย
    doc_freq = {"java": 9, "git": 9, "spring boot": 1, "kafka": 1, "python": 5}
    job = ["Java", "Git", "Spring Boot", "Kafka"]
    has_common = match_skills(["Java", "Git"], job, doc_freq, 10)       # มีแต่ทักษะที่พบบ่อย
    has_rare = match_skills(["Spring Boot", "Kafka"], job, doc_freq, 10)  # มีแต่ทักษะหายาก
    assert has_common["cosine"] == has_rare["cosine"] == 70.7            # แบบ 0/1 แยกไม่ออก
    assert has_rare["score"] > has_common["score"]                        # TF-IDF แยกได้
    assert 0 < has_common["score"] < 70.7 < has_rare["score"] < 100


def test_match_tfidf_skill_in_every_document_still_counts():
    # ทักษะที่มีในทุกเอกสารต้องไม่ได้น้ำหนัก 0 (ใช้ IDF แบบ +1)
    r = match_skills(["Java"], ["Java"], {"java": 10}, 10)
    assert r["score"] == 100.0


def test_match_case_and_duplicates():
    r = match_skills(["java", "JAVA", " Git "], ["Java", "Git"])
    assert r["score"] == 100.0 and r["missing"] == []


def test_match_empty_job_or_resume():
    assert match_skills(["Java"], [])["score"] == 0.0
    r = match_skills([], ["Java"])
    assert r["score"] == 0.0 and r["missing"] == ["Java"]


def test_api_extract_and_match():
    r = client.post("/extract", json={"text": "ใช้ Java และ Spring Boot ได้"})
    assert r.status_code == 200
    got = [s["name"] for s in r.json()["skills"]]
    assert got == ["Java", "Spring Boot"]
    m = client.post("/match", json={"resume_skills": got, "job_skills": ["Java", "MySQL"]})
    assert m.status_code == 200 and m.json()["score"] == 50.0


def test_api_validation_error():
    assert client.post("/extract", json={}).status_code == 422


def test_english_resume_with_skills():
    assert names("Experienced with Python, Django, REST API and Git. Good communication skills.") >= {
        "Python", "Django", "REST API", "Git"}


def test_english_resume_without_skills():
    text = "I enjoy reading books, traveling and playing football with my friends on weekends."
    assert extract_skills(text) == []
    r = client.post("/extract", json={"text": text})
    assert r.status_code == 200 and r.json() == {"skills": []}
    assert match_skills([], ["Python", "Git"])["score"] == 0.0


def test_common_english_words_need_capital_letter():
    # "excel" ที่เป็นกริยา (ทำได้ดีเยี่ยม) ไม่ใช่โปรแกรม Excel; ชื่อเครื่องมือเขียนตัวใหญ่จึงจะนับ
    assert names("ผมมุ่งมั่นที่จะ excel ในสายงานพัฒนาแอป") == set()
    assert names("ใช้ Excel ทำรายงาน และ EXCEL pivot") == {"Excel"}
    assert names("swift response to customers, team unity") == set()
    assert names("เขียนแอป iOS ด้วย Swift และเกมด้วย Unity") >= {"Swift", "Unity"}
