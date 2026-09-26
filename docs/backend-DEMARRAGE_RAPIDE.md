# Dmarrage Rapide - Backend Koras

## Vous tes ICI : `d:\STARTUP\koras\backend`

### Solution Ultra-Rapide

```powershell
# 1. Installer Gradle avec Chocolatey
choco install gradle

# 2. Lancer
gradle :backend-services:run
```

**C'est tout ! L'API sera sur http://localhost:8080**

---

## tapes Dtailles

### tape 1 : Installer Chocolatey (si ncessaire)

Chocolatey est un gestionnaire de paquets pour Windows (comme apt sur Linux).

**Ouvrir PowerShell en Administrateur :**
1. Clic droit sur le bouton Dmarrer
2. Slectionner "Windows PowerShell (Admin)" ou "Terminal (Admin)"

**Excuter :**
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### tape 2 : Installer Gradle

```powershell
choco install gradle
```

**Vrifier l'installation :**
```powershell
gradle --version
# Devrait afficher: Gradle 8.x
```

### tape 3 : Compiler et Lancer

```powershell
cd d:\STARTUP\koras\backend
gradle :backend-services:run
```

**Rsultat attendu :**
```
> Task :backend-services:run
Dmarrage de l'assistant vocal Koras...
Application Koras dmarre sur http://0.0.0.0:8080
```

### tape 4 : Vrifier que a fonctionne

Ouvrir un **autre terminal** :

```powershell
curl http://localhost:8080/health
```

**Rponse attendue :**
```json
{
"status": "UP",
"timestamp": "2024-12-20T...",
"version": "1.0.0"
}
```

---

## Commandes Utiles

```powershell
# Compiler sans lancer
gradle build

# Tests
gradle test

# Nettoyer et recompiler
gradle clean build

# Arrter l'application
Ctrl + C dans le terminal
```

---

## Problmes Courants

### "gradle: command not found"
**Solution :** Fermez et rouvrez PowerShell aprs l'installation de Gradle

### "Port 8080 dj utilis"
**Solution :**
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### "Java not found"
**Solution :** Installez Java 17+ :
```powershell
choco install temurin17
```

### Gradle tlcharge des dpendances (long)
**Normal :** Premire fois seulement. Patience (~2-5 minutes)

---

## Checklist

- [ ] Chocolatey install
- [ ] Gradle install (`gradle --version` fonctionne)
- [ ] Java 17+ install (`java -version` affiche 17+)
- [ ] Dans le bon dossier (`cd d:\STARTUP\koras\backend`)
- [ ] Lancer : `gradle :backend-services:run`
- [ ] Tester : `curl http://localhost:8080/health`

---

## C'Est Parti !

**Une seule commande :**

```powershell
gradle :backend-services:run
```

**Puis testez :**
- http://localhost:8080/health
- http://localhost:8080/

**Frontend :**
```powershell
cd ..\frontend
npm install
npm run dev
```

---

**Questions ? Voir INSTALLATION_GRADLE.md pour plus de solutions**
