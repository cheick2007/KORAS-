package com.koras.assistantvocal.services.gateway

import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

/**
 * Interface du rate limiter.
 * Limite le nombre de requêtes par utilisateur/endpoint.
 */
interface RateLimiter {
    /**
     * Autorise une requête selon les quotas.
     * 
     * @param userId ID utilisateur
     * @param endpoint Endpoint appelé
     * @param limite Limite de requêtes par minute
     * @return true si autorisé, false si limite dépassée
     */
    fun autoriser(userId: UUID, endpoint: String, limite: Int): Boolean
    
    /**
     * Réinitialise le compteur pour un utilisateur/endpoint.
     */
    fun reinitialiser(userId: UUID, endpoint: String)
    
    /**
     * Obtient les statistiques pour un utilisateur.
     */
    fun obtenirStats(userId: UUID): StatsRateLimit
}

/**
 * Implémentation avec sliding window en mémoire (Redis pour production).
 */
class RateLimiterMemoire : RateLimiter {
    
    private data class FenetreRequetes(
        val timestamps: MutableList<Long> = mutableListOf(),
        var derniereVerification: Long = System.currentTimeMillis()
    )
    
    // Clé : "userId:endpoint"
    private val fenetres = ConcurrentHashMap<String, FenetreRequetes>()
    
    override fun autoriser(userId: UUID, endpoint: String, limite: Int): Boolean {
        val cle = "$userId:$endpoint"
        val maintenant = System.currentTimeMillis()
        val fenetreMs = 60_000L // 1 minute
        
        val fenetre = fenetres.computeIfAbsent(cle) { FenetreRequetes() }
        
        synchronized(fenetre) {
            // Nettoyer les timestamps hors de la fenêtre
            fenetre.timestamps.removeIf { it < maintenant - fenetreMs }
            
            // Vérifier limite
            if (fenetre.timestamps.size >= limite) {
                logger.debug { 
                    "Rate limit dépassé pour $userId sur $endpoint: " +
                    "${fenetre.timestamps.size}/$limite" 
                }
                return false
            }
            
            // Ajouter timestamp
            fenetre.timestamps.add(maintenant)
            fenetre.derniereVerification = maintenant
            
            logger.trace { 
                "Requête autorisée pour $userId sur $endpoint: " +
                "${fenetre.timestamps.size}/$limite" 
            }
            
            return true
        }
    }
    
    override fun reinitialiser(userId: UUID, endpoint: String) {
        val cle = "$userId:$endpoint"
        fenetres.remove(cle)
        logger.debug { "Rate limit réinitialisé pour $userId sur $endpoint" }
    }
    
    override fun obtenirStats(userId: UUID): StatsRateLimit {
        val maintenant = System.currentTimeMillis()
        val fenetreMs = 60_000L
        
        val statsParEndpoint = mutableMapOf<String, Int>()
        var totalRequetes = 0
        
        fenetres.forEach { (cle, fenetre) ->
            if (cle.startsWith("$userId:")) {
                val endpoint = cle.substringAfter(":")
                
                synchronized(fenetre) {
                    // Compter requêtes dans la fenêtre
                    val requetesActives = fenetre.timestamps.count { 
                        it >= maintenant - fenetreMs 
                    }
                    
                    statsParEndpoint[endpoint] = requetesActives
                    totalRequetes += requetesActives
                }
            }
        }
        
        return StatsRateLimit(
            userId = userId,
            totalRequetes = totalRequetes,
            parEndpoint = statsParEndpoint
        )
    }
    
    /**
     * Nettoyage périodique des fenêtres expirées.
     * À appeler régulièrement (toutes les 5 minutes par exemple).
     */
    fun nettoyerFenetresExpirees() {
        val maintenant = System.currentTimeMillis()
        val timeoutMs = 5 * 60_000L // 5 minutes
        
        val aSupprimer = fenetres.filterValues { fenetre ->
            synchronized(fenetre) {
                maintenant - fenetre.derniereVerification > timeoutMs
            }
        }.keys
        
        aSupprimer.forEach { fenetres.remove(it) }
        
        if (aSupprimer.isNotEmpty()) {
            logger.debug { "Nettoyage: ${aSupprimer.size} fenêtres rate limit supprimées" }
        }
    }
}

/**
 * Implémentation Redis pour production (stub pour MVP).
 */
class RateLimiterRedis(
    private val redisClient: Any? = null // Lettuce RedisClient pour production
) : RateLimiter {
    
    // Pour le MVP, déléguer à l'implémentation mémoire
    private val fallback = RateLimiterMemoire()
    
    override fun autoriser(userId: UUID, endpoint: String, limite: Int): Boolean {
        // TODO: Implémenter avec Redis
        // redis.eval(luaScript, keys = [userId:endpoint], args = [limite, fenêtre])
        
        return fallback.autoriser(userId, endpoint, limite)
    }
    
    override fun reinitialiser(userId: UUID, endpoint: String) {
        fallback.reinitialiser(userId, endpoint)
    }
    
    override fun obtenirStats(userId: UUID): StatsRateLimit {
        return fallback.obtenirStats(userId)
    }
}

/**
 * Statistiques de rate limiting pour un utilisateur.
 */
data class StatsRateLimit(
    val userId: UUID,
    val totalRequetes: Int,
    val parEndpoint: Map<String, Int>
)

/**
 * Configuration des limites par endpoint.
 */
data class ConfigurationRateLimit(
    val limitesParEndpoint: Map<String, Int> = mapOf(
        "interprete" to 10,      // 10 req/min pour interprétation
        "execute" to 100,        // 100 req/min pour exécution
        "historique" to 100,     // 100 req/min pour historique
        "preferences" to 100     // 100 req/min pour préférences
    ),
    val limiteGlobale: Int = 200 // 200 req/min toutes routes confondues
)

/**
 * Script Lua pour Redis (atomic rate limiting).
 * À utiliser en production avec EVALSHA.
 */
object ScriptsLuaRateLimit {
    /**
     * Script sliding window pour Redis.
     * 
     * KEYS[1] = clé de la fenêtre (userId:endpoint)
     * ARGV[1] = limite
     * ARGV[2] = fenêtre en millisecondes (60000 pour 1 min)
     * ARGV[3] = timestamp actuel
     * 
     * Retourne: 1 si autorisé, 0 si limite dépassée
     */
    val slidingWindow = """
        local key = KEYS[1]
        local limite = tonumber(ARGV[1])
        local fenetre = tonumber(ARGV[2])
        local maintenant = tonumber(ARGV[3])
        local debut_fenetre = maintenant - fenetre
        
        -- Supprimer timestamps expirés
        redis.call('ZREMRANGEBYSCORE', key, '-inf', debut_fenetre)
        
        -- Compter requêtes dans la fenêtre
        local count = redis.call('ZCARD', key)
        
        if count < limite then
            -- Ajouter nouveau timestamp
            redis.call('ZADD', key, maintenant, maintenant)
            redis.call('EXPIRE', key, 120) -- TTL 2 minutes
            return 1
        else
            return 0
        end
    """.trimIndent()
}
