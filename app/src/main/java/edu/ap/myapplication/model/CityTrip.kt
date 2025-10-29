package edu.ap.myapplication.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class CityTrip(
    val id: String = "",
    val title: String = "",
    val city: String = "",
    val country: String = "",
    val description: String = "",
    val image: String = "",
    val dateAdded: Long? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): CityTrip {
            val map = doc.data ?: emptyMap<String, Any?>()
            val dateAny = map["dateAdded"]
            val dateLong = when (dateAny) {
                is Long -> dateAny
                is Int -> dateAny.toLong()
                is Timestamp -> dateAny.seconds
                else -> null
            }
            val latAny = map["latitude"]
            val lngAny = map["longitude"]
            val lat = when (latAny) {
                is Double -> latAny
                is Float -> latAny.toDouble()
                is Long -> latAny.toDouble()
                is Int -> latAny.toDouble()
                is String -> latAny.toDoubleOrNull()
                else -> null
            }
            val lng = when (lngAny) {
                is Double -> lngAny
                is Float -> lngAny.toDouble()
                is Long -> lngAny.toDouble()
                is Int -> lngAny.toDouble()
                is String -> lngAny.toDoubleOrNull()
                else -> null
            }
            return CityTrip(
                id = doc.id,
                title = map["title"] as? String ?: "",
                city = map["city"] as? String ?: "",
                country = map["country"] as? String ?: "",
                description = map["description"] as? String ?: "",
                image = map["image"] as? String ?: "",
                dateAdded = dateLong,
                category = map["category"] as? String,
                latitude = lat,
                longitude = lng
            )
        }
    }
}
