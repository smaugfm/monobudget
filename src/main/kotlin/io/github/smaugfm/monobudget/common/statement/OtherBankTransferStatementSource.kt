package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.OtherBankStatementItem
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.annotation.Single

@Single
class OtherBankTransferStatementSource : StatementSource {
    private val flow = MutableSharedFlow<StatementProcessingContext>()

    override suspend fun statements() = flow

    suspend fun emit(otherBankStatementItem: OtherBankStatementItem) {
        flow.emit(
            StatementProcessingContext(otherBankStatementItem),
        )
    }
}
