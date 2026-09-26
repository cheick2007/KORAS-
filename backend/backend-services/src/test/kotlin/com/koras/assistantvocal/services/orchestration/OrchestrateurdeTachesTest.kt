package com.koras.assistantvocal.services.orchestration

import com.koras.assistantvocal.domaine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.math.BigDecimal

class OrchestrateurdeTachesTest : DescribeSpec({
    
    val orchestrateur = OrchestrateurdeTachesImpl()
    
    describe("OrchestrateurdeTaches") {
        
        context("génération de plans pour intentions simples") {
            
            it("devrait générer un plan pour APPEL") {
                val intention = Intention(
                    type = TypeIntention.APPEL,
                    entites = mapOf("contact" to EntiteNLU.Contact("maman"))
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 2
                plan.etapes[0].action shouldBe ActionType.RESOUDRE_CONTACT
                plan.etapes[1].action shouldBe ActionType.INITIER_APPEL
                
                // Vérifier les préconditions
                plan.etapes[0].preconditions shouldContain Precondition.PERMISSION_CONTACTS
                plan.etapes[1].preconditions shouldContain Precondition.PERMISSION_PHONE
            }
            
            it("devrait générer un plan pour SMS") {
                val intention = Intention(
                    type = TypeIntention.SMS,
                    entites = mapOf(
                        "contact" to EntiteNLU.Contact("papa"),
                        "message" to EntiteNLU.Texte("message test")
                    )
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 2
                plan.etapes[0].action shouldBe ActionType.RESOUDRE_CONTACT
                plan.etapes[1].action shouldBe ActionType.ENVOYER_SMS
            }
            
            it("devrait générer un plan pour ALARME_CREATION") {
                val heure = Instant.fromEpochMilliseconds(System.currentTimeMillis() + 3600000)
                val intention = Intention(
                    type = TypeIntention.ALARME_CREATION,
                    entites = mapOf("heure" to EntiteNLU.Temporel(heure))
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 1
                plan.etapes[0].action shouldBe ActionType.CREER_ALARME
            }
            
            it("devrait générer un plan pour NAVIGATION") {
                val intention = Intention(
                    type = TypeIntention.NAVIGATION_GPS,
                    entites = mapOf("destination" to EntiteNLU.Lieu("gare"))
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 2
                plan.etapes[0].action shouldBe ActionType.RESOUDRE_ADRESSE
                plan.etapes[1].action shouldBe ActionType.DEMARRER_NAVIGATION
                
                // Vérifier les préconditions
                plan.etapes[0].preconditions shouldContain Precondition.CONNECTIVITE_INTERNET
                plan.etapes[1].preconditions shouldContain Precondition.PERMISSION_LOCATION
            }
            
            it("devrait générer un plan pour OCR_CAPTURE") {
                val intention = Intention(
                    type = TypeIntention.OCR_CAPTURE,
                    entites = emptyMap()
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 3
                plan.etapes[0].action shouldBe ActionType.CAPTURER_IMAGE
                plan.etapes[1].action shouldBe ActionType.EXECUTER_OCR
                plan.etapes[2].action shouldBe ActionType.LIRE_TEXTE
            }
        }
        
        context("génération de plans pour intentions sensibles") {
            
            it("devrait marquer les étapes de PAIEMENT comme sensibles") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(BigDecimal("5000"), "XOF"),
                        "destinataire" to EntiteNLU.Contact("boutique")
                    )
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                plan.etapes.size shouldBe 3
                
                // Toutes les étapes du paiement doivent être sensibles
                plan.etapes.forEach { etape ->
                    etape.sensible shouldBe true
                    etape.niveauConfirmation shouldBe NiveauConfirmation.CRITIQUE
                }
                
                // Vérifier les actions
                plan.etapes[0].action shouldBe ActionType.VALIDER_MONTANT
                plan.etapes[1].action shouldBe ActionType.CONFIRMER_PAIEMENT
                plan.etapes[2].action shouldBe ActionType.EXECUTER_PAIEMENT
                
                // Vérifier la stratégie de compensation
                plan.etapes[2].strategieCompensation shouldBe CompensationStrategy.COMPENSATION_SPECIFIQUE
            }
        }
        
        context("gestion des erreurs") {
            
            it("devrait lever une exception si contact manquant pour APPEL") {
                val intention = Intention(
                    type = TypeIntention.APPEL,
                    entites = emptyMap() // Manque le contact !
                )
                
                shouldThrow<IllegalArgumentException> {
                    runBlocking {
                        orchestrateur.genererPlan(intention)
                    }
                }.message shouldBe "Contact manquant pour APPEL"
            }
            
            it("devrait lever une exception si montant manquant pour PAIEMENT") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "destinataire" to EntiteNLU.Contact("boutique")
                        // Manque le montant !
                    )
                )
                
                shouldThrow<IllegalArgumentException> {
                    runBlocking {
                        orchestrateur.genererPlan(intention)
                    }
                }
            }
        }
        
        context("calcul d'estimations") {
            
            it("devrait estimer correctement la durée totale") {
                val intention = Intention(
                    type = TypeIntention.OCR_CAPTURE,
                    entites = emptyMap()
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // 3 étapes : 200ms + 500ms + 100ms = 800ms
                plan.estimationDureeMs shouldBe 800
                
                // Vérifier que c'est bien la somme
                val somme = plan.etapes.sumOf { it.estimationMs }
                plan.estimationDureeMs shouldBe somme
            }
            
            it("devrait avoir des estimations raisonnables pour tous les plans") {
                val intentions = listOf(
                    Intention(TypeIntention.AIDE, emptyMap()),
                    Intention(
                        TypeIntention.APPEL,
                        mapOf("contact" to EntiteNLU.Contact("test"))
                    ),
                    Intention(
                        TypeIntention.NAVIGATION_GPS,
                        mapOf("destination" to EntiteNLU.Lieu("test"))
                    )
                )
                
                intentions.forEach { intention ->
                    val plan = runBlocking {
                        orchestrateur.genererPlan(intention)
                    }
                    
                    // Toutes les estimations doivent être > 0
                    plan.estimationDureeMs shouldBeGreaterThan 0
                    plan.etapes.forEach { etape ->
                        etape.estimationMs shouldBeGreaterThan 0
                    }
                }
            }
        }
        
        context("préconditions") {
            
            it("devrait identifier les préconditions pour NAVIGATION") {
                val intention = Intention(
                    type = TypeIntention.NAVIGATION_GPS,
                    entites = mapOf("destination" to EntiteNLU.Lieu("test"))
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Collecter toutes les préconditions
                val preconditions = plan.etapes.flatMap { it.preconditions }.toSet()
                
                preconditions shouldContain Precondition.CONNECTIVITE_INTERNET
                preconditions shouldContain Precondition.PERMISSION_LOCATION
                preconditions shouldContain Precondition.CONNECTIVITE_GPS
            }
            
            it("devrait identifier les préconditions pour PAIEMENT") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(BigDecimal("1000"), "XOF"),
                        "destinataire" to EntiteNLU.Contact("test")
                    )
                )
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                val preconditions = plan.etapes.flatMap { it.preconditions }.toSet()
                
                preconditions shouldContain Precondition.SOLDE_SUFFISANT
                preconditions shouldContain Precondition.CONNECTIVITE_INTERNET
            }
        }
        
        context("alternatives") {
            
            it("devrait proposer des alternatives pour préconditions manquantes") {
                val intention = Intention(
                    type = TypeIntention.SMS,
                    entites = mapOf("contact" to EntiteNLU.Contact("test"))
                )
                
                val alternatives = runBlocking {
                    orchestrateur.proposerAlternatives(
                        intention,
                        listOf(Precondition.PERMISSION_SMS)
                    )
                }
                
                alternatives.shouldNotBeEmpty()
                
                // L'alternative devrait inclure une étape pour demander la permission
                val premiereAlternative = alternatives.first()
                val actionsAlternative = premiereAlternative.etapes.map { it.action }
                
                actionsAlternative shouldContain ActionType.VERIFIER_PERMISSION
            }
        }
        
        context("performance") {
            
            it("devrait générer un plan en moins de 300ms") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(BigDecimal("5000"), "XOF"),
                        "destinataire" to EntiteNLU.Contact("test")
                    )
                )
                
                val debut = System.currentTimeMillis()
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                val duree = System.currentTimeMillis() - debut
                
                // Exigence 2.1 : génération < 300ms
                duree < 300 shouldBe true
                
                plan shouldNotBe null
            }
        }
    }
})
