@echo off
echo.
echo ========================================
echo   KORAS Backend - Tests
echo ========================================
echo.

cd /d %~dp0

echo Execution des tests...
echo.

gradlew.bat test

echo.
echo ========================================
echo   Tests termines !
echo ========================================
echo.
echo Rapport disponible dans:
echo   backend-services\build\reports\tests\test\index.html
echo.

pause
