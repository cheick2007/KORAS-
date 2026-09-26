package com.koras.assistantvocal.services.orchestration

import com.koras.assistantvocal.domaine.*
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Interface de l'orchestrateur de tâches.
 * Génère et gère les plans d'actions pour réaliser les intentions utilisateur.
 */
interface OrchestrateurdeTaches {
    /**
     * Génère un plan d'actions complet pour une intention.
     * 
     * @param intention L'intention utilisateur à réaliser
     * @return Le plan d'actions généré avec étapes ordonnées
     * @throws IllegalArgumentException Si l'intention est invalide
     */
    suspend fun genererPlan(intention: Intention): PlanAction
    
    /**
     * Obtient l'état de progression d'un plan en cours d'exécution.
     * 
     * @param planId L'identifiant du plan
     * @return L'état de progression actuel
     */
    suspend fun obtenirEtatProgression(planId: UUID): EtatProgression?
    
    /**
     * Propose des plans alternatifs si des préconditions sont manquantes.
     * 
     * @param intention L'intention initiale
     * @param preconditionsManquantes Les préconditions non satisfaites
     * @return Liste de plans alternatifs
     */
    suspend fun proposerAlternatives(
        intention: Intention,
        preconditionsManquantes: List<Precondition>
    ): List<PlanAction>
}

/**
 * Implémentation de l'orchestrateur avec génération de plans tactiques.
 */
