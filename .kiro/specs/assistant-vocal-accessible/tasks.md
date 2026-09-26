# Plan d'Implémentation : Assistant Vocal Accessible

## Vue d'Ensemble

Ce plan décompose l'implémentation du système backend de l'assistant vocal accessible en tâches incrémentales. L'architecture privilégie une approche edge-first avec 8 composants modulaires, implémentés en Kotlin pour le backend et le client Android.

**Stratégie d'implémentation :**
1. Établir les fondations (modèles de domaine, infrastructure)
2. Implémenter les composants core (NLU, Orchestrateur, Exécuteur)
3. Ajouter la persistance et la sécurité (Store, Audit, Gateway)
4. Intégrer Android (Connecteur, Client)
5. Tests de propriétés et intégration

## Tâches

- [ ] 1. Initialiser la structure du projet et les dépendances
  - Créer un projet multi-module Gradle avec Kotlin
  - Modules : `domaine`, `backend-services`, `client-android`, `infrastructure`
  - Configurer les dépendances : Ktor (backend), Kotlin Coroutines, Kotest (tests de propriétés)
  - Configurer SQLCipher pour Android, PostgreSQL pour backend
  - Ajouter dépendances cryptographiques : Bouncy Castle
  - Configurer le linting (ktlint) et formatage du code
  - _Exigences : Toutes (infrastructure commune)_

- [ ] 2. Définir les modèles de domaine et types de base
  - [ ] 2.1 Créer les sealed classes et enums du domaine
    - Implémenter `TypeIntention` (20 types : APPEL, SMS, EMAIL, etc.)
    - Implémenter `Intention`, `EntiteNLU` (Contact, Temporel, Texte, Montant)
    - Implémenter `Langue` (8 langues supportées)
    - Implémenter `ActionType` (résolution, communication, calendrier, système)
    - Implémenter `StatutExecution`, `NiveauConfirmation`, `EtatEtape`
    - Créer les data classes : `ContexteUtilisateur`, `PreferencesUtilisateur`, `Localisation`
    - _Exigences : 1.3, 1.6, 2.2, 3.1_

  - [ ]* 2.2 Écrire les tests unitaires pour les modèles de domaine
    - Tester la sérialisation/désérialisation JSON de tous les modèles
    - Tester les validations sur `EntiteNLU` (montants positifs, dates valides)
    - _Exigences : 11.6_

- [ ] 3. Implémenter le module de parsing et validation des commandes
  - [ ] 3.1 Créer le `ParserCommandes`
    - Implémenter la grammaire de parsing pour les 20 intentions prioritaires
    - Extraire les entités structurées (contacts, dates, montants, texte libre)
    - Implémenter la validation des entités (format téléphone, dates cohérentes)
    - Retourner `Intention` structurée ou erreur descriptive avec position
    - _Exigences : 11.1, 11.2, 11.3, 11.6_

  - [ ] 3.2 Créer le `FormateurCommandes`
    - Implémenter la génération de texte lisible à partir d'objets `Intention`
    - Supporter le formatage dans les 8 langues
    - _Exigences : 11.4_

  - [ ]* 3.3 Écrire les tests de propriété pour le parsing
    - **Propriété 1 : Round-trip parsing des intentions**
    - **Valide : Exigence 11.5**
    - Générer des intentions aléatoires, vérifier `parse(format(intention)) == intention`
    - Tester avec 100+ itérations via Kotest Property Testing
    - _Exigences : 11.5_

  - [ ]* 3.4 Écrire les tests de propriété pour l'extraction d'entités
    - **Propriété 7 : Préservation des entités lors du parsing NLU**
    - **Valide : Exigence 11.6, 1.6**
    - Générer des commandes avec entités connues, vérifier extraction complète
    - _Exigences : 11.6, 1.6_

- [ ] 4. Implémenter le Service NLU (edge + cloud)
  - [ ] 4.1 Créer l'interface `ServiceNLU` et modèles de données
    - Définir `ResultatInterpretation`, `ResultatNLU`, `SourceNLU`
    - Implémenter les générateurs Kotest pour `AudioBuffer` (tests)
    - _Exigences : 1.6_

  - [ ] 4.2 Implémenter le NLU edge (règles locales)
    - Intégrer un modèle ASR léger (Vosk ou simulé pour MVP)
    - Implémenter le matching de patterns pour les 20 intentions prioritaires
    - Détecter la langue automatiquement (8 langues)
    - Calculer le niveau de confiance (0-100%)
    - Appliquer filtrage adaptatif du bruit de fond (traitement signal)
    - _Exigences : 1.1, 1.2, 1.3, 1.5, 1.6, 13.2_

  - [ ] 4.3 Implémenter le fallback cloud pour NLU complexe
    - Créer un client HTTP pour appel API externe (Whisper/GPT-4 ou mock)
    - Implémenter la logique de fallback si confiance < 70%
    - Gérer le timeout et les erreurs réseau
    - _Exigences : 1.2, 1.4_

  - [ ]* 4.4 Écrire les tests de propriété pour le seuil de confiance
    - **Propriété 9 : Détection d'intentions avec seuil de confiance**
    - **Valide : Exigence 1.2**
    - Simuler résultats avec confiance < 70%, vérifier rejet et demande de reformulation
    - _Exigences : 1.2_

  - [ ]* 4.5 Écrire les tests unitaires pour le NLU edge
    - Tester l'interprétation des 20 intentions avec audio de test
    - Tester la détection de langue pour les 8 langues supportées
    - Tester le filtrage du bruit avec audio bruité
    - _Exigences : 1.1, 1.3, 1.5, 13.2_

