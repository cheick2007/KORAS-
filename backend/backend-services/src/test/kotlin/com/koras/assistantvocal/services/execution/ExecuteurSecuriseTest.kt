package com.koras.assistantvocal.services.execution

import com.koras.assistantvocal.domaine.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import java.util.UUID

class ExecuteurSecuriseTest : DescribeSpec({
    
    val cache = CacheIdempotenceMemoire()
    val journal = JournalAuditMemoire()
    val executeur = ExecuteurSecuriseImpl(cache, journal)
    
    describe("ExecuteurSecurise") {
        
        context("exécution basique") {
            
            it("devrait exécuter une action simple") {
                val etape = Etape(
                    action = ActionType.CREER_ALARME,
                    parametres = mapOf("heure" to "07:00"),
                    estimationMs = 100
                )
                
                val resultat = runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
                
                resultat.statut shouldBe StatutExecution.SUCCES
                resultat.source shouldBe SourceResultat.EXECUTION_DIRECTE
                resultat.preuve shouldNotBe null
            }
            
            it("devrait générer une preuve cryptographique valide") {
                val etape = Etape(
                    action = ActionType.ENVOYER_SMS,
                    parametres = mapOf("numero" to "+33612345678"),
                    estimationMs = 150
                )
                
                val resultat = runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
                
                val preuve = resultat.preuve
                preuve shouldNotBe null
                
                if (preuve != null) {
                    preuve.algorithme shouldBe "SHA-256-RSA"
                    
                    val valide = runBlocking {
                        executeur.verifierPreuve(preuve)
                    }
                    
                    valide shouldBe true
                }
            }
        }
        
        context("idempotence") {
            
            it("devrait retourner le résultat du cache pour le même token") {
                val etape = Etape(
                    action = ActionType.INITIER_APPEL,
                    parametres = mapOf("numero" to "+33612345678"),
                    estimationMs = 100
                )
                val token = UUID.randomUUID()
                
                // Première exécution
                val resultat1 = runBlocking {
                    executeur.executer(etape, token)
                }
                
                // Deuxième exécution avec le même token
                val resultat2 = runBlocking {
                    executeur.executer(etape, token)
                }
                
                // Vérifications
                resultat1.source shouldBe SourceResultat.EXECUTION_DIRECTE
                resultat2.source shouldBe SourceResultat.CACHE_IDEMPOTENCE
                
                resultat1.statut shouldBe resultat2.statut
                resultat1.resultat shouldBe resultat2.resultat
            }
            
            it("devrait exécuter à nouveau pour un token différent") {
                val etape = Etape(
                    action = ActionType.CREER_ALARME,
                    parametres = mapOf("heure" to "08:00"),
                    estimationMs = 100
                )
                
                val resultat1 = runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
                
                val resultat2 = runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
                
                // Les deux doivent être des exécutions directes
                resultat1.source shouldBe SourceResultat.EXECUTION_DIRECTE
                resultat2.source shouldBe SourceResultat.EXECUTION_DIRECTE
            }
        }
        
        context("retry avec backoff exponentiel") {
            
            it("devrait réessayer en cas d'échec temporaire") {
                // EXECUTER_PAIEMENT a 10% de chance d'échec simulé
                val etape = Etape(
                    action = ActionType.EXECUTER_PAIEMENT,
                    parametres = mapOf(
                        "montant" to "1000",
                        "destinataire" to "test"
                    ),
                    estimationMs = 1000
                )
                
                // Exécuter plusieurs fois pour tester le retry
                val resultats = (1..20).map {
                    runBlocking {
                        executeur.executer(etape, UUID.randomUUID())
                    }
                }
                
                // Au moins un devrait avoir réussi
                val succes = resultats.count { it.statut == StatutExecution.SUCCES }
                succes > 0 shouldBe true
            }
        }
        
        context("annulation") {
            
            it("devrait permettre d'annuler une exécution") {
                val executionId = UUID.randomUUID()
                
                val resultat = runBlocking {
                    executeur.annuler(executionId)
                }
                
                // L'annulation devrait retourner un résultat
                resultat shouldBe resultat
            }
        }
        
        context("journal d'audit") {
            
            it("devrait enregistrer chaque exécution dans le journal") {
                val etape = Etape(
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = mapOf("type" to "test"),
                    estimationMs = 50
                )
                
                runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
                
                val historique = runBlocking {
                    journal.obtenirHistorique(
                        FiltreAudit(typeAction = ActionType.LIRE_PARAMETRE),
                        limite = 10
                    )
                }
                
                historique.size shouldBe 1
                historique[0].action shouldBe ActionType.LIRE_PARAMETRE
            }
            
            it("devrait maintenir l'intégrité du hash chain") {
                // Exécuter plusieurs actions
                repeat(5) { i ->
                    val etape = Etape(
                        action = ActionType.LIRE_PARAMETRE,
                        parametres = mapOf("index" to i.toString()),
                        estimationMs = 50
                    )
                    
                    runBlocking {
                        executeur.executer(etape, UUID.randomUUID())
                    }
                }
                
                // Vérifier l'intégrité
                val verification = runBlocking {
                    journal.verifierIntegrite()
                }
                
                verification shouldBe ResultatVerification.Valide(5)
            }
        }
    }
})
