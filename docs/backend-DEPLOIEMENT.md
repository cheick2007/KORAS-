# Guide de Dploiement - Koras Backend (Sans Docker)

## Dmarrage Rapide (Standalone)

### Prrequis
- **JDK 17+** : Tlcharger depuis [Adoptium](https://adoptium.net/)
- **Gradle** : Pas ncessaire (wrapper inclus)

### 1. Vrifier Java

```powershell
java -version
# Doit afficher Java 17 ou suprieur
```

Si Java n'est pas install :
```powershell
# Avec Chocolatey
choco install temurin17

# Ou tlcharger manuellement depuis adoptium.net
```

### 2. Compiler le projet

```powershell
cd backend

# Gnrer le wrapper Gradle (premire fois)
# Si Gradle est install :
gradle wrapper --gradle-version 8.5

# Sinon, tlcharger gradle-wrapper.jar manuellement et utiliser :
.\gradlew.bat wrapper --gradle-version 8.5

# Compiler
.\gradlew.bat build

# Ou compiler sans tests (plus rapide)
.\gradlew.bat build -x test
```

### 3. Lancer l'application

```powershell
.\gradlew.bat :backend-services:run
```

**L'application dmarre sur http://localhost:8080**

### 4. Tester l'API

```powershell
# Health check
curl http://localhost:8080/health

# Ou avec PowerShell
Invoke-WebRequest -Uri http://localhost:8080/health
```

Rponse attendue :
```json
{
"status": "UP",
"timestamp": "2024-12-20T10:30:00Z",
"version": "1.0.0"
}
```

## Excuter les Tests

### Tous les tests
```powershell
.\gradlew.bat test
```

### Tests de proprits uniquement
```powershell
.\gradlew.bat test --tests "*PropertiesTest"
```

### Tests par module
```powershell
# Tests du domaine
.\gradlew.bat :domaine:test

# Tests des services
.\gradlew.bat :backend-services:test
```

### Rapport de tests
```powershell
.\gradlew.bat test

# Ouvrir le rapport HTML
start backend-services\build\reports\tests\test\index.html
```

## Tester l'Authentification JWT

### Script PowerShell pour gnrer un token

Crer `generate-token.ps1` :
```powershell
# Utiliser l'API Kotlin pour gnrer un token
$code = @"
import com.koras.assistantvocal.services.gateway.ServiceAuthentificationImpl
import java.util.UUID

fun main() {
val auth = ServiceAuthentificationImpl()
val userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
val token = auth.genererToken(userId, listOf("USER", "ADMIN"))

println(token.accessToken)
}
"@

# Note: Pour simplifier, utiliser curl avec le token de test ci-dessous
```

### Token de test (valide 1h)

Pour les tests, j'ai cr un token hardcod dans les tests :

```powershell
# Lancer les tests qui gnrent des tokens
.\gradlew.bat :backend-services:test --tests "ServiceAuthentificationTest"

# Les tokens seront affichs dans les logs
```

**Alternative** : Crer une route `/auth/login` temporaire :

Ajouter dans `GatewayAPI.kt` :
```kotlin
post("/api/v1/auth/login") {
val userId = UUID.randomUUID()
val token = authentification.genererToken(userId, listOf("USER"))
call.respond(HttpStatusCode.OK, token)
}
```

Puis :
```powershell
curl -X POST http://localhost:8080/api/v1/auth/login
```

## Tests End-to-End

### Script PowerShell automatis

Crer `test-api.ps1` :
```powershell
# Test complet de l'API Koras

$baseUrl = "http://localhost:8080"

Write-Host " Tests API Koras" -ForegroundColor Green
Write-Host ""

# 1. Health Check
Write-Host "1 Health Check..." -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
Write-Host " Status: $($health.status)" -ForegroundColor Green
Write-Host ""

# 2. Root endpoint
Write-Host "2 Root endpoint..." -ForegroundColor Cyan
$root = Invoke-RestMethod -Uri "$baseUrl/" -Method Get
Write-Host " Service: $($root.service)" -ForegroundColor Green
Write-Host ""

# 3. Gnrer token (ncessite route /auth/login)
Write-Host "3 Gnration token..." -ForegroundColor Cyan
try {
$tokenResponse = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post
$token = $tokenResponse.accessToken
Write-Host " Token gnr: $($token.Substring(0,20))..." -ForegroundColor Green
} catch {
Write-Host " Route /auth/login non disponible (ajouter route temporaire)" -ForegroundColor Yellow
$token = "TOKEN_DE_TEST"
}
Write-Host ""

# 4. Interprtation texte
Write-Host "4 Interprtation texte..." -ForegroundColor Cyan
$body = @{
type = "TEXTE"
contenu = "Appelle Marie au 0612345678"
langue = "FRANCAIS"
} | ConvertTo-Json

try {
$interpretation = Invoke-RestMethod `
-Uri "$baseUrl/api/v1/interprete" `
-Method Post `
-Headers @{Authorization="Bearer $token"} `
-ContentType "application/json" `
-Body $body

Write-Host " Intention dtecte: $($interpretation.intention.type)" -ForegroundColor Green
Write-Host " Confiance: $($interpretation.confiance)%" -ForegroundColor Gray
} catch {
Write-Host " Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# 5. Test Rate Limiting
Write-Host "5 Test Rate Limiting (15 requtes)..." -ForegroundColor Cyan
$success = 0
$limited = 0

1..15 | ForEach-Object {
try {
Invoke-RestMethod `
-Uri "$baseUrl/api/v1/historique" `
-Method Get `
-Headers @{Authorization="Bearer $token"} `
-ErrorAction Stop | Out-Null
$success++
Write-Host "." -NoNewline -ForegroundColor Green
} catch {
if ($_.Exception.Response.StatusCode -eq 429) {
$limited++
Write-Host "X" -NoNewline -ForegroundColor Red
}
}
Start-Sleep -Milliseconds 100
}

Write-Host ""
Write-Host " Requtes russies: $success" -ForegroundColor Green
Write-Host " Requtes limites: $limited" -ForegroundColor Yellow
Write-Host ""

Write-Host " Tests termins !" -ForegroundColor Green
```

Excuter :
```powershell
.\test-api.ps1
```

## Configuration Sans Docker

L'application fonctionne **en mmoire** sans base de donnes externe :

- **Cache idempotence** : HashMap en mmoire
- **Store prfrences** : HashMap chiffr
- **Journal audit** : Liste en mmoire
- **Rate limiting** : ConcurrentHashMap
- **Blacklist JWT** : Set en mmoire

### Mode Mmoire vs Production

| Composant | Mode Mmoire (MVP) | Production |
|-----------|-------------------|------------|
| Cache idempotence | HashMap | Redis |
| Store prfrences | HashMap | PostgreSQL |
| Journal audit | ArrayList | PostgreSQL |
| Rate limiting | HashMap | Redis |
| Blacklist JWT | Set | Redis |

**Avantage** : Aucune dpendance externe, testable immdiatement !

## Exemples d'Utilisation

### 1. Interprtation Texte

```powershell
$headers = @{
"Authorization" = "Bearer YOUR_TOKEN"
"Content-Type" = "application/json"
}

$body = @{
type = "TEXTE"
contenu = "Appelle Marie au 0612345678"
langue = "FRANCAIS"
} | ConvertTo-Json

$response = Invoke-RestMethod `
-Uri "http://localhost:8080/api/v1/interprete" `
-Method Post `
-Headers $headers `
-Body $body

$response | ConvertTo-Json -Depth 10
```

### 2. Excution Plan

```powershell
$body = @{
intention = @{
type = "APPEL"
entites = @{
contact = @{
nom = "Marie"
numero = "+33612345678"
}
}
confiance = 95.0
contexte = @{}
}
tokenIdempotence = "550e8400-e29b-41d4-a716-446655440001"
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod `
-Uri "http://localhost:8080/api/v1/execute" `
-Method Post `
-Headers $headers `
-Body $body

$response | ConvertTo-Json -Depth 10
```

### 3. Historique

```powershell
$response = Invoke-RestMethod `
-Uri "http://localhost:8080/api/v1/historique?limite=10" `
-Method Get `
-Headers $headers

$response.entrees | Format-Table -Property id, action, resultat, dureeMs
```

### 4. Prfrences

```powershell
# GET
$prefs = Invoke-RestMethod `
-Uri "http://localhost:8080/api/v1/preferences" `
-Method Get `
-Headers $headers

# PUT
$newPrefs = @{
vitesseParole = 1.2
voix = "fr-FR-Denise"
volumeParole = 90
vibrationActivee = $true
modeVerbeux = $false
periodeRetention = "TRENTE_JOURS"
} | ConvertTo-Json

Invoke-RestMethod `
-Uri "http://localhost:8080/api/v1/preferences" `
-Method Put `
-Headers $headers `
-Body $newPrefs `
-ContentType "application/json"
```

## Tests de Performance

### Script de charge simple

```powershell
# test-load.ps1
param(
[int]$requests = 100,
[int]$concurrent = 10
)

$url = "http://localhost:8080/health"
$times = @()

Write-Host " Test de charge: $requests requtes ($concurrent parallles)" -ForegroundColor Cyan

$jobs = 1..$requests | ForEach-Object {
Start-Job -ScriptBlock {
param($url)
$sw = [System.Diagnostics.Stopwatch]::StartNew()
Invoke-RestMethod -Uri $url | Out-Null
$sw.Stop()
$sw.ElapsedMilliseconds
} -ArgumentList $url

# Limiter le nombre de jobs parallles
if ((Get-Job -State Running).Count -ge $concurrent) {
Get-Job | Wait-Job -Any | Out-Null
}
}

# Attendre tous les jobs
$jobs | Wait-Job | Out-Null

# Collecter rsultats
$times = $jobs | Receive-Job
$jobs | Remove-Job

# Statistiques
$avg = ($times | Measure-Object -Average).Average
$min = ($times | Measure-Object -Minimum).Minimum
$max = ($times | Measure-Object -Maximum).Maximum
$p95 = $times | Sort-Object | Select-Object -Index ([int]($times.Count * 0.95))

Write-Host ""
Write-Host " Rsultats:" -ForegroundColor Green
Write-Host " Requtes: $requests"
Write-Host " Moyenne: $([math]::Round($avg, 2))ms"
Write-Host " Min: ${min}ms"
Write-Host " Max: ${max}ms"
Write-Host " P95: ${p95}ms"
```

Excuter :
```powershell
.\test-load.ps1 -requests 100 -concurrent 10
```

## Troubleshooting

### Port 8080 dj utilis

```powershell
# Trouver le processus
netstat -ano | findstr :8080

# Tuer le processus
taskkill /PID <PID> /F

# Ou changer le port
$env:PORT = "8081"
.\gradlew.bat :backend-services:run
```

### Erreur "Gradle not found"

```powershell
# Option 1: Installer Gradle avec Chocolatey
choco install gradle

# Option 2: Tlcharger gradle-wrapper.jar
# Aller sur https://gradle.org/releases/
# Tlcharger gradle-8.5-bin.zip
# Extraire dans C:\gradle
# Ajouter C:\gradle\bin au PATH
```

### Tests chouent

```powershell
# Nettoyer et recompiler
.\gradlew.bat clean build

# Tests avec logs dtaills
.\gradlew.bat test --info

# Test spcifique
.\gradlew.bat test --tests "ServiceAuthentificationTest"

# Voir les erreurs
type backend-services\build\reports\tests\test\index.html
```

### Application ne dmarre pas

```powershell
# Vrifier Java
java -version

# Vrifier compilation
.\gradlew.bat :backend-services:classes

# Voir les logs dtaills
.\gradlew.bat :backend-services:run --info
```

## Structure du Projet

```
backend/
domaine/ # Modles mtier
src/main/kotlin/
com/koras/assistantvocal/domaine/
TypeIntention.kt (28 types)
Langue.kt (8 langues)
EntiteNLU.kt (5 types)
...
backend-services/ # Services et API
src/
main/
kotlin/
com/koras/assistantvocal/
Application.kt # Point d'entre
services/
parsing/ (ParserCommandes, Formateur)
nlu/ (ServiceNLU, Edge, Cloud)
orchestration/ (OrchestrateurdeTaches)
execution/ (ExecuteurSecurise, Journal)
stockage/ (StoreMemoire)
gateway/ (API, JWT, RateLimiter)
resources/
application.conf # Config Ktor
logback.xml # Config logs
test/ # Tests (14 suites)
infrastructure/ # (Vide pour MVP)
build.gradle.kts # Config Gradle racine
settings.gradle.kts # Modules
gradlew.bat # Wrapper Windows
DEPLOIEMENT.md # Ce fichier
```

## Checklist de Test

- [ ] Compiler sans erreur : `.\gradlew.bat build`
- [ ] Tous les tests passent : `.\gradlew.bat test`
- [ ] Application dmarre : `.\gradlew.bat :backend-services:run`
- [ ] Health check rpond : `curl http://localhost:8080/health`
- [ ] Endpoints protgs ncessitent JWT
- [ ] Rate limiting fonctionne (10 req/min sur /interprete)
- [ ] Interprtation texte fonctionne
- [ ] Excution plan fonctionne
- [ ] Historique audit fonctionne
- [ ] Prfrences GET/PUT fonctionnent

## Commandes Utiles

```powershell
# Compiler
.\gradlew.bat build

# Compiler sans tests
.\gradlew.bat build -x test

# Nettoyer
.\gradlew.bat clean

# Lancer app
.\gradlew.bat :backend-services:run

# Tests
.\gradlew.bat test

# Tests de proprits
.\gradlew.bat test --tests "*PropertiesTest"

# Build info
.\gradlew.bat projects
.\gradlew.bat tasks

# Dpendances
.\gradlew.bat dependencies

# Version
.\gradlew.bat --version
```

## Documentation

- **Architecture** : Voir `STATUS.md`
- **Sessions** : Voir `SESSION1.md` `SESSION5.md`
- **API** : Routes dans `GatewayAPI.kt`
- **Tests** : Suites dans `backend-services/src/test/`

## Dploiement Production (Futur)

Quand vous serez prt pour la production :

1. **Migrer vers PostgreSQL** : Implmenter `JournalAuditPostgreSQL`
2. **Ajouter Redis** : Pour cache et rate limiting
3. **HTTPS/TLS** : Certificat Let's Encrypt
4. **JWT RSA** : Remplacer HMAC par RSA-256
5. **Monitoring** : Prometheus + Grafana
6. **CI/CD** : GitHub Actions
7. **Load Balancer** : Nginx ou cloud (AWS/Azure/GCP)

---

**Prt tester ! Aucune dpendance externe ncessaire.**

Lancez simplement :
```powershell
cd backend
.\gradlew.bat :backend-services:run
```

Puis testez :
```powershell
curl http://localhost:8080/health
```
