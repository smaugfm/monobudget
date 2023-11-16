package io.github.smaugfm.monobudget.common.context

class StatementProcessingContext {
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
