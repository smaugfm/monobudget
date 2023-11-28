package io.github.smaugfm.monobudget.common.misc

import io.github.smaugfm.monobudget.common.util.misc.StringSimilarityPayeeSuggestionService
import org.junit.jupiter.api.Test

class PayeeSuggestorTest {
    @Test
    fun payeeSuggestorSimpleTest() {
        val suggestor = StringSimilarityPayeeSuggestionService()
        val result =
            suggestor.twoPassJaroWindlerSimilarity(
                "Intellij Idea Ultimate",
                listOf("intellij idea ultimate", "Intellij Idea", "idea", "ultimate"),
            )
        println(result)
    }
}
