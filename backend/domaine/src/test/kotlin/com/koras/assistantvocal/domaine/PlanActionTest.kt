package com.koras.assistantvocal.domaine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class PlanActionTest : DescribeSpec({
    
    describe("PlanAction") {
        context("invariants structurels") {
            it("devrait valider un plan avec une étape") {
                val intention = Intention(
                    type = TypeIntention.APPEL,
                    entites = mapOf("contact" to EntiteNLU.Contact("test"))
                )
                
                val etape = Etape(
                    action = ActionType.INITIER_APPEL,
                    parametres = mapOf("numero" to "+33612345678"),
                    estimationMs = 100
                )
                
                val plan = PlanAction(
                    id = UUID.randomUUID(),
                    intention = intention,
                    etapes = listOf(etape),
                    estimationDureeMs = 100
                )
                
                plan.etapes.size shouldBe 1
                plan.estimationDureeMs shouldBe 100
            }
            
            it("devrait échouer si aucune étape") {
                val intention = Intention(
                    type = TypeIntention.AIDE,
                    entites = emptyMap()
                )
                
                shouldThrow<IllegalArgumentException> {
                    PlanAction(
                        id = UUID.randomUUID(),
                        intention = intention,
                        etapes = emptyList(),
                        estimationDureeMs = 0
                    )
                }.message shouldBe "Un plan d'actions doit contenir au moins une étape"
            }
            
            it("devrait échouer si estimation incohérente") {
                val intention = Intention(
                    type = TypeIntention.SMS,
                    entites = mapOf("contact" to EntiteNLU.Contact("test"))
                )
                
                val etapes = listOf(
                    Etape(action = ActionType.RESOUDRE_CONTACT, parametres = emptyMap(), estimationMs = 50),
                    Etape(action = ActionType.ENVOYER_SMS, parametres = emptyMap(), estimationMs = 100)
                )
                
                shouldThrow<IllegalArgumentException> {
                    PlanAction(
                        id = UUID.randomUUID(),
                        intention = intention,
                        etapes = etapes,
                        estimationDureeMs = 200 // devrait être 150
                    )
                }
            }
            
            it("devrait échouer si IDs d'étapes non uniques") {
                val intention = Intention(
                    type = TypeIntention.APPEL,
                    entites = mapOf("contact" to EntiteNLU.Contact("test"))
                )
                
                val idCommun = UUID.randomUUID()
                val etapes = listOf(
                    Etape(id = idCommun, action = ActionType.RESOUDRE_CONTACT, parametres = emptyMap(), estimationMs = 50),
                    Etape(id = idCommun, action = ActionType.INITIER_APPEL, parametres = emptyMap(), estimationMs = 100)
                )
                
                shouldThrow<IllegalArgumentException> {
                    PlanAction(
                        id = UUID.randomUUID(),
                        intention = intention,
                        etapes = etapes,
                        estimationDureeMs = 150
                    )
                }.message shouldBe "Tous les IDs d'étapes doivent être uniques"
            }
            
            it("devrait échouer si étape sensible sans confirmation") {
                val intention = Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(
                            valeur = java.math.BigDecimal("1000"),
                            devise = "XOF"
                        )
                    )
                )
                
                val etape = Etape(
                    action = ActionType.EXECUTER_PAIEMENT,
                    parametres = emptyMap(),
                    estimationMs = 500,
                    sensible = true,
                    niveauConfirmation = NiveauConfirmation.AUCUN // Invalide !
                )
                
                shouldThrow<IllegalArgumentException> {
                    PlanAction(
                        id = UUID.randomUUID(),
                        intention = intention,
                        etapes = listOf(etape),
                        estimationDureeMs = 500
                    )
                }
            }
        }
    }
    
    describe("Etape") {
        it("devrait valider une étape simple") {
            val etape = Etape(
                action = ActionType.ENVOYER_SMS,
                parametres = mapOf("numero" to "+33612345678", "message" to "test"),
                estimationMs = 200
            )
            
            etape.etat shouldBe EtatEtape.EN_ATTENTE
            etape.sensible shouldBe false
            etape.niveauConfirmation shouldBe NiveauConfirmation.AUCUN
        }
        
        it("devrait valider une étape sensible avec confirmation") {
            val etape = Etape(
                action = ActionType.EXECUTER_PAIEMENT,
                parametres = emptyMap(),
                estimationMs = 1000,
                sensible = true,
                niveauConfirmation = NiveauConfirmation.CRITIQUE
            )
            
            etape.sensible shouldBe true
            etape.niveauConfirmation shouldBe NiveauConfirmation.CRITIQUE
        }
    }
})
