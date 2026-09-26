package com.koras.assistantvocal.domaine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import java.math.BigDecimal

class EntiteNLUTest : DescribeSpec({
    
    describe("EntiteNLU.Contact") {
        it("devrait valider un contact avec nom et numéro") {
            val contact = EntiteNLU.Contact("maman", "+33612345678")
            
            contact.nom shouldBe "maman"
            contact.numero shouldBe "+33612345678"
        }
        
        it("devrait valider un contact sans numéro") {
            val contact = EntiteNLU.Contact("boutique")
            
            contact.nom shouldBe "boutique"
            contact.numero shouldBe null
        }
        
        it("devrait échouer avec un nom vide") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Contact("", "+33612345678")
            }
        }
        
        it("devrait échouer avec un numéro invalide") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Contact("test", "abc")
            }
        }
    }
    
    describe("EntiteNLU.Montant") {
        it("devrait valider un montant positif") {
            val montant = EntiteNLU.Montant(
                valeur = BigDecimal("1000.50"),
                devise = "XOF"
            )
            
            montant.valeur shouldBe BigDecimal("1000.50")
            montant.devise shouldBe "XOF"
        }
        
        it("devrait accepter un montant zéro") {
            val montant = EntiteNLU.Montant(BigDecimal.ZERO, "EUR")
            montant.valeur shouldBe BigDecimal.ZERO
        }
        
        it("devrait échouer avec un montant négatif") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Montant(BigDecimal("-100"), "USD")
            }.message shouldBe "Le montant ne peut pas être négatif"
        }
        
        it("devrait échouer avec une devise invalide") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Montant(BigDecimal("100"), "EURO")
            }.message shouldBe "La devise doit être un code ISO 4217 valide (3 lettres)"
        }
    }
    
    describe("EntiteNLU.Temporel") {
        it("devrait valider une date valide") {
            val instant = Instant.fromEpochMilliseconds(1700000000000)
            val temporel = EntiteNLU.Temporel(instant)
            
            temporel.instant shouldBe instant
        }
        
        it("devrait échouer avec une date avant l'époque Unix") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Temporel(Instant.fromEpochMilliseconds(-1000))
            }
        }
    }
    
    describe("EntiteNLU.Lieu") {
        it("devrait valider un lieu avec coordonnées") {
            val lieu = EntiteNLU.Lieu(
                adresse = "10 Rue de la Paix, Paris",
                latitude = 48.8566,
                longitude = 2.3522
            )
            
            lieu.adresse shouldBe "10 Rue de la Paix, Paris"
            lieu.latitude shouldBe 48.8566
            lieu.longitude shouldBe 2.3522
        }
        
        it("devrait valider un lieu sans coordonnées") {
            val lieu = EntiteNLU.Lieu("Dakar, Sénégal")
            
            lieu.adresse shouldBe "Dakar, Sénégal"
            lieu.latitude shouldBe null
        }
        
        it("devrait échouer avec latitude invalide") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Lieu("test", latitude = 95.0, longitude = 0.0)
            }.message shouldBe "La latitude doit être entre -90 et 90"
        }
        
        it("devrait échouer avec longitude invalide") {
            shouldThrow<IllegalArgumentException> {
                EntiteNLU.Lieu("test", latitude = 0.0, longitude = 200.0)
            }.message shouldBe "La longitude doit être entre -180 et 180"
        }
    }
})
