# Guide de Déploiement - Koras Backend (Sans Docker)

## 🚀 Démarrage Rapide (Standalone)

### Prérequis
- **JDK 17+** : Télécharger depuis [Adoptium](https://adoptium.net/)
- **Gradle** : Pas nécessaire (wrapper inclus)

### 1. Vérifier Java

```powershell
java -version
# Doit afficher Java 17 ou supérieur
```

Si Java n'est pas installé :
```powershell
# Avec Chocolatey
choco install temurin17

# Ou télécharger manuellement depuis adoptium.net
```

### 2. Compiler le projet

```powershell
cd backend

# Générer le wrapper Gradle (première fois)
# Si Gradle est installé :
gradle wrapper --gradle-version 8.5

# Sinon, télécharger gradle-wrapper.jar manuellement et utiliser :
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

**L'application démarre sur http://localhost:8080** 🚀

### 4. Tester l'API

```powershell
# Health check
curl http://localhost:8080/health

# Ou avec PowerShell
Invoke-WebRequest -Uri http://localhost:8080/health
```

Réponse attendue :
```json
{
  "status": "UP",
  "timestamp": "2024-12-20T10:30:00Z",
  "version": "1.0.0"
}
```

## 🧪 Exécuter les Tests

### Tous les tests
```powershell
.\gradlew.bat test
```

### Tests de propriétés uniquement
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

## 🔐 Tester l'Authentification JWT

### Script PowerShell pour générer un token

Créer `generate-token.ps1` :
```powershell
# Utiliser l'API Kotlin pour générer un token
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

Pour les tests, j'ai créé un token hardcodé dans les tests :

```powershell
# Lancer les tests qui génèrent des tokens
.\gradlew.bat :backend-services:test --tests "ServiceAuthentificationTest"

# Les tokens seront affichés dans les logs
```

**Alternative** : Créer une route `/auth/login` temporaire :

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

## 📊 Tests End-to-End

### Script PowerShell automatisé

Créer `test-api.ps1` :
```powershell
# Test complet de l'API Koras

$baseUrl = "http://localhost:8080"

Write-Host "🧪 Tests API Koras" -ForegroundColor Green
Write-Host ""

# 1. Health Check
Write-Host "1️⃣ Health Check..." -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
Write-Host "✅ Status: $($health.status)" -ForegroundColor Green
Write-Host ""

# 2. Root endpoint
Write-Host "2️⃣ Root endpoint..." -ForegroundColor Cyan
$root = Invoke-RestMethod -Uri "$baseUrl/" -Method Get
Write-Host "✅ Service: $($root.service)" -ForegroundColor Green
Write-Host ""

# 3. Générer token (nécessite route /auth/login)
Write-Host "3️⃣ Génération token..." -ForegroundColor Cyan
try {
    $tokenResponse = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post
    $token = $tokenResponse.accessToken
    Write-Host "✅ Token généré: $($token.Substring(0,20))..." -ForegroundColor Green
} catch {
    Write-Host "⚠️ Route /auth/login non disponible (ajouter route temporaire)" -ForegroundColor Yellow
    $token = "TOKEN_DE_TEST"
}
Write-Host ""

# 4. Interprétation texte
Write-Host "4️⃣ Interprétation texte..." -ForegroundColor Cyan
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
    
    Write-Host "✅ Intention détectée: $($interpretation.intention.type)" -ForegroundColor Green
    Write-Host "   Confiance: $($interpretation.confiance)%" -ForegroundColor Gray
} catch {
    Write-Host "❌ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# 5. Test Rate Limiting
Write-Host "5️⃣ Test Rate Limiting (15 requêtes)..." -ForegroundColor Cyan
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
Write-Host "✅ Requêtes réussies: $success" -ForegroundColor Green
Write-Host "🛑 Requêtes limitées: $limited" -ForegroundColor Yellow
Write-Host ""

Write-Host "✅ Tests terminés !" -ForegroundColor Green
```

Exécuter :
```powershell
.\test-api.ps1
```

## 🔧 Configuration Sans Docker

L'application fonctionne **en mémoire** sans base de données externe :

- **Cache idempotence** : HashMap en mémoire
- **Store préférences** : HashMap chiffré
- **Journal audit** : Liste en mémoire
- **Rate limiting** : ConcurrentHashMap
- **Blacklist JWT** : Set en mémoire

### Mode Mémoire vs Production

| Composant | Mode Mémoire (MVP) | Production |
|-----------|-------------------|------------|
| Cache idempotence | HashMap | Redis |
| Store préférences | HashMap | PostgreSQL |
| Journal audit | ArrayList | PostgreSQL |
| Rate limiting | HashMap | Redis |
| Blacklist JWT | Set | Redis |

**Avantage** : Aucune dépendance externe, testable immédiatement !

## 📝 Exemples d'Utilisation

### 1. Interprétation Texte

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

### 2. Exécution Plan

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

### 4. Préférences

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

## 🎯 Tests de Performance

### Script de charge simple

```powershell
# test-load.ps1
param(
    [int]$requests = 100,
    [int]$concurrent = 10
)

$url = "http://localhost:8080/health"
$times = @()

Write-Host "🚀 Test de charge: $requests requêtes ($concurrent parallèles)" -ForegroundColor Cyan

$jobs = 1..$requests | ForEach-Object {
    Start-Job -ScriptBlock {
        param($url)
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        Invoke-RestMethod -Uri $url | Out-Null
        $sw.Stop()
        $sw.ElapsedMilliseconds
    } -ArgumentList $url
    
    # Limiter le nombre de jobs parallèles
    if ((Get-Job -State Running).Count -ge $concurrent) {
        Get-Job | Wait-Job -Any | Out-Null
    }
}

# Attendre tous les jobs
$jobs | Wait-Job | Out-Null

# Collecter résultats
$times = $jobs | Receive-Job
$jobs | Remove-Job

# Statistiques
$avg = ($times | Measure-Object -Average).Average
$min = ($times | Measure-Object -Minimum).Minimum
$max = ($times | Measure-Object -Maximum).Maximum
$p95 = $times | Sort-Object | Select-Object -Index ([int]($times.Count * 0.95))

Write-Host ""
Write-Host "📊 Résultats:" -ForegroundColor Green
Write-Host "   Requêtes: $requests"
Write-Host "   Moyenne: $([math]::Round($avg, 2))ms"
Write-Host "   Min: ${min}ms"
Write-Host "   Max: ${max}ms"
Write-Host "   P95: ${p95}ms"
```

Exécuter :
```powershell
.\test-load.ps1 -requests 100 -concurrent 10
```

## 🚨 Troubleshooting

### Port 8080 déjà utilisé

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

# Option 2: Télécharger gradle-wrapper.jar
# Aller sur https://gradle.org/releases/
# Télécharger gradle-8.5-bin.zip
# Extraire dans C:\gradle
# Ajouter C:\gradle\bin au PATH
```

### Tests échouent

```powershell
# Nettoyer et recompiler
.\gradlew.bat clean build

# Tests avec logs détaillés
.\gradlew.bat test --info

# Test spécifique
.\gradlew.bat test --tests "ServiceAuthentificationTest"

# Voir les erreurs
type backend-services\build\reports\tests\test\index.html
```

### Application ne démarre pas

```powershell
# Vérifier Java
java -version

# Vérifier compilation
.\gradlew.bat :backend-services:classes

# Voir les logs détaillés
.\gradlew.bat :backend-services:run --info
```

## 📁 Structure du Projet

```
backend/
├── domaine/                           # Modèles métier
│   └── src/main/kotlin/
│       └── com/koras/assistantvocal/domaine/
│           ├── TypeIntention.kt       (28 types)
│           ├── Langue.kt              (8 langues)
│           ├── EntiteNLU.kt           (5 types)
│           └── ...
│
├── backend-services/                  # Services et API
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   │   └── com/koras/assistantvocal/
│       │   │       ├── Application.kt        # Point d'entrée ⭐
│       │   │       └── services/
│       │   │           ├── parsing/          (ParserCommandes, Formateur)
│       │   │           ├── nlu/              (ServiceNLU, Edge, Cloud)
│       │   │           ├── orchestration/    (OrchestrateurdeTaches)
│       │   │           ├── execution/        (ExecuteurSecurise, Journal)
│       │   │           ├── stockage/         (StoreMemoire)
│       │   │           └── gateway/          (API, JWT, RateLimiter)
│       │   └── resources/
│       │       ├── application.conf          # Config Ktor
│       │       └── logback.xml              # Config logs
│       └── test/                            # Tests (14 suites)
│
├── infrastructure/                    # (Vide pour MVP)
├── build.gradle.kts                  # Config Gradle racine
├── settings.gradle.kts               # Modules
├── gradlew.bat                       # Wrapper Windows ⭐
└── DEPLOIEMENT.md                    # Ce fichier
```

## ✅ Checklist de Test

- [ ] Compiler sans erreur : `.\gradlew.bat build`
- [ ] Tous les tests passent : `.\gradlew.bat test`
- [ ] Application démarre : `.\gradlew.bat :backend-services:run`
- [ ] Health check répond : `curl http://localhost:8080/health`
- [ ] Endpoints protégés nécessitent JWT
- [ ] Rate limiting fonctionne (10 req/min sur /interprete)
- [ ] Interprétation texte fonctionne
- [ ] Exécution plan fonctionne
- [ ] Historique audit fonctionne
- [ ] Préférences GET/PUT fonctionnent

## 🎓 Commandes Utiles

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

# Tests de propriétés
.\gradlew.bat test --tests "*PropertiesTest"

# Build info
.\gradlew.bat projects
.\gradlew.bat tasks

# Dépendances
.\gradlew.bat dependencies

# Version
.\gradlew.bat --version
```

## 📚 Documentation

- **Architecture** : Voir `STATUS.md`
- **Sessions** : Voir `SESSION1.md` à `SESSION5.md`
- **API** : Routes dans `GatewayAPI.kt`
- **Tests** : Suites dans `backend-services/src/test/`

## 🚀 Déploiement Production (Futur)

Quand vous serez prêt pour la production :

1. **Migrer vers PostgreSQL** : Implémenter `JournalAuditPostgreSQL`
2. **Ajouter Redis** : Pour cache et rate limiting
3. **HTTPS/TLS** : Certificat Let's Encrypt
4. **JWT RSA** : Remplacer HMAC par RSA-256
5. **Monitoring** : Prometheus + Grafana
6. **CI/CD** : GitHub Actions
7. **Load Balancer** : Nginx ou cloud (AWS/Azure/GCP)

---

**Prêt à tester ! Aucune dépendance externe nécessaire.** 🎉

Lancez simplement :
```powershell
cd backend
.\gradlew.bat :backend-services:run
```

Puis testez :
```powershell
curl http://localhost:8080/health
```
