package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization

import com.google.ar.core.codelabs.hellogeospatial.persistence.GeoAnchor
import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.common.StringParser
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GeoAnchorParser : StringParser<GeoAnchor> {
    override fun parseMore(input: List<String>): List<GeoAnchor> =
        input.map { parseOne(it) }

    override fun parseOne(input: String): GeoAnchor =
        Json.decodeFromString(input)
}