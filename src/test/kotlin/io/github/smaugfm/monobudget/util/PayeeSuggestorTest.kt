package io.github.smaugfm.monobudget.util

import io.github.smaugfm.monobudget.components.suggestion.StringSimilarityPayeeSuggestionService
import org.junit.jupiter.api.Test

class PayeeSuggestorTest {
    @Test
    fun test() {
        val suggestor = StringSimilarityPayeeSuggestionService()
        val result = suggestor.twoPass(
            "Intellij Idea Ultimate",
            listOf("intellij idea ultimate", "Intellij Idea", "idea", "ultimate")
        )
        println(result)
    }
}
