package com.koras.assistantvocal.domaine

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Entités extraites des commandes vocales par le service NLU.
 * Représentent les informations structurées nécessaires pour exécuter les actions.
 */
@Serializable
sealed class EntiteNLU {
    /**
     * Contact (nom et optionnellement numéro de téléphone).
     */
    @Serializable
    data class Contact(
        val nom: String,
        val numero: String? = null
    ) : EntiteNLU() {
        init {
            require(nom.isNotBlank()) { "Le nom du contact ne peut pas être vide" }
            numero?.let {
                require(it.matches(Regex("^\\+?[0-9\\s-]{8,20}$"))) {
                    "Le numéro de téléphone doit être valide"
                }
            }
        }
    }
    
    /**
     * Information temporelle (date/heure absolue).
     */
    @Serializable
    data class Temporel(
        @Serializable(with = InstantSerializer::class)
        val instant: Instant
    ) : EntiteNLU() {
        init {
            require(instant >= Instant.fromEpochMilliseconds(0)) {
                "La date ne peut pas être avant l'époque Unix"
            }
        }
    }
    
    /**
     * Texte libre (message, description, etc.).
     */
    @Serializable
    data class Texte(
        val contenu: String
    ) : EntiteNLU() {
        init {
            require(contenu.isNotBlank()) { "Le texte ne peut pas être vide" }
        }
    }
    
    /**
     * Montant monétaire avec devise.
     */
    @Serializable
    data class Montant(
        @Serializable(with = BigDecimalSerializer::class)
        val valeur: BigDecimal,
        val devise: String = "XOF"  // Franc CFA par défaut
    ) : EntiteNLU() {
        init {
            require(valeur >= BigDecimal.ZERO) {
                "Le montant ne peut pas être négatif"
            }
            require(devise.matches(Regex("^[A-Z]{3}$"))) {
                "La devise doit être un code ISO 4217 valide (3 lettres)"
            }
        }
    }
    
    /**
     * Adresse ou lieu géographique.
     */
    @Serializable
    data class Lieu(
        val adresse: String,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : EntiteNLU() {
        init {
            require(adresse.isNotBlank()) { "L'adresse ne peut pas être vide" }
            if (latitude != null) {
                require(latitude in -90.0..90.0) {
                    "La latitude doit être entre -90 et 90"
                }
            }
            if (longitude != null) {
                require(longitude in -180.0..180.0) {
                    "La longitude doit être entre -180 et 180"
                }
            }
        }
    }
}
