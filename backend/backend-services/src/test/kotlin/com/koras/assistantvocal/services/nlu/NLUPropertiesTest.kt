package com.koras.assistantvocal.services.nlu

import com.koras.assistantvocal.domaine.AudioBuffer
import com.koras.assistantvocal.domaine.ConfianceInsuffisanteException
import com.koras.assistantvocal.services.parsing.ParserCommandes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import java.util.Base64

/**
 * Tests de propriétés pour le service NLU.
 * Valide la Propriété 9 : Détection d'intentions avec seuil de confiance (Exigence 1.2).
 */
class NLUPropertiesTest : DescribeSpec({
    
    val nluEdge = NLUEdge(ParserCommandes())
    val serviceNLU = ServiceNLUImpl(nluEdge, null)
    
    describe("Propriété 9 : Détection d'intentions avec seuil de confiance") {
        
        it("devrait rejeter les commandes avec confiance < 70%") {
            // Générer des commandes incompréhensibles qui devraient avoir confiance < 70%
            val commandesIncomprehensibles = listOf(
                "xyz abc def ghi",
                "blabla test random",
                "aaa bbb ccc ddd",
                "1234567890",
                "!!!???###",
                "  "
            )
            
            commandesIncomprehensibles.forEach { commande ->
                val audio = creerAudioTest(commande)
                
                val exception = shouldThrow<ConfianceInsuffisanteException> {
                    runBlocking {
                        serviceNLU.interpreter(audio)
                    }
                }
                
                // Vérifier que la confiance est bien < 70%
                exception.confiance < 70f shouldBe true
            }
        }
        
        it("devrait accepter les commandes claires avec confiance >= 70%") {
            val commandesClaires = listOf(
                "appelle maman",
                "envoie un message à papa",
                "réveille-moi à 7h",
                "navigue vers la gare",
                "recherche restaurant",
                "météo",
                "actualités",
                "aide"
            )
            
            commandesClaires.forEach { commande ->
                val audio = creerAudioTest(commande)
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                // Vérifier que la confiance est >= 70%
                resultat.confiance >= 70f shouldBe true
                resultat.estConfiant() shouldBe true
            }
        }
        
        it("devrait avoir une confiance décroissante avec la longueur du texte") {
            checkAll(50, Arb.string(1..10), Arb.string(100..200)) { commandeCourte, commandeLongue ->
                // Ajouter un préfixe valide pour que le parsing fonctionne
                val audioCourt = creerAudioTest("appelle $commandeCourte")
                val audioLong = creerAudioTest("appelle $commandeLongue")
                
                val resultatCourt = runBlocking {
                    try {
                        serviceNLU.interpreter(audioCourt)
                    } catch (e: ConfianceInsuffisanteException) {
                        null
                    }
                }
                
                val resultatLong = runBlocking {
                    try {
                        serviceNLU.interpreter(audioLong)
                    } catch (e: ConfianceInsuffisanteException) {
                        null
                    }
                }
                
                // Si les deux ont réussi, vérifier que le court a plus de confiance
                if (resultatCourt != null && resultatLong != null) {
                    resultatCourt.confiance >= resultatLong.confiance shouldBe true
                }
            }
        }
    }
    
    describe("Invariant : niveau de confiance entre 0 et 100") {
        
        it("devrait toujours retourner une confiance entre 0 et 100%") {
            val commandes = listOf(
                "appelle marie",
                "xyz abc",
                "météo",
                "blabla incompréhensible long texte qui ne veut rien dire du tout"
            )
            
            commandes.forEach { commande ->
                val audio = creerAudioTest(commande)
                
                try {
                    val resultat = runBlocking {
                        serviceNLU.interpreter(audio)
                    }
                    
                    resultat.confiance >= 0f shouldBe true
                    resultat.confiance <= 100f shouldBe true
                } catch (e: ConfianceInsuffisanteException) {
                    e.confiance >= 0f shouldBe true
                    e.confiance <= 100f shouldBe true
                }
            }
        }
    }
})

/**
 * Crée un AudioBuffer de test à partir de texte.
 */
private fun creerAudioTest(texte: String): AudioBuffer {
    val donneesBase64 = Base64.getEncoder().encodeToString(texte.toByteArray())
    
    return AudioBuffer(
        donnees = donneesBase64,
        format = "TEXT_MOCK",
        frequenceEchantillonnage = 16000,
        canaux = 1,
        dureeMs = texte.length.toLong() * 50
    )
}
