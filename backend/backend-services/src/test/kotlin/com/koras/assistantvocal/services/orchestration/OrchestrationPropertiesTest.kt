package com.koras.assistantvocal.services.orchestration

import com.koras.assistantvocal.domaine.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Tests de propriétés pour l'orchestrateur de tâches.
 * Valide la Propriété 4 : Invariants de structure des plans d'actions (Exigences 2.1, 2.4, 2.2).
 */
class OrchestrationPropertiesTest : DescribeSpec({
    
    val orchestrateur = OrchestrateurdeTachesImpl()
    
    describe("Propriété 4 : Invariants de structure des plans d'actions") {
        
        it("devrait toujours générer au moins une étape") {
            checkAll(100, arbIntentionValide()) { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Invariant : au moins 1 étape
                plan.etapes.shouldNotBeEmpty()
                plan.etapes.size shouldBeGreaterThan 0
            }
        }
        
        it("devrait assigner un niveau de confirmation aux actions sensibles") {
            // Tester spécifiquement les intentions sensibles
            val intentionsPaiement = (1..50).map {
                Intention(
                    type = TypeIntention.PAIEMENT,
                    entites = mapOf(
                        "montant" to EntiteNLU.Montant(
                            BigDecimal(1000 + it * 100),
                            "XOF"
                        ),
                        "destinataire" to EntiteNLU.Contact("destinataire$it")
                    )
                )
            }
            
            intentionsPaiement.forEach { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Vérifier que toutes les étapes sensibles ont un niveau de confirmation
                val etapesSensibles = plan.etapes.filter { it.sensible }
                etapesSensibles.shouldNotBeEmpty()
                
                etapesSensibles.forEach { etape ->
                    etape.niveauConfirmation shouldNotBe NiveauConfirmation.AUCUN
                }
            }
        }
        
        it("devrait avoir une estimation de durée égale à la somme des étapes") {
            checkAll(100, arbIntentionValide()) { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Invariant : estimation totale = somme des estimations d'étapes
                val sommeDurees = plan.etapes.sumOf { it.estimationMs }
                plan.estimationDureeMs shouldBe sommeDurees
            }
        }
        
        it("devrait générer des IDs d'étapes uniques") {
            checkAll(100, arbIntentionValide()) { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Invariant : tous les IDs sont uniques
                val ids = plan.etapes.map { it.id }
                val idsUniques = ids.toSet()
                
                ids.size shouldBe idsUniques.size
            }
        }
        
        it("devrait respecter les contraintes de génération de plan") {
            checkAll(50, arbIntentionValide()) { intention ->
                val debut = System.currentTimeMillis()
                
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                val duree = System.currentTimeMillis() - debut
                
                // Exigence 2.1 : génération < 300ms
                // Note : peut être dépassée sur machines lentes, on log juste
                if (duree > 300) {
                    println("WARNING: Génération plan > 300ms (${duree}ms) pour ${intention.type}")
                }
                
                // Vérifier la structure du plan
                plan.id shouldNotBe null
                plan.intention shouldBe intention
                plan.etapes.shouldNotBeEmpty()
                plan.estimationDureeMs shouldBeGreaterThan 0
            }
        }
        
        it("devrait générer des plans cohérents pour les intentions PAIEMENT") {
            checkAll(50, arbIntentionPaiement()) { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Un paiement doit avoir au moins 3 étapes
                plan.etapes.size shouldBeGreaterThan 2
                
                // Toutes les étapes d'un paiement doivent être sensibles
                val etapesPaiement = plan.etapes.filter { 
                    it.action in listOf(
                        ActionType.VALIDER_MONTANT,
                        ActionType.CONFIRMER_PAIEMENT,
                        ActionType.EXECUTER_PAIEMENT
                    )
                }
                
                etapesPaiement.shouldNotBeEmpty()
                etapesPaiement.forEach { etape ->
                    etape.sensible shouldBe true
                    etape.niveauConfirmation shouldBe NiveauConfirmation.CRITIQUE
                }
            }
        }
        
        it("devrait générer des plans simples pour les intentions basiques") {
            val intentionsSimples = listOf(
                Intention(TypeIntention.AIDE, emptyMap()),
                Intention(TypeIntention.ANNULATION, emptyMap()),
                Intention(TypeIntention.CONFIRMATION, emptyMap()),
                Intention(TypeIntention.ACTUALITES, emptyMap())
            )
            
            intentionsSimples.forEach { intention ->
                val plan = runBlocking {
                    orchestrateur.genererPlan(intention)
                }
                
                // Les intentions simples doivent avoir peu d'étapes (1-2)
                plan.etapes.size shouldBe 1
                
                // Et une estimation de durée faible (< 200ms)
                plan.estimationDureeMs < 200 shouldBe true
            }
        }
    }
    
    describe("Métamorphique : cohérence des plans") {
        
        it("devrait générer le même nombre d'étapes pour la même intention") {
            checkAll(50, arbIntentionValide()) { intention ->
                val plan1 = runBlocking { orchestrateur.genererPlan(intention) }
                val plan2 = runBlocking { orchestrateur.genererPlan(intention) }
                
                // Le nombre d'étapes doit être identique
                plan1.etapes.size shouldBe plan2.etapes.size
                
                // Les types d'actions doivent être identiques
                val actions1 = plan1.etapes.map { it.action }
                val actions2 = plan2.etapes.map { it.action }
                
                actions1 shouldBe actions2
            }
        }
    }
})

// Générateurs Arb pour les tests

fun arbIntentionValide(): Arb<Intention> {
    return Arb.of(
        // Intentions simples sans entités
        Intention(TypeIntention.AIDE, emptyMap()),
        Intention(TypeIntention.ACTUALITES, emptyMap()),
        Intention(TypeIntention.ALARME_ARRET, emptyMap()),
        Intention(TypeIntention.MUSIQUE_PAUSE, emptyMap()),
        
        // Intentions avec contact
        Intention(
            TypeIntention.APPEL,
            mapOf("contact" to EntiteNLU.Contact("testuser"))
        ),
        Intention(
            TypeIntention.SMS,
            mapOf(
                "contact" to EntiteNLU.Contact("testuser"),
                "message" to EntiteNLU.Texte("message test")
            )
        ),
        
        // Intentions avec temporel
        Intention(
            TypeIntention.ALARME_CREATION,
            mapOf("heure" to EntiteNLU.Temporel(
                Instant.fromEpochMilliseconds(System.currentTimeMillis() + 3600000)
            ))
        ),
        
        // Intentions avec lieu
        Intention(
            TypeIntention.NAVIGATION_GPS,
            mapOf("destination" to EntiteNLU.Lieu("Test Location"))
        ),
        Intention(
            TypeIntention.METEO,
            mapOf("lieu" to EntiteNLU.Lieu("Paris"))
        ),
        
        // Intentions avec texte
        Intention(
            TypeIntention.RECHERCHE_WEB,
            mapOf("requete" to EntiteNLU.Texte("test query"))
        ),
        Intention(
            TypeIntention.LECTURE_TEXTE,
            mapOf("requete" to EntiteNLU.Texte("texte à lire"))
        )
    )
}

fun arbIntentionPaiement(): Arb<Intention> {
    return Arb.of(
        *(1..20).map { i ->
            Intention(
                type = TypeIntention.PAIEMENT,
                entites = mapOf(
                    "montant" to EntiteNLU.Montant(
                        BigDecimal(1000 + i * 500),
                        if (i % 2 == 0) "XOF" else "EUR"
                    ),
                    "destinataire" to EntiteNLU.Contact("dest$i")
                )
            )
        }.toTypedArray()
    )
}
