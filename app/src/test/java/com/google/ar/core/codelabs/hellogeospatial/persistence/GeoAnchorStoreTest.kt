package com.google.ar.core.codelabs.hellogeospatial.persistence

import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.AnchorStore
import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.GeoAnchorParser
import org.junit.Test

class GeoAnchorStoreTest {
    @Test
    fun `It should correctly load this json string`() {
        val input = """
            [
                {
                    "id": 24,
                    "latitude": 23432432.34,
                    "altitude": 58493503.23,
                    "longitude": 5438904.23,
                    "rotation": [],
                    "resourceId": "test",
                    "name": "Name"
                }
            ]
        """.trimIndent()

        val parser = GeoAnchorParser()
        val parsed = parser.parseMore(input)

        assert(5 == 5)
        assert(parsed.size == 1)
    }

    @Test
    fun `It should correctly append this anchor`() {
        val anchor = GeoAnchor("23", "My resource")
        anchor.setPosition(23.0, 34.0, 32.0, floatArrayOf(3f, 4f, 2f, 1f))

        val store = AnchorStore("file.json")
        store.appendToStore(listOf(anchor))

        store.appendToStore(listOf(anchor))
    }
}