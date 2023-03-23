package io.github.smaugfm.monobudget.service.mono

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.util.SimpleCache
import io.github.smaugfm.monobudget.util.formatAmount
import io.github.smaugfm.monobudget.util.pp
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class DuplicateWebhooksFilter(private val monoAccountsService: MonoAccountsService) {

    private val webhookResponsesCache = SimpleCache<MonoWebhookResponseData, Unit> {}

    fun checkIsDuplicate(webhookResponseData: MonoWebhookResponseData): Boolean {
        if (!webhookResponsesCache.alreadyHasKey(webhookResponseData, Unit)) {
            log.info { "Duplicate ${MonoWebhookResponseData::class.simpleName} $webhookResponseData" }
            return true
        }

        with(webhookResponseData) {
            log.info {
                "Incoming transaction from ${monoAccountsService.getMonoAccAlias(account)}'s account.\n" +
                    with(statementItem) {
                        if (log.isDebugEnabled)
                            this.pp()
                        else
                            "\tAmount: ${currencyCode.formatAmount(amount)}\n" +
                            "\tDescription: $description"
                            "\tMemo: $comment"
                    }
            }
        }

        return false
    }
}
