package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable

/**
 * Types d'intentions vocales supportées par le système.
 * Représente les 20+ actions prioritaires que l'utilisateur peut demander.
 */
@Serializable
enum class TypeIntention {
    // Communication
    APPEL,
    SMS,
    EMAIL,
    
    // Calendrier et rappels
    CALENDRIER_AJOUT,
    CALENDRIER_CONSULTATION,
    ALARME_CREATION,
    ALARME_ARRET,
    RAPPEL,
    
    // Recherche et navigation
    RECHERCHE_WEB,
    RECHERCHE_CONTACT,
    NAVIGATION_GPS,
    
    // Accessibilité
    LECTURE_TEXTE,
    OCR_CAPTURE,
    
    // Informations
    METEO,
    ACTUALITES,
    
    // Multimédia
    MUSIQUE_LECTURE,
    MUSIQUE_PAUSE,
    
    // Système
    OUVERTURE_APP,
    PARAMETRES_MODIFICATION,
    AIDE,
    HISTORIQUE_CONSULTATION,
    
    // Contrôle
    ANNULATION,
    CONFIRMATION,
    REPETITION,
    PAUSE,
    REPRISE,
    ARRET,
    
    // Transactions
    PAIEMENT,
    PARTAGE
}
