package io.github.smaugfm.monobudget.components.suggestion

import io.github.smaugfm.monobudget.util.jaroWinklerSimilarity
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class StringSimilarityPayeeSuggestionService {
    fun suggest(value: String, payees: List<String>): List<String> {
        log.debug { "Looking for best payee match for memo: $value" }
        return twoPass(value, payees).map { it.first }.also {
            log.debug { "Found best match: $it" }
        }
    }

    internal fun twoPass(value: String, payees: List<String>): List<Pair<String, Double>> {
        val firstPass = value
            .split(spaceRegex)
            .map { word ->
                payees
                    .map { it to jaroWinklerSimilarity(word, it, true) }
                    .filter { it.second > CASE_INSENSITIVE_JARO_THRESHOLD }
            }
            .flatten()
            .sortedByDescending { it.second }

        return firstPass
            .map { (result) -> result to jaroWinklerSimilarity(value, result, false) }
            .sortedByDescending { it.second }
    }

    companion object {
        private val spaceRegex = Regex("\\s+")
        private const val CASE_INSENSITIVE_JARO_THRESHOLD = 0.9
    }
}
