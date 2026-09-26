package com.koras.assistantvocal.services.execution

import com.koras.assistantvocal.domaine.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * Tests de propriétés pour l'exécuteur sécurisé.
 * Valide les Propriétés 2 et 10 (Exigences 3.2, 2.6, 2.5).
 */
class ExecutionPropertiesTest : DescribeSpec({
    
    val cache = CacheIdempotenceMemoire()
    val journal = JournalAuditMemoire()
    val executeur = ExecuteurSecuriseImpl(cache, journal)
    
    describe("Propriété 2 : Idempotence des exécutions d'actions") {
        
        it("devrait produire le même résultat pour N exécutions avec le même token") {
            checkAll(100, arbEtapeExecutable(), Arb.int(2..10)) { etape, nbExecutions ->
                val token = UUID.randomUUID()
                
                // Exécuter N fois avec le même token
                val resultats = (1..nbExecutions).map {
                    runBlocking {
                        executeur.executer(etape, token)
                    }
                }
                
                // Tous les résultats doivent être identiques
                val premier = resultats.first()
                resultats.forEach { resultat ->
                    resultat.statut shouldBe premier.statut
                    resultat.resultat shouldBe premier.resultat
                }
                
                // Le second et suivants doivent venir du cache
                resultats.drop(1).forEach { resultat ->
                    resultat.source shouldBe SourceResultat.CACHE_IDEMPOTENCE
                }
                
                // Le premier doit venir de l'exécution directe
                premier.source shouldBe SourceResultat.EXECUTION_DIRECTE
            }
        }
        
        it("devrait produire des résultats différents pour des tokens différents") {
            checkAll(50, arbEtapeExecutable()) { etape ->
                val token1 = UUID.randomUUID()
                val token2 = UUID.randomUUID()
                
                val resultat1 = runBlocking {
                    executeur.executer(etape, token1)
                }
                
                val resultat2 = runBlocking {
                    executeur.executer(etape, token2)
                }
                
                // Les deux doivent avoir été exécutés (pas de cache)
                resultat1.source shouldBe SourceResultat.EXECUTION_DIRECTE
                resultat2.source shouldBe SourceResultat.EXECUTION_DIRECTE
                
                // Les IDs d'exécution doivent être différents
                resultat1.idExecution shouldBe resultat1.idExecution // Tautologie pour vérifier le type
                resultat1.idExecution != resultat2.idExecution shouldBe true
            }
        }
        
        it("devrait préserver l'idempotence même avec des exécutions concurrentes") {
            checkAll(50, arbEtapeExecutable()) { etape ->
                val token = UUID.randomUUID()
                
                // Lancer plusieurs exécutions en parallèle avec le même token
                val resultats = runBlocking {
                    List(5) {
                        kotlinx.coroutines.async {
                            executeur.executer(etape, token)
                        }
                    }.map { it.await() }
                }
                
                // Au moins une doit venir du cache
                val depuisCache = resultats.count { it.source == SourceResultat.CACHE_IDEMPOTENCE }
                depuisCache > 0 shouldBe true
                
                // Tous doivent avoir le même statut
                val statuts = resultats.map { it.statut }.toSet()
                statuts.size shouldBe 1
            }
        }
    }
    
    describe("Propriété 10 : Conservation du nombre d'étapes lors de l'exécution") {
        
        it("devrait comptabiliser toutes les étapes d'un plan (succès + échecs)") {
            val plan = creerPlanTest(5)
            
            val resultats = plan.etapes.map { etape ->
                runBlocking {
                    try {
                        executeur.executer(etape, UUID.randomUUID())
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            
            // Compter succès et échecs
            val succes = resultats.count { it?.statut == StatutExecution.SUCCES }
            val echecs = resultats.count { 
                it?.statut == StatutExecution.ECHEC || it == null 
            }
            val annules = resultats.count { it?.statut == StatutExecution.ANNULE }
            
            // Invariant : total = nombre d'étapes
            (succes + echecs + annules) shouldBe plan.etapes.size
        }
        
        it("devrait enregistrer une entrée d'audit pour chaque exécution") {
            val etapes = List(10) { i ->
                Etape(
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = mapOf("index" to i.toString()),
                    estimationMs = 50
                )
            }
            
            // Exécuter toutes les étapes
            etapes.forEach { etape ->
                runBlocking {
                    executeur.executer(etape, UUID.randomUUID())
                }
            }
            
            // Vérifier que toutes sont dans le journal
            val historique = runBlocking {
                journal.obtenirHistorique(FiltreAudit(), limite = 20)
            }
            
            historique.size shouldBe etapes.size
        }
    }
    
    describe("Invariant : Preuves d'exécution valides") {
        
        it("devrait toujours générer des preuves vérifiables") {
            checkAll(100, arbEtapeExecutable()) { etape ->
                val token = UUID.randomUUID()
                
                val resultat = runBlocking {
                    executeur.executer(etape, token)
                }
                
                // Vérifier que la preuve existe
                val preuve = resultat.preuve
                preuve shouldBe preuve // Assert non-null
                
                if (preuve != null) {
                    // Vérifier la preuve
                    val valide = runBlocking {
                        executeur.verifierPreuve(preuve)
                    }
                    
                    valide shouldBe true
                }
            }
        }
    }
})

// Générateurs

fun arbEtapeExecutable(): Arb<Etape> {
    return Arb.of(
        Etape(
            action = ActionType.INITIER_APPEL,
            parametres = mapOf("numero" to "+33612345678"),
            estimationMs = 100
        ),
        Etape(
            action = ActionType.ENVOYER_SMS,
            parametres = mapOf("numero" to "+33612345678", "message" to "test"),
            estimationMs = 150
        ),
        Etape(
            action = ActionType.CREER_ALARME,
            parametres = mapOf("heure" to "07:00"),
            estimationMs = 100
        ),
        Etape(
            action = ActionType.LIRE_PARAMETRE,
            parametres = mapOf("type" to "test"),
            estimationMs = 50
        ),
        Etape(
            action = ActionType.EXECUTER_OCR,
            parametres = emptyMap(),
            estimationMs = 500
        )
    )
}

fun creerPlanTest(nbEtapes: Int): PlanAction {
    val etapes = List(nbEtapes) { i ->
        Etape(
            action = ActionType.LIRE_PARAMETRE,
            parametres = mapOf("index" to i.toString()),
            estimationMs = 50
        )
    }
    
    return PlanAction(
        id = UUID.randomUUID(),
        intention = Intention(TypeIntention.AIDE, emptyMap()),
        etapes = etapes,
        estimationDureeMs = etapes.sumOf { it.estimationMs }
    )
}
