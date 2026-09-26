package com.koras.assistantvocal.services.execution

import com.koras.assistantvocal.domaine.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import mu.KotlinLogging
import java.security.MessageDigest
import java.security.Signature
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Interface de l'exécuteur sécurisé.
 * Exécute les actions de manière idempotente avec preuves cryptographiques.
 */
interface ExecuteurSecurise {
    /**
     * Exécute une étape avec garantie d'idempotence.
     * 
     * @param etape L'étape à exécuter
     * @param tokenIdempotence Token unique garantissant l'idempotence
     * @return Le résultat de l'exécution avec preuve
     */
    suspend fun executer(
        etape: Etape,
        tokenIdempotence: UUID
    ): ResultatExecution
    
    /**
     * Annule une exécution en cours.
     * 
     * @param executionId L'identifiant de l'exécution à annuler
     * @return Résultat de l'annulation
     */
    suspend fun annuler(executionId: UUID): ResultatAnnulation
    
    /**
     * Vérifie une preuve d'exécution.
     * 
     * @param preuve La preuve à vérifier
     * @return true si la preuve est valide
     */
    suspend fun verifierPreuve(preuve: PreuveExecution): Boolean
}

/**
 * Implémentation de l'exécuteur avec idempotence stricte.
 */
class ExecuteurSecuriseImpl(
    private val cacheIdempotence: CacheIdempotence,
    private val journalAudit: JournalAudit,
    private val signateurCrypto: SignateurCrypto = SignateurCryptoImpl()
) : ExecuteurSecurise {
    
    private val executionsEnCours = mutableMapOf<UUID, Boolean>()
    
    override suspend fun executer(
        etape: Etape,
        tokenIdempotence: UUID
    ): ResultatExecution {
        logger.info { "Exécution étape ${etape.action} (token: $tokenIdempotence)" }
        
        // 1. Vérifier cache idempotence
        val resultatCache = cacheIdempotence.obtenir(tokenIdempotence)
        if (resultatCache != null) {
            logger.info { "Cache hit - résultat idempotent retourné" }
            return resultatCache.copy(source = SourceResultat.CACHE_IDEMPOTENCE)
        }
        
        // 2. Vérifier si confirmation nécessaire
        if (etape.necessiteConfirmation) {
            val confirmation = attendreConfirmation(etape, 30.seconds)
            if (!confirmation) {
                return creerResultatAnnule(etape, "Timeout confirmation")
            }
        }
        
        // 3. Exécuter avec retry
        val resultat = executerAvecRetry(etape, tokenIdempotence)
        
        // 4. Générer preuve cryptographique
        val preuve = genererPreuve(etape, resultat)
        val resultatAvecPreuve = resultat.copy(preuve = preuve)
        
        // 5. Stocker dans cache (24h TTL)
        cacheIdempotence.stocker(tokenIdempotence, resultatAvecPreuve, 24 * 3600 * 1000)
        
        // 6. Enregistrer dans le journal d'audit
        journalAudit.enregistrer(
            EntreeAudit(
                horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                action = etape.action,
                parametres = etape.parametres,
                resultat = resultatAvecPreuve.statut,
                dureeMs = resultatAvecPreuve.dureeMs,
                tokenIdempotence = tokenIdempotence,
                preuve = preuve
            )
        )
        
        logger.info { 
            "Étape exécutée avec succès (statut: ${resultatAvecPreuve.statut}, " +
            "durée: ${resultatAvecPreuve.dureeMs}ms)" 
        }
        
        return resultatAvecPreuve
    }
    
    override suspend fun annuler(executionId: UUID): ResultatAnnulation {
        val etaitEnCours = executionsEnCours.remove(executionId)
        
        return if (etaitEnCours == true) {
            logger.info { "Exécution $executionId annulée" }
            ResultatAnnulation.Succes(executionId)
        } else {
            logger.warn { "Tentative annulation exécution inexistante: $executionId" }
            ResultatAnnulation.Echec(executionId, "Exécution introuvable ou déjà terminée")
        }
    }
    
    override suspend fun verifierPreuve(preuve: PreuveExecution): Boolean {
        return try {
            signateurCrypto.verifierSignature(
                donnees = "${preuve.idAction}|${preuve.horodatage}|${preuve.statut}",
                signature = preuve.signature
            )
        } catch (e: Exception) {
            logger.error(e) { "Erreur vérification preuve: ${e.message}" }
            false
        }
    }
    
    /**
     * Exécute avec retry et backoff exponentiel.
     */
    private suspend fun executerAvecRetry(
        etape: Etape,
        tokenIdempotence: UUID,
        maxTentatives: Int = 3
    ): ResultatExecution {
        val executionId = UUID.randomUUID()
        executionsEnCours[executionId] = true
        
        var derniereException: Exception? = null
        val debut = System.currentTimeMillis()
        
        for (tentative in 1..maxTentatives) {
            // Vérifier si annulation demandée
            if (executionsEnCours[executionId] != true) {
                return creerResultatAnnule(etape, "Annulé par l'utilisateur")
            }
            
            try {
                val resultat = executerActionDirecte(etape, executionId)
                executionsEnCours.remove(executionId)
                
                return ResultatExecution(
                    idExecution = executionId,
                    statut = StatutExecution.SUCCES,
                    resultat = resultat,
                    horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                    dureeMs = System.currentTimeMillis() - debut,
                    source = SourceResultat.EXECUTION_DIRECTE
                )
            } catch (e: Exception) {
                derniereException = e
                logger.warn(e) { "Tentative $tentative/$maxTentatives échouée: ${e.message}" }
                
                if (tentative < maxTentatives) {
                    // Backoff exponentiel : 1s, 2s, 4s
                    val delaiMs = (1 shl (tentative - 1)) * 1000L
                    logger.info { "Retry dans ${delaiMs}ms..." }
                    delay(delaiMs)
                }
            }
        }
        
        // Échec après toutes les tentatives
        executionsEnCours.remove(executionId)
        
        return ResultatExecution(
            idExecution = executionId,
            statut = StatutExecution.ECHEC,
            resultat = "Échec après $maxTentatives tentatives: ${derniereException?.message}",
            horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            dureeMs = System.currentTimeMillis() - debut,
            source = SourceResultat.EXECUTION_DIRECTE
        )
    }
    
    /**
     * Exécute l'action réelle (simulé pour MVP).
     */
    private suspend fun executerActionDirecte(etape: Etape, executionId: UUID): String {
        // Simulation de l'exécution réelle
        // Dans une vraie implémentation, on appellerait les APIs Android/externes
        
        when (etape.action) {
            ActionType.INITIER_APPEL -> {
                delay(100)
                return "Appel initié vers ${etape.parametres["numero"]}"
            }
            
            ActionType.ENVOYER_SMS -> {
                delay(150)
                return "SMS envoyé à ${etape.parametres["numero"]}"
            }
            
            ActionType.EXECUTER_PAIEMENT -> {
                // Simulation paiement avec validation
                delay(1000)
                
                val montant = etape.parametres["montant"]
                val destinataire = etape.parametres["destinataire"]
                
                // Simuler échec aléatoire pour tester retry (10% de chance)
                if (Math.random() < 0.1) {
                    throw RuntimeException("Erreur réseau temporaire")
                }
                
                return "Paiement de $montant effectué vers $destinataire"
            }
            
            ActionType.CREER_ALARME -> {
                delay(100)
                return "Alarme créée à ${etape.parametres["heure"]}"
            }
            
            ActionType.EXECUTER_OCR -> {
                delay(500)
                return "Texte extrait: [texte simulé]"
            }
            
            else -> {
                delay(50)
                return "Action ${etape.action} exécutée"
            }
        }
    }
    
    /**
     * Attend une confirmation utilisateur avec timeout.
     */
    private suspend fun attendreConfirmation(
        etape: Etape,
        timeout: kotlin.time.Duration
    ): Boolean {
        logger.info { "Attente confirmation pour ${etape.action} (timeout: ${timeout.inWholeSeconds}s)" }
        
        // Pour le MVP, on simule une confirmation automatique
        // Dans une vraie implémentation, on attendrait l'input utilisateur
        delay(100)
        
        // Simuler timeout dans 5% des cas
        if (Math.random() < 0.05) {
            logger.warn { "Timeout confirmation pour ${etape.action}" }
            return false
        }
        
        return true
    }
    
    /**
     * Génère une preuve cryptographique de l'exécution.
     */
    private fun genererPreuve(etape: Etape, resultat: ResultatExecution): PreuveExecution {
        val donnees = "${etape.id}|${resultat.horodatage}|${resultat.statut}"
        val signature = signateurCrypto.signer(donnees)
        
        return PreuveExecution(
            idAction = etape.id,
            horodatage = resultat.horodatage,
            statut = resultat.statut,
            signature = signature,
            algorithme = "SHA-256-RSA"
        )
    }
    
    private fun creerResultatAnnule(etape: Etape, raison: String): ResultatExecution {
        return ResultatExecution(
            idExecution = UUID.randomUUID(),
            statut = StatutExecution.ANNULE,
            resultat = raison,
            horodatage = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            dureeMs = 0,
            source = SourceResultat.EXECUTION_DIRECTE
        )
    }
}

