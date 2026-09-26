# Script d'installation de Gradle sans droits admin
# Installe Gradle dans le dossier utilisateur

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Installation Gradle (sans admin)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$gradleVersion = "8.5"
$gradleDir = "$env:USERPROFILE\.gradle-install"
$gradleBin = "$gradleDir\gradle-$gradleVersion\bin"
$gradleZip = "$env:TEMP\gradle-$gradleVersion-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"

# Vérifier si déjà installé
if (Test-Path "$gradleBin\gradle.bat") {
    Write-Host "✅ Gradle déjà installé dans : $gradleBin" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ajoutez au PATH si nécessaire :" -ForegroundColor Yellow
    Write-Host "`$env:Path += `";$gradleBin`"" -ForegroundColor Gray
    exit 0
}

Write-Host "📦 Téléchargement de Gradle $gradleVersion..." -ForegroundColor Yellow
Write-Host "   URL: $gradleUrl" -ForegroundColor Gray

try {
    # Télécharger avec TLS 1.2
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($gradleUrl, $gradleZip)
    
    Write-Host "✅ Téléchargement terminé" -ForegroundColor Green
    
    # Extraire
    Write-Host ""
    Write-Host "📂 Extraction dans : $gradleDir" -ForegroundColor Yellow
    
    if (!(Test-Path $gradleDir)) {
        New-Item -ItemType Directory -Path $gradleDir | Out-Null
    }
    
    Expand-Archive -Path $gradleZip -DestinationPath $gradleDir -Force
    
    Write-Host "✅ Extraction terminée" -ForegroundColor Green
    
    # Nettoyer
    Remove-Item $gradleZip -Force
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  ✅ Installation Réussie !" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Gradle installé dans : $gradleBin" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📋 Prochaines étapes :" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "1. Ajouter au PATH pour cette session :" -ForegroundColor White
    Write-Host "   `$env:Path += ';$gradleBin'" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "2. Vérifier :" -ForegroundColor White
    Write-Host "   gradle --version" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "3. Lancer le backend :" -ForegroundColor White
    Write-Host "   gradle :backend-services:run" -ForegroundColor Cyan
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "❌ Erreur lors du téléchargement" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Solution alternative :" -ForegroundColor Yellow
    Write-Host "1. Téléchargez manuellement depuis : https://gradle.org/releases/" -ForegroundColor Gray
    Write-Host "2. Extrayez dans : $gradleDir" -ForegroundColor Gray
    Write-Host "3. Ajoutez au PATH : `$env:Path += ';$gradleBin'" -ForegroundColor Gray
}

Write-Host ""