- [ ] 5. Checkpoint - Vérifier les tests de parsing et NLU
  - S'assurer que tous les tests passent, demander à l'utilisateur si des questions surviennent.

- [ ] 6. Implémenter l'Orchestrateur de Tâches
  - [ ] 6.1 Créer l'interface `OrchestrateurdeTaches` et modèles
    - Définir `PlanAction`, `Etape`, `Precondition`, `CompensationStrategy`
    - Implémenter `EtatProgression` pour suivi temps réel
    - _Exigences : 2.2, 2.5_

  - [ ] 6.2 Implémenter la génération de plans d'actions
    - Créer la logique de planification pour chaque type d'intention
    - Générer les étapes ordonnées avec préconditions
    - Détecter les actions sensibles (paiement, suppression, modification sécurité)
    - Assigner les niveaux de confirmation (AUCUN, VOCAL, CRITIQUE)
    - Calculer l'estimation de durée totale
    - _Exigences : 2.1, 2.2, 2.4_

  - [ ] 6.3 Implémenter la vérification des préconditions et alternatives
    - Vérifier les préconditions (permissions, connectivité, données)
    - Proposer des plans alternatifs si préconditions manquantes
    - Maintenir l'état de progression accessible (pourcentage, étapes restantes)
    - _Exigences : 2.3, 2.5_

  - [ ]* 6.4 Écrire les tests de propriété pour les invariants de plans
    - **Propriété 4 : Invariants de structure des plans d'actions**
    - **Valide : Exigence 2.1, 2.4, 2.2**
    - Vérifier : au moins 1 étape, actions sensibles marquées, durée = somme étapes, IDs uniques
    - _Exigences : 2.1, 2.2, 2.4_

  - [ ]* 6.5 Écrire les tests unitaires pour l'orchestration
    - Tester la génération de plans pour chaque type d'intention
    - Tester la détection d'actions sensibles (paiement, suppression)
    - Tester les plans alternatifs en cas de préconditions manquantes
    - _Exigences : 2.1, 2.3, 2.4_

- [ ] 7. Implémenter l'Exécuteur Sécurisé avec idempotence
  - [ ] 7.1 Créer l'interface `ExecuteurSecurise` et cache d'idempotence
    - Définir `ResultatExecution`, `PreuveExecution`, `StatutExecution`
    - Implémenter le `CacheIdempotence` avec stockage en mémoire (Redis pour prod)
    - _Exigences : 3.1, 3.2_

  - [ ] 7.2 Implémenter le mécanisme d'idempotence
    - Générer un UUID v4 comme token d'idempotence côté client
    - Vérifier le cache avant exécution, retourner résultat stocké si existant
    - Stocker les résultats dans le cache avec TTL 24h
    - _Exigences : 3.1, 3.2_

  - [ ] 7.3 Implémenter l'exécution avec retry et backoff exponentiel
    - Exécuter l'action avec gestion d'erreurs
    - Implémenter retry automatique (3 tentatives : 1s, 2s, 4s)
    - Supporter l'annulation via commande vocale pendant exécution
    - _Exigences : 3.3, 3.4_

  - [ ] 7.4 Implémenter la génération de preuves d'exécution cryptographiques
    - Générer `PreuveExecution` avec timestamp, statut, signature SHA-256-RSA
    - Signer les données d'exécution avec clé privée
    - Implémenter la vérification de signature avec clé publique
    - _Exigences : 3.5_

  - [ ] 7.5 Gérer les confirmations pour actions sensibles
    - Mettre en pause l'exécution si confirmation requise
    - Implémenter un timeout de 30 secondes pour attente confirmation
    - Annuler l'action si timeout dépassé avec notification vocale
    - _Exigences : 3.6_

  - [ ]* 7.6 Écrire les tests de propriété pour l'idempotence
    - **Propriété 2 : Idempotence des exécutions d'actions**
    - **Valide : Exigence 3.2, 2.6**
    - Exécuter N fois (1-10) la même action avec même token, vérifier résultat identique
    - Vérifier que les exécutions 2+ proviennent du cache
    - _Exigences : 3.2, 2.6_

  - [ ]* 7.7 Écrire les tests de propriété pour la conservation des étapes
    - **Propriété 10 : Conservation du nombre d'étapes lors de l'exécution**
    - **Valide : Exigence 2.5**
    - Vérifier que succès + échecs + annulés = total étapes
    - _Exigences : 2.5_

  - [ ]* 7.8 Écrire les tests unitaires pour l'exécuteur
    - Tester le retry avec échecs simulés
    - Tester l'annulation pendant exécution
    - Tester la génération et vérification de preuves cryptographiques
    - Tester le timeout de confirmation pour actions sensibles
    - _Exigences : 3.3, 3.4, 3.5, 3.6_