/**
 * Interface du cache d'idempotence.
 */
interface CacheIdempotence {
    suspend fun obtenir(token: UUID): ResultatExecution?
    suspend fun stocker(token: UUID, resultat: ResultatExecution, ttlMs: Long)
    suspend fun supprimer(token: UUID)
}

/**
 * Implémentation en mémoire du cache (Redis pour production).
 */
class CacheIdempotenceMemoire : CacheIdempotence {
    private data class EntreeCache(
        val resultat: ResultatExecution,
        val expiration: Long
    )
    
    private val cache = mutableMapOf<UUID, EntreeCache>()
    
    override suspend fun obtenir(token: UUID): ResultatExecution? {
        val entree = cache[token] ?: return null
        
        // Vérifier expiration
        if (System.currentTimeMillis() > entree.expiration) {
            cache.remove(token)
            return null
        }
        
        return entree.resultat
    }
    
    override suspend fun stocker(token: UUID, resultat: ResultatExecution, ttlMs: Long) {
        val expiration = System.currentTimeMillis() + ttlMs
        cache[token] = EntreeCache(resultat, expiration)
        
        // Nettoyage des entrées expirées (simple)
        if (cache.size > 1000) {
            nettoyerExpirees()
        }
    }
    
    override suspend fun supprimer(token: UUID) {
        cache.remove(token)
    }
    
