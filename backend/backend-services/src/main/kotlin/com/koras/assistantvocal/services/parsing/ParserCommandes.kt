package com.koras.assistantvocal.services.parsing

import com.koras.assistantvocal.domaine.*
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Parser de commandes vocales avec grammaire de patterns.
 * Extrait les intentions et entités structurées à partir de texte.
 */
class ParserCommandes {
    
    private val patterns = mapOf(
        TypeIntention.APPEL to listOf(
            Regex("appelle?\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("passe un appel à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("téléphone à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("appeler\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.SMS to listOf(
            Regex("envoie un (?:message|sms) à\\s+([^:]+)\\s*:?\\s*(.+)?", RegexOption.IGNORE_CASE),
            Regex("sms à\\s+([^:]+)\\s*:?\\s*(.+)?", RegexOption.IGNORE_CASE),
            Regex("message à\\s+([^:]+)\\s*:?\\s*(.+)?", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.MESSAGE_WHATSAPP to listOf(
            Regex("(?:envoie.*|écris.*)?whatsapp\\s+à\\s+(.+?)\\s+(?:disant|pour dire|que|:)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:envoie.*|écris.*)?whatsapp\\s+à\\s+(\\S+)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:envoie.*|écris.*)?whatsapp\\s+à\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.ALARME_CREATION to listOf(
            Regex("réveille-moi à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("alarme à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("alarme pour\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("crée une alarme à\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.ALARME_ARRET to listOf(
            Regex("arrête l'alarme", RegexOption.IGNORE_CASE),
            Regex("stop alarme", RegexOption.IGNORE_CASE),
            Regex("éteins l'alarme", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.CALENDRIER_AJOUT to listOf(
            Regex("ajoute un événement\\s+(.+?)\\s+(?:le|à)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("crée un rendez-vous\\s+(.+?)\\s+(?:le|à)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("rendez-vous\\s+(.+?)\\s+(?:le|à)\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.NAVIGATION_GPS to listOf(
            Regex("navigue vers\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("direction\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("comment aller à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("itinéraire vers\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.RECHERCHE_YOUTUBE to listOf(
            Regex("ouvre\\s*youtube\\s*et\\s*(?:recherche|cherche)\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("(?:recherche|cherche)\\s+(.+?)\\s+sur\\s*youtube", RegexOption.IGNORE_CASE),
            Regex("mets\\s+(.+?)\\s+sur\\s*youtube", RegexOption.IGNORE_CASE),
            Regex("youtube\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex(".*youtube.*(?:recherche|cherche)\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.RECHERCHE_WEB to listOf(
            Regex("recherche\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("cherche\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("trouve\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.LECTURE_TEXTE to listOf(
            Regex("lis(?:-moi)?\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("lecture de\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.OCR_CAPTURE to listOf(
            Regex("lis l'écran", RegexOption.IGNORE_CASE),
            Regex("que vois-tu", RegexOption.IGNORE_CASE),
            Regex("qu'est-ce qui est écrit", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.METEO to listOf(
            Regex("(?:quel temps fait-il|météo)(?:\\s+à\\s+(.+))?", RegexOption.IGNORE_CASE),
            Regex("il fait quel temps(?:\\s+à\\s+(.+))?", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.ACTUALITES to listOf(
            Regex("(?:quelles sont les )?actualités", RegexOption.IGNORE_CASE),
            Regex("news", RegexOption.IGNORE_CASE),
            Regex("infos du jour", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.MUSIQUE_LECTURE to listOf(
            Regex("joue(?:\\s+de la musique)?(?:\\s+(.+))?", RegexOption.IGNORE_CASE),
            Regex("lance(?:\\s+la musique)?(?:\\s+(.+))?", RegexOption.IGNORE_CASE),
            Regex("musique(?:\\s+(.+))?", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.MUSIQUE_PAUSE to listOf(
            Regex("pause(?:\\s+la)?\\s+musique", RegexOption.IGNORE_CASE),
            Regex("arrête(?:\\s+la)?\\s+musique", RegexOption.IGNORE_CASE),
            Regex("stop musique", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.AIDE to listOf(
            Regex("aide", RegexOption.IGNORE_CASE),
            Regex("help", RegexOption.IGNORE_CASE),
            Regex("qu'est-ce que tu peux faire", RegexOption.IGNORE_CASE),
            Regex("comment ça marche", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.OUVERTURE_APP to listOf(
            Regex("ouvre(?:\\s+l'application|\\s+l'appli|\\s+l'app)?\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("lance(?:\\s+l'application|\\s+l'appli|\\s+l'app)?\\s+(.+)", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.HISTORIQUE_CONSULTATION to listOf(
            Regex("que as-tu fait\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("historique(?:\\s+de\\s+(.+))?", RegexOption.IGNORE_CASE),
            Regex("montre-moi l'historique", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.ANNULATION to listOf(
            Regex("annule", RegexOption.IGNORE_CASE),
            Regex("cancel", RegexOption.IGNORE_CASE),
            Regex("laisse tomber", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.CONFIRMATION to listOf(
            Regex("oui", RegexOption.IGNORE_CASE),
            Regex("confirme", RegexOption.IGNORE_CASE),
            Regex("d'accord", RegexOption.IGNORE_CASE),
            Regex("ok", RegexOption.IGNORE_CASE),
            Regex("vas-y", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.REPETITION to listOf(
            Regex("répète", RegexOption.IGNORE_CASE),
            Regex("pardon", RegexOption.IGNORE_CASE),
            Regex("quoi", RegexOption.IGNORE_CASE),
            Regex("redis", RegexOption.IGNORE_CASE)
        ),
        TypeIntention.PAIEMENT to listOf(
            Regex("paie(?:ment)?\\s+(.+?)\\s+à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("envoie\\s+(.+?)\\s+à\\s+(.+)", RegexOption.IGNORE_CASE),
            Regex("transfert(?:\\s+de)?\\s+(.+?)\\s+(?:vers|à)\\s+(.+)", RegexOption.IGNORE_CASE)
        )
    )
    
    /**
     * Parse une commande textuelle et retourne une intention structurée.
     * 
     * @param commande Le texte de la commande à parser
     * @return L'intention extraite ou null si aucun pattern ne correspond
     * @throws ErreurParsing Si la structure est reconnue mais les entités sont invalides
     */
    fun parser(commande: String): Intention {
        val commandeNettoyee = commande.trim()
        
        if (commandeNettoyee.isBlank()) {
            throw ErreurParsing(
                position = 0,
                message = "La commande ne peut pas être vide"
            )
        }
        
        // Essayer de matcher avec chaque pattern
        for ((typeIntention, regexList) in patterns) {
            for (regex in regexList) {
                val match = regex.find(commandeNettoyee)
                if (match != null) {
                    return extraireIntention(typeIntention, match, commandeNettoyee)
                }
            }
        }
        
        // Aucun pattern trouvé
        throw ErreurParsing(
            position = 0,
            message = "Aucune intention reconnue dans la commande: \"$commandeNettoyee\""
        )
    }
    
    private fun extraireIntention(
        type: TypeIntention,
        match: MatchResult,
        commandeOriginale: String
    ): Intention {
        val entites = mutableMapOf<String, EntiteNLU>()
        
        when (type) {
            TypeIntention.APPEL -> {
                val contact = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Contact manquant")
                entites["contact"] = EntiteNLU.Contact(contact)
            }
            
            TypeIntention.SMS, TypeIntention.MESSAGE_WHATSAPP -> {
                val contact = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Destinataire manquant")
                val message = match.groupValues.getOrNull(2)?.trim()
                
                entites["contact"] = EntiteNLU.Contact(contact)
                if (!message.isNullOrBlank()) {
                    entites["message"] = EntiteNLU.Texte(message)
                }
            }
            
            TypeIntention.ALARME_CREATION -> {
                val heureStr = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Heure manquante")
                
                try {
                    val instant = parseHeure(heureStr)
                    entites["heure"] = EntiteNLU.Temporel(instant)
                } catch (e: Exception) {
                    throw ErreurParsing(
                        position = commandeOriginale.indexOf(heureStr),
                        message = "Format d'heure invalide: $heureStr"
                    )
                }
            }
            
            TypeIntention.CALENDRIER_AJOUT -> {
                val titre = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Titre de l'événement manquant")
                val dateStr = match.groupValues.getOrNull(2)?.trim()
                    ?: throw ErreurParsing(0, "Date manquante")
                
                entites["titre"] = EntiteNLU.Texte(titre)
                
                try {
                    val instant = parseDate(dateStr)
                    entites["date"] = EntiteNLU.Temporel(instant)
                } catch (e: Exception) {
                    throw ErreurParsing(
                        position = commandeOriginale.indexOf(dateStr),
                        message = "Format de date invalide: $dateStr"
                    )
                }
            }
            
            TypeIntention.NAVIGATION_GPS -> {
                val destination = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Destination manquante")
                entites["destination"] = EntiteNLU.Lieu(destination)
            }
            
            TypeIntention.RECHERCHE_WEB, TypeIntention.LECTURE_TEXTE, TypeIntention.RECHERCHE_YOUTUBE -> {
                val requete = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Requête manquante")
                entites["requete"] = EntiteNLU.Texte(requete)
            }
            
            TypeIntention.METEO -> {
                val lieu = match.groupValues.getOrNull(1)?.trim()
                if (!lieu.isNullOrBlank()) {
                    entites["lieu"] = EntiteNLU.Lieu(lieu)
                }
            }
            
            TypeIntention.MUSIQUE_LECTURE -> {
                val titre = match.groupValues.getOrNull(1)?.trim()
                if (!titre.isNullOrBlank()) {
                    entites["titre"] = EntiteNLU.Texte(titre)
                }
            }
            
            TypeIntention.OUVERTURE_APP -> {
                val nomApp = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Nom de l'application manquant")
                entites["app"] = EntiteNLU.Texte(nomApp)
            }
            
            TypeIntention.PAIEMENT -> {
                val montantStr = match.groupValues.getOrNull(1)?.trim()
                    ?: throw ErreurParsing(0, "Montant manquant")
                val destinataire = match.groupValues.getOrNull(2)?.trim()
                    ?: throw ErreurParsing(0, "Destinataire manquant")
                
                try {
                    val montant = parseMontant(montantStr)
                    entites["montant"] = montant
                } catch (e: Exception) {
                    throw ErreurParsing(
                        position = commandeOriginale.indexOf(montantStr),
                        message = "Format de montant invalide: $montantStr"
                    )
                }
                
                entites["destinataire"] = EntiteNLU.Contact(destinataire)
            }
            
            TypeIntention.HISTORIQUE_CONSULTATION -> {
                val typeHistorique = match.groupValues.getOrNull(1)?.trim()
                if (!typeHistorique.isNullOrBlank()) {
                    entites["type"] = EntiteNLU.Texte(typeHistorique)
                }
            }
            
            // Les autres intentions n'ont pas d'entités spécifiques
            else -> {}
        }
        
        return Intention(
            type = type,
            entites = entites
        )
    }
    
    /**
     * Parse une heure au format "14h30", "14:30", "2h de l'après-midi", etc.
     */
    private fun parseHeure(heureStr: String): Instant {
        // Patterns simples pour MVP
        val patterns = listOf(
            Regex("(\\d{1,2})h(\\d{2})"),  // 14h30
            Regex("(\\d{1,2}):(\\d{2})"),  // 14:30
            Regex("(\\d{1,2})h")           // 14h
        )
        
        for (pattern in patterns) {
            val match = pattern.find(heureStr)
            if (match != null) {
                val heures = match.groupValues[1].toInt()
                val minutes = match.groupValues.getOrNull(2)?.toInt() ?: 0
                
                if (heures !in 0..23 || minutes !in 0..59) {
                    throw IllegalArgumentException("Heure invalide: $heureStr")
                }
                
                // Pour l'instant, on utilise la date/heure actuelle + décalage
                // Dans une implémentation complète, on calculerait la prochaine occurrence
                val maintenant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val offsetMs = (heures * 3600 + minutes * 60) * 1000L
                
                return Instant.fromEpochMilliseconds(
                    maintenant.toEpochMilliseconds() + offsetMs
                )
            }
        }
        
        throw IllegalArgumentException("Format d'heure non reconnu: $heureStr")
    }
    
    /**
     * Parse une date au format "demain", "lundi prochain", "15 janvier", etc.
     */
    private fun parseDate(dateStr: String): Instant {
        // Implémentation simplifiée pour MVP
        val maintenant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        
        return when (dateStr.lowercase()) {
            "aujourd'hui" -> maintenant
            "demain" -> Instant.fromEpochMilliseconds(
                maintenant.toEpochMilliseconds() + 24 * 3600 * 1000
            )
            "après-demain" -> Instant.fromEpochMilliseconds(
                maintenant.toEpochMilliseconds() + 2 * 24 * 3600 * 1000
            )
            else -> {
                // Pour le MVP, on retourne la date actuelle + 1 jour
                // Une implémentation complète utiliserait une bibliothèque de parsing de dates
                Instant.fromEpochMilliseconds(
                    maintenant.toEpochMilliseconds() + 24 * 3600 * 1000
                )
            }
        }
    }
    
    /**
     * Parse un montant : "1000", "1000 francs", "1000 XOF", "mille francs"
     */
    private fun parseMontant(montantStr: String): EntiteNLU.Montant {
        val tokens = montantStr.split(Regex("\\s+"))
        
        // Extraire le nombre
        val nombreStr = tokens.firstOrNull { it.matches(Regex("\\d+([.,]\\d+)?")) }
            ?: throw IllegalArgumentException("Nombre non trouvé dans: $montantStr")
        
        val valeur = BigDecimal(nombreStr.replace(',', '.'))
        
        // Extraire la devise si présente
        val devise = when {
            montantStr.contains(Regex("franc|XOF|CFA", RegexOption.IGNORE_CASE)) -> "XOF"
            montantStr.contains(Regex("euro|EUR|€", RegexOption.IGNORE_CASE)) -> "EUR"
            montantStr.contains(Regex("dollar|USD|\\$", RegexOption.IGNORE_CASE)) -> "USD"
            else -> "XOF" // Par défaut
        }
        
        return EntiteNLU.Montant(valeur, devise)
    }
}

/**
 * Exception levée lors d'une erreur de parsing.
 */
data class ErreurParsing(
    val position: Int,
    override val message: String
) : Exception("Erreur de parsing à la position $position: $message")
