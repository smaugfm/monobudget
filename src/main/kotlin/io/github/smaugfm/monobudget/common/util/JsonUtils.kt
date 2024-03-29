package io.github.smaugfm.monobudget.common.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonNamingStrategy

fun makeJson(convertSnakeCase: Boolean = false): Json = Json { buildJson(convertSnakeCase) }

fun JsonBuilder.buildJson(convertSnakeCase: Boolean = false) {
    if (convertSnakeCase) {
        namingStrategy = JsonNamingStrategy.SnakeCase
    }
    prettyPrint = true
    ignoreUnknownKeys = true
}
