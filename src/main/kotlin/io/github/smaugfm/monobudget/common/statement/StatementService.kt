package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.flow.Flow

interface StatementService {
    suspend fun prepare(): Boolean
    suspend fun statements(): Flow<StatementItem>
}
