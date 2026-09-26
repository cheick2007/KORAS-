package com.koras.assistantvocal.services.nlu

import com.koras.assistantvocal.domaine.*
import com.koras.assistantvocal.services.parsing.ParserCommandes
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Interface du service NLU (Natural Language Understanding).
 * Interprète les commandes vocales et extrait les intentions utilisateur.
 */
interface ServiceNLU {
    /**
     * Interprète une commande vocale et extrait l'intention.
     * 
     * @param audio Buffer audio contenant la commande vocale
     * @param contexte Contexte utilisateur optionnel pour améliorer l'interprétation
     * @return Résultat de l'interprétation avec niveau de confiance
     * @throws ConfianceInsuffisanteException Si le niveau de confiance est < 70%
     */
    suspend fun interpreter(
        audio: AudioBuffer,
        contexte: ContexteUtilisateur? = null
    ): ResultatInterpretation
    
    /**
     * Détecte la langue parlée dans un audio.
     * 
     * @param audio Buffer audio à analyser
     * @return La langue détectée
     */
    suspend fun detecterLangue(audio: AudioBuffer): Langue
    
    /**
     * Valide si le niveau de confiance est suffisant (>= 70%).
     */
    fun validerConfiance(resultat: ResultatInterpretation): Boolean {
        return resultat.estConfiant()
    }
}

/**
 * Implémentation du service NLU avec architecture edge-first.
 * Privilégie l'exécution locale avec fallback cloud.
 */
class ServiceNLUImpl(
    private val nluEdge: NLUEdge,
    private val nluCloud: NLUCloud? = null
) : ServiceNLU {
    
    override suspend fun interpreter(
        audio: AudioBuffer,
        contexte: ContexteUtilisateur?
    ): ResultatInterpretation {
        val debut = System.currentTimeMillis()
        
        logger.info { "Début interprétation NLU (durée audio: ${audio.dureeMs}ms)" }
        
        // 1. Essayer avec le NLU edge d'abord
        val resultatEdge = try {
            nluEdge.interpreter(audio, contexte)
        } catch (e: Exception) {
            logger.warn(e) { "Échec NLU edge, tentative cloud" }
            null
        }
        
        // 2. Si confiance suffisante, retourner le résultat edge
        if (resultatEdge != null && resultatEdge.confiance >= 70f) {
            logger.info { 
                "NLU edge succès (confiance: ${resultatEdge.confiance}%, " +
                "intention: ${resultatEdge.intention.type})" 
            }
            return resultatEdge
        }
        
        // 3. Sinon, fallback vers le cloud si disponible
        if (nluCloud != null && resultatEdge != null && resultatEdge.confiance < 70f) {
            logger.info { 
                "Confiance edge insuffisante (${resultatEdge.confiance}%), " +
                "fallback cloud" 
            }
            
            return try {
                val resultatCloud = nluCloud.interpreter(audio, contexte)
                
                if (resultatCloud.confiance >= 70f) {
                    logger.info { 
                        "NLU cloud succès (confiance: ${resultatCloud.confiance}%)" 
                    }
                    resultatCloud
                } else {
                    logger.warn { 
                        "Confiance cloud insuffisante (${resultatCloud.confiance}%)" 
                    }
                    throw ConfianceInsuffisanteException(resultatCloud.confiance)
                }
            } catch (e: Exception) {
                logger.error(e) { "Échec NLU cloud" }
                // Retourner le résultat edge même avec confiance faible
                resultatEdge
            }
        }
        
        // 4. Si aucun résultat ou confiance insuffisante, lever une exception
        if (resultatEdge == null || resultatEdge.confiance < 70f) {
            throw ConfianceInsuffisanteException(
                resultatEdge?.confiance ?: 0f,
                "Impossible d'interpréter la commande avec confiance suffisante"
            )
        }
        
        return resultatEdge
    }
    
    override suspend fun detecterLangue(audio: AudioBuffer): Langue {
        // Privilégier le NLU edge pour la détection de langue
        return nluEdge.detecterLangue(audio)
    }
}

/**
 * NLU Edge : traitement local avec modèles embarqués.
 * Utilise des patterns et règles pour les 20 intentions prioritaires.
 */
