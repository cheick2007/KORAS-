package com.koras.assistantvocal.domaine

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Entrée dans le journal d'audit immuable.
 * Enregistre chaque action exécutée avec chaînage cryptographique.
 */
@Serializable
data class EntreeAudit(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    
    val action: ActionType,
    
    val parametres: Map<String, String>,
    
    val resultat: StatutExecution,
    
    val dureeMs: Long,
    
    @Serializable(with = UUIDSerializer::class)
    val tokenIdempotence: UUID,
    
    val preuve: PreuveExecution? = null,
    
    /**
     * Hash de l'entrée précédente (chaînage cryptographique).
     * Encodé en base64.
     */
    val hashPrecedent: String = "",
    
    /**
     * Hash de cette entrée (SHA-256).
     * Encodé en base64.
     */
    val hash: String = "",
    
    /**
     * Signature cryptographique de cette entrée.
     * Encodée en base64.
     */
    val signature: String = ""
) {
    fun obtenirDuree(): Duration = dureeMs.milliseconds
}

/**
 * Résultat de la vérification d'intégrité du journal d'audit.
 */
@Serializable
sealed class ResultatVerification {
    /**
     * Le journal est valide et intègre.
     */
    @Serializable
    data class Valide(val nbEntrees: Int) : ResultatVerification()
    
    /**
     * Le journal a été compromis (modification détectée).
     */
    @Serializable
    data class Compromis(
        val position: Int,
        val raison: String
    ) : ResultatVerification()
}

/**
 * Filtre pour requêter l'historique d'audit.
 */
@Serializable
data class FiltreAudit(
    val typeAction: ActionType? = null,
    
    @Serializable(with = InstantSerializer::class)
    val dateDebut: Instant? = null,
    
    @Serializable(with = InstantSerializer::class)
    val dateFin: Instant? = null,
    
    val statutResultat: StatutExecution? = null,
    
    @Serializable(with = UUIDSerializer::class)
    val utilisateurId: UUID? = null
)
