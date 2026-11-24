# ==== Workmatch · NLP · ทดสอบกฎคำปฏิเสธและสิ่งที่อยากเรียน (แผน 28 พ.ย.) ====
# รัน: cd nlp-service && venv/Scripts/python -m pytest tests/test_negation_rules.py
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from nlp_service.skill_extractor import extract_skills  # noqa: E402


def names(text, **kw):
    return [s["name"] for s in extract_skills(text, **kw)]


@pytest.mark.parametrize("text, expected", [
    ("ไม่เคยใช้ Docker และ Kubernetes แต่ใช้ Git เป็นประจำ", ["Git"]),          # ตัดช่วงด้วย "แต่"
    ("อยากเรียน React เพิ่มเติม ปัจจุบันใช้ Vue ทำโปรเจกต์", ["Vue"]),             # ตัดช่วงด้วย "ปัจจุบัน"
    ("I have never used Java, but I write Python every day.", ["Python"]),
    ("Interested in learning Docker. Experienced with Git and Linux.", ["Git", "Linux"]),
    ("No experience with AWS; currently using GCP.", ["GCP"]),
    ("I plan to learn Rust, and I already use C# with .NET.", ["C#", ".NET"]),  # ตัดช่วงด้วย ", and"
    ("ใช้ Node.js ทำ API ได้ แต่ไม่ถนัด CSS", ["Node.js"]),                       # จุดใน Node.js ไม่ใช่ตัวตัด
])
def test_skills_after_negation_in_same_clause_are_dropped(text, expected):
    assert names(text) == expected


@pytest.mark.parametrize("text, expected", [
    ("ไม่ว่าจะเป็น Python หรือ Java ก็เขียนได้คล่อง", ["Python", "Java"]),       # "ไม่" เดี่ยว ๆ ไม่ใช่คำปฏิเสธ
    ("ไม่มีปัญหาในการใช้ Excel และ Power BI ทำรายงาน", ["Excel", "Power BI"]),
    ("ไม่เพียงแต่เขียน Java ได้ ยังใช้ Spring Boot", ["Java", "Spring Boot"]),    # ข้อยกเว้น ไม่เพียงแต่
    ("แต่ละโปรเจกต์ใช้ React และ Node.js", ["React", "Node.js"]),                 # แต่ละ ไม่ใช่ตัวตัด
    ("Not only Java but also Kotlin for Android apps.", ["Java", "Kotlin", "Android"]),
])
def test_phrases_that_look_negative_keep_skills(text, expected):
    assert names(text) == expected


def test_rule_can_be_turned_off_for_before_after_measurement():
    text = "ไม่เคยใช้ Docker แต่ใช้ Git"
    assert names(text, apply_negation=False) == ["Docker", "Git"]
    assert names(text) == ["Git"]


# ---------- คำที่เพิ่มในพจนานุกรม (แผน 29 พ.ย.) ----------

@pytest.mark.parametrize("text, expected", [
    ("เขียนโปรแกรมด้วยจาวาและจาวาสคริปต์ ใช้เอ็กเซลทำรายงาน", ["Java", "JavaScript", "Excel"]),   # คำสะกดไทย
    ("Built a React web app and a React Native mobile app", ["React", "React Native"]),          # React Native ไม่กลายเป็น React
    ("มีความเป็นผู้นำ ทำงานร่วมกันเป็นทีมได้ดี และมีความรับผิดชอบสูง", ["ภาวะผู้นำ", "การทำงานเป็นทีม", "ความรับผิดชอบ"]),
    ("Real-time Collaboration Engine for documents", []),                                        # collaboration ไม่ใช่คำพ้อง
])
def test_added_dictionary_words(text, expected):
    assert names(text) == expected


def test_negation_reach_is_limited_when_thai_text_has_no_punctuation():
    text = "ไม่เคยใช้ Docker แต่ใช้ Git ทุกวัน อยากเรียน Kubernetes ในอนาคต มีความเป็นผู้นำ และทำงานเป็นทีมได้ดี"
    assert names(text) == ["Git", "ภาวะผู้นำ", "การทำงานเป็นทีม"]
