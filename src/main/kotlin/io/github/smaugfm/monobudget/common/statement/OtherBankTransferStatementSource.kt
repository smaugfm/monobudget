package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.model.financial.OtherBankStatementItem
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.annotation.Single

@Single
class OtherBankTransferStatementSource : StatementSource {
    private val flow = MutableSharedFlow<StatementProcessingContext>()
    override suspend fun statements() = flow

    suspend fun emit(otherBankStatementItem: OtherBankStatementItem) {
        flow.emit(
            StatementProcessingContext(otherBankStatementItem)
        )
    }
}
