package com.google.ar.core.codelabs.hellogeospatial.persistence

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class GeoAnchor(
    val resourceId: String,
    val name: String,
    ) {
    val id: Int = idFactory.produce()

    var latitude: Double = 0.0
    var altitude: Double = 0.0
    var longitude: Double = 0.0
    var rotation: FloatArray = floatArrayOf()

    /**
     * Set the position of this anchor
     * @param l Latitude
     * @param long Longitude
     * @param a Altitude
     */
    fun setPosition(l: Double, long: Double, a: Double, rt: FloatArray) {
        apply {
            latitude = l
            altitude = a
            longitude = long
            rotation = rt
        }
    }

    val dx: Float?
        get() = rotation.firstOrNull()

    val dy: Float?
        get() = if (rotation.size == 4) rotation[1] else null

    val dz: Float?
        get() = if (rotation.size == 4) rotation[2] else null

    val dw: Float?
        get() = rotation.lastOrNull()
}