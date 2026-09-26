package com.koras.assistantvocal.services.execution

import com.koras.assistantvocal.domaine.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Tests de propriétés pour le journal d'audit.
 * Valide la Propriété 3 (Exigence 2.8 : intégrité hash chain).
 */
class AuditPropertiesTest : DescribeSpec({
    
    describe("Propriété 3 : Intégrité du journal d'audit (hash chain)") {
        
        it("devrait maintenir l'intégrité après N insertions séquentielles") {
            checkAll(50, Arb.int(1..50)) { nbEntrees ->
                val journal = JournalAuditProduction()
                
                // Insérer N entrées
                repeat(nbEntrees) { i ->
                    val entree = creerEntreeAuditTest(i)
                    runBlocking {
                        journal.enregistrer(entree)
                    }
                }
                
                // Vérifier l'intégrité
                val verification = runBlocking {
                    journal.verifierIntegrite()
                }
                
                verification.shouldBeInstanceOf<ResultatVerification.Valide>()
                (verification as ResultatVerification.Valide).nbEntrees shouldBe nbEntrees
            }
        }
        
        it("devrait détecter une corruption de hash") {
            val journal = JournalAuditProduction()
            
            // Insérer plusieurs entrées
            repeat(10) { i ->
                val entree = creerEntreeAuditTest(i)
                runBlocking {
                    journal.enregistrer(entree)
                }
            }
            
            // Simuler corruption en accédant directement à la liste (via réflexion)
            val champsEntrees = journal.javaClass.getDeclaredField("entrees")
            champsEntrees.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val entrees = champsEntrees.get(journal) as MutableList<EntreeAudit>
            
            if (entrees.size > 5) {
                // Corrompre le hash de l'entrée 5
                val entreeCorrompu = entrees[5].copy(
                    hash = "hash_corrompu_xxx"
                )
                entrees[5] = entreeCorrompu
                
                // Vérifier que la corruption est détectée
                val verification = runBlocking {
                    journal.verifierIntegrite()
                }
                
                verification.shouldBeInstanceOf<ResultatVerification.Compromis>()
                val compromis = verification as ResultatVerification.Compromis
                compromis.position shouldBe 5
            }
        }
        
        it("devrait détecter une rupture de chaîne") {
            val journal = JournalAuditProduction()
            
            // Insérer plusieurs entrées
            repeat(10) { i ->
                val entree = creerEntreeAuditTest(i)
                runBlocking {
                    journal.enregistrer(entree)
                }
            }
            
            // Simuler rupture de chaîne
            val champsEntrees = journal.javaClass.getDeclaredField("entrees")
            champsEntrees.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val entrees = champsEntrees.get(journal) as MutableList<EntreeAudit>
            
            if (entrees.size > 3) {
                // Modifier le hashPrecedent de l'entrée 3
                val entreeModifiee = entrees[3].copy(
                    hashPrecedent = "hash_precedent_invalide"
                )
                entrees[3] = entreeModifiee
                
                // Vérifier que la rupture est détectée
                val verification = runBlocking {
                    journal.verifierIntegrite()
                }
                
                verification.shouldBeInstanceOf<ResultatVerification.Compromis>()
                val compromis = verification as ResultatVerification.Compromis
                compromis.position shouldBe 3
            }
        }
        
        it("devrait détecter une incohérence temporelle") {
            val journal = JournalAuditProduction()
            
            val maintenant = System.currentTimeMillis()
            
            // Insérer des entrées avec horodatages cohérents
            repeat(5) { i ->
                val entree = EntreeAudit(
                    horodatage = Instant.fromEpochMilliseconds(maintenant + i * 1000),
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = mapOf("index" to i.toString()),
                    resultat = StatutExecution.SUCCES,
                    dureeMs = 50,
                    tokenIdempotence = UUID.randomUUID()
                )
                runBlocking {
                    journal.enregistrer(entree)
                }
            }
            
            // Simuler incohérence temporelle
            val champsEntrees = journal.javaClass.getDeclaredField("entrees")
            champsEntrees.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val entrees = champsEntrees.get(journal) as MutableList<EntreeAudit>
            
            if (entrees.size > 3) {
                // Mettre un horodatage dans le passé pour l'entrée 3
                val entreePassee = entrees[3].copy(
                    horodatage = Instant.fromEpochMilliseconds(maintenant - 10000)
                )
                entrees[3] = entreePassee
                
                // Vérifier que l'incohérence est détectée
                val verification = runBlocking {
                    journal.verifierIntegrite()
                }
                
                verification.shouldBeInstanceOf<ResultatVerification.Compromis>()
                val compromis = verification as ResultatVerification.Compromis
                compromis.position shouldBe 3
                compromis.raison shouldBe "Incohérence temporelle détectée"
            }
        }
        
        it("devrait valider un journal vide") {
            val journal = JournalAuditProduction()
            
            val verification = runBlocking {
                journal.verifierIntegrite()
            }
            
            verification.shouldBeInstanceOf<ResultatVerification.Valide>()
            (verification as ResultatVerification.Valide).nbEntrees shouldBe 0
        }
        
        it("devrait préserver l'intégrité avec des insertions concurrentes") {
            val journal = JournalAuditProduction()
            
            // Lancer plusieurs insertions en parallèle
            runBlocking {
                List(20) { i ->
                    kotlinx.coroutines.async {
                        val entree = creerEntreeAuditTest(i)
                        journal.enregistrer(entree)
                    }
                }.forEach { it.await() }
            }
            
            // Vérifier l'intégrité malgré les accès concurrents
            val verification = runBlocking {
                journal.verifierIntegrite()
            }
            
            verification.shouldBeInstanceOf<ResultatVerification.Valide>()
            (verification as ResultatVerification.Valide).nbEntrees shouldBe 20
        }
    }
    
    describe("Historique et filtrage") {
        
        it("devrait filtrer par type d'action") {
            val journal = JournalAuditProduction()
            
            // Insérer différents types d'actions
            repeat(5) { i ->
                val entree = EntreeAudit(
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    action = ActionType.INITIER_APPEL,
                    parametres = mapOf("index" to i.toString()),
                    resultat = StatutExecution.SUCCES,
                    dureeMs = 100,
                    tokenIdempotence = UUID.randomUUID()
                )
                runBlocking { journal.enregistrer(entree) }
            }
            
            repeat(3) { i ->
                val entree = EntreeAudit(
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    action = ActionType.ENVOYER_SMS,
                    parametres = mapOf("index" to i.toString()),
                    resultat = StatutExecution.SUCCES,
                    dureeMs = 150,
                    tokenIdempotence = UUID.randomUUID()
                )
                runBlocking { journal.enregistrer(entree) }
            }
            
            // Filtrer par INITIER_APPEL
            val historique = runBlocking {
                journal.obtenirHistorique(
                    FiltreAudit(typeAction = ActionType.INITIER_APPEL),
                    limite = 100
                )
            }
            
            historique.size shouldBe 5
            historique.all { it.action == ActionType.INITIER_APPEL } shouldBe true
        }
        
        it("devrait filtrer par statut de résultat") {
            val journal = JournalAuditProduction()
            
            // Insérer avec différents statuts
            repeat(3) {
                val entree = EntreeAudit(
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = emptyMap(),
                    resultat = StatutExecution.SUCCES,
                    dureeMs = 50,
                    tokenIdempotence = UUID.randomUUID()
                )
                runBlocking { journal.enregistrer(entree) }
            }
            
            repeat(2) {
                val entree = EntreeAudit(
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = emptyMap(),
                    resultat = StatutExecution.ECHEC,
                    dureeMs = 50,
                    tokenIdempotence = UUID.randomUUID()
                )
                runBlocking { journal.enregistrer(entree) }
            }
            
            // Filtrer par ECHEC
            val historique = runBlocking {
                journal.obtenirHistorique(
                    FiltreAudit(statutResultat = StatutExecution.ECHEC),
                    limite = 100
                )
            }
            
            historique.size shouldBe 2
            historique.all { it.resultat == StatutExecution.ECHEC } shouldBe true
        }
    }
})

// Helpers

fun creerEntreeAuditTest(index: Int): EntreeAudit {
    return EntreeAudit(
        horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis() + index),
        action = ActionType.LIRE_PARAMETRE,
        parametres = mapOf("index" to index.toString()),
        resultat = StatutExecution.SUCCES,
        dureeMs = 50,
        tokenIdempotence = UUID.randomUUID()
    )
}
