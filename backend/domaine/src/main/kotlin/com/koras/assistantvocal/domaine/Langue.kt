package com.koras.assistantvocal.domaine

import kotlinx.serialization.Serializable

/**
 * Langues et dialectes supportés par le système NLU.
 * Support de 8 langues pour l'accessibilité maximale.
 */
@Serializable
enum class Langue(val code: String, val nomNatif: String) {
    FRANCAIS("fr", "Français"),
    ANGLAIS("en", "English"),
    ARABE("ar", "العربية"),
    WOLOF("wo", "Wolof"),
    BAMBARA("bm", "Bamanankan"),
    SWAHILI("sw", "Kiswahili"),
    LINGALA("ln", "Lingála"),
    CREOLE_HAITIEN("ht", "Kreyòl ayisyen");
    
    companion object {
        fun depuisCode(code: String): Langue? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
