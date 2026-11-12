# ==== Workmatch · NLP · สกัดทักษะ ====
# หน้าที่: รับข้อความเรซูเม่/รายละเอียดงาน แล้วคืนรายการทักษะ {name, category}
# ดึงข้อมูลจาก: skill_dictionary.csv (ชื่อทักษะ คำพ้อง หมวด) + พจนานุกรมคำไทยของ PyThaiNLP
# ถูกเรียกโดย: api_server.py (POST /extract, GET/PUT /dictionary), evaluation/*.py, tests/*.py
# วิธีจับคำ: ภาษาอังกฤษจับแบบคำเต็มด้วย regex / ภาษาไทยตัดคำด้วย newmm แล้วเทียบทีละคำ
# ====
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
    _latin_pattern.cache_clear()
    return len(rows)


THAI = re.compile(r"[\u0E00-\u0E7F]")


def _is_thai(form: str) -> bool:
    return bool(THAI.search(form))


@lru_cache(maxsize=1)
def _trie():
    """พจนานุกรมตัดคำภาษาไทย + คำทักษะภาษาไทย (คำภาษาอังกฤษจับด้วย regex แยกต่างหาก)"""
    surface, _, _ = load_skills()
    return Trie(list(thai_words()) + [f for f in surface if _is_thai(f)])


@lru_cache(maxsize=1)
def _latin_pattern():
    """regex เดียวของคำทักษะภาษาอังกฤษทั้งหมด เรียงยาวก่อน (tailwind css ชนะ css)
    ต้องเป็นคำเต็ม: ด้านหน้าห้ามเป็นตัวอักษร/ตัวเลข/จุด (กัน D3.js -> js) ด้านหลังห้ามเป็นตัวอักษร/ตัวเลข/+/#
    (กัน Excellent -> Excel, JSON -> js, GitLab -> Git) ส่วน / - , เว้นวรรค ถือเป็นตัวคั่น (C/C++/Python)"""
    surface, _, _ = load_skills()
    forms = sorted((f for f in surface if not _is_thai(f)), key=len, reverse=True)
    if not forms:
        return None
    alt = "|".join(re.escape(f) for f in forms)
    return re.compile(r"(?<![a-z0-9.])(?:" + alt + r")(?![a-z0-9+#])")


def clean(text: str) -> str:
    """ลบเครื่องหมายที่ทำให้ token ติดกัน เช่น (pandas, -> pandas (แทนด้วยช่องว่าง ความยาวข้อความเท่าเดิม)
    เก็บ . + # / - ไว้ เพราะอยู่ในชื่อทักษะ (Node.js, C#, UI/UX, scikit-learn)"""
    return re.sub(r"[(),;:!?\[\]{}\"“”‘’]", " ", text)


def extract_skills(text: str) -> list[dict]:
    """คืนรายการทักษะที่เจอ ไม่ซ้ำ เรียงตามลำดับที่เจอในข้อความ
    คำภาษาอังกฤษ: จับแบบคำเต็มด้วย regex / คำภาษาไทย: ตัดคำด้วย PyThaiNLP (newmm) แล้วเทียบทีละ token"""
    surface, categories, _ = load_skills()
    lowered = clean(text).lower()
    hits = []  # (ตำแหน่ง, ชื่อทักษะ)

    pattern = _latin_pattern()
    if pattern:
        for m in pattern.finditer(lowered):
            hits.append((m.start(), surface[m.group(0)]))

    if THAI.search(lowered):
        pos = 0
        for tok in word_tokenize(lowered, engine="newmm", custom_dict=_trie()):
            key = tok.strip()
            if key and _is_thai(key) and key in surface:
                hits.append((pos, surface[key]))
            pos += len(tok)

    found, seen = [], set()
    for _, name in sorted(hits, key=lambda h: h[0]):
        if name not in seen:
            seen.add(name)
            found.append({"name": name, "category": categories[name]})
    return found
