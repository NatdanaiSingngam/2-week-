"""FastAPI service ของฝั่ง NLP: Spring Boot เรียก /extract และ /match
รัน: uvicorn nlp_service.api_server:app --port 8000   เอกสาร API อัตโนมัติ: http://localhost:8000/docs
"""
from fastapi import FastAPI
from pydantic import BaseModel, Field

from .skill_extractor import extract_skills
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
