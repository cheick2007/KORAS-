@echo off
title KORAS - Lancement Serveur et Tunnel
color 0A
cd /d "%~dp0"

echo ======================================================================
echo           DEMARRAGE AUTOMATIQUE DU SERVEUR KORAS ET DU TUNNEL
echo ======================================================================
echo.
echo [1/2] Lancement du Backend Kotlin (Ktor)...
start "KORAS [1/2] - Backend Kotlin" cmd /k "cd /d ""%~dp0backend"" && .\gradlew.bat :backend-services:run"

echo.
echo En attente du demarrage du backend (10 secondes)...
timeout /t 10 /nobreak >nul

echo.
echo [2/2] Lancement du Tunnel Cloudflare HTTPS...
start "KORAS [2/2] - Tunnel Public" cmd /k "cd /d ""%~dp0"" && .\cloudflared.exe tunnel --url http://localhost:8080"

echo.
echo ======================================================================
echo Les deux services sont lances dans des fenetres separees :
echo.
echo 1. Fenetre Backend : Fait tourner le moteur NLU et l'interpreteur.
echo 2. Fenetre Tunnel  : Affiche l'adresse publique HTTPS mondiale.
echo.
echo Reperez la ligne :
echo    https://xxxx.trycloudflare.com
echo.
echo Copiez cette adresse et collez-la dans l'application KORAS
echo (icone Parametres / Serveur Backend).
echo ======================================================================
echo.
pause
