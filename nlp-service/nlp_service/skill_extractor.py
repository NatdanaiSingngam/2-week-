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


def read_dictionary() -> list[dict]:
    """คืนพจนานุกรมทั้งหมดเป็น [{"name","category","aliases":[...]}] (ใช้ให้ backend นำเข้าครั้งแรก)"""
    items = []
    with open(SKILLS_FILE, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            items.append({
                "name": row["skill"].strip(),
                "category": row["category"].strip(),
                "aliases": [a.strip() for a in row["aliases"].split("|") if a.strip()],
            })
    return items


def write_dictionary(items: list[dict]) -> int:
    """แทนพจนานุกรมทั้งชุด (ผู้ดูแลระบบแก้จากหน้าเว็บ) เขียนไฟล์ใหม่แบบ atomic แล้วล้าง cache ให้ใช้ชุดใหม่ทันที
    ตรวจ: ชื่อห้ามว่าง/ห้ามซ้ำ (ไม่สนตัวพิมพ์) คำพ้องห้ามชนกับชื่อหรือคำพ้องของทักษะอื่น"""
    seen, rows = {}, []
    for item in items:
        name = (item.get("name") or "").strip()
        if not name:
            raise ValueError("ชื่อทักษะว่าง")
        if "|" in name or "," in name:
            raise ValueError(f"ชื่อทักษะมีอักขระต้องห้าม: {name}")
        key = name.lower()
        if key in seen:
            raise ValueError(f"ชื่อทักษะซ้ำ: {name}")
        seen[key] = name
        rows.append((name, [a.strip() for a in item.get("aliases") or [] if a and a.strip()],
                     (item.get("category") or "").strip() or "other"))
    for name, aliases, _ in rows:
        for alias in aliases:
            owner = seen.get(alias.lower())
            if owner and owner != name:
                raise ValueError(f"คำพ้อง '{alias}' ชนกับทักษะ {owner}")
            if "|" in alias:
                raise ValueError(f"คำพ้องมีอักขระต้องห้าม: {alias}")
            seen.setdefault(alias.lower(), name)
    tmp = SKILLS_FILE.with_suffix(".csv.tmp")
    with open(tmp, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["skill", "aliases", "category"])
        for name, aliases, category in rows:
            writer.writerow([name, "|".join(aliases), category])
    tmp.replace(SKILLS_FILE)
    load_skills.cache_clear()
    _trie.cache_clear()
    return len(rows)


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
