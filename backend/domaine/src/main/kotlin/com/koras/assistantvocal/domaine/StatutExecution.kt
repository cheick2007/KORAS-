package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable

/**
 * Statut d'exécution d'une action ou d'une étape.
 */
@Serializable
enum class StatutExecution {
    /**
     * L'action s'est terminée avec succès.
     */
    SUCCES,
    
    /**
     * L'action a échoué après toutes les tentatives de retry.
     */
    ECHEC,
    
    /**
     * L'action a été annulée par l'utilisateur ou par le système.
     */
    ANNULE,
    
    /**
     * L'action est en attente de confirmation de l'utilisateur.
     */
    ATTEND_CONFIRMATION
}

/**
 * État d'une étape dans un plan d'actions.
 */
@Serializable
enum class EtatEtape {
    /**
     * L'étape n'a pas encore été exécutée.
     */
    EN_ATTENTE,
    
    /**
     * L'étape est en cours d'exécution.
     */
    EN_COURS,
    
    /**
     * L'étape s'est terminée avec succès.
     */
    SUCCES,
    
    /**
     * L'étape a échoué.
     */
    ECHEC,
    
    /**
     * L'étape a été annulée.
     */
    ANNULE,
    
    /**
     * L'étape attend une confirmation utilisateur.
     */
    ATTEND_CONFIRMATION
}

/**
 * Niveau de confirmation requis pour une action sensible.
 */
@Serializable
enum class NiveauConfirmation {
    /**
     * Pas de confirmation requise, exécution automatique.
     */
    AUCUN,
    
    /**
     * Confirmation vocale simple requise.
     */
    VOCAL,
    
    /**
     * Confirmation vocale + PIN/biométrie requise.
     */
    CRITIQUE
}
