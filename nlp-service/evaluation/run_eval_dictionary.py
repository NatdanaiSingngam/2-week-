# ==== Workmatch · NLP · วัดผลการเพิ่มคำในพจนานุกรม (ก่อน-หลัง) ====
# หน้าที่: วัด Precision/Recall/F1 ของการสกัดทักษะด้วยพจนานุกรม "ก่อนเพิ่มคำ" เทียบกับพจนานุกรมปัจจุบัน
#          แผน 29 พ.ย.: เพิ่มคำสะกดไทยของทักษะภาษาอังกฤษ, รูปภาษาอังกฤษของ soft skill และ React Native
# ดึงข้อมูลจาก: evaluation/skill_dictionary_before_2026-11-29.csv (พจนานุกรมก่อนเพิ่มคำ), nlp_service/skill_dictionary.csv (หลัง)
#              sample-data/dictionary_cases.csv (12 ประโยคที่ใช้คำใหม่), ชุดจำลองเดิม, ประโยคคำปฏิเสธ, eval-real (ถ้ามี)
# รัน: cd nlp-service && venv/Scripts/python evaluation/run_eval_dictionary.py [ไฟล์ผลลัพธ์.md]
# ====
import csv
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.path.insert(0, str(HERE))
from nlp_service import skill_extractor  # noqa: E402
from run_eval_rules import DATA, negation_set, real_set, score, split, synthetic_set  # noqa: E402

BEFORE = HERE / "skill_dictionary_before_2026-11-29.csv"


def dictionary_set():
    with open(DATA / "dictionary_cases.csv", encoding="utf-8", newline="") as f:
        return [(f"dictionary {r['id']}", r["text"], split(r["expected_skills"])) for r in csv.DictReader(f)]


def use_dictionary(path):
    """สลับไฟล์พจนานุกรมที่ตัวสกัดอ่าน แล้วล้าง cache ให้โหลดใหม่"""
    skill_extractor.SKILLS_FILE = path
    skill_extractor.load_skills.cache_clear()
    skill_extractor._trie.cache_clear()
    skill_extractor._latin_pattern.cache_clear()


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    after = skill_extractor.SKILLS_FILE
    sets = [("ประโยคใช้คำใหม่ (12 ข้อ)", dictionary_set()), ("ชุดจำลองเดิม (เรซูเม่ 10 + ประกาศ 10)", synthetic_set()),
            ("ประโยคคำปฏิเสธ (20 ข้อ)", negation_set())]
    real = real_set()
    if real:
        sets.append((f"เรซูเม่ภายนอก eval-real ({len(real)} ฉบับ)", real))
    lines = ["# ผลวัดการเพิ่มคำในพจนานุกรม 29 พ.ย. (ก่อน-หลัง)\n",
             "ก่อน = `evaluation/skill_dictionary_before_2026-11-29.csv`, หลัง = `nlp_service/skill_dictionary.csv` (เปิดกฎคำปฏิเสธทั้งสองรุ่น)\n",
             "| ชุดข้อมูล | รุ่น | TP | FP | FN | Precision % | Recall % | F1 % |",
             "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    for name, items in sets:
        for label, path in (("ก่อนเพิ่มคำ", BEFORE), ("หลังเพิ่มคำ", after)):
            use_dictionary(path)
            (tp, fp, fn, p, r, f1), _ = score(items, True)
            lines.append(f"| {name} | {label} | {tp} | {fp} | {fn} | {p:.1f} | {r:.1f} | {f1:.1f} |")
    use_dictionary(after)
    lines += ["\n## คำที่เพิ่ม\n",
              "- ทักษะใหม่: React Native, macOS, Canva",
              "- คำสะกดไทย: จาวา, จาวาสคริปต์, เอ็กเซล/เอกเซล, ด็อกเกอร์, ลินุกซ์/ลีนุกซ์, เอสคิวแอล, ฟิกม่า, โฟโต้ช็อป, ฟลัตเตอร์, รีแอค/รีแอคท์",
              "- soft skill ภาษาอังกฤษ/ไทยเพิ่ม: leadership skills, ความเป็นผู้นำ, teamwork skills, ทำงานร่วมกันเป็นทีม,"
              " responsibility, presentation skills, public speaking, พูดในที่สาธารณะ",
              "- Software Testing: quality assurance, qa engineer, software tester",
              "- ลองแล้วเอาออก: collaboration (ไปจับ Collaboration Engine), team lead (เป็นชื่อตำแหน่ง) ทำให้ Precision ของ eval-real ลด"]
    text = "\n".join(lines) + "\n"
    print(text)
    if out:
        out.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
