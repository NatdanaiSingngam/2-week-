#!/usr/bin/env bash
# Workmatch: start NLP service + Spring Boot (demo profile) in one command (macOS / Linux)
# ต้องมีก่อน: MySQL เปิดอยู่, export DB_USER / DB_PASSWORD, Python 3.10+, JDK 17+
set -euo pipefail
cd "$(dirname "$0")"
: "${DB_USER:?ยังไม่ได้ตั้ง DB_USER / DB_PASSWORD ดู backend/README.md ขั้นตอนที่ 3}"

VENV="${NLP_VENV:-nlp-service/venv}"
if [ ! -x "$VENV/bin/python" ]; then
  echo "[1/3] สร้าง venv ของ NLP ครั้งแรก..."
  python3 -m venv "$VENV"
fi
"$VENV/bin/python" -m pip install -q -r nlp-service/requirements.txt
echo "[1/3] เปิด NLP service ที่ http://localhost:8000 ..."
( cd nlp-service && exec "../$VENV/bin/python" -m uvicorn nlp_service.api_server:app --port 8000 ) &
NLP_PID=$!
trap 'kill $NLP_PID 2>/dev/null || true' EXIT

echo "[2/3] รอให้ NLP พร้อม..."
for i in $(seq 1 60); do
  if curl -s http://localhost:8000/health | grep -q ok; then echo "      NLP พร้อมแล้ว"; break; fi
  [ "$i" -eq 60 ] && { echo "[!] NLP ไม่ตอบภายใน 2 นาที"; exit 1; }
  sleep 2
done

echo "[3/3] เปิด Spring Boot (profile demo) ... แล้วเปิด http://localhost:8080"
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
