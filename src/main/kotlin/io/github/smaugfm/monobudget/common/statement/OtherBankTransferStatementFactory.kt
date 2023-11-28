package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.model.financial.OtherBankStatementItem
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.model.settings.OtherBanksTransferSettings
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingEventListener
import org.koin.core.annotation.Single

@Single
class OtherBankTransferStatementFactory(
    private val transferSettings: List<OtherBanksTransferSettings>,
    private val otherBanksStatementService: OtherBankTransferStatementSource,
) : StatementProcessingEventListener.New {
    override suspend fun handleNewStatement(ctx: StatementProcessingContext): Boolean {
        with(ctx) {
            if (item is OtherBankStatementItem) {
                return true
            }

            val matchingSetting = findMatchingSettings(item)
            if (matchingSetting != null) {
                emitTransfer(matchingSetting, item)
            }

            return true
        }
    }

    private fun findMatchingSettings(item: StatementItem) =
        transferSettings.find {
            it.mcc == item.mcc && it.descriptionRegex.matches(item.description ?: "")
        }

    private suspend fun emitTransfer(
        matchingSetting: OtherBanksTransferSettings,
        item: StatementItem,
    ) {
        otherBanksStatementService.emit(
            OtherBankStatementItem(
                accountId = matchingSetting.transferAccountId,
                description = matchingSetting.transferDescription,
                mcc = item.mcc,
                amount = -item.amount,
                operationAmount = -item.operationAmount,
                currency = item.currency,
            ),
        )
    }
}
