package com.koras.assistantvocal.services.gateway

import com.koras.assistantvocal.domaine.*
import com.koras.assistantvocal.services.execution.ExecuteurSecurise
import com.koras.assistantvocal.services.execution.JournalAudit
import com.koras.assistantvocal.services.nlu.ServiceNLU
import com.koras.assistantvocal.services.orchestration.OrchestrateurdeTaches
import com.koras.assistantvocal.services.stockage.StoreMemoire
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Configuration de la Gateway API REST.
 * Expose les endpoints pour l'assistant vocal.
 */
fun Application.configureGatewayAPI(
    serviceNLU: ServiceNLU,
    orchestrateur: OrchestrateurdeTaches,
    executeur: ExecuteurSecurise,
    journal: JournalAudit,
    store: StoreMemoire,
    authentification: ServiceAuthentification,
    rateLimiter: RateLimiter
) {
    routing {
        // Health check (pas d'authentification)
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "UP",
                    "timestamp" to Clock.System.now().toString(),
                    "version" to "1.0.0"
                )
            )
        }
        
        // Routes authentifiées
        authenticate("auth-jwt") {
            route("/api/v1") {
                
                // POST /api/v1/interprete : Interprétation audio/texte
                post("/interprete") {
                    val principal = call.principal<UtilisateurPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    
                    // Rate limiting : 10 req/min pour interprétation
                    if (!rateLimiter.autoriser(principal.userId, "interprete", limite = 10)) {
                        return@post call.respond(
                            HttpStatusCode.TooManyRequests,
                            ReponseErreur("Rate limit dépassé : 10 requêtes/minute maximum")
                        )
                    }
                    
                    val requete = call.receive<RequeteInterpretation>()
                    
                    try {
                        val contexte = ContexteUtilisateur(
                            userId = principal.userId,
                            langue = requete.langue,
                            localisation = requete.localisation,
                            horodatage = Clock.System.now(),
                            preferences = PreferencesUtilisateur()
                        )
                        
                        val resultat = when (requete.type) {
                            TypeEntree.TEXTE -> {
                                serviceNLU.interpreterTexte(requete.contenu, contexte)
                            }
                            TypeEntree.AUDIO -> {
                                val audio = AudioBuffer(
                                    donnees = requete.contenu.toByteArray(),
                                    format = requete.formatAudio ?: "wav",
                                    frequenceEchantillonnage = 16000,
                                    nombreCanaux = 1,
                                    dureeMs = requete.dureeMs ?: 0
                                )
                                serviceNLU.interpreterAudio(audio, contexte)
                            }
                        }
                        
                        call.respond(
                            HttpStatusCode.OK,
                            ReponseInterpretation(
                                intention = resultat.intention,
                                confiance = resultat.confiance,
                                langue = resultat.langue,
                                dureeMs = resultat.dureeMs,
                                source = resultat.source
                            )
                        )
                    } catch (e: ConfianceInsuffisanteException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ReponseErreur("Confiance insuffisante : ${e.message}")
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Erreur interprétation: ${e.message}" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReponseErreur("Erreur interne : ${e.message}")
                        )
                    }
                }
                
                // POST /api/v1/execute : Exécution plan avec idempotence
                post("/execute") {
                    val principal = call.principal<UtilisateurPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    
                    // Rate limiting : 100 req/min standard
                    if (!rateLimiter.autoriser(principal.userId, "execute", limite = 100)) {
                        return@post call.respond(
                            HttpStatusCode.TooManyRequests,
                            ReponseErreur("Rate limit dépassé : 100 requêtes/minute maximum")
                        )
                    }
                    
                    val requete = call.receive<RequeteExecution>()
                    
                    try {
                        val contexte = ContexteUtilisateur(
                            userId = principal.userId,
                            langue = requete.langue ?: Langue.FRANCAIS,
                            horodatage = Clock.System.now(),
                            preferences = PreferencesUtilisateur()
                        )
                        
                        // Générer plan
                        val plan = orchestrateur.genererPlan(requete.intention, contexte)
                        
                        // Exécuter les étapes avec idempotence
                        val resultats = plan.etapes.map { etape ->
                            val token = requete.tokenIdempotence ?: UUID.randomUUID()
                            executeur.executer(etape, token)
                        }
                        
                        call.respond(
                            HttpStatusCode.OK,
                            ReponseExecution(
                                planId = plan.id,
                                statut = determinerStatutGlobal(resultats),
                                resultats = resultats.map { it.toDTO() },
                                dureeMs = resultats.sumOf { it.dureeMs }
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Erreur exécution: ${e.message}" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReponseErreur("Erreur exécution : ${e.message}")
                        )
                    }
                }
                
                // GET /api/v1/historique : Consultation audit
                get("/historique") {
                    val principal = call.principal<UtilisateurPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    
                    // Rate limiting : 100 req/min
                    if (!rateLimiter.autoriser(principal.userId, "historique", limite = 100)) {
                        return@get call.respond(
                            HttpStatusCode.TooManyRequests,
                            ReponseErreur("Rate limit dépassé")
                        )
                    }
                    
                    try {
                        val typeAction = call.parameters["action"]?.let { 
                            ActionType.valueOf(it) 
                        }
                        val dateDebut = call.parameters["dateDebut"]?.let { 
                            Instant.parse(it) 
                        }
                        val dateFin = call.parameters["dateFin"]?.let { 
                            Instant.parse(it) 
                        }
                        val limite = call.parameters["limite"]?.toIntOrNull() ?: 50
                        
                        val filtre = FiltreAudit(
                            typeAction = typeAction,
                            dateDebut = dateDebut,
                            dateFin = dateFin,
                            utilisateurId = principal.userId
                        )
                        
                        val historique = journal.obtenirHistorique(filtre, limite)
                        
                        call.respond(
                            HttpStatusCode.OK,
                            ReponseHistorique(
                                entrees = historique.map { it.toDTO() },
                                total = historique.size
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Erreur historique: ${e.message}" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReponseErreur("Erreur historique : ${e.message}")
                        )
                    }
                }
                
                // GET /api/v1/preferences : Récupération préférences
                get("/preferences") {
                    val principal = call.principal<UtilisateurPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    
                    if (!rateLimiter.autoriser(principal.userId, "preferences", limite = 100)) {
                        return@get call.respond(
                            HttpStatusCode.TooManyRequests,
                            ReponseErreur("Rate limit dépassé")
                        )
                    }
                    
                    try {
                        val cle = "preferences:${principal.userId}"
                        val json = store.recuperer(cle)
                        
                        if (json != null) {
                            call.respondText(json, ContentType.Application.Json)
                        } else {
                            // Préférences par défaut
                            call.respond(
                                HttpStatusCode.OK,
                                PreferencesUtilisateur()
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Erreur préférences: ${e.message}" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReponseErreur("Erreur préférences : ${e.message}")
                        )
                    }
                }
                
                // PUT /api/v1/preferences : Modification préférences
                put("/preferences") {
                    val principal = call.principal<UtilisateurPrincipal>()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized)
                    
                    if (!rateLimiter.autoriser(principal.userId, "preferences", limite = 100)) {
                        return@put call.respond(
                            HttpStatusCode.TooManyRequests,
                            ReponseErreur("Rate limit dépassé")
                        )
                    }
                    
                    try {
                        val preferences = call.receive<PreferencesUtilisateur>()
                        val cle = "preferences:${principal.userId}"
                        
                        // Sérialiser en JSON
                        val json = kotlinx.serialization.json.Json.encodeToString(
                            PreferencesUtilisateur.serializer(),
                            preferences
                        )
                        
                        store.stocker(
                            cle,
                            json,
                            com.koras.assistantvocal.services.stockage.CategorieSecurite.CONFIDENTIELLE
                        )
                        
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                    } catch (e: Exception) {
                        logger.error(e) { "Erreur mise à jour préférences: ${e.message}" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReponseErreur("Erreur mise à jour : ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

// --- DTOs ---

@Serializable
data class RequeteInterpretation(
    val type: TypeEntree,
    val contenu: String,
    val langue: Langue,
    val localisation: Localisation? = null,
    val formatAudio: String? = null,
    val dureeMs: Long? = null
)

@Serializable
enum class TypeEntree {
    TEXTE, AUDIO
}

@Serializable
data class ReponseInterpretation(
    val intention: Intention,
    val confiance: Double,
    val langue: Langue,
    val dureeMs: Long,
    val source: SourceNLU
)

@Serializable
data class RequeteExecution(
    val intention: Intention,
    val langue: Langue? = null,
    @Serializable(with = UUIDSerializer::class)
    val tokenIdempotence: UUID? = null
)

@Serializable
data class ReponseExecution(
    @Serializable(with = UUIDSerializer::class)
    val planId: UUID,
    val statut: StatutExecution,
    val resultats: List<ResultatExecutionDTO>,
    val dureeMs: Long
)

@Serializable
data class ResultatExecutionDTO(
    @Serializable(with = UUIDSerializer::class)
    val idExecution: UUID,
    val statut: StatutExecution,
    val resultat: String,
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    val dureeMs: Long
)

@Serializable
data class ReponseHistorique(
    val entrees: List<EntreeAuditDTO>,
    val total: Int
)

@Serializable
data class EntreeAuditDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = InstantSerializer::class)
    val horodatage: Instant,
    val action: ActionType,
    val resultat: StatutExecution,
    val dureeMs: Long
)

@Serializable
data class ReponseErreur(
    val message: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Clock.System.now()
)

// --- Helpers ---

fun ResultatExecution.toDTO() = ResultatExecutionDTO(
    idExecution = idExecution,
    statut = statut,
    resultat = resultat,
    horodatage = horodatage,
    dureeMs = dureeMs
)

fun EntreeAudit.toDTO() = EntreeAuditDTO(
    id = id,
    horodatage = horodatage,
    action = action,
    resultat = resultat,
    dureeMs = dureeMs
)

fun determinerStatutGlobal(resultats: List<ResultatExecution>): StatutExecution {
    return when {
        resultats.all { it.statut == StatutExecution.SUCCES } -> StatutExecution.SUCCES
        resultats.any { it.statut == StatutExecution.ECHEC } -> StatutExecution.ECHEC
        resultats.any { it.statut == StatutExecution.ANNULE } -> StatutExecution.ANNULE
        else -> StatutExecution.ATTEND_CONFIRMATION
    }
}

/**
 * Principal JWT pour authentification.
 */
data class UtilisateurPrincipal(
    val userId: UUID,
    val roles: List<String>
) : Principal
