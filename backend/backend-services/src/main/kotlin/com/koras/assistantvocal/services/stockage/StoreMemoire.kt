package com.koras.assistantvocal.services.stockage

import kotlinx.datetime.Instant
import mu.KotlinLogging
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger {}

/**
 * Interface du store mémoire avec chiffrement.
 * Gère les préférences utilisateur et données sensibles.
 */
interface StoreMemoire {
    /**
     * Stocke une valeur chiffrée.
     */
    suspend fun stocker(cle: String, valeur: String, categorieSecurite: CategorieSecurite)
    
    /**
     * Récupère et déchiffre une valeur.
     */
    suspend fun recuperer(cle: String): String?
    
    /**
     * Supprime une valeur de manière sécurisée.
     */
    suspend fun supprimer(cle: String): Boolean
    
    /**
     * Liste toutes les clés (sans déchiffrer les valeurs).
     */
    suspend fun listerCles(categorie: CategorieSecurite? = null): List<String>
    
    /**
     * Exporte toutes les données chiffrées.
     */
    suspend fun exporter(): DonneesExportees
    
    /**
     * Importe des données chiffrées.
     */
    suspend fun importer(donnees: DonneesExportees): ResultatImport
    
    /**
     * Vide complètement le store.
     */
    suspend fun vider(): Int
}

/**
 * Catégorie de sécurité des données.
 */
enum class CategorieSecurite {
    PUBLIQUE,       // Non chiffrée
    CONFIDENTIELLE, // Chiffrée AES-256
    CRITIQUE        // Chiffrée + signature
}

/**
 * Implémentation du store avec chiffrement AES-256-GCM.
 */
