# ========================================================
# KORAS Backend - Multi-stage Production Dockerfile
# Kotlin / Ktor High-Performance Microservice
# ========================================================

# Étape 1 : Compilation du projet avec Gradle
FROM gradle:8.13-jdk17-alpine AS builder
WORKDIR /home/gradle/project

# Copie des fichiers sources du backend
COPY backend/ .

# Compilation et création de la distribution autonome
RUN chmod +x ./gradlew && ./gradlew :backend-services:installDist --no-daemon

# Étape 2 : Image d'exécution minimale et sécurisée
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -S koras && adduser -S koras -G koras
USER koras

# Récupération de la distribution compilée
COPY --chown=koras:koras --from=builder /home/gradle/project/backend-services/build/install/backend-services /app

# Exposition du port de l'API
EXPOSE 8080
ENV PORT=8080

# Démarrage du microservice KORAS
ENTRYPOINT ["/app/bin/backend-services"]
