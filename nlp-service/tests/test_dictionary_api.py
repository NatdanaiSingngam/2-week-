# ==== Workmatch · NLP · ทดสอบ (pytest) ====
# หน้าที่: ทดสอบ GET/PUT /dictionary โดยเขียนลงไฟล์ชั่วคราว ไม่แตะ skill_dictionary.csv จริง
# ====
"""ทดสอบ GET/PUT /dictionary (หน้าผู้ดูแลระบบจัดการพจนานุกรมทักษะ) โดยเขียนลงไฟล์ชั่วคราว ไม่แตะ skill_dictionary.csv จริง"""
import shutil

import pytest
from fastapi.testclient import TestClient

from nlp_service import skill_extractor
from nlp_service.api_server import app


@pytest.fixture()
def client(tmp_path, monkeypatch):
    temp_csv = tmp_path / "skill_dictionary.csv"
    shutil.copy(skill_extractor.SKILLS_FILE, temp_csv)
    monkeypatch.setattr(skill_extractor, "SKILLS_FILE", temp_csv)
    skill_extractor.load_skills.cache_clear()
    skill_extractor._trie.cache_clear()
    skill_extractor._latin_pattern.cache_clear()
    yield TestClient(app)
    skill_extractor.load_skills.cache_clear()
    skill_extractor._trie.cache_clear()
    skill_extractor._latin_pattern.cache_clear()


def test_get_dictionary_returns_skills_with_aliases(client):
    body = client.get("/dictionary").json()
    js = next(s for s in body["skills"] if s["name"] == "JavaScript")
    assert "js" in js["aliases"]
    assert js["category"] == "programming"


def test_put_dictionary_replaces_skills_and_synonyms_immediately(client):
    new = {"skills": [
        {"name": "Kotlin", "category": "programming", "aliases": ["kt"]},
        {"name": "Git", "category": "tools", "aliases": []},
    ]}
    assert client.put("/dictionary", json=new).json()["count"] == 2
    found = client.post("/extract", json={"text": "เขียน KT และใช้ Git ส่วน Java ไม่อยู่ในพจนานุกรมแล้ว"}).json()["skills"]
    assert [s["name"] for s in found] == ["Kotlin", "Git"]


def test_put_dictionary_rejects_duplicate_or_conflicting_alias(client):
    dup = {"skills": [{"name": "Java", "category": "programming"}, {"name": "java", "category": "programming"}]}
    assert client.put("/dictionary", json=dup).status_code == 422
    clash = {"skills": [{"name": "Java", "category": "programming"},
                        {"name": "JavaScript", "category": "programming", "aliases": ["java"]}]}
    assert client.put("/dictionary", json=clash).status_code == 422
    assert client.put("/dictionary", json={"skills": []}).status_code == 422
