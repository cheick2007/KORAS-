package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable

/**
 * Types d'actions exécutables par le système.
 * Représente les étapes granulaires composant un plan d'actions.
 */
@Serializable
enum class ActionType {
    // Résolution
    RESOUDRE_CONTACT,
    RESOUDRE_ADRESSE,
    VALIDER_MONTANT,
    VERIFIER_PERMISSION,
    
    // Communication
    INITIER_APPEL,
    ENVOYER_SMS,
    ENVOYER_EMAIL,
    
    // Calendrier
    CREER_EVENEMENT,
    CONSULTER_EVENEMENTS,
    MODIFIER_EVENEMENT,
    SUPPRIMER_EVENEMENT,
    
    // Alarmes et rappels
    CREER_ALARME,
    ARRETER_ALARME,
    CREER_RAPPEL,
    
    // Système
    MODIFIER_PARAMETRE,
    LIRE_PARAMETRE,
    
    // Navigation
    DEMARRER_NAVIGATION,
    ARRETER_NAVIGATION,
    
    // Accessibilité
    LIRE_TEXTE,
    CAPTURER_IMAGE,
    EXECUTER_OCR,
    
    // Transactions
    EXECUTER_PAIEMENT,
    CONFIRMER_PAIEMENT,
    ANNULER_PAIEMENT,
    
    // Intégration tierce
    LANCER_APPLICATION,
    INTERAGIR_UI,
    
    // Recherche
    RECHERCHER_WEB,
    OBTENIR_METEO,
    OBTENIR_ACTUALITES,
    
    // Multimédia
    LIRE_MUSIQUE,
    PAUSE_MUSIQUE,
    REPRENDRE_MUSIQUE,
    ARRETER_MUSIQUE
}