- [ ] 8. Checkpoint - Vérifier l'orchestration et l'exécution
  - S'assurer que tous les tests passent, valider l'idempotence et les preuves cryptographiques avec l'utilisateur.

- [ ] 9. Implémenter le Store Mémoire avec chiffrement
  - [ ] 9.1 Créer l'interface `StoreMémoire` et schéma de base de données
    - Définir les tables SQLCipher : `preferences`, `contacts_locaux`, `historique_local`, `file_sync`
    - Implémenter la connexion à SQLCipher avec chiffrement AES-256
    - _Exigences : 7.1_

  - [ ] 9.2 Implémenter le chiffrement des données
    - Générer la clé maîtresse via PBKDF2 (100k iterations) depuis PIN/biométrie
    - Implémenter le chiffrement AES-256-GCM avec IV aléatoire
    - Implémenter le déchiffrement avec validation d'intégrité (Auth Tag)
    - _Exigences : 7.1_

  - [ ] 9.3 Implémenter la gestion des préférences utilisateur
    - Sauvegarder/obtenir/supprimer préférences chiffrées
    - Supporter les types : String, Int, Float, Boolean, JSONB
    - _Exigences : 7.1_

  - [ ] 9.4 Implémenter la gestion de l'historique local
    - Ajouter des entrées d'historique avec timestamp
    - Filtrer l'historique par type d'intention, date, résultat
    - _Exigences : 7.1_

  - [ ] 9.5 Implémenter la gestion des contacts locaux
    - Sauvegarder et rechercher des contacts (nom, numéros, emails)
    - Supporter la recherche floue par nom
    - _Exigences : 4.1_

  - [ ] 9.6 Implémenter l'export/import de données
    - Exporter toutes les données vers un fichier chiffré (AES-256)
    - Importer et restaurer les données depuis un fichier chiffré
    - _Exigences : 7.3_

  - [ ] 9.7 Implémenter la suppression sécurisée et la rétention
    - Supprimer définitivement les données avec overwrite + unlink
    - Configurer la période de rétention (7, 30, 90 jours, jamais)
    - Implémenter le nettoyage automatique des données expirées
    - _Exigences : 7.2, 7.6_

  - [ ]* 9.8 Écrire les tests de propriété pour le chiffrement round-trip
    - **Propriété 8 : Encryption round-trip des données locales**
    - **Valide : Exigence 7.1, 7.7**
    - Générer données aléatoires, vérifier chiffrer(déchiffrer(données)) == données
    - Vérifier que texte chiffré ≠ données originales
    - _Exigences : 7.1, 7.7_

  - [ ]* 9.9 Écrire les tests unitaires pour le store
    - Tester la sauvegarde et récupération de préférences
    - Tester la recherche de contacts
    - Tester l'export/import de données
    - Tester la suppression sécurisée avec vérification filesystem
    - Tester l'application de la politique de rétention
    - _Exigences : 7.1, 7.2, 7.3, 7.6_

