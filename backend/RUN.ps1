# Script PowerShell pour lancer le backend Koras sans Gradle

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   KORAS Backend - Lancement Direct" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier Java
Write-Host "Vérification Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java non trouvé. Installez JDK 17+ depuis https://adoptium.net/" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Vérifier si Gradle est installé
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    Write-Host "✅ Gradle détecté. Compilation et lancement..." -ForegroundColor Green
    Write-Host ""
    
    # Compiler et lancer avec Gradle
    gradle :backend-services:run
} else {
    Write-Host "⚠️  Gradle non installé" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Installation rapide avec Chocolatey:" -ForegroundColor Cyan
    Write-Host "  1. Installer Chocolatey: https://chocolatey.org/install" -ForegroundColor Gray
    Write-Host "  2. Puis: choco install gradle" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Ou télécharger manuellement:" -ForegroundColor Cyan
    Write-Host "  https://gradle.org/releases/" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Après installation, relancez ce script." -ForegroundColor Yellow
}

Write-Host ""
