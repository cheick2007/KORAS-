# 🎙️ Koras Backend - Assistant Vocal Accessible

Backend Kotlin pour assistant vocal multilingue accessible, conçu pour les personnes malvoyantes et les communautés linguistiques africaines.

## ✨ Caractéristiques

### 🚀 Innovation
- **Edge-first NLU** : Interprétation locale < 500ms, fallback cloud
- **Multilingue** : 8 langues (FR, EN, AR, WO, BM, SW, LN, HT)
- **Idempotence stricte** : Cache 24h, retry avec backoff exponentiel
- **Sécurité** : JWT, chiffrement AES-256-GCM, audit immuable

### 📋 Fonctionnalités
- **28 intentions** : Appel, SMS, Email, Calendrier, Navigation, Paiement...
- **Orchestration intelligente** : Plans d'action avec préconditions
- **Confirmation vocale** : Actions sensibles nécessitent confirmation
- **Audit complet** : Hash chain SHA-256, intégrité vérifiable
- **Rate limiting** : Protection DDoS, quotas par utilisateur/endpoint

### 🏗️ Architecture

```
┌─────────────────┐
│   Client App    │  (Android/iOS)
└────────┬────────┘
         │ HTTPS
         ▼
┌─────────────────┐
│  Gateway API    │  (JWT, Rate Limiting)
│   Ktor Server   │
└────────┬────────┘
         │
    ┌────┴────┬────────────┬────────────┐
    │         │            │            │
    ▼         ▼            ▼            ▼
┌──────┐  ┌──────┐  ┌──────────┐  ┌──────────┐
│ NLU  │  │ Orch │  │ Executor │  │  Store   │
│ Edge │  │      │  │ Sécurisé │  │ Chiffré  │
└──────┘  └──────┘  └──────────┘  └──────────┘
```

## 🚀 Démarrage Rapide

### Prérequis
- **JDK 17+** ([Télécharger](https://adoptium.net/))
- Aucune autre dépendance ! (tout en mémoire)

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

**L'API démarre sur http://localhost:8080** 🎉

### Test Rapide

```powershell
# Health check
curl http://localhost:8080/health

# Réponse : {"status":"UP","version":"1.0.0"}
```

## 📊 Tests

### Exécuter tous les tests
```powershell
.\gradlew.bat test
```

**Résultats** :
- ✅ 10/10 propriétés validées
- ✅ 550+ scénarios testés
- ✅ Tests de propriétés (Property-Based Testing)

### Tests automatisés
```powershell
# Script de test complet
.\test-api.ps1
```

## 📚 Documentation

- **[DEPLOIEMENT.md](./DEPLOIEMENT.md)** : Guide complet de déploiement
- **[STATUS.md](./STATUS.md)** : État d'avancement, architecture, décisions
- **[SESSION1-5.md](./SESSION5.md)** : Historique développement

## 🔐 Sécurité

### Chiffrement
- **AES-256-GCM** : Données sensibles chiffrées
- **IV aléatoire** : 96 bits par opération
- **3 niveaux** : PUBLIQUE, CONFIDENTIELLE, CRITIQUE

### Authentification
- **JWT** : HMAC-SHA256 (MVP), RSA-SHA256 (production)
- **Rotation** : Refresh token one-time use
- **Révocation** : Blacklist en mémoire

### Intégrité
- **Hash chain** : SHA-256, blockchain-like
- **4 niveaux validation** : Hash, chaîne, preuve, temporalité
- **Audit immuable** : Détection corruption/antidatage

## 🎯 Endpoints API

### Public
- `GET /health` : Health check

### Authentifiés (JWT requis)
- `POST /api/v1/interprete` : Interprétation audio/texte
- `POST /api/v1/execute` : Exécution plan avec idempotence
- `GET /api/v1/historique` : Consultation audit
- `GET /api/v1/preferences` : Récupération préférences
- `PUT /api/v1/preferences` : Modification préférences

## 📈 Performance

- **Latence NLU** : < 500ms (edge)
- **Idempotence** : Cache 24h, O(1)
- **Rate limiting** : 10-100 req/min selon endpoint
- **Mémoire** : ~200MB (mode standalone)

## 🏆 Tests de Propriétés

Le backend est testé avec **Property-Based Testing** (Kotest) :

1. ✅ **Round-trip parsing** : parse(format(x)) = x
2. ✅ **Idempotence** : execute(N) = execute(1)
3. ✅ **Intégrité audit** : Hash chain inviolable
4. ✅ **Invariants structurels** : Plans valides
5. ✅ **Authentification** : JWT obligatoire
6. ✅ **Rate limiting** : Quotas respectés
7. ✅ **Entités** : Extraction complète
8. ✅ **Encryption** : encrypt(decrypt(x)) = x
9. ✅ **Confiance NLU** : Seuil 70%
10. ✅ **Conservation** : Toutes étapes comptées

## 🛠️ Technologies

- **Kotlin 1.9.21** : Langage principal
- **Ktor 2.3.7** : Framework serveur
- **Kotest 5.8.0** : Tests de propriétés
- **JWT (Auth0)** : Authentification
- **Kotlinx Serialization** : JSON
- **Coroutines** : Async/await
- **Gradle 8.5** : Build tool

## 📦 Modules

```
backend/
├── domaine/              # Modèles métier (11 fichiers)
├── backend-services/     # Services et API (23 fichiers)
└── infrastructure/       # (Futur: PostgreSQL, Redis)
```

**Total** : 37 fichiers, ~7,900 lignes

## 🎓 Approche Senior

### Tactique
- ✅ Sliding window rate limiting (pas de burst)
- ✅ One-time refresh tokens (détecte vols)
- ✅ Hash chain 4 niveaux (corruption + antidatage)
- ✅ IV aléatoire par encryption (sécurité maximale)

### Stratégique
- ✅ Architecture évolutive (HashMap → Redis transparent)
- ✅ Interfaces claires (migration facile)
- ✅ Mode mémoire MVP (aucune dépendance)
- ✅ Paths production documentés

### Professionnel
- ✅ Nommage français (lisibilité)
- ✅ Tests exhaustifs (550+ scénarios)
- ✅ Documentation complète
- ✅ Décisions expliquées

## 🚧 Roadmap

### MVP (Actuel) ✅
- [x] NLU edge-first
- [x] Orchestration intelligente
- [x] Exécution sécurisée
- [x] API REST + JWT
- [x] Tests complets

### Production (Futur)
- [ ] PostgreSQL (audit persistant)
- [ ] Redis (cache distribué)
- [ ] Monitoring Prometheus
- [ ] CI/CD GitHub Actions
- [ ] Kubernetes deployment
- [ ] HTTPS/TLS
- [ ] Load balancing

## 👥 Contribution

Le projet suit une approche **innovation senior** :
- Code modulaire et testable
- Décisions documentées
- Patterns tactiques et stratégiques
- Aucune erreur tolérée (tests stricts)

## 📄 Licence

[À définir]

## 🌍 Vision

Rendre la technologie vocale accessible à tous, en particulier :
- 👁️ Personnes malvoyantes
- 🌍 Communautés africaines (8 langues)
- 📱 Contextes à faible connectivité (edge-first)

---

**Développé avec ❤️ pour l'accessibilité universelle**

🚀 **Prêt à l'emploi ! Lancez : `.\gradlew.bat :backend-services:run`**
