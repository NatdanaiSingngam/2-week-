@echo off
chcp 65001 >nul
rem ============================================================
rem  Workmatch: start NLP service + Spring Boot (demo profile) in one command
rem  ใช้: ดับเบิลคลิกไฟล์นี้ หรือพิมพ์ start-demo.bat ที่โฟลเดอร์บนสุดของ repo
rem  ต้องมีก่อน: MySQL เปิดอยู่, ตั้ง DB_USER / DB_PASSWORD แล้ว, Python 3.10+, JDK 17+
rem  ถ้ามี venv ของ NLP อยู่ที่อื่น ให้ตั้งตัวแปร NLP_VENV (เช่น set NLP_VENV=D:\nlp-venv)
rem ============================================================
setlocal
cd /d "%~dp0"

if "%DB_USER%"=="" echo [!] ยังไม่ได้ตั้ง DB_USER / DB_PASSWORD ดู backend\README.md ขั้นตอนที่ 3 & pause & exit /b 1

rem ---- 1) NLP service (Python) ----
set "PY=%NLP_VENV%\Scripts\python.exe"
if "%NLP_VENV%"=="" set "PY=%~dp0nlp-service\venv\Scripts\python.exe"
if not exist "%PY%" (
  echo [1/3] สร้าง venv ของ NLP ครั้งแรก อาจใช้เวลาหลายนาที...
  py -3 -m venv "%~dp0nlp-service\venv" 2>nul || python -m venv "%~dp0nlp-service\venv"
  set "PY=%~dp0nlp-service\venv\Scripts\python.exe"
)
"%PY%" -m pip install -q -r "%~dp0nlp-service\requirements.txt"
echo [1/3] เปิด NLP service ที่ http://localhost:8000 ...
start "Workmatch NLP (close to stop)" /min /d "%~dp0nlp-service" "%PY%" -m uvicorn nlp_service.api_server:app --port 8000

rem ---- 2) wait for /health ----
echo [2/3] รอให้ NLP พร้อม...
set /a tries=0
:wait
curl -s http://localhost:8000/health | find "ok" >nul && goto ready
set /a tries+=1
if %tries% geq 60 echo [!] NLP ไม่ตอบภายใน 2 นาที ดูหน้าต่าง "Workmatch NLP" & pause & exit /b 1
timeout /t 2 /nobreak >nul
goto wait
:ready
echo       NLP พร้อมแล้ว

rem ---- 3) Spring Boot with demo data ----
echo [3/3] เปิด Spring Boot (profile demo) ... รอจนเห็น "Started WorkmatchApplication" แล้วเปิด http://localhost:8080
rem บังคับ locale อังกฤษให้ Maven (Windows ภาษาไทยใช้ปี พ.ศ. ทำให้ Maven บางคำสั่งล้ม)
set "MAVEN_OPTS=-Duser.language=en -Duser.country=US -Duser.region=US"
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
endlocal
