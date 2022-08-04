package com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.common

interface Parser<T, Output> {
    fun parseOne(input: T): Output

    fun parseMore(input: List<T>): List<Output>
}

interface StringParser<T> : Parser<String, T>