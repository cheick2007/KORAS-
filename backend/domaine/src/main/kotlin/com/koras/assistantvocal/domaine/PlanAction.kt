package com.koras.assistantvocal.domaine

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Plan d'actions généré par l'orchestrateur pour réaliser une intention.
 * Contient la séquence ordonnée d'étapes à exécuter.
 */
@Serializable
data class PlanAction(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    
    val intention: Intention,
    
    val etapes: List<Etape>,
    
    val preconditionsManquantes: List<Precondition> = emptyList(),
    
    /**
     * Estimation de la durée totale en millisecondes.
     */
    val estimationDureeMs: Long,
    
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
) {
    init {
        require(etapes.isNotEmpty()) {
            "Un plan d'actions doit contenir au moins une étape"
        }
        
        // Vérifier que l'estimation correspond à la somme des étapes
        val sommeEstimations = etapes.sumOf { it.estimationMs }
        require(estimationDureeMs == sommeEstimations) {
            "L'estimation de durée ($estimationDureeMs ms) doit correspondre à la somme des étapes ($sommeEstimations ms)"
        }
        
        // Vérifier que tous les IDs d'étapes sont uniques
        val ids = etapes.map { it.id }
        require(ids.size == ids.toSet().size) {
            "Tous les IDs d'étapes doivent être uniques"
        }
        
        // Vérifier que les actions sensibles ont un niveau de confirmation approprié
        etapes.filter { it.sensible }.forEach { etape ->
            require(etape.niveauConfirmation != NiveauConfirmation.AUCUN) {
                "L'étape sensible ${etape.id} doit avoir un niveau de confirmation"
            }
        }
    }
    
    fun obtenirDuree(): Duration = estimationDureeMs.milliseconds
}

/**
 * Étape individuelle d'un plan d'actions.
 */
@Serializable
data class Etape(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID = UUID.randomUUID(),
    
    val action: ActionType,
    
    val parametres: Map<String, String>,
    
    val preconditions: List<Precondition> = emptyList(),
    
    val strategieCompensation: CompensationStrategy? = null,
    
    /**
     * Indique si l'action nécessite un consentement explicite.
     */
    val sensible: Boolean = false,
    
    val necessiteConfirmation: Boolean = false,
    
    val niveauConfirmation: NiveauConfirmation = NiveauConfirmation.AUCUN,
    
    /**
     * Estimation de la durée d'exécution en millisecondes.
     */
    val estimationMs: Long,
    
    val etat: EtatEtape = EtatEtape.EN_ATTENTE
) {
    init {
        require(estimationMs > 0) {
            "L'estimation de durée doit être positive"
        }
        
        if (sensible) {
            require(niveauConfirmation != NiveauConfirmation.AUCUN) {
                "Une étape sensible doit avoir un niveau de confirmation"
            }
        }
    }
    
    fun obtenirDuree(): Duration = estimationMs.milliseconds
}

/**
 * Préconditions à vérifier avant l'exécution d'une étape.
 */
@Serializable
enum class Precondition {
    PERMISSION_CONTACTS,
    PERMISSION_PHONE,
    PERMISSION_SMS,
    PERMISSION_CALENDAR,
    PERMISSION_LOCATION,
    PERMISSION_CAMERA,
    PERMISSION_MICROPHONE,
    PERMISSION_STORAGE,
    
    CONNECTIVITE_INTERNET,
    CONNECTIVITE_GPS,
    
    SOLDE_SUFFISANT,
    BATTERIE_SUFFISANTE,
    
    APPLICATION_INSTALLEE,
    SERVICE_DISPONIBLE
}

/**
 * Stratégies de compensation en cas d'échec d'une étape.
 */
@Serializable
enum class CompensationStrategy {
    /**
     * Ignorer l'échec et continuer avec les étapes suivantes.
     */
    IGNORER,
    
    /**
     * Réessayer l'étape avec un délai.
     */
    REESSAYER,
    
    /**
     * Annuler toutes les actions précédentes (rollback).
     */
    ROLLBACK,
    
    /**
     * Demander une alternative à l'utilisateur.
     */
    DEMANDER_ALTERNATIVE,
    
    /**
     * Exécuter une action de compensation spécifique (ex: remboursement).
     */
    COMPENSATION_SPECIFIQUE
}
