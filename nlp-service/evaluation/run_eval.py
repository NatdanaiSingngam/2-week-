# ==== Workmatch · NLP · วัดผล (ข้อมูลจำลอง) ====
# หน้าที่: วัด Precision/Recall ของการสกัดทักษะ และความถูกต้องของคะแนนจับคู่
# ดึงข้อมูลจาก: sample-data/resumes/*.txt, sample-data/jobs.csv, sample-data/expected.csv, sample-data/pairs.csv
# เรียกใช้: nlp_service/skill_extractor.py, nlp_service/match_scorer.py
# ส่งผลไปที่: หน้าจอ หรือไฟล์ .md ที่ระบุ (เช่น evaluation/results_after_dictionary_update.md)
# ====
"""วัดผลการดึงทักษะและคะแนนจับคู่กับข้อมูลจำลองใน sample-data/

รัน:  cd nlp-service && python evaluation/run_eval.py [ไฟล์ผลลัพธ์.md]
ผลทุกตัวเลขมาจากการรันจริงเทียบกับ sample-data/expected.csv
"""
import csv
import sys
from pathlib import Path

# คอนโซล Windows ค่าเริ่มต้นไม่ใช่ UTF-8 พิมพ์ภาษาไทยแล้วล้ม (UnicodeEncodeError) จึงบังคับ UTF-8
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from nlp_service.match_scorer import match_skills  # noqa: E402
from nlp_service.skill_extractor import extract_skills  # noqa: E402

DATA = ROOT.parent / "sample-data"


