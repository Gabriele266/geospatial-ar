package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization

import com.google.ar.core.codelabs.hellogeospatial.persistence.GeoAnchor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*

class AnchorStore(val filename: String) {

    /**
     * An operation that reads the file
     */
    private fun <T> fileReadOperation(work: (stream: FileInputStream) -> T): T {
        val inputStream = FileInputStream(filename)

        val result = work(inputStream)
        inputStream.close()

        return result
    }

    /**
    Operation to write something on the file
     */
    private fun fileWriteOperation(work: (writer: BufferedWriter) -> Unit) {
        val writer = BufferedWriter(FileWriter(filename))

        work(writer)
        writer.close()
    }

    /**
     * Update the anchor store
     */
    fun updateStore(data: List<GeoAnchor>): Boolean {
        fileWriteOperation {
            it.write(Json.encodeToString(data))
        }

        return true
    }

    /**
     * Clear the anchor store
     */
    fun clearStore() {
        fileWriteOperation {
            it.write(Json.encodeToString(listOf<GeoAnchor>()))
        }
    }

    /**
     * Append to the store
     */
    fun appendToStore(data: List<GeoAnchor>) {
        // Load previous content
        val previous = try {
            loadContent().toMutableList()
        } catch (e: IOException) {
            mutableListOf()
        }

        previous.addAll(data)

        fileWriteOperation {
            it.write(Json.encodeToString(previous))
        }
    }

    /**
     * Load all the file content
     */
    fun loadContent(): List<GeoAnchor> = fileReadOperation {
        val bytes = it.readBytes()

        val str = String(bytes)

        if (str.isNotEmpty() && str.isNotBlank()) {
            val parser = GeoAnchorParser()
            return@fileReadOperation parser.parseMore(str)
        }

        return@fileReadOperation listOf<GeoAnchor>()

    }
}