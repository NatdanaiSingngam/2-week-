@echo off
rem Workmatch: run without MySQL. Data is kept in backend\data (H2 file database).
setlocal
set "DEMO_PROFILES=demo,h2"
set "DB_USER=sa"
call "%~dp0start-demo.bat"
endlocal
