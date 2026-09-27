# Koras - Assistant Vocal Accessible

Koras est un assistant vocal intelligent et accessible, conu pour faciliter l'interaction avec le systme Android grce des commandes vocales. Il s'adresse notamment aux personnes souhaitant un usage main-libre ainsi qu'aux utilisateurs ayant des besoins spcifiques d'accessibilit.

Il s'appuie sur une architecture hybride : un backend performant en Kotlin/Java ddi la comprhension du langage naturel (NLU), et une application front-end dveloppe en Flutter agissant comme une interface systme fluide et intgre en surcouche du systme d'exploitation.

---

## Fonctionnalits Principales

1. **Exprience Utilisateur en Superposition**
   - L'application ne ncessite pas l'ouverture d'une vue plein cran. Koras s'active et s'affiche en superposition transparente par-dessus les applications en cours d'excution.
   - Il peut tre dfini comme Assistant Numrique par dfaut dans les paramtres d'Android. Une fois configur, un appui prolong sur le bouton d'accueil ou un geste depuis l'angle de l'cran invoque l'assistant.

2. **Lancement Intelligent d'Applications**
   - Rception de commandes telles que "Ouvre WhatsApp", "Lance YouTube" ou "Ouvre la calculatrice".
   - Le systme effectue une recherche contextuelle dans le registre des applications installes (via `device_apps`) et exploite un algorithme de secours pour garantir une rponse rapide et prcise.

3. **Envoi de SMS en Arrire-Plan**
   - Exemple : "Envoie un message  Jean : j'arrive dans 5 minutes."
   - Le systme rcupre les coordonnes du contact dans le rpertoire, rige le message et l'envoie en tche de fond sans ouvrir l'application de messagerie native.

4. **Appels Tlphoniques Vocaux**
   - Exemple : "Appelle Maman." Le systme dclenche directement l'appel tlphonique en exploitant les Intents Android.

5. **Moteur d'Intelligence Artificielle NLU Personnalis**
   - Le moteur NLU (Natural Language Understanding) est hberg sur le serveur Kotlin.
   - Il intgre un systme de parsing complexe capable d'extraire la fois l'Intention de l'utilisateur (par exemple, `OUVERTURE_APP`) et les Entits associes (par exemple, `whatsapp`).
   - Le moteur prend actuellement en charge plus de 15 intentions (Alarme, Mto, Musique, Calendrier, SMS, Appels, etc.).

---

## Architecture du Projet

Le projet est dcoup en deux modules principaux :

### 1. Application Mobile (Flutter)
- **Rpertoire :** `/mobile_app`
- **Rle :** Interface utilisateur (capture audio, synthse vocale, UI flottante) et couche d'interaction avec le matriel Android (accs aux contacts, gestion des tches d'arrire-plan, lancement d'applications).
- **Technologies employes :**
  - `speech_to_text` : Pour la transcription de la voix en texte en temps rel.
  - `flutter_tts` : Pour la gnration de la rponse vocale (Text-To-Speech).
  - `flutter_contacts` : Pour l'extraction et la recherche dans le rpertoire.
  - `device_apps` : Pour le rfrencement et l'ouverture des autres applications.
  - `telephony` : Pour la gestion et l'envoi de SMS en arrire-plan.

### 2. Backend / Moteur NLU (Kotlin + Spring Boot)
- **Rpertoire :** `/backend`
- **Rle :** Analyse et comprhension des phrases transmises par le client mobile, puis gnration d'un plan d'action dtaill sous format JSON.
- **Technologies employes :**
  - Architecture Hexagonale s'appuyant sur les principes du Domain-Driven Design (Domaine, Infrastructure, Services).
  - Implmentation du traitement NLU en Kotlin pur (`ParserCommandes.kt`).
  - Flux de donnes : le client envoie une requte textuelle (ex: "Ouvre TikTok"), et le backend rpond avec un objet JSON dcrivant l'action effectuer (Intention : `OUVERTURE_APP`, Entit : `TikTok`).

---

## Guide d'Installation et de Dploiement

### tape 1 : Dmarrage du Serveur Backend
L'application mobile requiert une connexion avec le backend local pour traiter les requtes (connexion sur le mme rseau Wi-Fi).
1. Ouvrez le dossier `/backend` avec IntelliJ IDEA ou l'diteur de votre choix.
2. Lancez le service Kotlin/Spring Boot (en ligne de commande : `./gradlew run`).
3. Rcuprez et notez l'adresse IP locale de votre machine (ex: `192.168.1.5`).

### tape 2 : Configuration et Dmarrage de l'Application Mobile
1. Ouvrez le dossier `/mobile_app` dans votre environnement de dveloppement (VS Code, Android Studio).
2. Modifiez la configuration rseau dans les fichiers `lib/main.dart` et `lib/screens/home_page.dart` afin que l'URL de l'API corresponde  l'adresse IP du serveur note l'tape prcdente.
3. Connectez un priphrique Android via USB ou Wi-Fi Debugging.
4. Lancez la compilation via la commande : `flutter run` ou gnrez l'APK avec `flutter build apk`.

### tape 3 : Configuration de l'Assistant par dfaut
Afin de bnficier de l'exprience utilisateur complte, l'application doit tre dfinie comme assistant par dfaut du systme.
- Lors du lancement de l'application, un bandeau de configuration s'affichera.
- Cliquez sur l'option de configuration.
- Dans les paramtres systme d'Android, slectionnez **Koras** comme "Application d'assistance numrique".
- L'assistant peut dsormais tre dclench depuis n'importe quel cran via une pression prolonge sur le bouton d'accueil.

---

## Dtail du Flux d'Excussion Technique

1. **Dclenchement** : L'utilisateur effectue une pression prolonge sur le bouton d'accueil et nonce sa requte : "Ouvre WhatsApp".
2. **Transcription** : Le module Flutter (`home_page.dart`) capture le flux audio et le convertit en texte.
3. **Transmission** : Le texte est envoy au serveur Backend via une requte HTTP POST (`/api/v1/interprete`).
4. **Analyse (NLU)** : Le Backend utilise les modles de parsing rguliers dfinis dans `ParserCommandes.kt` pour dterminer l'intention (`OUVERTURE_APP`) et extraire l'entit ("WhatsApp").
5. **Rponse** : Le serveur gnre un plan d'action JSON et le renvoie au client.
6. **Rception** : Flutter intercepte la rponse et dclenche la mthode native ddie `_launchApp("WhatsApp")`.
7. **Excussion** : L'interface signale la validation via le synthtiseur vocal et le systme Android lance l'application cible.
