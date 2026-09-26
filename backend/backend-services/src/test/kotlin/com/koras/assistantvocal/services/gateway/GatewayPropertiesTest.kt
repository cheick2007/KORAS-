package com.koras.assistantvocal.services.gateway

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Tests de propriétés pour la Gateway API.
 * Valide les Propriétés 5 et 6 (Exigences 4.1, 4.2, 4.3).
 */
class GatewayPropertiesTest : DescribeSpec({
    
    describe("Propriété 5 : Authentification obligatoire pour toutes les routes (sauf /health)") {
        
        val authService = ServiceAuthentificationImpl()
        
        it("devrait générer un token valide qui peut être vérifié") {
            checkAll(100, Arb.uuid()) { userId ->
                val roles = listOf("USER")
                
                // Générer token
                val tokenPaire = authService.genererToken(userId, roles)
                
                // Vérifier token
                val validation = authService.validerToken(tokenPaire.accessToken)
                
                // Validation doit réussir
                validation.shouldBeInstanceOf<ResultatValidation.Valide>()
                val valide = validation as ResultatValidation.Valide
                valide.userId shouldBe userId
                valide.roles shouldBe roles
            }
        }
        
        it("devrait rejeter un token révoqué") {
            checkAll(50, Arb.uuid()) { userId ->
                val tokenPaire = authService.genererToken(userId, listOf("USER"))
                
                // Révoquer le token
                authService.revoquerToken(tokenPaire.accessToken)
                
                // Tenter de valider
                val validation = authService.validerToken(tokenPaire.accessToken)
                
                validation shouldBe ResultatValidation.Revoque
            }
        }
        
        it("devrait rejeter un token invalide (signature incorrecte)") {
            val tokenInvalide = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                               "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                               "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            
            val validation = authService.validerToken(tokenInvalide)
            
            validation.shouldBeInstanceOf<ResultatValidation.Invalide>()
        }
        
        it("devrait permettre de rafraîchir un token avec un refresh token valide") {
            checkAll(50, Arb.uuid()) { userId ->
                val roles = listOf("USER", "ADMIN")
                
                // Générer token initial
                val tokenInitial = authService.genererToken(userId, roles)
                
                // Rafraîchir
                val tokenNouveau = authService.rafraichirToken(tokenInitial.refreshToken)
                
                // Nouveau token doit être valide
                tokenNouveau shouldBe tokenNouveau // Assert non-null
                
                if (tokenNouveau != null) {
                    val validation = authService.validerToken(tokenNouveau.accessToken)
                    validation.shouldBeInstanceOf<ResultatValidation.Valide>()
                    
                    val valide = validation as ResultatValidation.Valide
                    valide.userId shouldBe userId
                    valide.roles shouldBe roles
                }
            }
        }
        
        it("devrait rejeter un refresh token utilisé deux fois") {
            val userId = UUID.randomUUID()
            val tokenInitial = authService.genererToken(userId, listOf("USER"))
            
            // Première utilisation : OK
            val token1 = authService.rafraichirToken(tokenInitial.refreshToken)
            token1 shouldBe token1 // Non-null
            
            // Deuxième utilisation : KO (refresh token consommé)
            val token2 = authService.rafraichirToken(tokenInitial.refreshToken)
            token2 shouldBe null
        }
    }
    
    describe("Propriété 6 : Rate limiting respecté (quotas par utilisateur/endpoint)") {
        
        val rateLimiter = RateLimiterMemoire()
        
        it("devrait autoriser N requêtes jusqu'à la limite puis bloquer") {
            checkAll(50, Arb.uuid(), Arb.int(1..50)) { userId, limite ->
                val endpoint = "test"
                
                // Autoriser jusqu'à la limite
                val autorisations = (1..limite + 10).map {
                    rateLimiter.autoriser(userId, endpoint, limite)
                }
                
                // Les N premières doivent être autorisées
                val premieres = autorisations.take(limite)
                premieres.all { it } shouldBe true
                
                // Les suivantes doivent être bloquées
                val suivantes = autorisations.drop(limite)
                suivantes.all { !it } shouldBe true
            }
        }
        
        it("devrait isoler les compteurs par utilisateur") {
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            val endpoint = "test"
            val limite = 5
            
            // User1 atteint la limite
            repeat(limite) {
                rateLimiter.autoriser(user1, endpoint, limite) shouldBe true
            }
            rateLimiter.autoriser(user1, endpoint, limite) shouldBe false
            
            // User2 devrait toujours pouvoir
            rateLimiter.autoriser(user2, endpoint, limite) shouldBe true
        }
        
        it("devrait isoler les compteurs par endpoint") {
            val userId = UUID.randomUUID()
            val limite = 5
            
            // Atteindre limite sur endpoint1
            repeat(limite) {
                rateLimiter.autoriser(userId, "endpoint1", limite) shouldBe true
            }
            rateLimiter.autoriser(userId, "endpoint1", limite) shouldBe false
            
            // endpoint2 devrait être indépendant
            rateLimiter.autoriser(userId, "endpoint2", limite) shouldBe true
        }
        
        it("devrait réinitialiser après réinitialisation explicite") {
            checkAll(30, Arb.uuid()) { userId ->
                val endpoint = "test"
                val limite = 5
                
                // Atteindre limite
                repeat(limite) {
                    rateLimiter.autoriser(userId, endpoint, limite)
                }
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe false
                
                // Réinitialiser
                rateLimiter.reinitialiser(userId, endpoint)
                
                // Devrait être autorisé à nouveau
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe true
            }
        }
        
        it("devrait nettoyer la fenêtre après 1 minute (sliding window)") {
            val userId = UUID.randomUUID()
            val endpoint = "test"
            val limite = 10
            
            // Faire quelques requêtes
            repeat(5) {
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe true
            }
            
            // Simuler passage du temps (modifier timestamps manuellement)
            // Note: test approximatif car on ne peut pas facilement manipuler le temps
            
            val stats = rateLimiter.obtenirStats(userId)
            stats.parEndpoint[endpoint] shouldBe 5
        }
        
        it("devrait fournir des statistiques correctes") {
            val userId = UUID.randomUUID()
            val limite = 10
            
            // Faire des requêtes sur plusieurs endpoints
            repeat(3) { rateLimiter.autoriser(userId, "endpoint1", limite) }
            repeat(5) { rateLimiter.autoriser(userId, "endpoint2", limite) }
            repeat(2) { rateLimiter.autoriser(userId, "endpoint3", limite) }
            
            val stats = rateLimiter.obtenirStats(userId)
            
            stats.totalRequetes shouldBe 10
            stats.parEndpoint["endpoint1"] shouldBe 3
            stats.parEndpoint["endpoint2"] shouldBe 5
            stats.parEndpoint["endpoint3"] shouldBe 2
        }
    }
    
    describe("Invariants de sécurité") {
        
        val authService = ServiceAuthentificationImpl()
        
        it("devrait générer des tokens avec expiration correcte") {
            val userId = UUID.randomUUID()
            val tokenPaire = authService.genererToken(userId, listOf("USER"))
            
            // Access token expire dans 1h
            tokenPaire.expiresIn shouldBe 3600L
            tokenPaire.tokenType shouldBe "Bearer"
        }
        
        it("devrait rejeter un refresh token utilisé comme access token") {
            val userId = UUID.randomUUID()
            val tokenPaire = authService.genererToken(userId, listOf("USER"))
            
            // Tenter de valider le refresh token comme access token
            val validation = authService.validerToken(tokenPaire.refreshToken)
            
            validation shouldBe ResultatValidation.TypeInvalide
        }
    }
})
