# ==== Workmatch · NLP · วัดผลกฎคำปฏิเสธ (ก่อน-หลัง) ====
# หน้าที่: วัด Precision/Recall/F1 ของการสกัดทักษะ โดยเทียบ "ก่อนแก้" (ปิดกฎคำปฏิเสธ) กับ "หลังแก้" (เปิดกฎ)
#          แผน 28 พ.ย. เกณฑ์เสร็จ: Precision ขึ้นเทียบก่อนแก้ และ Recall ไม่ลดมาก
# ดึงข้อมูลจาก: sample-data/negation_cases.csv (ประโยคคำปฏิเสธ 20 ข้อ), sample-data/resumes + jobs.csv + expected.csv (ชุดจำลองเดิม)
#              eval-real/ (เรซูเม่ภายนอก 20 ฉบับ ไม่อยู่ใน git: ถ้ามีจะรวมผลแบบสรุปเท่านั้น ไม่พิมพ์ข้อความเรซูเม่)
# รัน: cd nlp-service && venv/Scripts/python evaluation/run_eval_rules.py [ไฟล์ผลลัพธ์.md]
# ====
import csv
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from nlp_service.skill_extractor import extract_skills  # noqa: E402

DATA = ROOT.parent / "sample-data"
REAL = ROOT.parent / "eval-real"


def split(s):
    return [x for x in (s or "").split("|") if x]


def negation_set():
    with open(DATA / "negation_cases.csv", encoding="utf-8", newline="") as f:
        return [(f"negation {r['id']}", r["text"], split(r["expected_skills"])) for r in csv.DictReader(f)]


def synthetic_set():
    exp = {}
    with open(DATA / "expected.csv", encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            exp[(r["type"], int(r["id"]))] = split(r["expected_skills"])
    items = []
    for i in range(1, 11):
        items.append((f"resume {i}", (DATA / "resumes" / f"resume_{i:02d}.txt").read_text(encoding="utf-8"), exp[("resume", i)]))
    with open(DATA / "jobs.csv", encoding="utf-8", newline="") as f:
        for i, r in enumerate(csv.DictReader(f), 1):
            items.append((f"job {i}", r["description"], exp[("job", i)]))
    return items


def real_set():
    path = REAL / "expected_real.csv"
    if not path.exists():
        return []
    items = []
    with open(path, encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            key = next(k for k in r if k.lower() in ("id", "file", "resume"))
            name = r[key] if r[key].endswith(".txt") else f"{r[key]}.txt"
            exp_key = next(k for k in r if "expected" in k.lower() or "skills" in k.lower())
            text_path = REAL / "resumes" / name
            if text_path.exists():
                items.append((r[key], text_path.read_text(encoding="utf-8"), split(r[exp_key])))
    return items


def score(items, apply_negation):
    tp = fp = fn = 0
    rows = []
    for key, text, expected in items:
        got = {s["name"] for s in extract_skills(text, apply_negation)}
        e = set(expected)
        tp += len(e & got)
        fp += len(got - e)
        fn += len(e - got)
        rows.append((key, sorted(got - e), sorted(e - got)))
    p = tp / (tp + fp) * 100 if tp + fp else 0.0
    r = tp / (tp + fn) * 100 if tp + fn else 0.0
    f1 = 2 * p * r / (p + r) if p + r else 0.0
    return (tp, fp, fn, p, r, f1), rows


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    sets = [("ประโยคคำปฏิเสธ (20 ข้อ)", negation_set()), ("ชุดจำลองเดิม (เรซูเม่ 10 + ประกาศ 10)", synthetic_set())]
    real = real_set()
    if real:
        sets.append((f"เรซูเม่ภายนอก eval-real ({len(real)} ฉบับ)", real))

    lines = ["# ผลวัดกฎคำปฏิเสธและสิ่งที่อยากเรียน (ก่อน-หลัง)\n",
             "ก่อนแก้ = ปิดกฎ (`extract_skills(text, apply_negation=False)`), หลังแก้ = เปิดกฎ (ค่าเริ่มต้นของระบบ)\n",
             "| ชุดข้อมูล | รุ่น | TP | FP | FN | Precision % | Recall % | F1 % |",
             "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    detail = {}
    for name, items in sets:
        for label, flag in (("ก่อนแก้", False), ("หลังแก้", True)):
            (tp, fp, fn, p, r, f1), rows = score(items, flag)
            lines.append(f"| {name} | {label} | {tp} | {fp} | {fn} | {p:.1f} | {r:.1f} | {f1:.1f} |")
            detail[(name, label)] = rows

    lines.append("\n## ความคลาดเคลื่อนของประโยคคำปฏิเสธ (หลังแก้)\n")
    for key, extra, missing in detail[(sets[0][0], "หลังแก้")]:
        if extra or missing:
            lines.append(f"- {key}: เกิน = {extra or '-'} ; ตกหล่น = {missing or '-'}")
    text = "\n".join(lines) + "\n"
    print(text)
    if out:
        out.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
