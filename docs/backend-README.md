# Koras Backend - Assistant Vocal Accessible

Backend Kotlin pour assistant vocal multilingue accessible, conu pour les personnes malvoyantes et les communauts linguistiques africaines.

## Caractristiques

### Innovation
- **Edge-first NLU** : Interprtation locale < 500ms, fallback cloud
- **Multilingue** : 8 langues (FR, EN, AR, WO, BM, SW, LN, HT)
- **Idempotence stricte** : Cache 24h, retry avec backoff exponentiel
- **Scurit** : JWT, chiffrement AES-256-GCM, audit immuable

### Fonctionnalits
- **28 intentions** : Appel, SMS, Email, Calendrier, Navigation, Paiement...
- **Orchestration intelligente** : Plans d'action avec prconditions
- **Confirmation vocale** : Actions sensibles ncessitent confirmation
- **Audit complet** : Hash chain SHA-256, intgrit vrifiable
- **Rate limiting** : Protection DDoS, quotas par utilisateur/endpoint

### Architecture

```
Client App (Android/iOS)
HTTPS
Gateway API (JWT, Rate Limiting)
Ktor Server
NLU Orch Executor Store
Edge Scuris Chiffr
```

## Dmarrage Rapide

### Prrequis
- **JDK 17+** ([Tlcharger](https://adoptium.net/))
- Aucune autre dpendance ! (tout en mmoire)

### Installation

```powershell
# 1. Cloner le repo
git clone https://github.com/votre-org/koras.git
cd koras/backend

# 2. Compiler
.\gradlew.bat build

# 3. Lancer
.\gradlew.bat :backend-services:run
```

**L'API dmarre sur http://localhost:8080**

### Test Rapide

```powershell
# Health check
curl http://localhost:8080/health

# Rponse : {"status":"UP","version":"1.0.0"}
```

## Tests

### Excuter tous les tests
```powershell
.\gradlew.bat test
```

**Rsultats** :
- 10/10 proprits valides
- 550+ scnarios tests
- Tests de proprits (Property-Based Testing)

### Tests automatiss
```powershell
# Script de test complet
.\test-api.ps1
```

## Documentation

- **[DEPLOIEMENT.md](./DEPLOIEMENT.md)** : Guide complet de dploiement
- **[STATUS.md](./STATUS.md)** : tat d'avancement, architecture, dcisions
- **[SESSION1-5.md](./SESSION5.md)** : Historique dveloppement

## Scurit

### Chiffrement
- **AES-256-GCM** : Donnes sensibles chiffres
- **IV alatoire** : 96 bits par opration
- **3 niveaux** : PUBLIQUE, CONFIDENTIELLE, CRITIQUE

### Authentification
- **JWT** : HMAC-SHA256 (MVP), RSA-SHA256 (production)
- **Rotation** : Refresh token one-time use
- **Rvocation** : Blacklist en mmoire

### Intgrit
- **Hash chain** : SHA-256, blockchain-like
- **4 niveaux validation** : Hash, chane, preuve, temporalit
- **Audit immuable** : Dtection corruption/antidatage

## Endpoints API

### Public
- `GET /health` : Health check

### Authentifis (JWT requis)
- `POST /api/v1/interprete` : Interprtation audio/texte
- `POST /api/v1/execute` : Excution plan avec idempotence
- `GET /api/v1/historique` : Consultation audit
- `GET /api/v1/preferences` : Rcupration prfrences
- `PUT /api/v1/preferences` : Modification prfrences

## Performance

- **Latence NLU** : < 500ms (edge)
- **Idempotence** : Cache 24h, O(1)
- **Rate limiting** : 10-100 req/min selon endpoint
- **Mmoire** : ~200MB (mode standalone)

## Tests de Proprits

Le backend est test avec **Property-Based Testing** (Kotest) :

1. **Round-trip parsing** : parse(format(x)) = x
2. **Idempotence** : execute(N) = execute(1)
3. **Intgrit audit** : Hash chain inviolable
4. **Invariants structurels** : Plans valides
5. **Authentification** : JWT obligatoire
6. **Rate limiting** : Quotas respects
7. **Entits** : Extraction complte
8. **Encryption** : encrypt(decrypt(x)) = x
9. **Confiance NLU** : Seuil 70%
10. **Conservation** : Toutes tapes comptes

## Technologies

- **Kotlin 1.9.21** : Langage principal
- **Ktor 2.3.7** : Framework serveur
- **Kotest 5.8.0** : Tests de proprits
- **JWT (Auth0)** : Authentification
- **Kotlinx Serialization** : JSON
- **Coroutines** : Async/await
- **Gradle 8.5** : Build tool

## Modules

```
backend/
domaine/ # Modles mtier (11 fichiers)
backend-services/ # Services et API (23 fichiers)
infrastructure/ # (Futur: PostgreSQL, Redis)
```

**Total** : 37 fichiers, ~7,900 lignes

## Approche Senior

### Tactique
- Sliding window rate limiting (pas de burst)
- One-time refresh tokens (dtecte vols)
- Hash chain 4 niveaux (corruption + antidatage)
- IV alatoire par encryption (scurit maximale)

### Stratgique
- Architecture volutive (HashMap Redis transparent)
- Interfaces claires (migration facile)
- Mode mmoire MVP (aucune dpendance)
- Paths production documents

### Professionnel
- Nommage franais (lisibilit)
- Tests exhaustifs (550+ scnarios)
- Documentation complte
- Dcisions expliques

## Roadmap

### MVP (Actuel)
- [x] NLU edge-first
- [x] Orchestration intelligente
- [x] Excution scurise
- [x] API REST + JWT
- [x] Tests complets

### Production (Futur)
- [ ] PostgreSQL (audit persistant)
- [ ] Redis (cache distribu)
- [ ] Monitoring Prometheus
- [ ] CI/CD GitHub Actions
- [ ] Kubernetes deployment
- [ ] HTTPS/TLS
- [ ] Load balancing

## Contribution

Le projet suit une approche **innovation senior** :
- Code modulaire et testable
- Dcisions documentes
- Patterns tactiques et stratgiques
- Aucune erreur tolre (tests stricts)

## Licence

[ dfinir]

## Vision

Rendre la technologie vocale accessible tous, en particulier :
- Personnes malvoyantes
- Communauts africaines (8 langues)
- Contextes faible connectivit (edge-first)

---

**Dvelopp avec pour l'accessibilit universelle**

**Prt l'emploi ! Lancez : `.\gradlew.bat :backend-services:run`**
