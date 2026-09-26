package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Résultat de l'interprétation NLU d'une commande vocale.
 */
@Serializable
data class ResultatInterpretation(
    val intention: Intention,
    
    /**
     * Niveau de confiance de l'interprétation (0-100%).
     */
    val confiance: Float,
    
    val langue: Langue,
    
    val entitesExtraites: Map<String, @Serializable(with = EntitePolymorphicSerializer::class) EntiteNLU>,
    
    /**
     * Source du traitement (edge ou cloud).
     */
    val sourceTraitement: SourceNLU,
    
    val dureeMs: Long
) {
    init {
        require(confiance in 0f..100f) {
            "Le niveau de confiance doit être entre 0 et 100"
        }
    }
    
    fun obtenirDuree(): Duration = dureeMs.milliseconds
    
    /**
     * Vérifie si le niveau de confiance est suffisant (>= 70%).
     */
    fun estConfiant(): Boolean = confiance >= 70f
}

/**
 * Source du traitement NLU.
 */
@Serializable
enum class SourceNLU {
    /**
     * Traitement effectué localement (edge) avec modèles embarqués.
     */
    EDGE,
    
    /**
     * Traitement effectué dans le cloud avec modèles plus larges.
     */
    CLOUD
}

/**
 * Buffer audio pour le traitement NLU.
 */
@Serializable
data class AudioBuffer(
    /**
     * Données audio brutes (encodées en base64).
     */
    val donnees: String,
    
    /**
     * Format audio (ex: PCM_16BIT, WAV, MP3).
     */
    val format: String,
    
    /**
     * Fréquence d'échantillonnage en Hz.
     */
    val frequenceEchantillonnage: Int,
    
    /**
     * Nombre de canaux (1 pour mono, 2 pour stéréo).
     */
    val canaux: Int = 1,
    
    /**
     * Durée en millisecondes.
     */
    val dureeMs: Long
) {
    init {
        require(donnees.isNotBlank()) {
            "Les données audio ne peuvent pas être vides"
        }
        require(frequenceEchantillonnage > 0) {
            "La fréquence d'échantillonnage doit être positive"
        }
        require(canaux in 1..2) {
            "Le nombre de canaux doit être 1 (mono) ou 2 (stéréo)"
        }
        require(dureeMs > 0) {
            "La durée doit être positive"
        }
    }
    
    fun obtenirDuree(): Duration = dureeMs.milliseconds
}

/**
 * Exception levée quand le niveau de confiance est insuffisant.
 */
class ConfianceInsuffisanteException(
    val confiance: Float,
    message: String = "Niveau de confiance insuffisant: $confiance%"
) : Exception(message)