- [ ] 10. Implémenter le Journal d'Audit avec hash chain
  - [ ] 10.1 Créer l'interface `JournalAudit` et schéma de base de données
    - Définir la table `journal_audit` avec champs cryptographiques
    - Implémenter `EntreeAudit` avec `hashPrecedent`, `hash`, `signature`
    - _Exigences : 9.1_

  - [ ] 10.2 Implémenter le chaînage cryptographique (hash chain)
    - Initialiser avec un hash genesis (32 bytes zéro)
    - Pour chaque nouvelle entrée, inclure le hash de l'entrée précédente
    - Calculer le hash SHA-256 de l'entrée complète
    - Signer l'entrée avec clé privée RSA
    - _Exigences : 9.4_

  - [ ] 10.3 Implémenter l'enregistrement des actions
    - Enregistrer toute action avec : timestamp, type, paramètres, résultat, durée, token
    - Générer une preuve d'exécution cryptographique
    - Stocker l'entrée de manière immuable (append-only)
    - _Exigences : 9.1, 3.5_

  - [ ] 10.4 Implémenter la vérification d'intégrité du journal
    - Parcourir toutes les entrées séquentiellement
    - Vérifier que chaque `hashPrecedent` correspond au hash de l'entrée précédente
    - Recalculer le hash de chaque entrée et comparer
    - Vérifier la signature cryptographique de chaque entrée
    - Retourner `ResultatVerification.Compromis` avec position si anomalie détectée
    - _Exigences : 9.4, 9.5_

  - [ ] 10.5 Implémenter le requêtage vocal de l'historique
    - Filtrer par type d'action, date, utilisateur
    - Générer des résumés vocaux (actions du jour, historique par type)
    - Supporter l'audit étendu (tentatives échouées, refus de permissions)
    - _Exigences : 9.2, 9.3, 9.6_

  - [ ]* 10.6 Écrire les tests de propriété pour l'intégrité du hash chain
    - **Propriété 3 : Intégrité du hash chain d'audit**
    - **Valide : Exigence 9.4, 9.5**
    - Enregistrer 5-20 entrées, vérifier intégrité complète
    - Modifier une entrée au milieu, vérifier détection de compromission
    - _Exigences : 9.4, 9.5_

  - [ ]* 10.7 Écrire les tests unitaires pour le journal d'audit
    - Tester l'enregistrement de diverses actions
    - Tester le calcul et la vérification des hashs
    - Tester la signature et la validation cryptographique
    - Tester le filtrage de l'historique
    - Tester la détection de tentatives de modification
    - _Exigences : 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 11. Implémenter le Gateway API avec authentification et quotas
  - [ ] 11.1 Créer l'interface `GatewayAPI` et configuration JWT
    - Définir `TokensPair`, `QuotasUtilisateur`, `Credentials`
    - Configurer les secrets JWT (clé privée/publique RSA ou HMAC)
    - _Exigences : 10.1_

  - [ ] 11.2 Implémenter l'authentification JWT avec refresh token rotation
    - Implémenter `/auth/login` : valider credentials (bcrypt), générer access + refresh tokens
    - Access token : durée 15 minutes, claims (userId, rôle)
    - Refresh token : durée 7 jours, stockage sécurisé avec rotation
    - Implémenter `/auth/refresh` : valider refresh token, générer nouveaux tokens, invalider ancien
    - Implémenter `/auth/logout` : invalider les tokens
    - _Exigences : 10.1, 10.2_

  - [ ] 11.3 Implémenter le middleware de validation JWT
    - Extraire le token du header `Authorization: Bearer <token>`
    - Valider la signature, l'expiration, et les claims
    - Rejeter avec HTTP 401 si token invalide ou expiré
    - _Exigences : 10.1, 10.2_

  - [ ] 11.4 Implémenter le système de quotas par utilisateur
    - Quotas par défaut : 1000 NLU/jour, 500 actions/jour, 50 sensibles/jour
    - Stocker les compteurs dans Redis avec TTL jusqu'à minuit UTC
    - Incrémenter les compteurs à chaque requête
    - Rejeter avec HTTP 429 si quota dépassé
    - _Exigences : 10.3, 10.4_

  - [ ] 11.5 Implémenter le routage vers les services backend
    - Router `/nlu/*` vers Service NLU
    - Router `/orchestration/*` vers Orchestrateur
    - Router `/execution/*` vers Exécuteur
    - Router `/store/*` vers Store Mémoire
    - Router `/audit/*` vers Journal Audit
    - Latence de routage < 50ms
    - _Exigences : 10.5_

  - [ ] 11.6 Implémenter le logging des tentatives d'accès
    - Logger toutes les tentatives d'authentification échouées
    - Logger les dépassements de quotas avec IP et timestamp
    - Enregistrer dans le Journal d'Audit
    - _Exigences : 10.6_

  - [ ]* 11.7 Écrire les tests de propriété pour la validité des JWT
    - **Propriété 6 : Validité des tokens JWT**
    - **Valide : Exigence 10.1, 10.2**
    - Générer tokens pour utilisateurs aléatoires, vérifier signature et claims
    - Vérifier que l'expiration est dans le futur et cohérente avec la durée
    - _Exigences : 10.1, 10.2_

  - [ ]* 11.8 Écrire les tests de propriété pour la monotonie des quotas
    - **Propriété 5 : Monotonie des quotas**
    - **Valide : Exigence 10.3, 10.4**
    - Effectuer 1-50 requêtes, vérifier que le quota restant diminue de manière monotone
    - _Exigences : 10.3, 10.4_

  - [ ]* 11.9 Écrire les tests unitaires pour le gateway
    - Tester le flux d'authentification complet (login, refresh, logout)
    - Tester le rejet de tokens expirés ou invalides
    - Tester les quotas et le rejet HTTP 429
    - Tester le routage vers les différents services
    - Tester le logging des tentatives échouées
    - _Exigences : 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [ ] 12. Checkpoint - Vérifier la persistance, l'audit et le gateway
  - S'assurer que tous les tests passent, valider la sécurité du chiffrement et des JWT avec l'utilisateur.