@OptIn(ExperimentalEncodingApi::class)
class StoreMemoireChiffre(
    private val cleChiffrement: SecretKey = genererCleAES256()
) : StoreMemoire {
    
    private data class EntreeStore(
        val valeurChiffree: ByteArray,
        val iv: ByteArray,
        val categorieSecurite: CategorieSecurite,
        val horodatage: Instant,
        val signature: String? = null
    )
    
    private val store = mutableMapOf<String, EntreeStore>()
    private val random = SecureRandom()
    
    override suspend fun stocker(
        cle: String,
        valeur: String,
        categorieSecurite: CategorieSecurite
    ) {
        logger.debug { "Stockage de '$cle' (catégorie: $categorieSecurite)" }
        
        when (categorieSecurite) {
            CategorieSecurite.PUBLIQUE -> {
                // Pas de chiffrement pour les données publiques
                store[cle] = EntreeStore(
                    valeurChiffree = valeur.toByteArray(),
                    iv = ByteArray(0),
                    categorieSecurite = categorieSecurite,
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                )
            }
            
            CategorieSecurite.CONFIDENTIELLE -> {
                // Chiffrement AES-256-GCM
                val iv = ByteArray(12)
                random.nextBytes(iv)
                
                val chiffre = valeur.chiffrerAES(cleChiffrement, iv)
                
                store[cle] = EntreeStore(
                    valeurChiffree = chiffre,
                    iv = iv,
                    categorieSecurite = categorieSecurite,
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                )
            }
            
            CategorieSecurite.CRITIQUE -> {
                // Chiffrement + signature
                val iv = ByteArray(12)
                random.nextBytes(iv)
                
                val chiffre = valeur.chiffrerAES(cleChiffrement, iv)
                val signature = signerDonnees(valeur)
                
                store[cle] = EntreeStore(
                    valeurChiffree = chiffre,
                    iv = iv,
                    categorieSecurite = categorieSecurite,
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    signature = signature
                )
            }
        }
        
        logger.info { "Clé '$cle' stockée avec succès" }
    }
    
    override suspend fun recuperer(cle: String): String? {
        val entree = store[cle] ?: run {
            logger.debug { "Clé '$cle' introuvable" }
            return null
        }
        
        return when (entree.categorieSecurite) {
            CategorieSecurite.PUBLIQUE -> {
                String(entree.valeurChiffree)
            }
            
            CategorieSecurite.CONFIDENTIELLE -> {
                entree.valeurChiffree.dechiffrerAES(cleChiffrement, entree.iv)
            }
            
            CategorieSecurite.CRITIQUE -> {
                val valeur = entree.valeurChiffree.dechiffrerAES(cleChiffrement, entree.iv)
                
                // Vérifier la signature
                if (entree.signature != null) {
                    val signatureValide = verifierSignature(valeur, entree.signature)
                    if (!signatureValide) {
                        logger.error { "Signature invalide pour la clé '$cle'" }
                        throw SecurityException("Signature invalide pour la clé '$cle'")
                    }
                }
                
                valeur
            }
        }
    }
    
    override suspend fun supprimer(cle: String): Boolean {
        val entree = store.remove(cle)
        
        if (entree != null) {
            // Suppression sécurisée : écraser les données en mémoire
            entree.valeurChiffree.fill(0)
            entree.iv.fill(0)
            
            logger.info { "Clé '$cle' supprimée de manière sécurisée" }
            return true
        }
        
        logger.debug { "Clé '$cle' introuvable pour suppression" }
        return false
    }
    
    override suspend fun listerCles(categorie: CategorieSecurite?): List<String> {
        return if (categorie == null) {
            store.keys.toList()
        } else {
            store.filterValues { it.categorieSecurite == categorie }
                .keys.toList()
        }
    }
    
    override suspend fun exporter(): DonneesExportees {
        logger.info { "Export de ${store.size} entrées" }
        
        val entrees = store.map { (cle, entree) ->
            EntreeExportee(
                cle = cle,
                valeurChiffree = Base64.encode(entree.valeurChiffree),
                iv = Base64.encode(entree.iv),
                categorieSecurite = entree.categorieSecurite,
                horodatage = entree.horodatage,
                signature = entree.signature
            )
        }
        
        return DonneesExportees(
            version = "1.0",
            dateExport = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            nbEntrees = entrees.size,
            entrees = entrees
        )
    }
    
    override suspend fun importer(donnees: DonneesExportees): ResultatImport {
        logger.info { "Import de ${donnees.nbEntrees} entrées (version: ${donnees.version})" }
        
        if (donnees.version != "1.0") {
            return ResultatImport.Echec("Version non supportée: ${donnees.version}")
        }
        
        var succes = 0
        var echecs = 0
        val erreurs = mutableListOf<String>()
        
        for (entree in donnees.entrees) {
            try {
                val entreeStore = EntreeStore(
                    valeurChiffree = Base64.decode(entree.valeurChiffree),
                    iv = Base64.decode(entree.iv),
                    categorieSecurite = entree.categorieSecurite,
                    horodatage = entree.horodatage,
                    signature = entree.signature
                )
                
                store[entree.cle] = entreeStore
                succes++
            } catch (e: Exception) {
                echecs++
                erreurs.add("Erreur import '${entree.cle}': ${e.message}")
                logger.error(e) { "Erreur import '${entree.cle}'" }
            }
        }
        
        return if (echecs == 0) {
            ResultatImport.Succes(succes)
        } else {
            ResultatImport.Partiel(succes, echecs, erreurs)
        }
    }
    
    override suspend fun vider(): Int {
        val nb = store.size
        
        // Suppression sécurisée de toutes les entrées
        store.values.forEach { entree ->
            entree.valeurChiffree.fill(0)
            entree.iv.fill(0)
        }
        
        store.clear()
        
        logger.info { "$nb entrées vidées du store" }
        return nb
    }
    
    // --- Méthodes privées ---
    
    private fun signerDonnees(donnees: String): String {
        // SHA-256 pour MVP, HMAC-SHA256 pour production
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(donnees.toByteArray())
            .toHexString()
    }
    
    private fun verifierSignature(donnees: String, signature: String): Boolean {
        val signatureCalculee = signerDonnees(donnees)
        return signatureCalculee == signature
    }
}

/**
 * Extensions pour chiffrement/déchiffrement AES.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun String.chiffrerAES(cle: SecretKey, iv: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.ENCRYPT_MODE, cle, spec)
    return cipher.doFinal(this.toByteArray())
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.dechiffrerAES(cle: SecretKey, iv: ByteArray): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, cle, spec)
    return String(cipher.doFinal(this))
}

/**
 * Génère une clé AES-256.
 */
private fun genererCleAES256(): SecretKey {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256, SecureRandom())
    return keyGen.generateKey()
}

/**
 * Conversion ByteArray vers hex.
 */
private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

/**
 * Données exportées.
 */
data class DonneesExportees(
    val version: String,
    val dateExport: Instant,
    val nbEntrees: Int,
    val entrees: List<EntreeExportee>
)

data class EntreeExportee(
    val cle: String,
    val valeurChiffree: String, // Base64
    val iv: String,              // Base64
    val categorieSecurite: CategorieSecurite,
    val horodatage: Instant,
    val signature: String?
)

/**
 * Résultat d'un import.
 */
sealed class ResultatImport {
    data class Succes(val nbEntrees: Int) : ResultatImport()
    data class Partiel(val succes: Int, val echecs: Int, val erreurs: List<String>) : ResultatImport()
    data class Echec(val raison: String) : ResultatImport()
}
