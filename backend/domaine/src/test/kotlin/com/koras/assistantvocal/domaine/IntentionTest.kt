package com.koras.assistantvocal.domaine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class IntentionTest : DescribeSpec({
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    describe("Intention") {
        context("validation des intentions") {
            it("devrait valider une intention APPEL avec contact") {
                val intention = Intention(
                    type = TypeIntention.APPEL,
                    entites = mapOf(
                        "contact" to EntiteNLU.Contact("maman", "+33612345678")
                    )
                )
                
                intention.type shouldBe TypeIntention.APPEL
                intention.entites.size shouldBe 1
                intention.obtenirEntite<EntiteNLU.Contact>("contact")?.nom shouldBe "maman"
            }
            
            it("devrait échouer si APPEL sans contact") {
                shouldThrow<IllegalArgumentException> {
                    Intention(
                        type = TypeIntention.APPEL,
                        entites = emptyMap()
                    )
                }.message shouldBe "L'intention APPEL nécessite une entité 'contact'"
            }
            
            it("devrait valider une intention PAIEMENT avec montant") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(
                            valeur = BigDecimal("1000.00"),
                            devise = "XOF"
                        ),
                        "destinataire" to EntiteNLU.Contact("boutique")
                    )
                )
                
                intention.type shouldBe TypeIntention.PAIEMENT
                val montant = intention.obtenirEntite<EntiteNLU.Montant>("montant")
                montant shouldNotBe null
                montant?.valeur shouldBe BigDecimal("1000.00")
            }
            
            it("devrait valider une intention ALARME_CREATION avec heure") {
                val maintenant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val intention = Intention(
                    type = TypeIntention.ALARME_CREATION,
                    entites = mapOf(
                        "heure" to EntiteNLU.Temporel(maintenant)
                    )
                )
                
                intention.type shouldBe TypeIntention.ALARME_CREATION
                intention.obtenirEntite<EntiteNLU.Temporel>("heure")?.instant shouldBe maintenant
            }
        }
        
        context("sérialisation JSON") {
            it("devrait sérialiser et désérialiser correctement") {
                val original = Intention(
                    type = TypeIntention.SMS,
                    entites = mapOf(
                        "contact" to EntiteNLU.Contact("papa", "+33623456789"),
                        "message" to EntiteNLU.Texte("Bonjour !")
                    )
                )
                
                val jsonString = json.encodeToString(original)
                val deserialize = json.decodeFromString<Intention>(jsonString)
                
                deserialize.type shouldBe original.type
                deserialize.entites.size shouldBe original.entites.size
                
                val contact = deserialize.obtenirEntite<EntiteNLU.Contact>("contact")
                contact?.nom shouldBe "papa"
                contact?.numero shouldBe "+33623456789"
            }
        }
    }
})
