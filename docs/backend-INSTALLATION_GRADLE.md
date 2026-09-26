# Installation Gradle (Solution au problme SSL)

## Problme Rencontr

Le Gradle wrapper choue tlcharger Gradle depuis `services.gradle.org` cause d'un problme de certificat SSL.

## Solutions (Choisir UNE mthode)

### Solution 1 : Installer Gradle avec Chocolatey (Recommand)

```powershell
# Installer Chocolatey si pas dj fait
# Voir : https://chocolatey.org/install

# Installer Gradle
choco install gradle --version=8.5

# Vrifier
gradle --version
```

Puis compiler directement :
```powershell
cd d:\STARTUP\koras\backend
gradle build
gradle :backend-services:run
```

### Solution 2 : Tlcharger Gradle Manuellement

1. **Tlcharger** : [https://gradle.org/releases/](https://gradle.org/releases/)
- Choisir : gradle-8.5-bin.zip

2. **Extraire** dans `C:\gradle-8.5`

3. **Ajouter au PATH** :
```powershell
$env:Path += ";C:\gradle-8.5\bin"
# Ou ajouter de faon permanente via Panneau de configuration
```

4. **Vrifier** :
```powershell
gradle --version
```

5. **Compiler** :
```powershell
cd d:\STARTUP\koras\backend
gradle build
gradle :backend-services:run
```

### Solution 3 : Utiliser Gradle existant pour gnrer le wrapper

Si Gradle est dj install sur votre systme :

```powershell
cd d:\STARTUP\koras\backend

# Gnrer wrapper avec Gradle install
gradle wrapper --gradle-version 8.5

# Puis utiliser le wrapper
.\gradlew.bat build
```

### Solution 4 : Copier Gradle depuis un autre projet

Si vous avez Gradle 8.5 dans un autre projet :

```powershell
# Copier le dossier .gradle depuis un projet fonctionnel
copy C:\autre-projet\.gradle d:\STARTUP\koras\backend\.gradle /s

# Puis ressayer
.\gradlew.bat build
```

### Solution 5 : Configuration Proxy/SSL (Si dans environnement d'entreprise)

Si vous tes derrire un proxy d'entreprise :

1. Crer `gradle.properties` :
```properties
# d:\STARTUP\koras\backend\gradle.properties
systemProp.http.proxyHost=proxy.entreprise.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.entreprise.com
systemProp.https.proxyPort=8080

# Si authentification ncessaire
systemProp.http.proxyUser=username
systemProp.http.proxyPassword=password
systemProp.https.proxyUser=username
systemProp.https.proxyPassword=password
```

2. Ressayer :
```powershell
.\gradlew.bat build
```

## Une Fois Gradle Fonctionnel

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

## Alternative Sans Gradle : Compiler Manuellement

Si vraiment bloqu, compiler avec `kotlinc` (ncessite Kotlin compilateur) :

```powershell
# Tlcharger Kotlin : https://kotlinlang.org/docs/command-line.html

# Compiler le domaine
kotlinc -d domaine\build\classes domaine\src\main\kotlin\**\*.kt

# Compiler les services (avec dpendances)
kotlinc -cp "domaine\build\classes;libs\*" -d backend-services\build\classes backend-services\src\main\kotlin\**\*.kt

# Lancer
java -cp "domaine\build\classes;backend-services\build\classes;libs\*" com.koras.assistantvocal.ApplicationKt
```

**Note** : Cette mthode ncessite de tlcharger toutes les dpendances (Ktor, etc.) manuellement.

## Support

### Erreur : "Gradle not found"
Utilisez Solution 1 ou 2 ci-dessus

### Erreur : "SSL certificate problem"
Utilisez Solution 5 (proxy) ou tlchargez manuellement (Solution 2)

### Erreur : "Cannot download distribution"
Tlchargez manuellement depuis https://gradle.org/releases/

### Besoin d'aide ?
Vrifiez :
1. Java install ? `java -version` (doit afficher 17+)
2. Proxy/firewall ? Configurez `gradle.properties`
3. Tlchargement manuel possible ?

## Vrification Installation Russie

```powershell
gradle --version
# Doit afficher :
# ------------------------------------------------------------
# Gradle 8.5
# ------------------------------------------------------------
# Build time: ...
# Revision: ...
# Kotlin: 1.9.20
# Groovy: 3.0.19
# Ant: Apache Ant(TM) version 1.10.13
# JVM: 17.x.x (...)
# OS: Windows ...
```

## Aprs Installation

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

**Choisissez la Solution 1 (Chocolatey) pour plus de simplicit !**

```powershell
choco install gradle --version=8.5
cd d:\STARTUP\koras\backend
gradle build
gradle :backend-services:run
```
