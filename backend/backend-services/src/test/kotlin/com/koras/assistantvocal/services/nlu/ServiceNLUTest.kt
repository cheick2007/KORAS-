package com.koras.assistantvocal.services.nlu

import com.koras.assistantvocal.domaine.*
import com.koras.assistantvocal.services.parsing.ParserCommandes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import java.util.Base64
import java.util.UUID

class ServiceNLUTest : DescribeSpec({
    
    val nluEdge = NLUEdge(ParserCommandes())
    val serviceNLU = ServiceNLUImpl(nluEdge, null)
    
    describe("ServiceNLU") {
        
        context("interprétation des 20 intentions prioritaires") {
            
            it("devrait interpréter une commande APPEL") {
                val audio = creerAudioTest("appelle maman")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.APPEL
                resultat.confiance shouldNotBe null
                resultat.sourceTraitement shouldBe SourceNLU.EDGE
                
                val contact = resultat.intention.obtenirEntite<EntiteNLU.Contact>("contact")
                contact?.nom shouldBe "maman"
            }
            
            it("devrait interpréter une commande SMS") {
                val audio = creerAudioTest("envoie un message à papa : je rentre tard")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.SMS
                
                val contact = resultat.intention.obtenirEntite<EntiteNLU.Contact>("contact")
                contact?.nom shouldBe "papa"
                
                val message = resultat.intention.obtenirEntite<EntiteNLU.Texte>("message")
                message?.contenu shouldBe "je rentre tard"
            }
            
            it("devrait interpréter une commande ALARME") {
                val audio = creerAudioTest("réveille-moi à 7h30")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.ALARME_CREATION
                
                val heure = resultat.intention.obtenirEntite<EntiteNLU.Temporel>("heure")
                heure shouldNotBe null
            }
            
            it("devrait interpréter une commande NAVIGATION") {
                val audio = creerAudioTest("navigue vers la gare")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.NAVIGATION_GPS
                
                val destination = resultat.intention.obtenirEntite<EntiteNLU.Lieu>("destination")
                destination?.adresse shouldBe "la gare"
            }
            
            it("devrait interpréter une commande RECHERCHE_WEB") {
                val audio = creerAudioTest("recherche restaurant dakar")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.RECHERCHE_WEB
                
                val requete = resultat.intention.obtenirEntite<EntiteNLU.Texte>("requete")
                requete?.contenu shouldBe "restaurant dakar"
            }
            
            it("devrait interpréter une commande METEO") {
                val audio = creerAudioTest("quel temps fait-il")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.METEO
            }
            
            it("devrait interpréter une commande ACTUALITES") {
                val audio = creerAudioTest("actualités")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.ACTUALITES
            }
            
            it("devrait interpréter une commande AIDE") {
                val audio = creerAudioTest("aide")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.AIDE
            }
            
            it("devrait interpréter une commande PAIEMENT") {
                val audio = creerAudioTest("paie 5000 francs à boutique")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.intention.type shouldBe TypeIntention.PAIEMENT
                
                val montant = resultat.intention.obtenirEntite<EntiteNLU.Montant>("montant")
                montant?.valeur?.toInt() shouldBe 5000
            }
        }
        
        context("seuil de confiance") {
            
            it("devrait rejeter une commande incompréhensible (confiance < 70%)") {
                val audio = creerAudioTest("blabla incompréhensible xyz")
                
                val exception = shouldThrow<ConfianceInsuffisanteException> {
                    runBlocking {
                        serviceNLU.interpreter(audio)
                    }
                }
                
                exception.confiance shouldBe 30f // Confiance par défaut pour échec de parsing
            }
            
            it("devrait valider une commande claire (confiance >= 70%)") {
                val audio = creerAudioTest("appelle marie")
                
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                
                resultat.estConfiant() shouldBe true
                resultat.confiance shouldNotBe null
                resultat.confiance!! >= 70f shouldBe true
            }
        }
        
        context("détection de langue") {
            
            it("devrait détecter la langue française") {
                val audio = creerAudioTest("bonjour")
                
                val langue = runBlocking {
                    serviceNLU.detecterLangue(audio)
                }
                
                // Pour le MVP, on retourne toujours FRANCAIS
                langue shouldBe Langue.FRANCAIS
            }
        }
        
        context("latence") {
            
            it("devrait interpréter en moins de 500ms") {
                val audio = creerAudioTest("appelle paul")
                
                val debut = System.currentTimeMillis()
                val resultat = runBlocking {
                    serviceNLU.interpreter(audio)
                }
                val duree = System.currentTimeMillis() - debut
                
                // Vérifier que la latence est < 500ms (exigence 1.1)
                duree < 500 shouldBe true
                
                // Vérifier que le résultat contient la durée
                resultat.dureeMs shouldNotBe null
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
        dureeMs = texte.length.toLong() * 50 // Simuler ~50ms par caractère
    )
}
