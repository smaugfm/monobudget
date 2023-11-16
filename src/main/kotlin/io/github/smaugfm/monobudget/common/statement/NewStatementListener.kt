package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

interface NewStatementListener {
    suspend fun onNewStatement(item: StatementItem): Boolean
}
