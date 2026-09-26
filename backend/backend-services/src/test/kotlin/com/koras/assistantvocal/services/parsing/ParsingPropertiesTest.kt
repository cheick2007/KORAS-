package com.koras.assistantvocal.services.parsing

import com.koras.assistantvocal.domaine.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Tests de propriétés pour le parsing et formatage des commandes.
 * Valide la Propriété 1 : Round-trip parsing des intentions (Exigence 11.5).
 */
class ParsingPropertiesTest : DescribeSpec({
    
    val parser = ParserCommandes()
    val formateur = FormateurCommandes()
    
    describe("Propriété 1 : Round-trip parsing des intentions") {
        
        it("devrait préserver les intentions APPEL via round-trip") {
            checkAll(100, arbIntentionAppel()) { intention ->
                // Format l'intention en texte
                val texte = formateur.formatter(intention, Langue.FRANCAIS)
                
                // Parse le texte
                val intentionParsee = parser.parser(texte)
                
                // Vérifications
                intentionParsee.type shouldBe intention.type
                
                val contactOriginal = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                val contactParse = intentionParsee.obtenirEntite<EntiteNLU.Contact>("contact")
                
                contactParse?.nom shouldBe contactOriginal?.nom
            }
        }
        
        it("devrait préserver les intentions SMS via round-trip") {
            checkAll(100, arbIntentionSMS()) { intention ->
                val texte = formateur.formatter(intention, Langue.FRANCAIS)
                val intentionParsee = parser.parser(texte)
                
                intentionParsee.type shouldBe intention.type
                
                val contactOriginal = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                val contactParse = intentionParsee.obtenirEntite<EntiteNLU.Contact>("contact")
                
                contactParse?.nom shouldBe contactOriginal?.nom
                
                // Le message peut être présent ou non
                val messageOriginal = intention.obtenirEntite<EntiteNLU.Texte>("message")
                if (messageOriginal != null) {
                    val messageParse = intentionParsee.obtenirEntite<EntiteNLU.Texte>("message")
                    messageParse?.contenu shouldBe messageOriginal.contenu
                }
            }
        }
        
        it("devrait préserver les intentions PAIEMENT via round-trip") {
            checkAll(100, arbIntentionPaiement()) { intention ->
                val texte = formateur.formatter(intention, Langue.FRANCAIS)
                val intentionParsee = parser.parser(texte)
                
                intentionParsee.type shouldBe intention.type
                
                val montantOriginal = intention.obtenirEntite<EntiteNLU.Montant>("montant")
                val montantParse = intentionParsee.obtenirEntite<EntiteNLU.Montant>("montant")
                
                // La valeur peut varier légèrement à cause du formatage
                // On accepte une différence minimale
                montantParse?.valeur?.toDouble()?.let { parsed ->
                    montantOriginal?.valeur?.toDouble()?.let { original ->
                        kotlin.math.abs(parsed - original) shouldBe 0.0
                    }
                }
                
                montantParse?.devise shouldBe montantOriginal?.devise
            }
        }
        
        it("devrait préserver les intentions sans entités via round-trip") {
            val intentionsSansEntites = listOf(
                Intention(TypeIntention.AIDE, emptyMap()),
                Intention(TypeIntention.ANNULATION, emptyMap()),
                Intention(TypeIntention.CONFIRMATION, emptyMap()),
                Intention(TypeIntention.ACTUALITES, emptyMap()),
                Intention(TypeIntention.OCR_CAPTURE, emptyMap())
            )
            
            intentionsSansEntites.forEach { intention ->
                val texte = formateur.formatter(intention, Langue.FRANCAIS)
                val intentionParsee = parser.parser(texte)
                
                intentionParsee.type shouldBe intention.type
            }
        }
    }
    
    describe("Propriété 7 : Préservation des entités lors du parsing") {
        
        it("devrait extraire toutes les entités d'une commande APPEL") {
            checkAll(100, Arb.string(5..20)) { nomContact ->
                val commande = "appelle $nomContact"
                val intention = parser.parser(commande)
                
                intention.type shouldBe TypeIntention.APPEL
                
                val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                contact?.nom shouldBe nomContact
            }
        }
        
        it("devrait extraire toutes les entités d'une commande SMS avec message") {
            checkAll(50, Arb.string(5..20), Arb.string(10..50)) { nomContact, message ->
                val commande = "envoie un message à $nomContact : $message"
                val intention = parser.parser(commande)
                
                intention.type shouldBe TypeIntention.SMS
                
                val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                contact?.nom shouldBe nomContact
                
                val texteParse = intention.obtenirEntite<EntiteNLU.Texte>("message")
                texteParse?.contenu shouldBe message
            }
        }
        
        it("devrait extraire correctement les montants avec devises") {
            val commandesPaiement = listOf(
                "paie 1000 francs à boutique" to BigDecimal("1000"),
                "envoie 5000 XOF à marie" to BigDecimal("5000"),
                "transfert de 250 à papa" to BigDecimal("250")
            )
            
            commandesPaiement.forEach { (commande, montantAttendu) ->
                val intention = parser.parser(commande)
                
                intention.type shouldBe TypeIntention.PAIEMENT
                
                val montant = intention.obtenirEntite<EntiteNLU.Montant>("montant")
                montant?.valeur shouldBe montantAttendu
            }
        }
    }
})

// Générateurs Kotest Arb pour les intentions

fun arbIntentionAppel(): Arb<Intention> {
    return arbitrary {
        val nomContact = Arb.string(5..20, Codepoint.alphanumeric()).bind()
        Intention(
            type = TypeIntention.APPEL,
            entites = mapOf(
                "contact" to EntiteNLU.Contact(nomContact)
            )
        )
    }
}

fun arbIntentionSMS(): Arb<Intention> {
    return arbitrary {
        val nomContact = Arb.string(5..20, Codepoint.alphanumeric()).bind()
        val message = if (Arb.boolean().bind()) {
            Arb.string(10..50, Codepoint.alphanumeric()).bind()
        } else null
        
        val entites = mutableMapOf<String, EntiteNLU>(
            "contact" to EntiteNLU.Contact(nomContact)
        )
        
        if (message != null) {
            entites["message"] = EntiteNLU.Texte(message)
        }
        
        Intention(
            type = TypeIntention.SMS,
            entites = entites
        )
    }
}

fun arbIntentionPaiement(): Arb<Intention> {
    return arbitrary {
        val montant = Arb.bigDecimal(
            min = BigDecimal("100"),
            max = BigDecimal("100000")
        ).bind()
        
        val devise = Arb.of("XOF", "EUR", "USD").bind()
        val destinataire = Arb.string(5..20, Codepoint.alphanumeric()).bind()
        
        Intention(
            type = TypeIntention.PAIEMENT,
            entites = mapOf(
                "montant" to EntiteNLU.Montant(montant, devise),
                "destinataire" to EntiteNLU.Contact(destinataire)
            )
        )
    }
}
