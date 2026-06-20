@echo off
title Vortragsgenerator Webserver

rem Lade Umgebungsvariablen aus .env Datei (falls vorhanden)
if exist .env (
    echo Lade Umgebungsvariablen aus .env...
    for /f "usebackq tokens=*" %%i in (`findstr /v "^#" .env`) do set %%i
)

echo ===================================================
echo   Vortragsgenerator Webserver wird gestartet
echo ===================================================
echo.
echo Der Server startet im Hintergrund...
echo Der Browser wird in ca. 5 Sekunden automatisch geoeffnet.
echo.
start /b cmd /c "timeout /t 5 >nul && start http://localhost:8080"
mvn spring-boot:run
pause
