# Installation Gradle (Solution au problème SSL)

## Problème Rencontré

Le Gradle wrapper échoue à télécharger Gradle depuis `services.gradle.org` à cause d'un problème de certificat SSL.

## ✅ Solutions (Choisir UNE méthode)

### Solution 1 : Installer Gradle avec Chocolatey (Recommandé)

```powershell
# Installer Chocolatey si pas déjà fait
# Voir : https://chocolatey.org/install

# Installer Gradle
choco install gradle --version=8.5

# Vérifier
gradle --version
```

Puis compiler directement :
```powershell
cd d:\STARTUP\koras\backend
gradle build
gradle :backend-services:run
```

### Solution 2 : Télécharger Gradle Manuellement

1. **Télécharger** : [https://gradle.org/releases/](https://gradle.org/releases/)
   - Choisir : gradle-8.5-bin.zip

2. **Extraire** dans `C:\gradle-8.5`

3. **Ajouter au PATH** :
   ```powershell
   $env:Path += ";C:\gradle-8.5\bin"
   # Ou ajouter de façon permanente via Panneau de configuration
   ```

4. **Vérifier** :
   ```powershell
   gradle --version
   ```

5. **Compiler** :
   ```powershell
   cd d:\STARTUP\koras\backend
   gradle build
   gradle :backend-services:run
   ```

### Solution 3 : Utiliser Gradle existant pour générer le wrapper

Si Gradle est déjà installé sur votre système :

```powershell
cd d:\STARTUP\koras\backend

# Générer wrapper avec Gradle installé
gradle wrapper --gradle-version 8.5

# Puis utiliser le wrapper
.\gradlew.bat build
```

### Solution 4 : Copier Gradle depuis un autre projet

Si vous avez Gradle 8.5 dans un autre projet :

```powershell
# Copier le dossier .gradle depuis un projet fonctionnel
copy C:\autre-projet\.gradle d:\STARTUP\koras\backend\.gradle /s

# Puis réessayer
.\gradlew.bat build
```

### Solution 5 : Configuration Proxy/SSL (Si dans environnement d'entreprise)

Si vous êtes derrière un proxy d'entreprise :

1. Créer `gradle.properties` :
```properties
# d:\STARTUP\koras\backend\gradle.properties
systemProp.http.proxyHost=proxy.entreprise.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.entreprise.com
systemProp.https.proxyPort=8080

# Si authentification nécessaire
systemProp.http.proxyUser=username
systemProp.http.proxyPassword=password
systemProp.https.proxyUser=username
systemProp.https.proxyPassword=password
```

2. Réessayer :
```powershell
.\gradlew.bat build
```

## 🎯 Une Fois Gradle Fonctionnel

```powershell
# Compiler
gradle build
# Ou
.\gradlew.bat build

# Lancer
gradle :backend-services:run
# Ou
.\gradlew.bat :backend-services:run

# Tests
gradle test
# Ou
.\gradlew.bat test
```

## ⚡ Alternative Sans Gradle : Compiler Manuellement

Si vraiment bloqué, compiler avec `kotlinc` (nécessite Kotlin compilateur) :

```powershell
# Télécharger Kotlin : https://kotlinlang.org/docs/command-line.html

# Compiler le domaine
kotlinc -d domaine\build\classes domaine\src\main\kotlin\**\*.kt

# Compiler les services (avec dépendances)
kotlinc -cp "domaine\build\classes;libs\*" -d backend-services\build\classes backend-services\src\main\kotlin\**\*.kt

# Lancer
java -cp "domaine\build\classes;backend-services\build\classes;libs\*" com.koras.assistantvocal.ApplicationKt
```

**Note** : Cette méthode nécessite de télécharger toutes les dépendances (Ktor, etc.) manuellement.

## 🆘 Support

### Erreur : "Gradle not found"
→ Utilisez Solution 1 ou 2 ci-dessus

### Erreur : "SSL certificate problem"
→ Utilisez Solution 5 (proxy) ou téléchargez manuellement (Solution 2)

### Erreur : "Cannot download distribution"
→ Téléchargez manuellement depuis https://gradle.org/releases/

### Besoin d'aide ?
Vérifiez :
1. Java installé ? `java -version` (doit afficher 17+)
2. Proxy/firewall ? Configurez `gradle.properties`
3. Téléchargement manuel possible ?

## ✅ Vérification Installation Réussie

```powershell
gradle --version
# Doit afficher :
# ------------------------------------------------------------
# Gradle 8.5
# ------------------------------------------------------------
# Build time:   ...
# Revision:     ...
# Kotlin:       1.9.20
# Groovy:       3.0.19
# Ant:          Apache Ant(TM) version 1.10.13
# JVM:          17.x.x (...)
# OS:           Windows ...
```

## 🚀 Après Installation

1. **Compiler** :
```powershell
cd d:\STARTUP\koras\backend
gradle build
```

2. **Lancer** :
```powershell
gradle :backend-services:run
```

3. **Tester** :
```powershell
curl http://localhost:8080/health
```

---

**Choisissez la Solution 1 (Chocolatey) pour plus de simplicité !**

```powershell
choco install gradle --version=8.5
cd d:\STARTUP\koras\backend
gradle build
gradle :backend-services:run
```
