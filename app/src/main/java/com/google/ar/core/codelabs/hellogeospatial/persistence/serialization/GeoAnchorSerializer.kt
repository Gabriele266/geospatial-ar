package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization

import com.google.ar.core.codelabs.hellogeospatial.persistence.GeoAnchor
import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.common.StringSerializer
import kotlinx.serialization.json.Json

/**
 * Serializer to serialize a geo anchor
 */
class GeoAnchorSerializer : StringSerializer<GeoAnchor> {
    /**
     * Serialize an anchor to a string
     */
    override fun serializeOne(input: GeoAnchor): String =
        Json.encodeToString(GeoAnchor.serializer(), input)

    override fun serializeMore(input: List<GeoAnchor>): List<String> =
        input.map { serializeOne(it) }
}