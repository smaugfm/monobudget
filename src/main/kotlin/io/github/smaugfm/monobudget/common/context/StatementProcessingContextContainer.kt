package io.github.smaugfm.monobudget.common.context

interface StatementProcessingContextContainer {
    fun getOrPut(statementId: String): StatementProcessingContext
    fun get(statementId: String): StatementProcessingContext
}
