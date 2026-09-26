package com.koras.assistantvocal.services.gateway

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

class ServiceAuthentificationTest : DescribeSpec({
    
    val authService = ServiceAuthentificationImpl()
    
    describe("ServiceAuthentification") {
        
        context("génération de tokens") {
            
            it("devrait générer une paire de tokens valide") {
                val userId = UUID.randomUUID()
                val roles = listOf("USER", "ADMIN")
                
                val tokenPaire = authService.genererToken(userId, roles)
                
                tokenPaire.accessToken shouldNotBe null
                tokenPaire.refreshToken shouldNotBe null
                tokenPaire.expiresIn shouldBe 3600L
                tokenPaire.tokenType shouldBe "Bearer"
            }
            
            it("devrait générer des tokens différents pour chaque appel") {
                val userId = UUID.randomUUID()
                val roles = listOf("USER")
                
                val token1 = authService.genererToken(userId, roles)
                val token2 = authService.genererToken(userId, roles)
                
                token1.accessToken shouldNotBe token2.accessToken
                token1.refreshToken shouldNotBe token2.refreshToken
            }
        }
        
        context("validation de tokens") {
            
            it("devrait valider un token correct") {
                val userId = UUID.randomUUID()
                val roles = listOf("USER")
                
                val tokenPaire = authService.genererToken(userId, roles)
                val validation = authService.validerToken(tokenPaire.accessToken)
                
                validation.shouldBeInstanceOf<ResultatValidation.Valide>()
                val valide = validation as ResultatValidation.Valide
                valide.userId shouldBe userId
                valide.roles shouldBe roles
            }
            
            it("devrait rejeter un token malformé") {
                val validation = authService.validerToken("token_invalide")
                
                validation.shouldBeInstanceOf<ResultatValidation.Invalide>()
            }
            
            it("devrait rejeter un token avec signature incorrecte") {
                val autreService = ServiceAuthentificationImpl(secret = "autre_secret")
                val userId = UUID.randomUUID()
                
                val tokenPaire = autreService.genererToken(userId, listOf("USER"))
                val validation = authService.validerToken(tokenPaire.accessToken)
                
                validation.shouldBeInstanceOf<ResultatValidation.Invalide>()
            }
        }
        
        context("révocation de tokens") {
            
            it("devrait révoquer un token avec succès") {
                val userId = UUID.randomUUID()
                val tokenPaire = authService.genererToken(userId, listOf("USER"))
                
                // Valider avant révocation
                val validationAvant = authService.validerToken(tokenPaire.accessToken)
                validationAvant.shouldBeInstanceOf<ResultatValidation.Valide>()
                
                // Révoquer
                authService.revoquerToken(tokenPaire.accessToken)
                
                // Valider après révocation
                val validationApres = authService.validerToken(tokenPaire.accessToken)
                validationApres shouldBe ResultatValidation.Revoque
            }
        }
        
        context("rafraîchissement de tokens") {
            
            it("devrait rafraîchir un token avec un refresh token valide") {
                val userId = UUID.randomUUID()
                val roles = listOf("USER", "PREMIUM")
                
                val tokenInitial = authService.genererToken(userId, roles)
                val tokenNouveau = authService.rafraichirToken(tokenInitial.refreshToken)
                
                tokenNouveau shouldNotBe null
                tokenNouveau!!.accessToken shouldNotBe tokenInitial.accessToken
                
                // Valider le nouveau token
                val validation = authService.validerToken(tokenNouveau.accessToken)
                validation.shouldBeInstanceOf<ResultatValidation.Valide>()
                
                val valide = validation as ResultatValidation.Valide
                valide.userId shouldBe userId
                valide.roles shouldBe roles
            }
            
            it("devrait rejeter un refresh token invalide") {
                val tokenNouveau = authService.rafraichirToken("refresh_invalide")
                
                tokenNouveau shouldBe null
            }
            
            it("devrait consommer le refresh token après utilisation") {
                val userId = UUID.randomUUID()
                val tokenInitial = authService.genererToken(userId, listOf("USER"))
                
                // Premier rafraîchissement : OK
                val token1 = authService.rafraichirToken(tokenInitial.refreshToken)
                token1 shouldNotBe null
                
                // Deuxième rafraîchissement avec le même : KO
                val token2 = authService.rafraichirToken(tokenInitial.refreshToken)
                token2 shouldBe null
            }
        }
        
        context("types de tokens") {
            
            it("devrait rejeter un refresh token utilisé comme access token") {
                val userId = UUID.randomUUID()
                val tokenPaire = authService.genererToken(userId, listOf("USER"))
                
                val validation = authService.validerToken(tokenPaire.refreshToken)
                
                validation shouldBe ResultatValidation.TypeInvalide
            }
        }
    }
})
