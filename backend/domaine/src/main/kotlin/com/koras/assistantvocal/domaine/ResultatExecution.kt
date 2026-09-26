package com.koras.assistantvocal.domaine

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Résultat de l'exécution d'une action.
 */
@Serializable
data class ResultatExecution(
    @Serializable(with = UUIDSerializer::class)
    val idExecution: UUID,
    
    val statut: StatutExecution,
    
    /**
     * Données résultantes de l'exécution (format JSON).
     */
    val resultat: String? = null,
    
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    
    val dureeMs: Long,
    
    val preuve: PreuveExecution? = null,
    
    val source: SourceResultat = SourceResultat.EXECUTION_DIRECTE
) {
    fun obtenirDuree(): Duration = dureeMs.milliseconds
}

/**
 * Source du résultat d'exécution.
 */
@Serializable
enum class SourceResultat {
    /**
     * Résultat d'une exécution directe.
     */
    EXECUTION_DIRECTE,
    
    /**
     * Résultat récupéré du cache d'idempotence.
     */
    CACHE_IDEMPOTENCE
}

/**
 * Preuve cryptographique de l'exécution d'une action.
 * Garantit l'authenticité et l'intégrité de l'exécution.
 */
@Serializable
data class PreuveExecution(
    @Serializable(with = UUIDSerializer::class)
    val idAction: UUID,
    
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    
    val statut: StatutExecution,
    
    /**
     * Signature cryptographique des données d'exécution (base64).
     */
    val signature: String,
    
    /**
     * Algorithme utilisé pour la signature (ex: SHA-256-RSA).
     */
    val algorithme: String
) {
    init {
        require(signature.isNotBlank()) {
            "La signature ne peut pas être vide"
        }
        require(algorithme.isNotBlank()) {
            "L'algorithme ne peut pas être vide"
        }
    }
}

/**
 * État de progression d'un plan d'actions en cours d'exécution.
 */
@Serializable
data class EtatProgression(
    @Serializable(with = UUIDSerializer::class)
    val planId: UUID,
    
    /**
     * Index de l'étape courante (0-indexed).
     */
    val etapeCourante: Int,
    
    val totalEtapes: Int,
    
    /**
     * Pourcentage de progression (0-100).
     */
    val pourcentage: Float,
    
    val tempsEcouleMs: Long,
    
    val tempsEstimeRestantMs: Long
) {
    init {
        require(etapeCourante in 0 until totalEtapes) {
            "L'étape courante ($etapeCourante) doit être entre 0 et $totalEtapes"
        }
        require(pourcentage in 0f..100f) {
            "Le pourcentage doit être entre 0 et 100"
        }
        require(tempsEcouleMs >= 0) {
            "Le temps écoulé ne peut pas être négatif"
        }
        require(tempsEstimeRestantMs >= 0) {
            "Le temps estimé restant ne peut pas être négatif"
        }
    }
    
    fun obtenirTempsEcoule(): Duration = tempsEcouleMs.milliseconds
    fun obtenirTempsEstimeRestant(): Duration = tempsEstimeRestantMs.milliseconds
}
