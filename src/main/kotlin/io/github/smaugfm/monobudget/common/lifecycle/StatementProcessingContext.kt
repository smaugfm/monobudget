package io.github.smaugfm.monobudget.common.lifecycle

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

open class StatementProcessingContext(val item: StatementItem) {
    protected val map: MutableMap<String, Any> = mutableMapOf()

    var isCompleted: Boolean = false
        private set

    fun markCompleted() {
        isCompleted = true
    }

    suspend fun execIfNotSet(key: String, block: suspend () -> Unit) {
        val flag = map[key] as Boolean?
        if (flag == null || !flag) {
            block().also {
                map[key] = true
            }
        }
    }

    suspend fun <T> getOrPut(key: String, lazyValue: suspend () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return map.getOrPut(key) { lazyValue() as Any } as T
    }
}