class OrchestrateurdeTachesImpl(
    private val verificateurPreconditions: VerificateurPreconditions = VerificateurPreconditionsImpl()
) : OrchestrateurdeTaches {
    
    private val plansEnCours = mutableMapOf<UUID, EtatProgression>()
    
    override suspend fun genererPlan(intention: Intention): PlanAction {
        logger.info { "Génération plan pour intention: ${intention.type}" }
        
        val debut = System.currentTimeMillis()
        
        // 1. Générer les étapes selon le type d'intention
        val etapes = genererEtapes(intention)
        
        // 2. Vérifier les préconditions
        val preconditionsManquantes = verifierPreconditions(etapes)
        
        // 3. Calculer l'estimation de durée
        val estimationDuree = etapes.sumOf { it.estimationMs }
        
        // 4. Créer le plan
        val plan = PlanAction(
            id = UUID.randomUUID(),
            intention = intention,
            etapes = etapes,
            preconditionsManquantes = preconditionsManquantes,
            estimationDureeMs = estimationDuree
        )
        
        val duree = System.currentTimeMillis() - debut
        logger.info { 
            "Plan généré en ${duree}ms : ${etapes.size} étapes, " +
            "estimation ${estimationDuree}ms" 
        }
        
        // Exigence 2.1 : génération < 300ms
        if (duree > 300) {
            logger.warn { "Génération plan > 300ms (${duree}ms)" }
        }
        
        return plan
    }
    
    override suspend fun obtenirEtatProgression(planId: UUID): EtatProgression? {
        return plansEnCours[planId]
    }
    
    override suspend fun proposerAlternatives(
        intention: Intention,
        preconditionsManquantes: List<Precondition>
    ): List<PlanAction> {
        val alternatives = mutableListOf<PlanAction>()
        
        // Générer des alternatives en fonction des préconditions manquantes
        for (precondition in preconditionsManquantes) {
            when (precondition) {
                Precondition.CONNECTIVITE_INTERNET -> {
                    // Proposer une version hors ligne si possible
                    if (intention.type in listOf(
                        TypeIntention.APPEL,
                        TypeIntention.SMS,
                        TypeIntention.ALARME_CREATION
                    )) {
                        alternatives.add(genererPlan(intention))
                    }
                }
                
                Precondition.PERMISSION_CONTACTS,
                Precondition.PERMISSION_PHONE,
                Precondition.PERMISSION_SMS -> {
                    // Proposer de demander la permission
                    val etapeDemandePermission = Etape(
                        action = ActionType.VERIFIER_PERMISSION,
                        parametres = mapOf("permission" to precondition.name),
                        estimationMs = 100
                    )
                    
                    val etapesOriginales = genererEtapes(intention)
                    val nouvellesEtapes = listOf(etapeDemandePermission) + etapesOriginales
                    
                    alternatives.add(
                        PlanAction(
                            id = UUID.randomUUID(),
                            intention = intention,
                            etapes = nouvellesEtapes,
                            preconditionsManquantes = emptyList(),
                            estimationDureeMs = nouvellesEtapes.sumOf { it.estimationMs }
                        )
                    )
                }
                
                else -> {
                    // Pas d'alternative pour cette précondition
                }
            }
        }
        
        return alternatives
    }
    
    /**
     * Génère les étapes d'un plan selon le type d'intention.
     */
    private fun genererEtapes(intention: Intention): List<Etape> {
        return when (intention.type) {
            TypeIntention.APPEL -> genererEtapesAppel(intention)
            TypeIntention.SMS -> genererEtapesSMS(intention)
            TypeIntention.EMAIL -> genererEtapesEmail(intention)
            TypeIntention.ALARME_CREATION -> genererEtapesAlarme(intention)
            TypeIntention.ALARME_ARRET -> genererEtapesArretAlarme()
            TypeIntention.CALENDRIER_AJOUT -> genererEtapesCalendrier(intention)
            TypeIntention.NAVIGATION_GPS -> genererEtapesNavigation(intention)
            TypeIntention.RECHERCHE_WEB -> genererEtapesRechercheWeb(intention)
            TypeIntention.METEO -> genererEtapesMeteo(intention)
            TypeIntention.ACTUALITES -> genererEtapesActualites()
            TypeIntention.LECTURE_TEXTE -> genererEtapesLectureTexte(intention)
            TypeIntention.OCR_CAPTURE -> genererEtapesOCR()
            TypeIntention.MUSIQUE_LECTURE -> genererEtapesMusique(intention)
            TypeIntention.MUSIQUE_PAUSE -> genererEtapesPauseMusique()
            TypeIntention.PAIEMENT -> genererEtapesPaiement(intention)
            TypeIntention.AIDE -> genererEtapesAide()
            TypeIntention.HISTORIQUE_CONSULTATION -> genererEtapesHistorique(intention)
            TypeIntention.ANNULATION -> genererEtapesAnnulation()
            TypeIntention.CONFIRMATION -> genererEtapesConfirmation()
            
            else -> listOf(
                Etape(
                    action = ActionType.LIRE_PARAMETRE,
                    parametres = mapOf("intention" to intention.type.name),
                    estimationMs = 100
                )
            )
        }
    }
    
    private fun genererEtapesAppel(intention: Intention): List<Etape> {
        val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
            ?: throw IllegalArgumentException("Contact manquant pour APPEL")
        
        return listOf(
            Etape(
                action = ActionType.RESOUDRE_CONTACT,
                parametres = mapOf("nom" to contact.nom),
                preconditions = listOf(Precondition.PERMISSION_CONTACTS),
                estimationMs = 50
            ),
            Etape(
                action = ActionType.INITIER_APPEL,
                parametres = mapOf("numero" to (contact.numero ?: "{{etape_precedente.numero}}")),
                preconditions = listOf(Precondition.PERMISSION_PHONE),
                estimationMs = 100,
                sensible = false // Appel n'est pas considéré sensible
            )
        )
    }
    
    private fun genererEtapesSMS(intention: Intention): List<Etape> {
        val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
            ?: throw IllegalArgumentException("Contact manquant pour SMS")
        val message = intention.obtenirEntite<EntiteNLU.Texte>("message")
        
        val etapes = mutableListOf<Etape>()
        
        // Étape 1 : Résoudre le contact
        etapes.add(
            Etape(
                action = ActionType.RESOUDRE_CONTACT,
                parametres = mapOf("nom" to contact.nom),
                preconditions = listOf(Precondition.PERMISSION_CONTACTS),
                estimationMs = 50
            )
        )
        
        // Étape 2 : Envoyer le SMS
        etapes.add(
            Etape(
                action = ActionType.ENVOYER_SMS,
                parametres = mapOf(
                    "numero" to (contact.numero ?: "{{etape_precedente.numero}}"),
                    "message" to (message?.contenu ?: "")
                ),
                preconditions = listOf(Precondition.PERMISSION_SMS),
                estimationMs = 150,
                sensible = false
            )
        )
        
        return etapes
    }
    
    private fun genererEtapesEmail(intention: Intention): List<Etape> {
        val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
            ?: throw IllegalArgumentException("Destinataire manquant pour EMAIL")
        val message = intention.obtenirEntite<EntiteNLU.Texte>("message")
        
        return listOf(
            Etape(
                action = ActionType.RESOUDRE_CONTACT,
                parametres = mapOf("nom" to contact.nom),
                preconditions = listOf(Precondition.PERMISSION_CONTACTS),
                estimationMs = 50
            ),
            Etape(
                action = ActionType.ENVOYER_EMAIL,
                parametres = mapOf(
                    "destinataire" to "{{etape_precedente.email}}",
                    "message" to (message?.contenu ?: "")
                ),
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                estimationMs = 200,
                sensible = false
            )
        )
    }
    
    private fun genererEtapesAlarme(intention: Intention): List<Etape> {
        val heure = intention.obtenirEntite<EntiteNLU.Temporel>("heure")
            ?: throw IllegalArgumentException("Heure manquante pour ALARME")
        
        return listOf(
            Etape(
                action = ActionType.CREER_ALARME,
                parametres = mapOf("heure" to heure.instant.toString()),
                estimationMs = 100,
                sensible = false
            )
        )
    }
    
    private fun genererEtapesArretAlarme(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.ARRETER_ALARME,
                parametres = emptyMap(),
                estimationMs = 50,
                sensible = false
            )
        )
    }
    
    private fun genererEtapesCalendrier(intention: Intention): List<Etape> {
        val titre = intention.obtenirEntite<EntiteNLU.Texte>("titre")
            ?: throw IllegalArgumentException("Titre manquant pour CALENDRIER")
        val date = intention.obtenirEntite<EntiteNLU.Temporel>("date")
            ?: throw IllegalArgumentException("Date manquante pour CALENDRIER")
        
        return listOf(
            Etape(
                action = ActionType.CREER_EVENEMENT,
                parametres = mapOf(
                    "titre" to titre.contenu,
                    "date" to date.instant.toString()
                ),
                preconditions = listOf(Precondition.PERMISSION_CALENDAR),
                estimationMs = 150,
                sensible = false
            )
        )
    }
    
    private fun genererEtapesNavigation(intention: Intention): List<Etape> {
        val destination = intention.obtenirEntite<EntiteNLU.Lieu>("destination")
            ?: throw IllegalArgumentException("Destination manquante pour NAVIGATION")
        
        return listOf(
            Etape(
                action = ActionType.RESOUDRE_ADRESSE,
                parametres = mapOf("adresse" to destination.adresse),
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                estimationMs = 200
            ),
            Etape(
                action = ActionType.DEMARRER_NAVIGATION,
                parametres = mapOf(
                    "latitude" to (destination.latitude?.toString() ?: "{{etape_precedente.latitude}}"),
                    "longitude" to (destination.longitude?.toString() ?: "{{etape_precedente.longitude}}")
                ),
                preconditions = listOf(
                    Precondition.PERMISSION_LOCATION,
                    Precondition.CONNECTIVITE_GPS
                ),
                estimationMs = 150
            )
        )
    }
    
    private fun genererEtapesRechercheWeb(intention: Intention): List<Etape> {
        val requete = intention.obtenirEntite<EntiteNLU.Texte>("requete")
            ?: throw IllegalArgumentException("Requête manquante pour RECHERCHE_WEB")
        
        return listOf(
            Etape(
                action = ActionType.RECHERCHER_WEB,
                parametres = mapOf("requete" to requete.contenu),
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                estimationMs = 500
            )
        )
    }
    
    private fun genererEtapesMeteo(intention: Intention): List<Etape> {
        val lieu = intention.obtenirEntite<EntiteNLU.Lieu>("lieu")
        
        return listOf(
            Etape(
                action = ActionType.OBTENIR_METEO,
                parametres = if (lieu != null) {
                    mapOf("lieu" to lieu.adresse)
                } else {
                    emptyMap() // Utiliser la localisation actuelle
                },
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                estimationMs = 300
            )
        )
    }
    
    private fun genererEtapesActualites(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.OBTENIR_ACTUALITES,
                parametres = emptyMap(),
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                estimationMs = 400
            )
        )
    }
    
    private fun genererEtapesLectureTexte(intention: Intention): List<Etape> {
        val texte = intention.obtenirEntite<EntiteNLU.Texte>("requete")
            ?: throw IllegalArgumentException("Texte manquant pour LECTURE_TEXTE")
        
        return listOf(
            Etape(
                action = ActionType.LIRE_TEXTE,
                parametres = mapOf("texte" to texte.contenu),
                estimationMs = 100
            )
        )
    }
    
    private fun genererEtapesOCR(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.CAPTURER_IMAGE,
                parametres = emptyMap(),
                preconditions = listOf(Precondition.PERMISSION_CAMERA),
                estimationMs = 200
            ),
            Etape(
                action = ActionType.EXECUTER_OCR,
                parametres = mapOf("image" to "{{etape_precedente.image}}"),
                estimationMs = 500
            ),
            Etape(
                action = ActionType.LIRE_TEXTE,
                parametres = mapOf("texte" to "{{etape_precedente.texte}}"),
                estimationMs = 100
            )
        )
    }
    
    private fun genererEtapesMusique(intention: Intention): List<Etape> {
        val titre = intention.obtenirEntite<EntiteNLU.Texte>("titre")
        
        return listOf(
            Etape(
                action = ActionType.LIRE_MUSIQUE,
                parametres = if (titre != null) {
                    mapOf("titre" to titre.contenu)
                } else {
                    emptyMap()
                },
                estimationMs = 200
            )
        )
    }
    
    private fun genererEtapesPauseMusique(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.PAUSE_MUSIQUE,
                parametres = emptyMap(),
                estimationMs = 50
            )
        )
    }
    
    private fun genererEtapesPaiement(intention: Intention): List<Etape> {
        val montant = intention.obtenirEntite<EntiteNLU.Montant>("montant")
            ?: throw IllegalArgumentException("Montant manquant pour PAIEMENT")
        val destinataire = intention.obtenirEntite<EntiteNLU.Contact>("destinataire")
            ?: throw IllegalArgumentException("Destinataire manquant pour PAIEMENT")
        
        return listOf(
            // Étape 1 : Valider le montant
            Etape(
                action = ActionType.VALIDER_MONTANT,
                parametres = mapOf(
                    "montant" to montant.valeur.toPlainString(),
                    "devise" to montant.devise
                ),
                preconditions = listOf(Precondition.SOLDE_SUFFISANT),
                estimationMs = 100,
                sensible = true,
                niveauConfirmation = NiveauConfirmation.CRITIQUE
            ),
            // Étape 2 : Confirmer le paiement
            Etape(
                action = ActionType.CONFIRMER_PAIEMENT,
                parametres = mapOf(
                    "montant" to montant.valeur.toPlainString(),
                    "devise" to montant.devise,
                    "destinataire" to destinataire.nom
                ),
                estimationMs = 100,
                sensible = true,
                necessiteConfirmation = true,
                niveauConfirmation = NiveauConfirmation.CRITIQUE
            ),
            // Étape 3 : Exécuter le paiement
            Etape(
                action = ActionType.EXECUTER_PAIEMENT,
                parametres = mapOf(
                    "montant" to montant.valeur.toPlainString(),
                    "devise" to montant.devise,
                    "destinataire" to destinataire.nom
                ),
                preconditions = listOf(Precondition.CONNECTIVITE_INTERNET),
                strategieCompensation = CompensationStrategy.COMPENSATION_SPECIFIQUE,
                estimationMs = 1000,
                sensible = true,
                niveauConfirmation = NiveauConfirmation.CRITIQUE
            )
        )
    }
    
    private fun genererEtapesAide(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.LIRE_PARAMETRE,
                parametres = mapOf("type" to "aide"),
                estimationMs = 50
            )
        )
    }
    
    private fun genererEtapesHistorique(intention: Intention): List<Etape> {
        val type = intention.obtenirEntite<EntiteNLU.Texte>("type")
        
        return listOf(
            Etape(
                action = ActionType.LIRE_PARAMETRE,
                parametres = if (type != null) {
                    mapOf("type" to "historique", "filtre" to type.contenu)
                } else {
                    mapOf("type" to "historique")
                },
                estimationMs = 100
            )
        )
    }
    
    private fun genererEtapesAnnulation(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.LIRE_PARAMETRE,
                parametres = mapOf("action" to "annulation"),
                estimationMs = 50
            )
        )
    }
    
    private fun genererEtapesConfirmation(): List<Etape> {
        return listOf(
            Etape(
                action = ActionType.LIRE_PARAMETRE,
                parametres = mapOf("action" to "confirmation"),
                estimationMs = 50
            )
        )
    }
    
    /**
     * Vérifie les préconditions de toutes les étapes.
     */
    private suspend fun verifierPreconditions(etapes: List<Etape>): List<Precondition> {
        val preconditionsManquantes = mutableSetOf<Precondition>()
        
        for (etape in etapes) {
            for (precondition in etape.preconditions) {
                if (!verificateurPreconditions.verifier(precondition)) {
                    preconditionsManquantes.add(precondition)
                }
            }
        }
        
        return preconditionsManquantes.toList()
    }
}