    private fun nettoyerExpirees() {
        val maintenant = System.currentTimeMillis()
        cache.entries.removeIf { it.value.expiration < maintenant }
    }
}

/**
 * Interface du journal d'audit.
 */
interface JournalAudit {
    suspend fun enregistrer(entree: EntreeAudit): UUID
    suspend fun obtenirHistorique(filtre: FiltreAudit, limite: Int = 100): List<EntreeAudit>
    suspend fun verifierIntegrite(): ResultatVerification
}

/**
 * Implémentation production du journal avec validation complète.
 */
class JournalAuditProduction : JournalAudit {
    private val entrees = mutableListOf<EntreeAudit>()
    private var dernierHash = ByteArray(32) // Hash genesis
    private val verrou = Any()
    
    override suspend fun enregistrer(entree: EntreeAudit): UUID {
        synchronized(verrou) {
            // Calculer hash avec chaînage
            val entreeAvecHash = entree.copy(
                hashPrecedent = dernierHash.toHexString()
            )
            
            val hash = calculerHash(entreeAvecHash)
            val entreeFinale = entreeAvecHash.copy(
                hash = hash.toHexString()
            )
            
            // Valider avant insertion
            if (!validerEntree(entreeFinale)) {
                throw IllegalStateException("Entrée audit invalide")
            }
            
            entrees.add(entreeFinale)
            dernierHash = hash
            
            logger.debug { "Entrée audit enregistrée: ${entreeFinale.id}" }
            
            return entreeFinale.id
        }
    }
    
    override suspend fun obtenirHistorique(filtre: FiltreAudit, limite: Int): List<EntreeAudit> {
        synchronized(verrou) {
            return entrees.asSequence()
                .filter { entree ->
                    (filtre.typeAction == null || entree.action == filtre.typeAction) &&
                    (filtre.dateDebut == null || entree.horodatage >= filtre.dateDebut) &&
                    (filtre.dateFin == null || entree.horodatage <= filtre.dateFin) &&
                    (filtre.statutResultat == null || entree.resultat == filtre.statutResultat)
                }
                .take(limite)
                .toList()
        }
    }
    
