package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import kotlinx.coroutines.flow.Flow

interface StatementSource {
    suspend fun prepare() {}
    suspend fun statements(): Flow<StatementProcessingContext>
}
