"""สกัดทักษะจากข้อความภาษาไทย/อังกฤษ ด้วย PyThaiNLP + dictionary ทักษะ (skill_dictionary.csv)"""
import csv
import re
from functools import lru_cache
from pathlib import Path

from pythainlp.corpus.common import thai_words
from pythainlp.tokenize import word_tokenize
from pythainlp.util import Trie

SKILLS_FILE = Path(__file__).parent / "skill_dictionary.csv"


@lru_cache(maxsize=1)
def load_skills():
    """อ่าน skill_dictionary.csv -> (surface_map, categories, canon_list)
    surface_map: คำที่อาจเจอในข้อความ (ตัวพิมพ์เล็ก) -> ชื่อทักษะมาตรฐาน
    """
    surface, categories, canon = {}, {}, []
    with open(SKILLS_FILE, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            name = row["skill"].strip()
            canon.append(name)
            categories[name] = row["category"].strip()
            forms = [name] + [a for a in row["aliases"].split("|") if a.strip()]
            for form in forms:
                surface[form.strip().lower()] = name
    return surface, categories, canon


@lru_cache(maxsize=1)
def _trie():
    surface, _, _ = load_skills()
    return Trie(list(thai_words()) + list(surface.keys()))


def clean(text: str) -> str:
    """ลบเครื่องหมายที่ทำให้ token ติดกัน เช่น (pandas, -> pandas
    เก็บ . + # / - ไว้ เพราะอยู่ในชื่อทักษะ (Node.js, C#, UI/UX, scikit-learn)"""
    return re.sub(r"[(),;:!?\[\]{}\"“”‘’]", " ", text)


def extract_skills(text: str) -> list[dict]:
    """คืนรายการทักษะที่เจอ ไม่ซ้ำ เรียงตามลำดับที่เจอในข้อความ"""
    surface, categories, _ = load_skills()
    tokens = word_tokenize(clean(text).lower(), engine="newmm", custom_dict=_trie())
    found, seen = [], set()
    for tok in tokens:
        name = surface.get(tok.strip())
        if name and name not in seen:
            seen.add(name)
            found.append({"name": name, "category": categories[name]})
    return found
