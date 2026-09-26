package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable

/**
 * Représente une intention utilisateur interprétée à partir d'une commande vocale.
 * 
 * @property type Le type d'intention (APPEL, SMS, etc.)
 * @property entites Les entités extraites nécessaires pour exécuter l'action
 * @property contexte Le contexte utilisateur au moment de la commande (optionnel)
 */
@Serializable
data class Intention(
    val type: TypeIntention,
    val entites: Map<String, @Serializable(with = EntitePolymorphicSerializer::class) EntiteNLU>,
    val contexte: ContexteUtilisateur? = null
) {
    init {
        // Validation basique : certaines intentions nécessitent des entités spécifiques
        when (type) {
            TypeIntention.APPEL, TypeIntention.SMS -> {
                require(entites.containsKey("contact")) {
                    "L'intention $type nécessite une entité 'contact'"
                }
            }
            TypeIntention.PAIEMENT -> {
                require(entites.containsKey("montant")) {
                    "L'intention PAIEMENT nécessite une entité 'montant'"
                }
            }
            TypeIntention.ALARME_CREATION, TypeIntention.CALENDRIER_AJOUT -> {
                require(entites.containsKey("heure") || entites.containsKey("date")) {
                    "L'intention $type nécessite une entité temporelle"
                }
            }
            else -> {} // Autres intentions peuvent avoir des entités optionnelles
        }
    }
    
    /**
     * Récupère une entité typée de manière sécurisée.
     */
    inline fun <reified T : EntiteNLU> obtenirEntite(cle: String): T? {
        return entites[cle] as? T
    }
}