/**
 * Interface pour vérifier les préconditions.
 */
interface VerificateurPreconditions {
    suspend fun verifier(precondition: Precondition): Boolean
}

/**
 * Implémentation du vérificateur de préconditions.
 * Pour le MVP, simule les vérifications.
 */
class VerificateurPreconditionsImpl : VerificateurPreconditions {
    override suspend fun verifier(precondition: Precondition): Boolean {
        // Pour le MVP, on considère que toutes les préconditions sont satisfaites
        // Dans une implémentation réelle, on vérifierait réellement :
        // - Les permissions Android
        // - La connectivité réseau/GPS
        // - Le solde disponible
        // - etc.
        return when (precondition) {
            Precondition.CONNECTIVITE_INTERNET,
            Precondition.CONNECTIVITE_GPS -> true // Simuler connectivité OK
            
            Precondition.PERMISSION_CONTACTS,
            Precondition.PERMISSION_PHONE,
            Precondition.PERMISSION_SMS,
            Precondition.PERMISSION_CALENDAR,
            Precondition.PERMISSION_LOCATION,
            Precondition.PERMISSION_CAMERA -> true // Simuler permissions OK
            
            Precondition.SOLDE_SUFFISANT -> true
            Precondition.BATTERIE_SUFFISANTE -> true
            
            else -> true
        }
    }
}
