package com.google.ar.core.codelabs.hellogeospatial.persistence

import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.GeoAnchorSerializer
import org.junit.Test

internal class GeoAnchorSerializerTest {
    @Test
    fun serializeOne() {
        val serializer = GeoAnchorSerializer()
        val anchor = GeoAnchor("23", "My resource")
        anchor.setPosition(23, 34, 32, floatArrayOf(3f, 4f, 2f, 1f))
        val result = serializer.serializeOne(anchor)
        assert(result.isNotEmpty())
        assert(result.contains("resourceId"))
        assert(result.contains("name"))
        assert(result.contains("id"))
    }

    @Test
    fun serializeMore() {
        var counter = 23
        val data = Array(5) { GeoAnchor((++counter).toString(), "A name") }.toList()
        val serializer = GeoAnchorSerializer()
        val result = serializer.serializeMore(data)

        assert(result.size == 5)
    }
}