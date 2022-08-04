package com.google.ar.core.codelabs.hellogeospatial.persistence

/**
 * Represents a Factory to produce something
 */
interface Factory<T> {
    /**
     * Produce one element
     */
    fun produce(): T

    /**
     * Produce N elements
     */
    fun produceMany(count: Int): List<T>
}