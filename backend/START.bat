@echo off
echo.
echo ========================================
echo   KORAS Backend - Assistant Vocal
echo ========================================
echo.
echo Demarrage de l'application...
echo.

cd /d %~dp0

echo Verification Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java 17+ requis. Installez depuis: https://adoptium.net/
    pause
    exit /b 1
)

echo.
echo Compilation et lancement...
echo.

gradlew.bat :backend-services:run

pause
