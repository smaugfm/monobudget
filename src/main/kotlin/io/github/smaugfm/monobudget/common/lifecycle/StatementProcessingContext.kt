package io.github.smaugfm.monobudget.common.lifecycle

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

class StatementProcessingContext(
    val item: StatementItem
) {
    private val map: MutableMap<String, Any> = mutableMapOf()

    var isCompleted: Boolean = false
        private set

    fun markCompleted() {
        isCompleted = true
    }

    fun <T> getOrPut(key: String, lazyValue: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return map.getOrPut(key, lazyValue as () -> Any) as T
    }
}
