package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import kotlinx.coroutines.flow.Flow

interface StatementService {
    suspend fun prepare(): Boolean
    suspend fun statements(): Flow<StatementProcessingContext>
}
