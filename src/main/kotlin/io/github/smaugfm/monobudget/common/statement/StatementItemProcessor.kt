package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

interface StatementItemProcessor {
    suspend fun process(item: StatementItem)
}
