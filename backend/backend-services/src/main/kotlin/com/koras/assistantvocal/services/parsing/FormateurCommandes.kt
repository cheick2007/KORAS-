package com.koras.assistantvocal.services.parsing

import com.koras.assistantvocal.domaine.*
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * Formateur de commandes : génère du texte lisible à partir d'intentions structurées.
 * Supporte le formatage dans les 8 langues du système.
 */
class FormateurCommandes {
    
    private val templates = mapOf(
        Langue.FRANCAIS to TemplatesLangue(
            appel = "Appeler {contact}",
            sms = "Envoyer un message à {contact}{message}",
            alarme = "Créer une alarme à {heure}",
            calendrier = "Ajouter l'événement \"{titre}\" le {date}",
            navigation = "Naviguer vers {destination}",
            recherche = "Rechercher \"{requete}\"",
            lecture = "Lire \"{requete}\"",
            ocr = "Lire l'écran",
            meteo = "Météo{lieu}",
            actualites = "Actualités du jour",
            musique = "Jouer{titre}",
            pause = "Pause musique",
            aide = "Aide",
            historique = "Historique{type}",
            annulation = "Annuler",
            confirmation = "Confirmer",
            repetition = "Répéter",
            paiement = "Payer {montant} à {destinataire}"
        ),
        Langue.ANGLAIS to TemplatesLangue(
            appel = "Call {contact}",
            sms = "Send message to {contact}{message}",
            alarme = "Create alarm at {heure}",
            calendrier = "Add event \"{titre}\" on {date}",
            navigation = "Navigate to {destination}",
            recherche = "Search for \"{requete}\"",
            lecture = "Read \"{requete}\"",
            ocr = "Read screen",
            meteo = "Weather{lieu}",
            actualites = "Today's news",
            musique = "Play{titre}",
            pause = "Pause music",
            aide = "Help",
            historique = "History{type}",
            annulation = "Cancel",
            confirmation = "Confirm",
            repetition = "Repeat",
            paiement = "Pay {montant} to {destinataire}"
        ),
        Langue.ARABE to TemplatesLangue(
            appel = "اتصل بـ {contact}",
            sms = "أرسل رسالة إلى {contact}{message}",
            alarme = "أنشئ منبهًا في {heure}",
            calendrier = "أضف حدث \"{titre}\" في {date}",
            navigation = "انتقل إلى {destination}",
            recherche = "ابحث عن \"{requete}\"",
            lecture = "اقرأ \"{requete}\"",
            ocr = "اقرأ الشاشة",
            meteo = "الطقس{lieu}",
            actualites = "أخبار اليوم",
            musique = "شغّل{titre}",
            pause = "إيقاف الموسيقى",
            aide = "مساعدة",
            historique = "السجل{type}",
            annulation = "إلغاء",
            confirmation = "تأكيد",
            repetition = "كرر",
            paiement = "ادفع {montant} إلى {destinataire}"
        ),
        Langue.WOLOF to TemplatesLangue(
            appel = "Wóolu {contact}",
            sms = "Yónnee bataaxal bu {contact}{message}",
            alarme = "Sos alaram ci {heure}",
            calendrier = "Yokk lii \"{titre}\" ci {date}",
            navigation = "Dem ci {destination}",
            recherche = "Seet \"{requete}\"",
            lecture = "Jàng \"{requete}\"",
            ocr = "Jàng ekraan bi",
            meteo = "Weer{lieu}",
            actualites = "Xibaar yu bees",
            musique = "Defar musig{titre}",
            pause = "Taxaw musig",
            aide = "Ndimbal",
            historique = "Jaar-jaaram{type}",
            annulation = "Bàyyi",
            confirmation = "Dëggu",
            repetition = "Wax ko",
            paiement = "Fey {montant} ba {destinataire}"
        )
    )
    
