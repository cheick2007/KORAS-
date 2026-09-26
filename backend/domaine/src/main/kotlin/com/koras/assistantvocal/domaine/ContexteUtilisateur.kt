package com.koras.assistantvocal.domaine

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Contexte utilisateur au moment d'une commande vocale.
 * Contient les informations nécessaires pour personnaliser le traitement.
 */
@Serializable
data class ContexteUtilisateur(
    @Serializable(with = UUIDSerializer::class)
    val utilisateurId: UUID,
    
    val langue: Langue,
    
    val localisation: Localisation? = null,
    
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    
    val preferences: PreferencesUtilisateur,
    
    /**
     * Historique des dernières intentions (max 5) pour le contexte conversationnel.
     */
    val historiqueRecent: List<TypeIntention> = emptyList()
) {
    init {
        require(historiqueRecent.size <= 5) {
            "L'historique récent ne peut contenir plus de 5 intentions"
        }
    }
}

/**
 * Coordonnées géographiques de l'utilisateur.
 */
@Serializable
data class Localisation(
    val latitude: Double,
    val longitude: Double,
    val precision: Float // en mètres
) {
    init {
        require(latitude in -90.0..90.0) {
            "La latitude doit être entre -90 et 90"
        }
        require(longitude in -180.0..180.0) {
            "La longitude doit être entre -180 et 180"
        }
        require(precision >= 0) {
            "La précision ne peut pas être négative"
        }
    }
}

/**
 * Préférences utilisateur pour la personnalisation de l'expérience.
 */
@Serializable
data class PreferencesUtilisateur(
    /**
     * Vitesse de parole pour la synthèse vocale (0.5x à 2x).
     */
    val vitesseParole: Float = 1.0f,
    
    /**
     * Voix préférée pour TTS (format: langue-PAYS-type-version).
     */
    val voixPreferee: String = "fr-FR-Standard-A",
    
    /**
     * Volume des signaux sonores (0-100).
     */
    val volumeSonore: Int = 80,
    
    /**
     * Intensité des vibrations (0-255).
     */
    val intensiteVibration: Int = 128,
    
    /**
     * Mode verbeux : narration détaillée de chaque étape.
     */
    val modeVerbeux: Boolean = false,
    
    /**
     * Stockage local uniquement (pas de synchronisation cloud).
     */
    val stockageLocalUniquement: Boolean = false,
    
    /**
     * Période de rétention des données.
     */
    val periodeRetention: PeriodeRetention = PeriodeRetention.TRENTE_JOURS
) {
    init {
        require(vitesseParole in 0.5f..2.0f) {
            "La vitesse de parole doit être entre 0.5x et 2x"
        }
        require(volumeSonore in 0..100) {
            "Le volume sonore doit être entre 0 et 100"
        }
        require(intensiteVibration in 0..255) {
            "L'intensité de vibration doit être entre 0 et 255"
        }
    }
}

/**
 * Périodes de rétention des données utilisateur.
 */
@Serializable
enum class PeriodeRetention {
    SEPT_JOURS,
    TRENTE_JOURS,
    QUATRE_VINGT_DIX_JOURS,
    JAMAIS
}
