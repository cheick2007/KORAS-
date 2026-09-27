# KORAS — L'Assistant Vocal Hybride & Accessible de Nouvelle Génération

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.7-087CFA?style=for-the-badge&logo=ktor&logoColor=white)](https://ktor.io/)
[![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B?style=for-the-badge&logo=flutter&logoColor=white)](https://flutter.dev/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Architecture](https://img.shields.io/badge/Architecture-DDD_Hexagonale-10B981?style=for-the-badge)]()
[![License](https://img.shields.io/badge/License-Proprietary-8B5CF6?style=for-the-badge)]()

> **KORAS** est un assistant vocal intelligent conçu pour libérer l'interaction mobile des silos applicatifs traditionnels. En combinant un moteur sémantique réactif (NLU) et une interface fluide en surcouche système (*Overlay flottant transparent*), KORAS permet d'exécuter des actions profondes directement dans les applications du quotidien (WhatsApp, YouTube, Téléphonie, SMS) par la voix, les mains libres et sans friction.

---

## Sommaire

- [Pourquoi KORAS ? (La Problématique)](#-pourquoi-koras--la-problématique)
- [Innovations Clés & Spécificités](#-innovations-clés--spécificités)
- [Démarrage Rapide en 1 Commande (Docker)](#-démarrage-rapide-en-1-commande-docker)
- [Installation de l'Application Mobile](#-installation-de-lapplication-mobile)
- [Architecture Technique](#-architecture-technique)
- [Commandes Vocales Démonstratives](#-commandes-vocales-démonstratives)
- [Sécurité & Confidentialité](#-sécurité--confidentialité)

---

## Pourquoi KORAS ? (La Problématique)

Les assistants vocaux dominants du marché (Google Assistant, Siri, Alexa) souffrent de limites critiques :
1. **Monopole de l'écran :** Ils coupent brutalement la tâche active de l'utilisateur en monopolisant l'écran entier.
2. **Inaction dans les applications tierces :** Ils se contentent souvent de proposer une recherche web textuelle au lieu d'exécuter l'action requise (ex: pré-remplir un message dans WhatsApp ou lancer une vidéo précise).
3. **Fracture d'accessibilité :** Ils négligent les réalités linguistiques, syntaxiques et pratiques des marchés émergents et francophones, compliquant la vie des conducteurs, personnes malvoyantes et non-lecteurs.

**KORAS résout cette rupture :** Il transforme la parole en action applicative directe, avec une interface discrète, un arrêt automatique intelligent au silence et une résilience totale en ligne comme hors-ligne.

---

## Innovations Clés & Spécificités

| Innovation | Description |
| :--- | :--- |
| **Ergonomie Dual-Mode** | **Mode App Complète** pour l'historique et les réglages + **Mode Bulle Flottante Transparente** (Gemini-like) déclenchée via le bouton Home / geste d'assistance sans quitter l'app en cours. |
| **Passerelle Applicative Profonde** | Automatisation directe de **WhatsApp** (contact + message pré-rempli prêt à envoyer), requêtes ciblées **YouTube**, appels téléphoniques et SMS natifs. |
| **Écoute Continue & Auto-Stop (3s)** | Algorithme de détection automatique du silence : l'enregistrement se clôture de lui-même dès que l'utilisateur a fini de parler. |
| **Résilience Hybride (Cloud + On-Device)** | Fonctionne avec le backend sémantique haute performance **ET** intègre un moteur autonome de secours local (fonctionne 100% hors-ligne même sans serveur). |
| **Design Glassmorphic Sombre** | Interface moderne aux nuances **Mauve Électrique**, **Vert Émeraude / Menthe** et verre dépoli translucide (*BackdropFilter*). |

---

## Démarrage Rapide en 1 Commande (Docker)

Pour permettre au jury et aux évaluateurs de tester le backend KORAS instantanément sur n'importe quel système d'exploitation (**Windows, macOS, Linux**) sans installer Java ni Gradle :

### Prérequis
- [Docker](https://docs.docker.com/get-docker/) et [Docker Compose](https://docs.docker.com/compose/) installés.

### Lancement
À la racine du projet, exécutez simplement :

```bash
docker compose up --build -d
```

Le serveur backend KORAS démarre automatiquement sur le port `8080` :
- **URL locale :** `http://localhost:8080`
- **Vérification de santé :**
  ```bash
  curl -I http://localhost:8080/
  ```

Pour stopper le conteneur :
```bash
docker compose down
```

---

### Alternative : Lancement Local sans Docker

Si vous disposez de Java 17+ :

#### Sous Windows (1 clic) :
Double-cliquez sur le script fourni à la racine :
* `LANCER_KORAS.bat`

#### En ligne de commande (macOS / Linux / Windows) :
```bash
cd backend
./gradlew :backend-services:run
```

---

## Installation de l'Application Mobile

Le package d'installation Android prêt à l'emploi est compilé et situé à la racine du dépôt :
* **Fichier APK :** `KORAS_Release.apk` *(~50 Mo, signé release)*

### Options d'utilisation dans l'application :

1. **Option A — Mode Démo Autonome (Zéro Configuration) :**
   - Ouvrez l'application.
   - Cliquez sur **"Accès Immédiat Démo (Mode Autonome)"**.
   - Vous accédez directement à l'assistant : toutes les actions (WhatsApp, YouTube, Appels, SMS, etc.) fonctionnent en local directement sur le téléphone !

2. **Option B — Connexion au Serveur Backend :**
   - Sur l'écran de connexion, cliquez sur l'icône de réglages en haut à droite (ou sur le badge *"Serveur"*).
   - Indiquez l'URL de votre backend (ex: `http://192.168.x.x:8080` ou votre URL de tunnel Cloudflare / Render).
   - Identifiants de test pré-remplis :
     - **Email :** `test@koras.com`
     - **Mot de passe :** `password`

3. **Activation de la Bulle Flottante (Bouton Home) :**
   - Rendez-vous dans *Paramètres Android > Applications par défaut > Application d'assistance numérique*.
   - Sélectionnez **Koras**.
   - Désormais, un appui prolongé sur le bouton Home ou un swipe depuis l'angle déclenche la bulle flottante transparente au-dessus de n'importe quel écran.

---

## Architecture Technique

Le projet respecte les principes de la **Clean Architecture** et du **Domain-Driven Design (DDD)** :

```
KORAS-main/
├── backend/                         # Microservice Sémantique (Kotlin / Ktor)
│   ├── domaine/                     # Règles métier pures, entités, types d'intentions
│   ├── infrastructure/              # Adaptateurs, sécurité JWT, accès données
│   └── backend-services/            # Moteur NLU, analyseur syntaxique, API HTTP
├── mobile_app/                      # Client Mobile (Flutter 3.x)
│   ├── lib/
│   │   ├── config/api_config.dart   # Gestionnaire d'URL dynamique & persistance
│   │   ├── screens/home_page.dart   # Vue Dual-Mode (App & Bulle Overlay) + Moteur local
│   │   └── main.dart                # Interface de connexion Glassmorphic
│   └── android/                     # Intégration Intents profonds & Assistant Role
├── Dockerfile                       # Multi-stage build optimisé (Alpine / Temurin 17)
├── docker-compose.yml               # Déploiement en 1 commande
├── LANCER_KORAS.bat                 # Script de démarrage tout-en-un Windows
└── KORAS_Release.apk                # APK Android Release prête à installer
```

---

## Commandes Vocales Démonstratives

Voici quelques exemples de commandes vocales comprises et exécutées nativement par KORAS :

- **WhatsApp :**
  - *"Envoie un message WhatsApp à Moussa : je serai là à 14h."*
  - *"Écris sur WhatsApp à Papa que je rentre bientôt."*
- **YouTube :**
  - *"Ouvre YouTube et recherche Didi B."*
  - *"Joue les meilleurs tutoriels Flutter sur YouTube."*
  - *"Ouvre YouTube."*
- **Téléphonie & Communication :**
  - *"Appelle le service client."*
  - *"Envoie un SMS à Amina : peux-tu me rappeler ?"*
- **Système & Applications :**
  - *"Ouvre la calculatrice."*
  - *"Lance WhatsApp."*
- **Assistance Générale :**
  - *"Quelle heure est-il ?"*
  - *"Qui es-tu ?"*

---

## Sécurité & Confidentialité

- **Chiffrement de bout en bout :** Communications protégées par TLS 1.3 et authentification JWT.
- **Respect absolu de la vie privée :** Aucune conservation ni commercialisation d'empreintes vocales.
- **Contrôle utilisateur intégral :** Historique de discussion stocké localement de manière chiffrée (*SharedPreferences*), effaçable à tout instant d'un simple clic.

---

**Projet KORAS Technologies — 2026**
*Conçu pour l'accessibilité, l'autonomie et l'excellence technique.*