    /**
     * Formate une intention en texte lisible dans la langue spécifiée.
     * 
     * @param intention L'intention à formatter
     * @param langue La langue de sortie (défaut: Français)
     * @return Le texte formaté
     */
    fun formatter(intention: Intention, langue: Langue = Langue.FRANCAIS): String {
        val template = templates[langue] ?: templates[Langue.FRANCAIS]!!
        
        return when (intention.type) {
            TypeIntention.APPEL -> {
                val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                    ?: return "Erreur: contact manquant"
                template.appel.replace("{contact}", contact.nom)
            }
            
            TypeIntention.SMS -> {
                val contact = intention.obtenirEntite<EntiteNLU.Contact>("contact")
                    ?: return "Erreur: contact manquant"
                val message = intention.obtenirEntite<EntiteNLU.Texte>("message")
                
                val textMessage = if (message != null) {
                    when (langue) {
                        Langue.FRANCAIS -> " : \"${message.contenu}\""
                        Langue.ANGLAIS -> ": \"${message.contenu}\""
                        else -> ": \"${message.contenu}\""
                    }
                } else {
                    ""
                }
                
                template.sms
                    .replace("{contact}", contact.nom)
                    .replace("{message}", textMessage)
            }
            
            TypeIntention.ALARME_CREATION -> {
                val heure = intention.obtenirEntite<EntiteNLU.Temporel>("heure")
                    ?: return "Erreur: heure manquante"
                
                val heureFormatee = formatterHeure(heure.instant, langue)
                template.alarme.replace("{heure}", heureFormatee)
            }
            
            TypeIntention.ALARME_ARRET -> template.pause
            
            TypeIntention.CALENDRIER_AJOUT -> {
                val titre = intention.obtenirEntite<EntiteNLU.Texte>("titre")
                    ?: return "Erreur: titre manquant"
                val date = intention.obtenirEntite<EntiteNLU.Temporel>("date")
                    ?: return "Erreur: date manquante"
                
                val dateFormatee = formatterDate(date.instant, langue)
                template.calendrier
                    .replace("{titre}", titre.contenu)
                    .replace("{date}", dateFormatee)
            }
            
            TypeIntention.NAVIGATION_GPS -> {
                val destination = intention.obtenirEntite<EntiteNLU.Lieu>("destination")
                    ?: return "Erreur: destination manquante"
                template.navigation.replace("{destination}", destination.adresse)
            }
            
            TypeIntention.RECHERCHE_WEB -> {
                val requete = intention.obtenirEntite<EntiteNLU.Texte>("requete")
                    ?: return "Erreur: requête manquante"
                template.recherche.replace("{requete}", requete.contenu)
            }
            
            TypeIntention.LECTURE_TEXTE -> {
                val requete = intention.obtenirEntite<EntiteNLU.Texte>("requete")
                    ?: return "Erreur: texte manquant"
                template.lecture.replace("{requete}", requete.contenu)
            }
            
            TypeIntention.OCR_CAPTURE -> template.ocr
            
            TypeIntention.METEO -> {
                val lieu = intention.obtenirEntite<EntiteNLU.Lieu>("lieu")
                val textLieu = if (lieu != null) {
                    when (langue) {
                        Langue.FRANCAIS -> " à ${lieu.adresse}"
                        Langue.ANGLAIS -> " in ${lieu.adresse}"
                        Langue.ARABE -> " في ${lieu.adresse}"
                        else -> " ${lieu.adresse}"
                    }
                } else {
                    ""
                }
                template.meteo.replace("{lieu}", textLieu)
            }
            
            TypeIntention.ACTUALITES -> template.actualites
            
            TypeIntention.MUSIQUE_LECTURE -> {
                val titre = intention.obtenirEntite<EntiteNLU.Texte>("titre")
                val textTitre = if (titre != null) " \"${titre.contenu}\"" else ""
                template.musique.replace("{titre}", textTitre)
            }
            
            TypeIntention.MUSIQUE_PAUSE -> template.pause
            
            TypeIntention.AIDE -> template.aide
            
            TypeIntention.HISTORIQUE_CONSULTATION -> {
                val type = intention.obtenirEntite<EntiteNLU.Texte>("type")
                val textType = if (type != null) {
                    when (langue) {
                        Langue.FRANCAIS -> " de ${type.contenu}"
                        Langue.ANGLAIS -> " of ${type.contenu}"
                        else -> " ${type.contenu}"
                    }
                } else {
                    ""
                }
                template.historique.replace("{type}", textType)
            }
            
            TypeIntention.ANNULATION -> template.annulation
            TypeIntention.CONFIRMATION -> template.confirmation
            TypeIntention.REPETITION -> template.repetition
            
            TypeIntention.PAIEMENT -> {
                val montant = intention.obtenirEntite<EntiteNLU.Montant>("montant")
                    ?: return "Erreur: montant manquant"
                val destinataire = intention.obtenirEntite<EntiteNLU.Contact>("destinataire")
                    ?: return "Erreur: destinataire manquant"
                
                val montantFormate = formatterMontant(montant, langue)
                template.paiement
                    .replace("{montant}", montantFormate)
                    .replace("{destinataire}", destinataire.nom)
            }
            
            else -> when (langue) {
                Langue.FRANCAIS -> "Action: ${intention.type.name}"
                Langue.ANGLAIS -> "Action: ${intention.type.name}"
                else -> intention.type.name
            }
        }
    }
    
    private fun formatterHeure(instant: Instant, langue: Langue): String {
        // Implémentation simplifiée pour MVP
        val millis = instant.toEpochMilliseconds()
        val heures = (millis / (3600 * 1000)) % 24
        val minutes = (millis / (60 * 1000)) % 60
        
        return when (langue) {
            Langue.FRANCAIS -> String.format("%02d:%02d", heures, minutes)
            Langue.ANGLAIS -> String.format("%02d:%02d", heures, minutes)
            else -> String.format("%02d:%02d", heures, minutes)
        }
    }
    
    private fun formatterDate(instant: Instant, langue: Langue): String {
        // Implémentation simplifiée pour MVP
        return when (langue) {
            Langue.FRANCAIS -> instant.toString().split('T')[0]
            Langue.ANGLAIS -> instant.toString().split('T')[0]
            else -> instant.toString().split('T')[0]
        }
    }
    
    private fun formatterMontant(montant: EntiteNLU.Montant, langue: Langue): String {
        val valeurFormatee = montant.valeur.toPlainString()
        
        return when (langue) {
            Langue.FRANCAIS -> "$valeurFormatee ${montant.devise}"
            Langue.ANGLAIS -> "${montant.devise} $valeurFormatee"
            Langue.ARABE -> "$valeurFormatee ${montant.devise}"
            else -> "$valeurFormatee ${montant.devise}"
        }
    }
}

/**
 * Templates de formatage pour une langue.
 */
private data class TemplatesLangue(
    val appel: String,
    val sms: String,
    val alarme: String,
    val calendrier: String,
    val navigation: String,
    val recherche: String,
    val lecture: String,
    val ocr: String,
    val meteo: String,
    val actualites: String,
    val musique: String,
    val pause: String,
    val aide: String,
    val historique: String,
    val annulation: String,
    val confirmation: String,
    val repetition: String,
    val paiement: String
)