- [ ] 13. Implémenter le Connecteur Android
  - [ ] 13.1 Créer l'interface `ConnecteurAndroid` et configuration AccessibilityService
    - Définir `ResultatAction`, `ActionUI`, `Evenement`, `ParametreSysteme`
    - Créer le service Android héritant de `AccessibilityService`
    - Déclarer les permissions nécessaires dans AndroidManifest.xml
    - _Exigences : 4.1, 4.6_

  - [ ] 13.2 Implémenter les fonctions de communication
    - `passerAppel(numero)` : utiliser Intent ACTION_CALL avec vérification permission
    - `envoyerSMS(numero, message)` : utiliser SmsManager avec vérification permission
    - _Exigences : 4.1_

  - [ ] 13.3 Implémenter les fonctions de calendrier et alarmes
    - `creerEvenement(evenement)` : utiliser CalendarContract
    - `creerAlarme(heure)` : utiliser AlarmManager
    - _Exigences : 4.1_

  - [ ] 13.4 Implémenter les fonctions système et paramètres
    - `modifierParametre(parametre, valeur)` : utiliser Settings API
    - `obtenirEtatBatterie()` : utiliser BatteryManager
    - _Exigences : 4.1_

  - [ ] 13.5 Implémenter l'interaction avec applications tierces via Intents
    - `lancerApplication(packageName)` : créer et envoyer Intent
    - Vérifier que l'application est installée avant lancement
    - Émettre un message vocal si application absente
    - _Exigences : 4.1, 4.5_

  - [ ] 13.6 Implémenter l'interaction UI via AccessibilityService
    - `interagirAvecUI()` : trouver les nodes par texte/ID et effectuer des actions
    - Supporter les actions : cliquer, saisir texte, défiler
    - Attendre le démarrage de l'application avant interaction (2 secondes)
    - _Exigences : 4.6_

  - [ ] 13.7 Implémenter l'OCR pour lecture de texte
    - `lireTexteEcran()` : capturer un screenshot et exécuter OCR (ML Kit)
    - `lireTexteImage(bitmap)` : appliquer OCR sur une image fournie
    - _Exigences : 4.1_

  - [ ] 13.8 Implémenter la gestion des permissions avec explications vocales
    - `verifierPermission(permission)` : vérifier si permission accordée
    - `demanderPermission(permission, explication)` : expliquer vocalement avant de demander
    - Proposer une alternative vocale si permission refusée
    - _Exigences : 4.2, 4.3_

  - [ ] 13.9 Enregistrer toutes les actions sensibles dans le Journal d'Audit
    - Logger chaque accès à fonctionnalité sensible avec timestamp et contexte
    - _Exigences : 4.4_

  - [ ]* 13.10 Écrire les tests unitaires pour le Connecteur Android
    - Tester l'envoi de SMS avec/sans permission
    - Tester le lancement d'application installée/absente
    - Tester la vérification et demande de permissions
    - Tester l'interaction UI avec AccessibilityService (mocked)
    - Tester l'OCR avec images de test
    - _Exigences : 4.1, 4.2, 4.3, 4.5, 4.6_

- [ ] 14. Implémenter le Client Android avec feedback multimodal
  - [ ] 14.1 Créer l'interface `GestionnaireVocal` et configuration audio
    - Définir `ResultatNLU`, `TypeSon`, `PatternVibration`, `ConfigurationVocale`
    - Configurer AudioManager, Vibrator, TextToSpeech
    - _Exigences : 5.1, 5.2, 5.3_

  - [ ] 14.2 Implémenter la capture et le prétraitement audio
    - Capturer l'audio via AudioRecord (flow réactif avec Coroutines)
    - Appliquer un filtrage adaptatif du bruit de fond
    - _Exigences : 1.5_

  - [ ] 14.3 Implémenter l'interprétation d'intention locale
    - Appeler le Service NLU local (edge) avec l'audio capturé
    - Gérer le fallback vers le NLU cloud si confiance < 70%
    - Demander reformulation vocale si intention non comprise
    - _Exigences : 1.1, 1.2, 1.4_

  - [ ] 14.4 Implémenter le feedback multimodal pour démarrage d'action
    - Émettre un bip court (100ms, tonalité neutre)
    - Émettre une vibration courte (100ms)
    - _Exigences : 5.1_

  - [ ] 14.5 Implémenter le feedback multimodal pour succès
    - Confirmer vocalement le résultat via TTS
    - Émettre une tonalité montante (200ms)
    - Émettre une vibration double (100ms, pause 50ms, 100ms)
    - _Exigences : 5.2_

  - [ ] 14.6 Implémenter le feedback multimodal pour échec
    - Expliquer vocalement la raison de l'échec
    - Proposer 2-3 alternatives vocalement
    - Émettre une tonalité descendante (200ms)
    - Émettre une vibration longue (500ms)
    - _Exigences : 5.3, 12.3_

  - [ ] 14.7 Implémenter le mode feedback verbeux
    - Narrer vocalement chaque étape intermédiaire d'un plan
    - Répondre à la commande "où en es-tu ?" avec état de progression
    - _Exigences : 5.4, 5.5_

  - [ ] 14.8 Implémenter la configuration vocale des préférences
    - Configurer vitesse de parole (0.5x à 2x)
    - Configurer type de voix (liste des voix TTS disponibles)
    - Configurer volume des signaux sonores (0-100)
    - Configurer intensité des vibrations (0-255)
    - _Exigences : 5.6_

  - [ ]* 14.9 Écrire les tests unitaires pour le feedback multimodal
    - Tester l'émission de signaux sonores (bip, tonalités)
    - Tester les patterns de vibration
    - Tester la synthèse vocale TTS pour confirmations/erreurs
    - Tester la configuration des préférences vocales
    - _Exigences : 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 15. Implémenter le mode hors ligne et la synchronisation
  - [ ] 15.1 Créer le gestionnaire de connectivité
    - Surveiller l'état réseau via ConnectivityManager
    - Détecter les changements de connectivité (online/offline)
    - Informer vocalement l'utilisateur lors de perte de connexion
    - Lister vocalement les fonctions disponibles hors ligne
    - _Exigences : 6.3_

  - [ ] 15.2 Implémenter la file d'attente des actions hors ligne
    - Persister les actions non-locales dans `file_sync` (SQLCipher)
    - Chiffrer les actions en attente (AES-256)
    - _Exigences : 6.4_

  - [ ] 15.3 Implémenter l'exécution locale des actions hors ligne
    - Supporter : appels, SMS, calendrier local, alarmes, lecture OCR, heure
    - Utiliser les modèles NLU embarqués (edge)
    - _Exigences : 6.1, 6.2_

  - [ ] 15.4 Implémenter la synchronisation opportuniste
    - Déclencher la sync via WorkManager à la reconnexion
    - Envoyer les actions en file d'attente au backend (retry exponentiel)
    - Notifier vocalement l'utilisateur du résultat de la sync
    - _Exigences : 6.5_

  - [ ] 15.5 Implémenter la gestion des actions nécessitant la connexion
    - Détecter les actions nécessitant Internet (recherche web, météo, actualités)
    - Expliquer vocalement l'indisponibilité temporaire
    - Ajouter automatiquement en file d'attente pour exécution ultérieure
    - _Exigences : 6.6_

  - [ ]* 15.6 Écrire les tests d'intégration pour le mode hors ligne
    - Tester l'exécution d'actions locales sans connexion
    - Tester la mise en file d'attente d'actions cloud
    - Tester la synchronisation à la reconnexion
    - Vérifier que les actions synchronisées produisent le même résultat (round-trip)
    - _Exigences : 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ] 16. Checkpoint - Vérifier les composants Android et le mode hors ligne
  - S'assurer que tous les tests passent, tester manuellement le feedback multimodal sur appareil Android.

