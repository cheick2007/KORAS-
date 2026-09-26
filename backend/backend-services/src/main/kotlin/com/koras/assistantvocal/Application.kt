package com.koras.assistantvocal

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.koras.assistantvocal.services.execution.*
import com.koras.assistantvocal.services.gateway.*
import com.koras.assistantvocal.services.nlu.*
import com.koras.assistantvocal.services.orchestration.OrchestrateurdeTaches
import com.koras.assistantvocal.services.orchestration.OrchestrateurdeTachesImpl
import com.koras.assistantvocal.services.parsing.FormateurCommandes
import com.koras.assistantvocal.services.parsing.ParserCommandes
import com.koras.assistantvocal.services.stockage.StoreMemoireChiffre
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Point d'entrée de l'application.
 */
fun main() {
    logger.info { "🚀 Démarrage de l'assistant vocal Koras..." }
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Configuration du module Ktor.
 */
fun Application.module() {
    // Configuration JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Initialiser les services
    val config = chargerConfiguration()
    val services = initialiserServices(config)
    
    // Configurer l'authentification JWT
    configurerAuthentificationJWT(services.authentification)
    
    // Configurer les routes
    configureGatewayAPI(
        serviceNLU = services.nlu,
        orchestrateur = services.orchestrateur,
        executeur = services.executeur,
        journal = services.journal,
        store = services.store,
        authentification = services.authentification,
        rateLimiter = services.rateLimiter
    )
    
    // Route racine
    routing {
        get("/") {
            call.respondText(
                """
                {
                  "service": "Koras Assistant Vocal",
                  "version": "1.0.0",
                  "status": "running",
                  "endpoints": {
                    "health": "/health",
                    "api": "/api/v1/*",
                    "docs": "/docs"
                  }
                }
                """.trimIndent(),
                ContentType.Application.Json
            )
        }
    }
    
    logger.info { "✅ Application Koras démarrée sur http://0.0.0.0:8080" }
}

/**
 * Configuration de l'authentification JWT.
 */
fun Application.configurerAuthentificationJWT(authService: ServiceAuthentification) {
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: "dev-secret-change-in-production-256-bits"
    val jwtIssuer = environment.config.propertyOrNull("jwt.issuer")?.getString()
        ?: "koras-assistant-vocal"
    val jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString()
        ?: "koras-clients"
    
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Koras Assistant Vocal"
            
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            
            validate { credential ->
                // Valider avec le service d'authentification
                val token = credential.payload.token
                when (val validation = authService.validerToken(token)) {
                    is ResultatValidation.Valide -> {
                        UtilisateurPrincipal(
                            userId = validation.userId,
                            roles = validation.roles
                        )
                    }
                    else -> null
                }
            }
            
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token invalide ou expiré")
            }
        }
    }
}

/**
 * Conteneur de services.
 */
data class Services(
    val nlu: ServiceNLU,
    val orchestrateur: OrchestrateurdeTaches,
    val executeur: ExecuteurSecurise,
    val journal: JournalAudit,
    val store: StoreMemoireChiffre,
    val authentification: ServiceAuthentification,
    val rateLimiter: RateLimiter
)

/**
 * Initialise tous les services de l'application.
 */
fun initialiserServices(config: Configuration): Services {
    logger.info { "📦 Initialisation des services..." }
    
    // Services de base
    val parser = ParserCommandes()
    val formateur = FormateurCommandes()
    val store = StoreMemoireChiffre()
    
    // NLU
    val nluEdge = NLUEdge(parser)
    val nluCloud = NLUCloud()
    val serviceNLU = ServiceNLUImpl(nluEdge, nluCloud)
    
    // Orchestration
    val orchestrateur = OrchestrateurdeTachesImpl(formateur)
    
    // Exécution
    val journal = JournalAuditProduction()
    val cacheIdempotence = CacheIdempotenceMemoire()
    val signeur = SignateurCryptoSHA256()
    val executeur = ExecuteurSecuriseImpl(
        cache = cacheIdempotence,
        journal = journal,
        signeur = signeur
    )
    
    // Gateway
    val authentification = ServiceAuthentificationImpl(
        secret = config.jwtSecret,
        issuer = config.jwtIssuer,
        audience = config.jwtAudience
    )
    val rateLimiter = RateLimiterMemoire()
    
    logger.info { "✅ Services initialisés avec succès" }
    
    return Services(
        nlu = serviceNLU,
        orchestrateur = orchestrateur,
        executeur = executeur,
        journal = journal,
        store = store,
        authentification = authentification,
        rateLimiter = rateLimiter
    )
}

/**
 * Configuration de l'application.
 */
data class Configuration(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val port: Int
)

/**
 * Charge la configuration depuis l'environnement ou les valeurs par défaut.
 */
fun Application.chargerConfiguration(): Configuration {
    return Configuration(
        jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
            ?: "dev-secret-change-in-production-256-bits-hmac-sha256-key",
        jwtIssuer = environment.config.propertyOrNull("jwt.issuer")?.getString()
            ?: "koras-assistant-vocal",
        jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString()
            ?: "koras-clients",
        port = environment.config.propertyOrNull("ktor.deployment.port")?.getString()?.toInt()
            ?: 8080
    )
}
