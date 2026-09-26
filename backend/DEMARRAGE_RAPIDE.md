# 🚀 Démarrage Rapide - Backend Koras

## Vous Êtes ICI : `d:\STARTUP\koras\backend`

### ⚡ Solution Ultra-Rapide

```powershell
# 1. Installer Gradle avec Chocolatey
choco install gradle

# 2. Lancer
gradle :backend-services:run
```

**C'est tout ! L'API sera sur http://localhost:8080** 🎉

---

## 📋 Étapes Détaillées

### Étape 1 : Installer Chocolatey (si nécessaire)

Chocolatey est un gestionnaire de paquets pour Windows (comme apt sur Linux).

**Ouvrir PowerShell en Administrateur :**
1. Clic droit sur le bouton Démarrer
2. Sélectionner "Windows PowerShell (Admin)" ou "Terminal (Admin)"

**Exécuter :**
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### Étape 2 : Installer Gradle

```powershell
choco install gradle
```

**Vérifier l'installation :**
```powershell
gradle --version
# Devrait afficher: Gradle 8.x
```

### Étape 3 : Compiler et Lancer

```powershell
cd d:\STARTUP\koras\backend
gradle :backend-services:run
```

**Résultat attendu :**
```
> Task :backend-services:run
🚀 Démarrage de l'assistant vocal Koras...
✅ Application Koras démarrée sur http://0.0.0.0:8080
```

### Étape 4 : Vérifier que ça fonctionne

Ouvrir un **autre terminal** :

```powershell
curl http://localhost:8080/health
```

**Réponse attendue :**
```json
{
  "status": "UP",
  "timestamp": "2024-12-20T...",
  "version": "1.0.0"
}
```

---

## 🎯 Commandes Utiles

```powershell
# Compiler sans lancer
gradle build

# Tests
gradle test

# Nettoyer et recompiler
gradle clean build

# Arrêter l'application
Ctrl + C dans le terminal
```

---

## 🚨 Problèmes Courants

### "gradle: command not found"
**Solution :** Fermez et rouvrez PowerShell après l'installation de Gradle

### "Port 8080 déjà utilisé"
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

### Gradle télécharge des dépendances (long)
**Normal :** Première fois seulement. Patience (~2-5 minutes)

---

## ✅ Checklist

- [ ] Chocolatey installé
- [ ] Gradle installé (`gradle --version` fonctionne)
- [ ] Java 17+ installé (`java -version` affiche 17+)
- [ ] Dans le bon dossier (`cd d:\STARTUP\koras\backend`)
- [ ] Lancer : `gradle :backend-services:run`
- [ ] Tester : `curl http://localhost:8080/health`

---

## 🎉 C'Est Parti !

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
