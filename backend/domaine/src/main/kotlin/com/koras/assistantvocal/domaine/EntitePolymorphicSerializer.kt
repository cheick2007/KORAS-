package com.koras.assistantvocal.domaine

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializer polymorphique pour EntiteNLU.
 * Permet de sérialiser/désérialiser les différents types d'entités.
 */
object EntitePolymorphicSerializer : KSerializer<EntiteNLU> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("EntiteNLU")
    
    override fun serialize(encoder: Encoder, value: EntiteNLU) {
        require(encoder is JsonEncoder)
        val jsonObject = when (value) {
            is EntiteNLU.Contact -> buildJsonObject {
                put("type", "Contact")
                put("nom", value.nom)
                value.numero?.let { put("numero", it) }
            }
            is EntiteNLU.Temporel -> buildJsonObject {
                put("type", "Temporel")
                put("instant", value.instant.toString())
            }
            is EntiteNLU.Texte -> buildJsonObject {
                put("type", "Texte")
                put("contenu", value.contenu)
            }
            is EntiteNLU.Montant -> buildJsonObject {
                put("type", "Montant")
                put("valeur", value.valeur.toPlainString())
                put("devise", value.devise)
            }
            is EntiteNLU.Lieu -> buildJsonObject {
                put("type", "Lieu")
                put("adresse", value.adresse)
                value.latitude?.let { put("latitude", it) }
                value.longitude?.let { put("longitude", it) }
            }
        }
        encoder.encodeJsonElement(jsonObject)
    }
    
    override fun deserialize(decoder: Decoder): EntiteNLU {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        
        val type = element["type"]?.jsonPrimitive?.content
            ?: error("Champ 'type' manquant")
        
        return when (type) {
            "Contact" -> EntiteNLU.Contact(
                nom = element["nom"]?.jsonPrimitive?.content ?: error("Champ 'nom' manquant"),
                numero = element["numero"]?.jsonPrimitive?.contentOrNull
            )
            "Temporel" -> EntiteNLU.Temporel(
                instant = kotlinx.datetime.Instant.parse(
                    element["instant"]?.jsonPrimitive?.content ?: error("Champ 'instant' manquant")
                )
            )
            "Texte" -> EntiteNLU.Texte(
                contenu = element["contenu"]?.jsonPrimitive?.content ?: error("Champ 'contenu' manquant")
            )
            "Montant" -> EntiteNLU.Montant(
                valeur = java.math.BigDecimal(
                    element["valeur"]?.jsonPrimitive?.content ?: error("Champ 'valeur' manquant")
                ),
                devise = element["devise"]?.jsonPrimitive?.content ?: "XOF"
            )
            "Lieu" -> EntiteNLU.Lieu(
                adresse = element["adresse"]?.jsonPrimitive?.content ?: error("Champ 'adresse' manquant"),
                latitude = element["latitude"]?.jsonPrimitive?.doubleOrNull,
                longitude = element["longitude"]?.jsonPrimitive?.doubleOrNull
            )
            else -> error("Type d'entité inconnu: $type")
        }
    }
}
