package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

interface StatementItemListener {
    suspend fun onNewStatementItem(item: StatementItem)
}
