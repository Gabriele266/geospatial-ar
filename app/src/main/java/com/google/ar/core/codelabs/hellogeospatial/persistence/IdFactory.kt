package com.google.ar.core.codelabs.hellogeospatial.persistence

class IdFactory(
    private var temp: Int = 0
) : Factory<Int> {

    override fun produce(): Int = ++temp

    override fun produceMany(count: Int): List<Int> =
        Array(count) {
            produce()
        }.toList()
}

/**
 * Pre-made id factory
 */
val idFactory = IdFactory()