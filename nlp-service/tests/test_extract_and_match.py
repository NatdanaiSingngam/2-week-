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
    r = match_skills(["Java", "Python", "Git"], ["Java", "Spring Boot", "Git", "MySQL"])
    assert r["score"] == 50.0
    assert r["matched"] == ["Java", "Git"]
    assert r["missing"] == ["Spring Boot", "MySQL"]


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
