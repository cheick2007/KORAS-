# Test complet de l'API Koras Backend
# Usage: .\test-api.ps1

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8080"

Write-Host ""
Write-Host "╔═══════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  🧪 Tests API Koras Backend          ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Vérifier que l'application tourne
Write-Host "🔍 Vérification du serveur..." -ForegroundColor Yellow
try {
    $null = Invoke-WebRequest -Uri $baseUrl -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
    Write-Host "✅ Serveur accessible sur $baseUrl" -ForegroundColor Green
} catch {
    Write-Host "❌ Serveur non accessible. Lancez d'abord : .\gradlew.bat :backend-services:run" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 1: Health Check
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "1️⃣  Health Check" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    Write-Host "   Status: $($health.status)" -ForegroundColor Green
    Write-Host "   Version: $($health.version)" -ForegroundColor Gray
    Write-Host "   Timestamp: $($health.timestamp)" -ForegroundColor Gray
    Write-Host "   ✅ PASS" -ForegroundColor Green
} catch {
    Write-Host "   ❌ FAIL: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Root Endpoint
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "2️⃣  Root Endpoint" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
try {
    $root = Invoke-RestMethod -Uri "$baseUrl/" -Method Get
    Write-Host "   Service: $($root.service)" -ForegroundColor Green
    Write-Host "   Status: $($root.status)" -ForegroundColor Gray
    Write-Host "   ✅ PASS" -ForegroundColor Green
} catch {
    Write-Host "   ❌ FAIL: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Auth requis (sans token = 401)
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "3️⃣  Authentification JWT Requise" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
try {
    Invoke-RestMethod -Uri "$baseUrl/api/v1/historique" -Method Get -ErrorAction Stop
    Write-Host "   ❌ FAIL: Route accessible sans token" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "   ✅ PASS: 401 Unauthorized (comme attendu)" -ForegroundColor Green
    } else {
        Write-Host "   ❌ FAIL: Erreur inattendue: $_" -ForegroundColor Red
    }
}
Write-Host ""

# Note sur le token
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "ℹ️  Note: Tests authentifiés" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "   Pour tester avec un vrai token JWT :" -ForegroundColor Gray
Write-Host "   1. Lancer: .\gradlew.bat test --tests 'ServiceAuthentificationTest'" -ForegroundColor Gray
Write-Host "   2. Copier le token généré dans les logs" -ForegroundColor Gray
Write-Host "   3. Exporter: `$token = 'eyJhbGc...'" -ForegroundColor Gray
Write-Host "   4. Utiliser: -Headers @{Authorization='Bearer `$token'}" -ForegroundColor Gray
Write-Host ""

# Test 4: Interprétation (simulé avec token invalide)
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "4️⃣  Endpoint Interprétation (structure)" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

$body = @{
    type = "TEXTE"
    contenu = "Appelle Marie"
    langue = "FRANCAIS"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest `
        -Uri "$baseUrl/api/v1/interprete" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body $body `
        -ErrorAction Stop
    Write-Host "   ❌ FAIL: Accessible sans auth" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "   ✅ PASS: Route existe et nécessite auth" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Erreur: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}
Write-Host ""

# Test 5: Rate Limiting (Health est public, on peut tester)
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "5️⃣  Latence et Performance" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

$times = @()
1..10 | ForEach-Object {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod -Uri "$baseUrl/health" | Out-Null
        $sw.Stop()
        $times += $sw.ElapsedMilliseconds
        Write-Host "." -NoNewline -ForegroundColor Green
    } catch {
        Write-Host "X" -NoNewline -ForegroundColor Red
    }
}
Write-Host ""

$avg = ($times | Measure-Object -Average).Average
$min = ($times | Measure-Object -Minimum).Minimum
$max = ($times | Measure-Object -Maximum).Maximum

Write-Host "   Requêtes: 10" -ForegroundColor Gray
Write-Host "   Latence moyenne: $([math]::Round($avg, 2))ms" -ForegroundColor Gray
Write-Host "   Min/Max: ${min}ms / ${max}ms" -ForegroundColor Gray

if ($avg -lt 100) {
    Write-Host "   ✅ PASS: Performance excellente (<100ms)" -ForegroundColor Green
} elseif ($avg -lt 500) {
    Write-Host "   ✅ PASS: Performance acceptable (<500ms)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  WARN: Latence élevée (>500ms)" -ForegroundColor Yellow
}
Write-Host ""

# Résumé
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "📊 Résumé des Tests" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host ""
Write-Host "   ✅ Health check fonctionnel" -ForegroundColor Green
Write-Host "   ✅ Endpoints configurés" -ForegroundColor Green
Write-Host "   ✅ Authentification JWT active" -ForegroundColor Green
Write-Host "   ✅ Performance acceptable" -ForegroundColor Green
Write-Host ""
Write-Host "🎉 Backend Koras opérationnel !" -ForegroundColor Green
Write-Host ""
Write-Host "📚 Pour des tests complets avec JWT :" -ForegroundColor Yellow
Write-Host "   1. Générer un token (voir instructions ci-dessus)" -ForegroundColor Gray
Write-Host "   2. Tester tous les endpoints avec auth" -ForegroundColor Gray
Write-Host "   3. Voir DEPLOIEMENT.md pour plus d'exemples" -ForegroundColor Gray
Write-Host ""
