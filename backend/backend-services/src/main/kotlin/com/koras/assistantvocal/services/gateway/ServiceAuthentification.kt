package com.koras.assistantvocal.services.gateway

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}

/**
 * Service d'authentification JWT.
 * Gère la génération, validation et rotation des tokens.
 */
interface ServiceAuthentification {
    /**
     * Génère un token JWT pour un utilisateur.
     */
    fun genererToken(userId: UUID, roles: List<String>): TokenPaire
    
    /**
     * Valide un token JWT.
     */
    fun validerToken(token: String): ResultatValidation
    
    /**
     * Rafraîchit un token avec le refresh token.
     */
    fun rafraichirToken(refreshToken: String): TokenPaire?
    
    /**
     * Révoque un token (blacklist).
     */
    fun revoquerToken(token: String)
}

/**
 * Implémentation du service avec HMAC-SHA256 (MVP) ou RSA (production).
 */
class ServiceAuthentificationImpl(
    private val secret: String = genererSecretAleatoire(),
    private val issuer: String = "koras-assistant-vocal",
    private val audience: String = "koras-clients"
) : ServiceAuthentification {
    
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
    
    // Blacklist simple en mémoire (Redis pour production)
    private val tokensRevoques = mutableSetOf<String>()
    
    // Store des refresh tokens (Redis pour production)
    private val refreshTokens = mutableMapOf<String, RefreshTokenData>()
    
    override fun genererToken(userId: UUID, roles: List<String>): TokenPaire {
        val maintenant = Clock.System.now()
        val expirationAccess = maintenant + 1.hours
        val expirationRefresh = maintenant + 7.days
        
        // Token d'accès (1h)
        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("roles", roles)
            .withClaim("type", "access")
            .withIssuedAt(Date.from(java.time.Instant.ofEpochMilli(maintenant.toEpochMilliseconds())))
            .withExpiresAt(Date.from(java.time.Instant.ofEpochMilli(expirationAccess.toEpochMilliseconds())))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
        
        // Refresh token (7 jours)
        val refreshToken = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("type", "refresh")
            .withIssuedAt(Date.from(java.time.Instant.ofEpochMilli(maintenant.toEpochMilliseconds())))
            .withExpiresAt(Date.from(java.time.Instant.ofEpochMilli(expirationRefresh.toEpochMilliseconds())))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
        
        // Stocker refresh token
        refreshTokens[refreshToken] = RefreshTokenData(
            userId = userId,
            roles = roles,
            expiration = expirationRefresh.toEpochMilliseconds()
        )
        
        logger.debug { "Token généré pour utilisateur $userId (expiration: ${expirationAccess})" }
        
        return TokenPaire(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600, // 1h en secondes
            tokenType = "Bearer"
        )
    }
    
    override fun validerToken(token: String): ResultatValidation {
        return try {
            // Vérifier si révoqué
            if (tokensRevoques.contains(token)) {
                logger.warn { "Token révoqué utilisé" }
                return ResultatValidation.Revoque
            }
            
            // Vérifier signature et expiration
            val decoded = verifier.verify(token)
            
            // Extraire claims
            val userId = UUID.fromString(decoded.subject)
            val roles = decoded.getClaim("roles").asList(String::class.java)
            val type = decoded.getClaim("type").asString()
            
            // Vérifier que c'est un access token
            if (type != "access") {
                return ResultatValidation.TypeInvalide
            }
            
            logger.debug { "Token valide pour utilisateur $userId" }
            
            ResultatValidation.Valide(
                userId = userId,
                roles = roles,
                jti = decoded.id
            )
        } catch (e: com.auth0.jwt.exceptions.TokenExpiredException) {
            logger.debug { "Token expiré: ${e.message}" }
            ResultatValidation.Expire
        } catch (e: Exception) {
            logger.error(e) { "Erreur validation token: ${e.message}" }
            ResultatValidation.Invalide(e.message ?: "Erreur inconnue")
        }
    }
    
    override fun rafraichirToken(refreshToken: String): TokenPaire? {
        return try {
            // Vérifier refresh token
            val decoded = verifier.verify(refreshToken)
            val type = decoded.getClaim("type").asString()
            
            if (type != "refresh") {
                logger.warn { "Token non-refresh utilisé pour rafraîchissement" }
                return null
            }
            
            // Vérifier si dans le store
            val data = refreshTokens[refreshToken]
            if (data == null) {
                logger.warn { "Refresh token non trouvé dans le store" }
                return null
            }
            
            // Vérifier expiration
            if (Clock.System.now().toEpochMilliseconds() > data.expiration) {
                refreshTokens.remove(refreshToken)
                logger.warn { "Refresh token expiré" }
                return null
            }
            
            // Générer nouveau token pair
            val nouveauToken = genererToken(data.userId, data.roles)
            
            // Supprimer ancien refresh token
            refreshTokens.remove(refreshToken)
            
            logger.info { "Token rafraîchi pour utilisateur ${data.userId}" }
            
            nouveauToken
        } catch (e: Exception) {
            logger.error(e) { "Erreur rafraîchissement token: ${e.message}" }
            null
        }
    }
    
    override fun revoquerToken(token: String) {
        tokensRevoques.add(token)
        logger.info { "Token révoqué" }
        
        // Nettoyage périodique des tokens expirés (simple)
        if (tokensRevoques.size > 10000) {
            nettoyerTokensExpires()
        }
    }
    
    private fun nettoyerTokensExpires() {
        val maintenant = Clock.System.now().toEpochMilliseconds()
        
        // Nettoyer refresh tokens expirés
        refreshTokens.entries.removeIf { it.value.expiration < maintenant }
        
        // Pour les tokens révoqués, on ne peut pas vérifier l'expiration
        // sans les décoder, donc on limite juste la taille
        if (tokensRevoques.size > 10000) {
            val aGarder = tokensRevoques.takeLast(5000)
            tokensRevoques.clear()
            tokensRevoques.addAll(aGarder)
        }
        
        logger.info { 
            "Nettoyage: ${refreshTokens.size} refresh tokens, " +
            "${tokensRevoques.size} tokens révoqués" 
        }
    }
}

/**
 * Paire de tokens (access + refresh).
 */
@Serializable
data class TokenPaire(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // secondes
    val tokenType: String = "Bearer"
)

/**
 * Résultat de validation d'un token.
 */
sealed class ResultatValidation {
    data class Valide(
        val userId: UUID,
        val roles: List<String>,
        val jti: String
    ) : ResultatValidation()
    
    object Expire : ResultatValidation()
    object Revoque : ResultatValidation()
    object TypeInvalide : ResultatValidation()
    data class Invalide(val raison: String) : ResultatValidation()
}

/**
 * Données associées à un refresh token.
 */
private data class RefreshTokenData(
    val userId: UUID,
    val roles: List<String>,
    val expiration: Long // timestamp millis
)

/**
 * Génère un secret aléatoire pour HMAC (256 bits).
 * En production, utiliser un secret configuré ou RSA.
 */
private fun genererSecretAleatoire(): String {
    val bytes = ByteArray(32)
    java.security.SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}