def load_expected():
    exp = {}
    with open(DATA / "expected.csv", encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            exp[(r["type"], int(r["id"]))] = [s for s in r["expected_skills"].split("|") if s]
    return exp


def load_texts():
    texts = {}
    for i in range(1, 11):
        texts[("resume", i)] = (DATA / "resumes" / f"resume_{i:02d}.txt").read_text(encoding="utf-8")
    with open(DATA / "jobs.csv", encoding="utf-8", newline="") as f:
        for i, r in enumerate(csv.DictReader(f), 1):
            texts[("job", i)] = r["description"]
    return texts


def corpus(skill_sets):
    """สถิติคลังเอกสารสำหรับ TF-IDF: แต่ละทักษะ (ตัวพิมพ์เล็ก) อยู่ในกี่เอกสาร และจำนวนเอกสารทั้งหมด"""
    df = {}
    for skills in skill_sets.values():
        for k in {s.strip().lower() for s in skills if s.strip()}:
            df[k] = df.get(k, 0) + 1
    return df, len(skill_sets)


def prf(tp, fp, fn):
    p = tp / (tp + fp) if tp + fp else 0.0
    r = tp / (tp + fn) if tp + fn else 0.0
    f = 2 * p * r / (p + r) if p + r else 0.0
    return p * 100, r * 100, f * 100


def main():
    out_path = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    exp, texts = load_expected(), load_texts()
    got = {k: [s["name"] for s in extract_skills(t)] for k, t in texts.items()}

    lines = []
    add = lines.append
    add("# ผลวัดการดึงทักษะและการจับคู่ (ข้อมูลจำลอง)\n")

    # ---------- ดึงทักษะ ----------
    add("## 1. การดึงทักษะ\n")
    add("| รายการ | ถูก (TP) | เกิน (FP) | ตกหล่น (FN) | Precision % | Recall % |")
    add("| --- | --- | --- | --- | --- | --- |")
    tot = {"resume": [0, 0, 0], "job": [0, 0, 0]}
    details = []
    for key in sorted(texts, key=lambda k: (k[0] != "resume", k[1])):
        e, g = set(exp[key]), set(got[key])
        tp, fp, fn = len(e & g), len(g - e), len(e - g)
        p, r, _ = prf(tp, fp, fn)
        add(f"| {key[0]} {key[1]} | {tp} | {fp} | {fn} | {p:.1f} | {r:.1f} |")
        for i, v in enumerate((tp, fp, fn)):
            tot[key[0]][i] += v
        if fp or fn:
            details.append(f"- {key[0]} {key[1]}: เกิน = {sorted(g - e) or '-'} ; ตกหล่น = {sorted(e - g) or '-'}")
    allv = [tot["resume"][i] + tot["job"][i] for i in range(3)]
    add("")
    add("| กลุ่ม | TP | FP | FN | Precision % | Recall % | F1 % |")
    add("| --- | --- | --- | --- | --- | --- | --- |")
    for name, v in (("resume 10 ฉบับ", tot["resume"]), ("ประกาศงาน 10 ตำแหน่ง", tot["job"]), ("รวมทั้งหมด", allv)):
        p, r, f = prf(*v)
        add(f"| {name} | {v[0]} | {v[1]} | {v[2]} | {p:.1f} | {r:.1f} | {f:.1f} |")
    add("\nความคลาดเคลื่อนรายรายการ:\n")
    lines.extend(details or ["- ไม่มี"])

    # ---------- คะแนนจับคู่ 10 คู่ ----------
    add("\n## 2. คะแนนจับคู่ 10 คู่\n")
    add("คะแนนคาดหวัง = คำนวณด้วยสูตรเดียวกันจากทักษะใน expected.csv; คะแนนระบบ = จากทักษะที่ระบบดึงได้\n")
    add("| คู่ (resume-job) | คะแนนคาดหวัง | คะแนนระบบ | ผลต่าง | Cosine ระบบ |")
    add("| --- | --- | --- | --- | --- |")
    want_df, want_n = corpus(exp)
    have_df, have_n = corpus(got)
    errs = []
    with open(DATA / "pairs.csv", encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            ri, ji = int(r["resume_id"]), int(r["job_id"])
            want = match_skills(exp[("resume", ri)], exp[("job", ji)], want_df, want_n)
            have = match_skills(got[("resume", ri)], got[("job", ji)], have_df, have_n)
            d = have["score"] - want["score"]
            errs.append(abs(d))
            add(f"| {ri}-{ji} | {want['score']:.1f} | {have['score']:.1f} | {d:+.1f} | {have['cosine']:.1f} |")
    mae = sum(errs) / len(errs)
    add(f"\nค่าคลาดเคลื่อนสัมบูรณ์เฉลี่ย (MAE) = {mae:.1f} คะแนน, สูงสุด = {max(errs):.1f} คะแนน, ตรงเป๊ะ {sum(1 for e in errs if e < 0.05)} จาก {len(errs)} คู่")

    # ---------- อันดับ ----------
    add("\n## 3. ตำแหน่งงานที่ได้คะแนนสูงสุดของแต่ละ resume (10 resume x 10 ตำแหน่ง)\n")
    add("| resume | ตำแหน่งสูงสุดตามคำตอบ | ตำแหน่งสูงสุดตามระบบ | ตรงกัน |")
    add("| --- | --- | --- | --- |")
    agree = 0
    for ri in range(1, 11):
        want = {j: match_skills(exp[("resume", ri)], exp[("job", j)], want_df, want_n)["score"] for j in range(1, 11)}
        have = {j: match_skills(got[("resume", ri)], got[("job", j)], have_df, have_n)["score"] for j in range(1, 11)}
        wbest = sorted(j for j, s in want.items() if s == max(want.values()))
        hbest = sorted(j for j, s in have.items() if s == max(have.values()))
        ok = bool(set(wbest) & set(hbest))
        agree += ok
        add(f"| {ri} | {wbest} | {hbest} | {'ใช่' if ok else 'ไม่'} |")
    add(f"\nตรงกัน {agree} จาก 10 resume (นับว่าตรงถ้าตำแหน่งสูงสุดของระบบอยู่ในกลุ่มที่คะแนนสูงสุดเท่ากันตามคำตอบ)")

    # ---------- กรณีพิเศษ ----------
    add("\n## 4. กรณีพิเศษ\n")
    add("| กรณี | ข้อความ | ทักษะที่ระบบดึงได้ | ผลที่คาดหวัง | ผ่าน |")
    add("| --- | --- | --- | --- | --- |")
    cases = [
        ("CV ภาษาอังกฤษมีทักษะ", "Final-year computer science student. Experienced with Python, Django, REST API and Git. Good communication skills.",
         {"Python", "Django", "REST API", "Git"}, "ดึงได้ครบ 4 ทักษะ (ภาษาอังกฤษใช้ได้)"),
        ("CV ภาษาอังกฤษไม่มีทักษะ", "I enjoy reading books, traveling and playing football with my friends on weekends.",
         set(), "ไม่พบทักษะ (รายการว่าง ไม่ error)"),
        ("ข้อความว่าง/ช่องว่าง", "   ", set(), "ไม่พบทักษะ (รายการว่าง ไม่ error)"),
    ]
    for name, text_, want, expect in cases:
        g = {s["name"] for s in extract_skills(text_)}
        ok = "ใช่" if g == want else "ไม่"
        add(f"| {name} | {text_.strip()[:60] or '(ว่าง)'} | {sorted(g) or '-'} | {expect} | {ok} |")
    m1 = match_skills([], ["Python", "Git"])
    m2 = match_skills(["Python"], [])
    add("")
    add(f"- ผู้สมัครไม่มีทักษะ จับคู่กับงานที่ต้องการ Python, Git: score {m1['score']} cosine {m1['cosine']} ขาด {m1['missing']}")
    add(f"- งานไม่ระบุทักษะ: score {m2['score']} cosine {m2['cosine']} (ไม่หารด้วยศูนย์)")

    text = "\n".join(lines) + "\n"
    print(text)
    if out_path:
        out_path.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
