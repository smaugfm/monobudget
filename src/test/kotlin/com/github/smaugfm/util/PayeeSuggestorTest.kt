package com.github.smaugfm.util

import com.github.smaugfm.service.transaction.PayeeSuggestingService
import org.junit.jupiter.api.Test

class PayeeSuggestorTest {
    @Test
    fun test() {
        val suggestor = PayeeSuggestingService()
        val result = suggestor.twoPass(
            "Intellij Idea Ultimate",
            listOf("intellij idea ultimate", "Intellij Idea", "idea", "ultimate"),
        )
        println(result)
    }
}