- [ ] 17. Implémenter la gestion du consentement pour actions sensibles
  - [ ] 17.1 Créer le classificateur d'actions sensibles
    - Définir 3 niveaux : AUTOMATIQUE, VOCAL, CRITIQUE
    - Classifier automatiquement les actions (paiement, suppression, modification sécurité = CRITIQUE)
    - Permettre à l'utilisateur de configurer vocalement les niveaux
    - _Exigences : 8.1, 8.6_

  - [ ] 17.2 Implémenter la demande de confirmation vocale
    - Pour actions VOCAL : demander "Confirmez-vous [action] ?"
    - Pour actions CRITIQUE : demander confirmation vocale + PIN/biométrie
    - Timeout de 30 secondes, annuler si pas de réponse
    - _Exigences : 8.2, 8.3, 8.4_

  - [ ] 17.3 Enregistrer les consentements dans le Journal d'Audit
    - Logger : timestamp confirmation, contenu action, méthode de confirmation
    - Lier la confirmation à l'entrée d'audit de l'action
    - _Exigences : 8.5_

  - [ ]* 17.4 Écrire les tests unitaires pour la gestion du consentement
    - Tester la classification automatique des actions
    - Tester la demande de confirmation vocale
    - Tester le timeout et l'annulation
    - Tester l'enregistrement des consentements dans l'audit
    - Vérifier qu'aucune action sensible ne peut s'exécuter sans confirmation valide
    - _Exigences : 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 18. Implémenter la tolérance aux erreurs et récupération gracieuse
  - [ ] 18.1 Créer le système de monitoring et logging centralisé
    - Configurer un logger structuré (Logback ou équivalent)
    - Logger toutes les erreurs avec : stack trace, contexte, timestamp, trace_id
    - Envoyer les logs vers un système centralisé (ELK ou Loki compatible)
    - _Exigences : 12.1, 14.5_

  - [ ] 18.2 Implémenter le Circuit Breaker pour services externes
    - Configurer : 5 échecs consécutifs → circuit ouvert pour 60 secondes
    - États : FERME, OUVERT, SEMI_OUVERT
    - Tenter une requête de test après 60s en état SEMI_OUVERT
    - _Exigences : 12.6_

  - [ ] 18.3 Implémenter la gestion des erreurs NLU
    - Si confiance < 70% : demander reformulation avec 2-3 suggestions
    - Utiliser le fallback cloud si l'edge échoue
    - _Exigences : 12.2_

  - [ ] 18.4 Implémenter la gestion des échecs d'exécution
    - Après 3 tentatives échouées : expliquer raison, proposer alternatives
    - Alternatives : réessayer ultérieurement, contacter support, essayer action alternative
    - _Exigences : 12.3_

  - [ ] 18.5 Implémenter le basculement en mode hors ligne en cas de panne
    - Si Gateway ou Orchestrateur indisponible : basculer en mode local
    - Informer vocalement des capacités réduites
    - _Exigences : 12.4_

  - [ ] 18.6 Implémenter la gestion des exceptions non catchées
    - Retourner une réponse générique sans détails techniques à l'utilisateur
    - Logger les détails complets avec stack trace pour debugging
    - _Exigences : 12.5_

  - [ ]* 18.7 Écrire les tests unitaires pour la tolérance aux erreurs
    - Tester le Circuit Breaker avec échecs simulés
    - Tester la reformulation pour confiance basse
    - Tester les tentatives de retry et les propositions d'alternatives
    - Tester le basculement en mode hors ligne
    - Tester le logging des erreurs
    - _Exigences : 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_