class NLUEdge(
    private val parser: ParserCommandes = ParserCommandes()
) {
    
    /**
     * Interprète une commande vocale localement.
     * Simule l'ASR (Automatic Speech Recognition) + parsing pour MVP.
     */
    suspend fun interpreter(
        audio: AudioBuffer,
        contexte: ContexteUtilisateur?
    ): ResultatInterpretation {
        val debut = System.currentTimeMillis()
        
        // 1. Simuler la transcription ASR (Vosk)
        // Dans une implémentation réelle, on utiliserait Vosk ou Whisper.cpp
        val texteTranscrit = simulerASR(audio)
        
        // 2. Appliquer le filtrage adaptatif du bruit (simplifié)
        val texteFiltre = filtrerBruit(texteTranscrit)
        
        // 3. Détecter la langue
        val langue = detecterLangue(audio)
        
        // 4. Parser le texte pour extraire l'intention
        val intention = try {
            parser.parser(texteFiltre)
        } catch (e: Exception) {
            logger.debug(e) { "Erreur de parsing: ${e.message}" }
            // Retourner une intention avec confiance faible
            return ResultatInterpretation(
                intention = Intention(TypeIntention.AIDE, emptyMap()),
                confiance = 30f,
                langue = langue,
                entitesExtraites = emptyMap(),
                sourceTraitement = SourceNLU.EDGE,
                dureeMs = System.currentTimeMillis() - debut
            )
        }
        
        // 5. Calculer le niveau de confiance
        val confiance = calculerConfiance(texteFiltre, intention, contexte)
        
        val duree = System.currentTimeMillis() - debut
        
        return ResultatInterpretation(
            intention = intention,
            confiance = confiance,
            langue = langue,
            entitesExtraites = intention.entites,
            sourceTraitement = SourceNLU.EDGE,
            dureeMs = duree
        )
    }
    
    /**
     * Détecte la langue d'un audio.
     * Implémentation simplifiée basée sur des heuristiques.
     */
    suspend fun detecterLangue(audio: AudioBuffer): Langue {
        // Dans une implémentation réelle, on utiliserait un modèle de détection de langue
        // Pour le MVP, on retourne la langue par défaut
        return Langue.FRANCAIS
    }
    
    /**
     * Simule l'ASR (Automatic Speech Recognition).
     * Dans une implémentation réelle, on utiliserait Vosk ou Whisper.
     */
    private fun simulerASR(audio: AudioBuffer): String {
        // Pour le MVP, on décode les données audio comme du texte
        // Dans la réalité, on ferait appel à Vosk ici
        return try {
            String(java.util.Base64.getDecoder().decode(audio.donnees))
        } catch (e: Exception) {
            // Si le décodage échoue, retourner une chaîne vide
            ""
        }
    }
    
    /**
     * Applique un filtrage adaptatif du bruit de fond.
     */
    private fun filtrerBruit(texte: String): String {
        // Implémentation simplifiée : nettoyage basique
        return texte.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }
    
    /**
     * Calcule le niveau de confiance de l'interprétation.
     * Prend en compte la qualité du match et le contexte.
     */
    private fun calculerConfiance(
        texte: String,
        intention: Intention,
        contexte: ContexteUtilisateur?
    ): Float {
        var confiance = 75f // Confiance de base pour un match réussi
        
        // Bonus si le contexte contient des intentions similaires récentes
        if (contexte != null && contexte.historiqueRecent.contains(intention.type)) {
            confiance += 10f
        }
        
        // Bonus si toutes les entités requises sont présentes
        val entitesRequises = when (intention.type) {
            TypeIntention.APPEL, TypeIntention.SMS -> setOf("contact")
            TypeIntention.PAIEMENT -> setOf("montant", "destinataire")
            TypeIntention.ALARME_CREATION -> setOf("heure")
            else -> emptySet()
        }
        
        if (entitesRequises.all { intention.entites.containsKey(it) }) {
            confiance += 5f
        }
        
        // Pénalité si le texte est très court (< 5 caractères)
        if (texte.length < 5) {
            confiance -= 20f
        }
        
        // Pénalité si le texte est trop long (> 200 caractères)
        if (texte.length > 200) {
            confiance -= 10f
        }
        
        // Limiter entre 0 et 100
        return confiance.coerceIn(0f, 100f)
    }
}

/**
 * NLU Cloud : traitement cloud avec modèles plus puissants.
 * Utilisé en fallback pour les intentions complexes.
 */
class NLUCloud(
    private val apiUrl: String,
    private val apiKey: String
) {
    
    /**
     * Interprète une commande vocale via l'API cloud.
     * Utilise Whisper pour l'ASR et GPT-4 pour l'extraction d'intentions.
     */
    suspend fun interpreter(
        audio: AudioBuffer,
        contexte: ContexteUtilisateur?
    ): ResultatInterpretation {
        val debut = System.currentTimeMillis()
        
        // Simuler un appel API avec latence
        delay(200) // Latence réseau simulée
        
        // Dans une implémentation réelle, on ferait :
        // 1. Appel Whisper API pour transcription
        // 2. Appel GPT-4 avec few-shot learning pour extraction d'intentions
        // 3. Parsing du résultat JSON
        
        // Pour le MVP, on simule un échec ou retour basique
        throw NotImplementedError("NLU Cloud non implémenté dans le MVP")
    }
}
