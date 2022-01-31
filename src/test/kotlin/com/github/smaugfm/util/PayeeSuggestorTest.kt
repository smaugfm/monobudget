package com.github.smaugfm.util

import kotlinx.coroutines.DelicateCoroutinesApi
import org.junit.jupiter.api.Test

class PayeeSuggestorTest {
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun test() {
        val suggestor = PayeeSuggestor()
        val result = suggestor.twoPass(
            "Борис М.",
            listOf("борисполь"),
        )
        println(result)
    }
}
