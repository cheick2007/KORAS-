package com.koras.assistantvocal.services.stockage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking

/**
 * Tests de propriétés pour le store mémoire.
 * Valide la Propriété 8 (Exigence 2.7 : chiffrement).
 */
class StockagePropertiesTest : DescribeSpec({
    
    describe("Propriété 8 : Round-trip encryption (chiffrer → déchiffrer = identité)") {
        
        it("devrait préserver la valeur après chiffrement/déchiffrement (données confidentielles)") {
            checkAll(200, Arb.string(1..1000)) { valeurOriginale ->
                val store = StoreMemoireChiffre()
                val cle = "test_${System.nanoTime()}"
                
                // Stocker avec chiffrement
                runBlocking {
                    store.stocker(cle, valeurOriginale, CategorieSecurite.CONFIDENTIELLE)
                }
                
                // Récupérer et vérifier
                val valeurRecuperee = runBlocking {
                    store.recuperer(cle)
                }
                
                valeurRecuperee shouldBe valeurOriginale
            }
        }
        
        it("devrait préserver la valeur pour données critiques (avec signature)") {
            checkAll(100, Arb.string(1..500)) { valeurOriginale ->
                val store = StoreMemoireChiffre()
                val cle = "critique_${System.nanoTime()}"
                
                // Stocker avec chiffrement + signature
                runBlocking {
                    store.stocker(cle, valeurOriginale, CategorieSecurite.CRITIQUE)
                }
                
                // Récupérer et vérifier
                val valeurRecuperee = runBlocking {
                    store.recuperer(cle)
                }
                
                valeurRecuperee shouldBe valeurOriginale
            }
        }
        
        it("devrait préserver les données publiques sans chiffrement") {
            checkAll(100, Arb.string(1..500)) { valeurOriginale ->
                val store = StoreMemoireChiffre()
                val cle = "public_${System.nanoTime()}"
                
                runBlocking {
                    store.stocker(cle, valeurOriginale, CategorieSecurite.PUBLIQUE)
                }
                
                val valeurRecuperee = runBlocking {
                    store.recuperer(cle)
                }
                
                valeurRecuperee shouldBe valeurOriginale
            }
        }
        
        it("devrait supporter export/import avec préservation des données") {
            checkAll(50, arbPairesCleValeur(5)) { paires ->
                val store1 = StoreMemoireChiffre()
                
                // Stocker dans store1
                paires.forEach { (cle, valeur, categorie) ->
                    runBlocking {
                        store1.stocker(cle, valeur, categorie)
                    }
                }
                
                // Exporter
                val export = runBlocking { store1.exporter() }
                
                // Importer dans store2
                val store2 = StoreMemoireChiffre()
                val resultatImport = runBlocking {
                    store2.importer(export)
                }
                
                // Vérifier que l'import a réussi
                when (resultatImport) {
                    is ResultatImport.Succes -> {
                        resultatImport.nbEntrees shouldBe paires.size
                    }
                    is ResultatImport.Partiel -> {
                        resultatImport.succes shouldBe paires.size
                    }
                    is ResultatImport.Echec -> {
                        throw AssertionError("Import échoué: ${resultatImport.raison}")
                    }
                }
                
                // Vérifier que toutes les données sont présentes
                paires.forEach { (cle, valeurOriginale, _) ->
                    val valeurRecuperee = runBlocking {
                        store2.recuperer(cle)
                    }
                    valeurRecuperee shouldBe valeurOriginale
                }
            }
        }
        
        it("devrait utiliser des IVs différents pour chaque stockage") {
            val store = StoreMemoireChiffre()
            val valeur = "test_identique"
            
            // Stocker la même valeur 10 fois avec des clés différentes
            val cles = (1..10).map { i ->
                val cle = "cle_$i"
                runBlocking {
                    store.stocker(cle, valeur, CategorieSecurite.CONFIDENTIELLE)
                }
                cle
            }
            
            // Exporter pour inspecter les IVs
            val export = runBlocking { store.exporter() }
            val ivs = export.entrees.map { it.iv }.toSet()
            
            // Tous les IVs doivent être différents
            ivs.size shouldBe cles.size
            
            // Mais toutes les valeurs déchiffrées doivent être identiques
            cles.forEach { cle ->
                val valeurRecuperee = runBlocking {
                    store.recuperer(cle)
                }
                valeurRecuperee shouldBe valeur
            }
        }
    }
    
    describe("Invariants de sécurité") {
        
        it("devrait rejeter une signature invalide pour données critiques") {
            val store = StoreMemoireChiffre()
            val cle = "critique_test"
            
            runBlocking {
                store.stocker(cle, "valeur_originale", CategorieSecurite.CRITIQUE)
            }
            
            // Simuler corruption : modifier directement le store (via export/import)
            val export = runBlocking { store.exporter() }
            val entreeCorrompu = export.entrees.first().copy(
                signature = "signature_invalide_xxx"
            )
            
            val exportCorrompu = export.copy(
                entrees = listOf(entreeCorrompu)
            )
            
            val store2 = StoreMemoireChiffre()
            runBlocking {
                store2.importer(exportCorrompu)
            }
            
            // La récupération devrait échouer
            shouldThrow<SecurityException> {
                runBlocking {
                    store2.recuperer(cle)
                }
            }
        }
        
        it("devrait supprimer de manière sécurisée") {
            val store = StoreMemoireChiffre()
            val cle = "a_supprimer"
            val valeur = "donnee_sensible"
            
            runBlocking {
                store.stocker(cle, valeur, CategorieSecurite.CONFIDENTIELLE)
            }
            
            // Vérifier présence
            val avant = runBlocking { store.recuperer(cle) }
            avant shouldBe valeur
            
            // Supprimer
            val supprime = runBlocking { store.supprimer(cle) }
            supprime shouldBe true
            
            // Vérifier absence
            val apres = runBlocking { store.recuperer(cle) }
            apres shouldBe null
        }
        
        it("devrait vider complètement le store") {
            val store = StoreMemoireChiffre()
            
            // Stocker plusieurs entrées
            repeat(10) { i ->
                runBlocking {
                    store.stocker("cle_$i", "valeur_$i", CategorieSecurite.CONFIDENTIELLE)
                }
            }
            
            // Vérifier présence
            val cleesAvant = runBlocking { store.listerCles() }
            cleesAvant.size shouldBe 10
            
            // Vider
            val nbVides = runBlocking { store.vider() }
            nbVides shouldBe 10
            
            // Vérifier absence
            val cleesApres = runBlocking { store.listerCles() }
            cleesApres.size shouldBe 0
        }
    }
})

// Générateurs

data class PaireCleValeur(
    val cle: String,
    val valeur: String,
    val categorie: CategorieSecurite
)

fun arbPairesCleValeur(nb: Int): Arb<List<PaireCleValeur>> {
    return Arb.of(
        List(nb) { i ->
            PaireCleValeur(
                cle = "cle_$i",
                valeur = "valeur_$i",
                categorie = CategorieSecurite.entries[i % 3]
            )
        }
    )
}
