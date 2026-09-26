# Assistant Vocal Accessible - Backend

Backend du système d'assistant vocal accessible, conçu avec une architecture edge-first modulaire en Kotlin.

## Architecture

Le projet est organisé en 3 modules Gradle :

- **`domaine`** : Modèles de domaine, types et interfaces métier
- **`backend-services`** : Services backend (NLU, Orchestrateur, Exécuteur, Gateway)
- **`infrastructure`** : Couche d'infrastructure (base de données, cache, cryptographie)

## Prérequis

- **JDK 17** ou supérieur
- **Gradle 8.5+** (ou utiliser le wrapper fourni)
- **PostgreSQL 14+** (pour le backend cloud)
- **Redis 7+** (pour le cache distribué)

## Installation

### 1. Initialiser le wrapper Gradle

Si Gradle n'est pas installé localement :

```bash
# Télécharger et installer Gradle depuis https://gradle.org/install/
# Ou utiliser SDKMan (Linux/Mac) ou Chocolatey (Windows)

# Windows (Chocolatey)
choco install gradle

# Linux/Mac (SDKMan)
sdk install gradle 8.5

# Générer le wrapper
gradle wrapper --gradle-version 8.5
```

### 2. Compiler le projet

```bash
./gradlew build
```

### 3. Exécuter les tests

```bash
./gradlew test
```

## Structure des modules

### Module `domaine`

Contient les types et modèles métier :

- `TypeIntention` : 28 types d'intentions vocales supportées
- `EntiteNLU` : Entités structurées (Contact, Temporel, Texte, Montant, Lieu)
- `PlanAction` et `Etape` : Représentation des plans d'exécution
- `ResultatExecution` et `PreuveExecution` : Résultats avec preuves cryptographiques
- `EntreeAudit` : Entrées du journal d'audit avec chaînage cryptographique
- `ContexteUtilisateur` et `PreferencesUtilisateur` : Personnalisation

### Module `backend-services`

Services backend :

- **ServiceNLU** : Interprétation des commandes vocales (edge + cloud)
- **OrchestrateurdeTaches** : Génération de plans d'actions
- **ExecuteurSecurise** : Exécution idempotente avec preuves
- **GatewayAPI** : Authentification JWT, routage, quotas
- **JournalAudit** : Journal immuable avec hash chain

### Module `infrastructure`

Couches d'infrastructure :

- **StoreMémoire** : Stockage chiffré AES-256
- **CacheIdempotence** : Cache Redis pour idempotence (24h TTL)
- **Cryptographie** : Signature RSA, chiffrement AES-256-GCM
- **Base de données** : Exposed ORM + PostgreSQL

## Fonctionnalités clés

### Edge-First

Le système privilégie l'exécution locale pour :
- NLU avec modèles embarqués (20 intentions prioritaires)
- Orchestration simple (actions directes)
- Exécution d'actions natives Android

Le cloud n'intervient que pour :
- NLU complexe (fallback si confiance < 70%)
- Orchestration multi-étapes
- Synchronisation et audit

### Idempotence

Toutes les actions utilisent des tokens d'idempotence (UUID v4) :
- Cache Redis avec TTL 24h
- Détection automatique de duplicats
- Résultat identique pour N exécutions

### Sécurité

- **Chiffrement** : AES-256-GCM pour données au repos
- **Signatures** : SHA-256-RSA pour preuves d'exécution
- **Hash chain** : Journal d'audit immuable
- **JWT** : Authentification avec refresh token rotation

### Property-Based Testing

Tests de propriétés avec Kotest (100+ itérations) :
- Round-trip parsing des intentions
- Idempotence stricte des exécutions
- Intégrité du hash chain d'audit
- Invariants structurels des plans d'actions

## Configuration

### Variables d'environnement

```bash
# Base de données
DATABASE_URL=postgresql://localhost:5432/assistant_vocal
DATABASE_USER=postgres
DATABASE_PASSWORD=secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=<générer_avec_openssl_rand_base64_32>
JWT_ISSUER=com.koras.assistantvocal
JWT_AUDIENCE=backend-api

# NLU Cloud (optionnel)
NLU_CLOUD_API_URL=https://api.openai.com/v1
NLU_CLOUD_API_KEY=<votre_clé>

# Cryptographie
CRYPTO_KEY_PATH=/secure/keys/private_key.pem
CRYPTO_PUBLIC_KEY_PATH=/secure/keys/public_key.pem
```

## Développement

### Linting et formatage

Le projet utilise ktlint pour le respect des conventions Kotlin :

```bash
# Vérifier le formatage
./gradlew ktlintCheck

# Appliquer le formatage automatiquement
./gradlew ktlintFormat
```

### Génération des clés cryptographiques

```bash
# Générer une paire de clés RSA 4096 bits
openssl genrsa -out private_key.pem 4096
openssl rsa -in private_key.pem -pubout -out public_key.pem

# Générer un secret JWT
openssl rand -base64 32
```

## Déploiement

### Docker

```bash
# Construire l'image
docker build -t assistant-vocal-backend .

# Lancer les services
docker-compose up -d
```

### Kubernetes

```bash
kubectl apply -f k8s/
```

## Tests

### Tests unitaires

```bash
./gradlew test
```

### Tests de propriétés

```bash
./gradlew test --tests "*PropertiesTest"
```

### Tests d'intégration

```bash
./gradlew integrationTest
```

### Couverture de code

```bash
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Performance

Objectifs :
- **Latence NLU** : < 500ms (P95)
- **Génération plan** : < 300ms (P95)
- **Throughput** : 1000 req/s concurrentes
- **Disponibilité** : 99.9% (avec mode hors ligne)

## Licence

Propriétaire - Koras

## Support

Pour toute question : support@koras.com