- [ ] 19. Implémenter le support multi-langues et dialectes
  - [ ] 19.1 Intégrer les modèles NLU pour les 8 langues
    - Français, Anglais, Arabe, Wolof, Bambara, Swahili, Lingala, Créole haïtien
    - Configurer Vosk avec les modèles pour chaque langue
    - _Exigences : 13.1_

  - [ ] 19.2 Implémenter la détection automatique de langue
    - Analyser l'audio et détecter la langue parlée (précision > 90%)
    - Utiliser la langue détectée pour l'interprétation et les retours
    - _Exigences : 13.2, 13.5_

  - [ ] 19.3 Implémenter la configuration manuelle de langue
    - Permettre à l'utilisateur de définir une langue préférée
    - Prioriser la langue configurée pour tous les retours vocaux
    - Supporter la commande vocale "parle-moi en [langue]"
    - _Exigences : 13.3, 13.4_

  - [ ] 19.4 Implémenter le système de modèles téléchargeables
    - Permettre le téléchargement de nouveaux modèles NLU sans mise à jour de l'app
    - Stocker les modèles dans un répertoire dédié
    - _Exigences : 13.6_

  - [ ]* 19.5 Écrire les tests unitaires pour le multi-langues
    - Tester la détection automatique de langue pour les 8 langues
    - Tester la cohérence langue détectée = langue des retours
    - Tester le changement de langue via commande vocale
    - Tester que la même intention dans 2 langues produit le même plan
    - _Exigences : 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_

- [ ] 20. Implémenter le système de métriques et monitoring
  - [ ] 20.1 Créer l'endpoint Prometheus pour exposition des métriques
    - Exposer : latence NLU (P50, P95, P99), taux de succès actions, requêtes/seconde
    - Exposer : utilisation CPU/mémoire par composant
    - _Exigences : 14.1_

  - [ ] 20.2 Implémenter les alertes basées sur SLA
    - Déclencher alerte si latence NLU > 500ms (P95)
    - Déclencher alerte si latence Orchestrateur > 300ms (P95)
    - Déclencher alerte si latence Exécuteur > 2s (P95)
    - _Exigences : 14.2_

  - [ ] 20.3 Implémenter le tracking du taux de réussite des intentions
    - Calculer : (intentions succès) / (intentions totales)
    - Objectif : > 95%, alerte si < 90%
    - _Exigences : 14.3, 14.4_

  - [ ] 20.4 Implémenter le logging structuré avec corrélation
    - Logger tous les événements avec : niveau, timestamp, composant, trace_id
    - Niveaux : DEBUG, INFO, WARNING, ERROR, CRITICAL
    - Intégrer avec système centralisé (ELK/Loki compatible)
    - _Exigences : 14.5_

  - [ ] 20.5 Implémenter le mode debug utilisateur avec pseudonymisation
    - Enrichir les logs avec userId quand activé
    - Appliquer la pseudonymisation conforme RGPD (hash irréversible)
    - _Exigences : 14.6_

  - [ ]* 20.6 Écrire les tests unitaires pour les métriques
    - Tester l'exposition Prometheus des métriques
    - Tester les alertes basées sur seuils
    - Tester le calcul du taux de réussite
    - Tester l'ajout de trace_id dans les logs
    - _Exigences : 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 21. Implémenter l'optimisation des performances et la scalabilité
  - [ ] 21.1 Implémenter le cache distribué pour intentions fréquentes
    - Utiliser Redis pour cacher les interprétations NLU
    - TTL de 5 minutes pour les entrées de cache
    - _Exigences : 15.3_

  - [ ] 21.2 Implémenter le connection pooling pour ressources partagées
    - Configurer le pool de connexions PostgreSQL (HikariCP)
    - Configurer le pool de connexions Redis
    - _Exigences : 15.4_

  - [ ] 21.3 Implémenter l'auto-scaling horizontal
    - Configurer l'auto-scaling basé sur la charge (> 80% capacité)
    - Scaling déclenché dans les 60 secondes
    - Utiliser Docker/Kubernetes ou équivalent
    - _Exigences : 15.2_

  - [ ] 21.4 Implémenter les timeouts pour requêtes lentes
    - Timeout de 10 secondes pour les appels de services
    - Retourner une erreur appropriée plutôt que de bloquer
    - _Exigences : 15.6_

  - [ ]* 21.5 Écrire les tests de charge et de performance
    - Tester 1000 requêtes concurrentes/seconde
    - Vérifier latence P95 < 1 seconde
    - Vérifier que l'auto-scaling se déclenche > 80% charge
    - Vérifier latence P95 < 3x latence médiane
    - _Exigences : 15.1, 15.2, 15.5_

- [ ] 22. Checkpoint final - Tests d'intégration end-to-end
  - S'assurer que tous les tests passent, valider les performances et la scalabilité avec l'utilisateur.

