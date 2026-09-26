package com.koras.assistantvocal.services.gateway

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class RateLimiterTest : DescribeSpec({
    
    val rateLimiter = RateLimiterMemoire()
    
    describe("RateLimiter") {
        
        context("limites de base") {
            
            it("devrait autoriser les requêtes jusqu'à la limite") {
                val userId = UUID.randomUUID()
                val endpoint = "test"
                val limite = 10
                
                // Les 10 premières doivent passer
                repeat(limite) { i ->
                    val autorise = rateLimiter.autoriser(userId, endpoint, limite)
                    autorise shouldBe true
                }
                
                // La 11ème doit être bloquée
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe false
            }
            
            it("devrait bloquer après dépassement de limite") {
                val userId = UUID.randomUUID()
                val endpoint = "api/test"
                val limite = 5
                
                // Dépasser la limite
                repeat(limite + 3) {
                    rateLimiter.autoriser(userId, endpoint, limite)
                }
                
                // Toutes les tentatives suivantes doivent être bloquées
                repeat(5) {
                    rateLimiter.autoriser(userId, endpoint, limite) shouldBe false
                }
            }
        }
        
        context("isolation par utilisateur") {
            
            it("devrait isoler les compteurs entre utilisateurs") {
                val user1 = UUID.randomUUID()
                val user2 = UUID.randomUUID()
                val endpoint = "shared"
                val limite = 5
                
                // User1 atteint sa limite
                repeat(limite) {
                    rateLimiter.autoriser(user1, endpoint, limite) shouldBe true
                }
                rateLimiter.autoriser(user1, endpoint, limite) shouldBe false
                
                // User2 a son propre compteur
                repeat(limite) {
                    rateLimiter.autoriser(user2, endpoint, limite) shouldBe true
                }
                rateLimiter.autoriser(user2, endpoint, limite) shouldBe false
            }
        }
        
        context("isolation par endpoint") {
            
            it("devrait isoler les compteurs entre endpoints") {
                val userId = UUID.randomUUID()
                val limite = 10
                
                // Remplir endpoint1
                repeat(limite) {
                    rateLimiter.autoriser(userId, "endpoint1", limite) shouldBe true
                }
                rateLimiter.autoriser(userId, "endpoint1", limite) shouldBe false
                
                // endpoint2 est indépendant
                repeat(limite) {
                    rateLimiter.autoriser(userId, "endpoint2", limite) shouldBe true
                }
            }
        }
        
        context("réinitialisation") {
            
            it("devrait réinitialiser le compteur") {
                val userId = UUID.randomUUID()
                val endpoint = "reset-test"
                val limite = 5
                
                // Atteindre limite
                repeat(limite) {
                    rateLimiter.autoriser(userId, endpoint, limite)
                }
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe false
                
                // Réinitialiser
                rateLimiter.reinitialiser(userId, endpoint)
                
                // Devrait fonctionner à nouveau
                rateLimiter.autoriser(userId, endpoint, limite) shouldBe true
            }
        }
        
        context("statistiques") {
            
            it("devrait fournir des stats correctes") {
                val userId = UUID.randomUUID()
                val limite = 20
                
                // Faire des requêtes variées
                repeat(5) { rateLimiter.autoriser(userId, "api/users", limite) }
                repeat(3) { rateLimiter.autoriser(userId, "api/posts", limite) }
                repeat(7) { rateLimiter.autoriser(userId, "api/comments", limite) }
                
                val stats = rateLimiter.obtenirStats(userId)
                
                stats.userId shouldBe userId
                stats.totalRequetes shouldBe 15
                stats.parEndpoint["api/users"] shouldBe 5
                stats.parEndpoint["api/posts"] shouldBe 3
                stats.parEndpoint["api/comments"] shouldBe 7
            }
            
            it("devrait retourner des stats vides pour utilisateur sans requêtes") {
                val userId = UUID.randomUUID()
                
                val stats = rateLimiter.obtenirStats(userId)
                
                stats.totalRequetes shouldBe 0
                stats.parEndpoint.isEmpty() shouldBe true
            }
        }
        
        context("limites différentes") {
            
            it("devrait respecter différentes limites par endpoint") {
                val userId = UUID.randomUUID()
                
                val limiteInterprete = 10
                val limiteExecute = 100
                
                // Remplir interprete
                repeat(limiteInterprete) {
                    rateLimiter.autoriser(userId, "interprete", limiteInterprete) shouldBe true
                }
                rateLimiter.autoriser(userId, "interprete", limiteInterprete) shouldBe false
                
                // Execute a une limite plus haute
                repeat(limiteExecute) {
                    rateLimiter.autoriser(userId, "execute", limiteExecute) shouldBe true
                }
                rateLimiter.autoriser(userId, "execute", limiteExecute) shouldBe false
            }
        }
    }
})
