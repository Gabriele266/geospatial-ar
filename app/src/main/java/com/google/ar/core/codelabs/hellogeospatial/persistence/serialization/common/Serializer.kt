package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.common

/**
 * Generic serializer interface to serialize some data into a specific format
 */
interface Serializer<T, Output> {
    /**
     * Serialize one element to a string
     */
    fun serializeOne(input: T): Output

    /**
     * Serialize more elements
     */
    fun serializeMore(input: List<T>): List<Output>
}

/**
 * Specific serializer interface to serialize into a string
 */
interface StringSerializer<T> : Serializer<T, String>