- [ ] 23. Intégration et tests end-to-end
  - [ ] 23.1 Créer le test E2E : commande vocale "appelle maman"
    - Tester le workflow complet : audio → NLU → orchestration → exécution → feedback
    - Vérifier l'intention, le plan, l'appel initié, le feedback multimodal, l'audit
    - _Exigences : 1.1, 2.1, 3.1, 4.1, 5.2, 9.1_

  - [ ] 23.2 Créer le test E2E : mode hors ligne avec synchronisation
    - Simuler perte de connexion, exécuter plusieurs actions locales
    - Vérifier mise en file d'attente chiffrée
    - Simuler reconnexion et vérifier synchronisation automatique
    - _Exigences : 6.1, 6.2, 6.4, 6.5_

  - [ ] 23.3 Créer le test E2E : action sensible avec confirmation
    - Tester une action de paiement nécessitant confirmation vocale + PIN
    - Vérifier la demande de confirmation, le timeout, l'enregistrement du consentement
    - _Exigences : 8.1, 8.2, 8.4, 8.5_

  - [ ] 23.4 Créer le test E2E : gestion d'erreurs et recovery
    - Simuler échec NLU (confiance basse) et vérifier reformulation
    - Simuler échec d'exécution et vérifier retry + alternatives
    - Simuler panne service et vérifier basculement mode hors ligne
    - _Exigences : 12.2, 12.3, 12.4_

  - [ ] 23.5 Créer le test E2E : multi-langues
    - Tester commande vocale en français, wolof, arabe
    - Vérifier détection automatique de langue et cohérence des retours
    - _Exigences : 13.2, 13.5_

- [ ] 24. Documentation et déploiement
  - [ ] 24.1 Documenter les APIs REST du backend
    - Générer la documentation OpenAPI/Swagger
    - Documenter tous les endpoints avec exemples de requêtes/réponses
    - _Exigences : Toutes (documentation)_

  - [ ] 24.2 Documenter l'architecture et les décisions de design
    - Créer un README.md décrivant l'architecture
    - Documenter les choix techniques (edge-first, idempotence, hash chain)
    - Inclure des diagrammes d'architecture et de séquence

  - [ ] 24.3 Préparer les scripts de déploiement
    - Créer les fichiers Docker/Kubernetes pour déploiement backend
    - Créer les scripts de migration de base de données
    - Configurer les variables d'environnement (secrets, URLs)

  - [ ] 24.4 Générer le fichier APK Android pour distribution
    - Configurer le signing de l'APK avec keystore
    - Générer l'APK de release
    - Tester l'installation sur appareils physiques

## Notes

- Les tâches marquées avec `*` sont des tests et peuvent être ignorées pour un MVP ultra-rapide, mais sont fortement recommandées pour la fiabilité.
- Les tests de propriétés utilisent Kotest Property Testing avec 100+ itérations minimum.
- L'implémentation suit une approche modulaire : chaque composant peut être développé et testé indépendamment.
- Les checkpoints permettent de valider l'avancement et d'obtenir du feedback utilisateur avant de continuer.
- Le code doit respecter les conventions Kotlin (nommage en français pour fonctions/dossiers comme spécifié).
- Toutes les données sensibles (JWT secrets, clés de chiffrement) doivent être externalisées dans des variables d'environnement.
- Les modèles NLU pour les 8 langues peuvent être simulés au début (mocks) puis remplacés par des vrais modèles (Vosk).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3", "3.4", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3"] },
    { "id": 5, "tasks": ["4.4", "4.5", "6.1"] },
    { "id": 6, "tasks": ["6.2", "6.3"] },
    { "id": 7, "tasks": ["6.4", "6.5", "7.1"] },
    { "id": 8, "tasks": ["7.2", "7.3", "7.4", "7.5"] },
    { "id": 9, "tasks": ["7.6", "7.7", "7.8", "9.1"] },
    { "id": 10, "tasks": ["9.2", "9.3", "9.4", "9.5", "9.6", "9.7"] },
    { "id": 11, "tasks": ["9.8", "9.9", "10.1"] },
    { "id": 12, "tasks": ["10.2", "10.3", "10.4", "10.5"] },
    { "id": 13, "tasks": ["10.6", "10.7", "11.1"] },
    { "id": 14, "tasks": ["11.2", "11.3", "11.4", "11.5", "11.6"] },
    { "id": 15, "tasks": ["11.7", "11.8", "11.9", "13.1"] },
    { "id": 16, "tasks": ["13.2", "13.3", "13.4", "13.5", "13.6", "13.7", "13.8", "13.9"] },
    { "id": 17, "tasks": ["13.10", "14.1"] },
    { "id": 18, "tasks": ["14.2", "14.3"] },
    { "id": 19, "tasks": ["14.4", "14.5", "14.6", "14.7", "14.8"] },
    { "id": 20, "tasks": ["14.9", "15.1"] },
    { "id": 21, "tasks": ["15.2", "15.3", "15.4", "15.5"] },
    { "id": 22, "tasks": ["15.6", "17.1"] },
    { "id": 23, "tasks": ["17.2", "17.3"] },
    { "id": 24, "tasks": ["17.4", "18.1"] },
    { "id": 25, "tasks": ["18.2", "18.3", "18.4", "18.5", "18.6"] },
    { "id": 26, "tasks": ["18.7", "19.1"] },
    { "id": 27, "tasks": ["19.2", "19.3", "19.4"] },
    { "id": 28, "tasks": ["19.5", "20.1"] },
    { "id": 29, "tasks": ["20.2", "20.3", "20.4", "20.5"] },
    { "id": 30, "tasks": ["20.6", "21.1"] },
    { "id": 31, "tasks": ["21.2", "21.3", "21.4"] },
    { "id": 32, "tasks": ["21.5", "23.1"] },
    { "id": 33, "tasks": ["23.2", "23.3", "23.4", "23.5"] },
    { "id": 34, "tasks": ["24.1", "24.2", "24.3", "24.4"] }
  ]
}
```
