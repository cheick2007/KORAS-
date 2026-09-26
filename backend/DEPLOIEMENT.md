
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

##  Structure du Projet

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
│       │   │       ├── Application.kt        # Point d'entrée 
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
├── gradlew.bat                       # Wrapper Windows 
└── DEPLOIEMENT.md                    # Ce fichier
```

##  Checklist de Test

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

##  Commandes Utiles

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
