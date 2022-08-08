package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization

import com.google.ar.core.codelabs.hellogeospatial.persistence.GeoAnchor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GeoAnchorParser {
    fun parseMore(input: String): List<GeoAnchor> =
        Json.decodeFromString<List<GeoAnchor>>(input)
}