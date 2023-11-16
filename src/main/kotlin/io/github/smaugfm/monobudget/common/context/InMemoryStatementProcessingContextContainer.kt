package io.github.smaugfm.monobudget.common.context

import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class InMemoryStatementProcessingContextContainer : StatementProcessingContextContainer {
    private val map = ConcurrentHashMap<String, StatementProcessingContext>()

    override fun getOrPut(statementId: String): StatementProcessingContext {
        return map.getOrPut(statementId) { StatementProcessingContext() }
    }

    override fun get(statementId: String) = map[statementId]
        ?: throw IllegalStateException(
            "This should not happen: " +
                "there always must be context present"
        )
}
