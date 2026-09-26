# Koras - Assistant Vocal Accessible 🎙️🤖

Koras est un assistant vocal intelligent et entièrement accessible, pensé pour les personnes aveugles, malvoyantes, ou toute personne souhaitant interagir avec son téléphone de manière 100% vocale. 

Il se distingue des autres assistants en étant conçu de A à Z avec un **Backend Kotlin/Java ultra-rapide** pour la compréhension du langage naturel (NLU) et une application **Flutter** agissant comme une interface flottante, fluide et parfaitement intégrée au système Android.

---

## 🚀 Fonctionnalités Clés

1. **Expérience "Bulle Flottante" (Style Siri)**
   - Contrairement aux applications classiques qui prennent tout l'écran, Koras s'ouvre de façon **transparente** en superposition au-dessus de vos applications actuelles. 
   - L'application peut être configurée comme **Assistant Numérique par défaut** sur Android. Un simple appui long sur le bouton Home ou un swipe depuis l'angle de votre écran fera surgir Koras de n'importe où !

2. **Lancement d'Applications Intelligents**
   - Dites simplement *"Ouvre WhatsApp"*, *"Lance YouTube"* ou *"Ouvre la calculatrice"*.
   - Koras cherche intelligemment dans votre liste d'applications (grâce à `device_apps` et un système de secours robuste) pour ouvrir la bonne application sans délai.

3. **Envoi de SMS en arrière-plan ("Fantôme")**
   - Exemple : *"Envoie un message à Jean : j'arrive dans 5 minutes"*.
   - Koras va chercher "Jean" dans vos contacts, écrire le message, et l'envoyer directement en arrière-plan sans même ouvrir votre application de messagerie.

4. **Appels Téléphoniques Vocaux**
   - Dites *"Appelle Maman"* et Koras lance directement l'appel via les Intents Android.

5. **Moteur d'Intelligence Artificielle NLU Personnalisé**
   - Le "cerveau" de Koras n'est pas un simple script. C'est un moteur NLU (Natural Language Understanding) écrit en Kotlin tournant sur un serveur.
   - Il utilise un système de parsing par expressions régulières extrêmement performant qui extrait à la fois l'**Intention** (ex: `OUVERTURE_APP`) et les **Entités** (ex: `whatsapp`).
   - Il supporte plus de 15 intentions (Alarme, Météo, Musique, Calendrier, SMS, Appels, etc.).

---

## 🏗️ Architecture du Projet

Le projet est divisé en deux grandes parties :

### 1. Application Mobile (Flutter)
- **Dossier :** `/mobile_app`
- **Rôle :** Interface utilisateur (micro, synthèse vocale, UI flottante) et interactions avec le matériel du téléphone (lancement d'applications, accès aux contacts, SMS, appels).
- **Technologies Clés :**
  - `speech_to_text` : Pour écouter et transcrire la voix en direct.
  - `flutter_tts` : Pour répondre vocalement avec une voix naturelle.
  - `flutter_contacts` : Pour fouiller intelligemment dans votre répertoire.
  - `device_apps` : Pour lancer d'autres applications.
  - `telephony` : Pour l'envoi de SMS en tâche de fond.

### 2. Cerveau IA / Backend (Kotlin + Spring Boot)
- **Dossier :** `/backend`
- **Rôle :** Comprendre le sens des phrases envoyées par l'application mobile et générer un plan d'action structuré au format JSON.
- **Technologies Clés :**
  - Architecture Hexagonale (Domaine, Infrastructure, Services).
  - Kotlin pur pour le traitement NLU (`ParserCommandes.kt`).
  - L'application mobile lui envoie le texte (ex: "Ouvre TikTok"), et le backend répond avec un JSON indiquant exactement ce qu'il faut faire (Intention : `OUVERTURE_APP`, Nom de l'app : `TikTok`).

---

## 🛠️ Comment l'installer et le tester ?

### Étape 1 : Lancer le Backend (Le Cerveau)
L'application mobile a besoin de communiquer avec le backend local (sur le même réseau Wi-Fi).
1. Ouvrez le dossier `/backend` avec IntelliJ IDEA.
2. Lancez l'application Kotlin/Spring Boot (ou via terminal : `./gradlew run`).
3. Notez l'adresse IP de votre ordinateur (ex: `192.168.1.5`).

### Étape 2 : Configurer et Lancer l'Application Mobile
1. Ouvrez le dossier `/mobile_app` avec VS Code ou Android Studio.
2. Assurez-vous que l'adresse IP dans `lib/main.dart` et `lib/screens/home_page.dart` correspond bien à l'adresse IP de votre ordinateur.
3. Connectez votre téléphone Android via câble USB ou Wi-Fi Debugging.
4. Lancez la compilation : `flutter run` ou `flutter build apk`.

### Étape 3 : Configurer Koras comme Assistant Principal
Pour profiter de l'expérience "Siri", Koras doit être l'assistant principal du téléphone.
- Ouvrez Koras, une bannière s'affichera en haut : **"Définissez Koras comme assistant principal..."**.
- Cliquez sur **"Configurer"**.
- Vous serez redirigé vers les paramètres cachés d'Android. Choisissez **Koras** comme "Application d'assistance numérique".
- Désormais, restez appuyé sur le bouton Home de votre Android, Koras s'ouvrira en superposition n'importe où !

---

## 🧠 Comment ça marche sous le capot ? (Exemple de flux)

1. **L'utilisateur** reste appuyé sur le bouton Home et dit : *"Ouvre WhatsApp"*.
2. **Flutter (`home_page.dart`)** enregistre la voix et la convertit en texte via `speech_to_text`.
3. **Flutter** envoie le texte "Ouvre WhatsApp" au **Backend Kotlin** via HTTP (`/api/v1/interprete`).
4. **Le Backend (`ParserCommandes.kt`)** lit la phrase, la fait passer dans ses Regex, et comprend que l'utilisateur veut faire une `OUVERTURE_APP` et extrait l'entité texte contenant "WhatsApp".
5. **Le Backend** renvoie un JSON clair au téléphone.
6. **Flutter (`home_page.dart`)** reçoit le JSON, déclenche la fonction locale `_launchApp("WhatsApp")`.
7. **Koras** annonce *"Ouverture de WhatsApp"* via synthèse vocale (`flutter_tts`) et Android ouvre l'application WhatsApp. 🚀
