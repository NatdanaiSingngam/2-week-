# ==== Workmatch · NLP · API (FastAPI) ====
# หน้าที่: ประตูรับคำขอของ NLP service รันที่ http://localhost:8000 (เอกสาร API อัตโนมัติ /docs)
# รับคำขอจาก: backend (backend/.../nlp/NlpClient.java) เท่านั้น เบราว์เซอร์ไม่ได้เรียกตรง
# Endpoint: GET /health, POST /extract, POST /match, GET /dictionary, PUT /dictionary
# ส่งต่อไปที่: skill_extractor.py (สกัดทักษะ), match_scorer.py (คิดคะแนน)
# ข้อตกลงรูปแบบข้อมูล: nlp-service/api-contract.md
# ====
"""FastAPI service ของฝั่ง NLP: Spring Boot เรียก /extract และ /match
รัน: uvicorn nlp_service.api_server:app --port 8000   เอกสาร API อัตโนมัติ: http://localhost:8000/docs
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .skill_extractor import extract_skills, read_dictionary, write_dictionary
from .match_scorer import match_skills

app = FastAPI(title="Workmatch NLP service", version="0.1.0")


class ExtractIn(BaseModel):
    text: str = Field(..., description="ข้อความเรซูเม่หรือประกาศงาน")


class Skill(BaseModel):
    name: str
    category: str


class ExtractOut(BaseModel):
    skills: list[Skill]


class MatchIn(BaseModel):
    resume_skills: list[str] = Field(..., description="ทักษะของผู้สมัคร")
    job_skills: list[str] = Field(..., description="ทักษะที่ตำแหน่งงานต้องการ")


class MatchOut(BaseModel):
    score: float = Field(..., description="matched / required x 100")
    cosine: float = Field(..., description="Cosine similarity x 100 (ไว้เทียบสูตร)")
    matched: list[str]
    missing: list[str]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/extract", response_model=ExtractOut)
def extract(body: ExtractIn):
    return {"skills": extract_skills(body.text)}


@app.post("/match", response_model=MatchOut)
def match(body: MatchIn):
    return match_skills(body.resume_skills, body.job_skills)


# ---------- พจนานุกรมทักษะ (หน้าผู้ดูแลระบบ: Skill Dictionary / Synonyms ในเอกสาร รูปที่ 50-51) ----------
# backend (Spring Boot) เป็นต้นฉบับ: นำเข้าจาก GET ครั้งแรก แล้วส่งชุดเต็มมาที่ PUT ทุกครั้งที่ผู้ดูแลแก้ไข


class DictionaryItem(BaseModel):
    name: str
    category: str
    aliases: list[str] = Field(default_factory=list, description="คำพ้องความหมาย เช่น js -> JavaScript")


class DictionaryBody(BaseModel):
    skills: list[DictionaryItem]


@app.get("/dictionary", response_model=DictionaryBody)
def get_dictionary():
    return {"skills": read_dictionary()}


@app.put("/dictionary")
def put_dictionary(body: DictionaryBody):
    if not body.skills:
        raise HTTPException(status_code=422, detail="พจนานุกรมต้องมีอย่างน้อย 1 ทักษะ")
    try:
        count = write_dictionary([s.model_dump() for s in body.skills])
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    return {"status": "ok", "count": count}
