package io.github.smaugfm.monobudget.common.context

import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.NewStatementListener
import org.koin.core.annotation.Single

@Single
class StatementProcessingContextNewStatementListener(
    private val container: StatementProcessingContextContainer
) : NewStatementListener {

    override suspend fun onNewStatement(item: StatementItem): Boolean {
        return !container.getOrPut(item.id).isCompleted
    }
}
