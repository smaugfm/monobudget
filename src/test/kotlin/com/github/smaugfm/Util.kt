package com.github.smaugfm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.junit.jupiter.api.assertDoesNotThrow

internal object Util {
    fun <T> CoroutineScope.checkAsync(block: suspend () -> T): Deferred<T> =
        async {
            assertDoesNotThrow { block() }
        }
}