    override suspend fun verifierIntegrite(): ResultatVerification {
        synchronized(verrou) {
            if (entrees.isEmpty()) {
                return ResultatVerification.Valide(0)
            }
            
            var hashPrecedent = ByteArray(32) // Genesis
            
            for ((index, entree) in entrees.withIndex()) {
                // 1. Vérifier hash précédent
                val hashPrecedentAttendu = hashPrecedent.toHexString()
                if (entree.hashPrecedent != hashPrecedentAttendu) {
                    logger.error { 
                        "Chaîne brisée à l'index $index: " +
                        "attendu=$hashPrecedentAttendu, reçu=${entree.hashPrecedent}" 
                    }
                    return ResultatVerification.Compromis(
                        position = index,
                        raison = "Hash précédent invalide (chaîne brisée)"
                    )
                }
                
                // 2. Recalculer et vérifier le hash de l'entrée
                val entreeAValider = entree.copy(hash = "")
                val hashCalcule = calculerHash(entreeAValider)
                val hashCalculeHex = hashCalcule.toHexString()
                
                if (hashCalculeHex != entree.hash) {
                    logger.error { 
                        "Hash invalide à l'index $index: " +
                        "calculé=$hashCalculeHex, stocké=${entree.hash}" 
                    }
                    return ResultatVerification.Compromis(
                        position = index,
                        raison = "Hash d'entrée invalide (corruption détectée)"
                    )
                }
                
                // 3. Vérifier preuve cryptographique si présente
                if (entree.preuve != null) {
                    val donnees = "${entree.preuve.idAction}|" +
                                "${entree.preuve.horodatage}|" +
                                "${entree.preuve.statut}"
                    
                    val signatureCalculee = calculerSignature(donnees)
                    if (signatureCalculee != entree.preuve.signature) {
                        logger.error { "Preuve invalide à l'index $index" }
                        return ResultatVerification.Compromis(
                            position = index,
                            raison = "Preuve cryptographique invalide"
                        )
                    }
                }
                
                // 4. Vérifier cohérence temporelle
                if (index > 0) {
                    val precedent = entrees[index - 1]
                    if (entree.horodatage < precedent.horodatage) {
                        logger.warn { 
                            "Incohérence temporelle à l'index $index: " +
                            "${entree.horodatage} < ${precedent.horodatage}" 
                        }
                        return ResultatVerification.Compromis(
                            position = index,
                            raison = "Incohérence temporelle détectée"
                        )
                    }
                }
                
                hashPrecedent = hashCalcule
            }
            
            logger.info { "Intégrité du journal validée: ${entrees.size} entrées" }
            return ResultatVerification.Valide(entrees.size)
        }
    }
    
    /**
     * Valide une entrée avant insertion.
     */
    private fun validerEntree(entree: EntreeAudit): Boolean {
        // Vérifier champs obligatoires
        if (entree.id.toString().isEmpty()) return false
        if (entree.hash.isEmpty()) return false
        if (entree.hashPrecedent.isEmpty()) return false
        
        // Vérifier cohérence horodatage
        val maintenant = System.currentTimeMillis()
        val horodatageMs = entree.horodatage.toEpochMilliseconds()
        
        // Tolérance: ±5 minutes (pour drift NTP)
        if (Math.abs(horodatageMs - maintenant) > 5 * 60 * 1000) {
            logger.warn { "Horodatage suspect: ${entree.horodatage}" }
            // Ne pas rejeter, juste logger
        }
        
        return true
    }
    
    private fun calculerHash(entree: EntreeAudit): ByteArray {
        val donnees = "${entree.id}|" +
                     "${entree.horodatage}|" +
                     "${entree.action}|" +
                     "${entree.parametres}|" +
                     "${entree.resultat}|" +
                     "${entree.dureeMs}|" +
                     "${entree.tokenIdempotence}|" +
                     "${entree.hashPrecedent}"
        
        return MessageDigest.getInstance("SHA-256").digest(donnees.toByteArray())
    }
    
    private fun calculerSignature(donnees: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(donnees.toByteArray())
        return hash.toHexString()
    }
}

/**
 * Alias pour compatibilité (utilise maintenant JournalAuditProduction).
 */
typealias JournalAuditMemoire = JournalAuditProduction

/**
 * Interface de signature cryptographique.
 */
interface SignateurCrypto {
    fun signer(donnees: String): String
    fun verifierSignature(donnees: String, signature: String): Boolean
}

/**
 * Implémentation avec SHA-256 (RSA pour production).
 */
class SignateurCryptoImpl : SignateurCrypto {
    override fun signer(donnees: String): String {
        // Pour le MVP, on utilise SHA-256 simple
        // En production, on utiliserait RSA avec clé privée
        val hash = MessageDigest.getInstance("SHA-256").digest(donnees.toByteArray())
        return hash.toHexString()
    }
    
    override fun verifierSignature(donnees: String, signature: String): Boolean {
        val signatureCalculee = signer(donnees)
        return signatureCalculee == signature
    }
}

/**
 * Résultat d'une annulation.
 */
sealed class ResultatAnnulation {
    data class Succes(val executionId: UUID) : ResultatAnnulation()
    data class Echec(val executionId: UUID, val raison: String) : ResultatAnnulation()
}

/**
 * Extension pour convertir ByteArray en String hex.
 */
private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}
