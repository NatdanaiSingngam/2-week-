"""สร้างตัวอย่างผลตัดคำ 3 ไฟล์ (งาน 1 ของแผน) จาก tests/sample_resume_*.txt

รัน:  cd nlp-service && python evaluation/tokenize_samples.py
ผลเขียนที่ evaluation/tokenize_samples/<ชื่อ>.md แต่ละไฟล์มี ข้อความเดิม, token จาก newmm,
ทักษะที่จับได้ และ token ที่ไม่ใช่ทักษะ (เพื่อดูว่าปัญหาอยู่ตรงไหน)
"""
import sys
from pathlib import Path

# คอนโซล Windows ค่าเริ่มต้นไม่ใช่ UTF-8 พิมพ์ภาษาไทยแล้วล้ม (UnicodeEncodeError) จึงบังคับ UTF-8
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from pythainlp.tokenize import word_tokenize  # noqa: E402
from nlp_service.skill_extractor import _trie, clean, extract_skills, load_skills  # noqa: E402

OUT = Path(__file__).resolve().parent / "tokenize_samples"
FILES = ["backend", "data", "frontend"]


def build(name: str) -> str:
    text = (ROOT / "tests" / f"sample_resume_{name}.txt").read_text(encoding="utf-8").strip()
    surface, _, _ = load_skills()
    tokens = [t for t in word_tokenize(clean(text).lower(), engine="newmm", custom_dict=_trie()) if t.strip()]
    skills = extract_skills(text)
    hit = [t for t in tokens if t in surface]
    other = [t for t in tokens if t not in surface]
    lines = [f"# ผลตัดคำ: sample_resume_{name}.txt\n", "## ข้อความเดิม\n", text, "",
             f"## Token ที่ newmm ตัดได้ ({len(tokens)} token)\n", " | ".join(tokens), "",
             f"## ทักษะที่จับได้ ({len(skills)} ทักษะ)\n"]
    lines += [f"- {s['name']} ({s['category']})" for s in skills] or ["- ไม่พบทักษะ"]
    lines += ["", "## Token ที่ไม่ใช่ทักษะ (ถูกข้ามเพราะไม่อยู่ในพจนานุกรม)\n", " | ".join(other) or "-", "",
              "## ข้อสังเกต\n",
              f"- token ที่ตรงพจนานุกรม {len(hit)} จาก {len(tokens)} ตัว ส่วนที่เหลือเป็นคำทั่วไปหรือเครื่องหมาย ถูกข้ามโดยอัตโนมัติ",
              "- ชื่อทักษะที่มีจุดหรือขีด (Node.js, scikit-learn) ต้องเพิ่มเข้า Trie ไม่เช่นนั้น newmm ตัดแยก จึงเพิ่ม surface form ทุกคำจากพจนานุกรมเข้า Trie",
              "- เครื่องหมายวงเล็บและจุลภาคถูกลบก่อนตัดคำ ไม่เช่นนั้นจะติดกับชื่อทักษะ เช่น (pandas,"]
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    for n in FILES:
        (OUT / f"{n}.md").write_text(build(n), encoding="utf-8")
        print("เขียน", OUT / f"{n}.md")
