package io.github.smaugfm.monobudget.common.telegram

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.misc.SimpleCache
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.util.formatAmount
import io.github.smaugfm.monobudget.common.util.pp
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

class TelegramWebhookResponseChecker : KoinComponent {
    private val monoAccountsService: MonoAccountsService by inject()
    private val webhookResponsesCache = SimpleCache<MonoWebhookResponseData, Unit> {}
    private val monoSettings: io.github.smaugfm.monobudget.common.model.Settings.MultipleMonoSettings by inject()

    fun check(responseData: MonoWebhookResponseData): Boolean =
        checkValid(responseData) && checkNotDuplicate(responseData)

    private fun checkValid(responseData: MonoWebhookResponseData): Boolean {
        if (!monoSettings.monoAccountsIds.contains(responseData.account)) {
            log.info {
                "Skipping transaction from Monobank " +
                    "accountId=${responseData.account} because this account is not configured."
            }
            return false
        }

        return true
    }

    private fun checkNotDuplicate(webhookResponseData: MonoWebhookResponseData): Boolean {
        if (!webhookResponsesCache.alreadyHasKey(webhookResponseData, Unit)) {
            log.info { "Duplicate ${MonoWebhookResponseData::class.simpleName} $webhookResponseData" }
            return true
        }

        with(webhookResponseData) {
            log.info {
                "Incoming transaction from ${monoAccountsService.getMonoAccAlias(account)}'s account.\n" +
                    with(statementItem) {
                        if (log.isDebugEnabled) {
                            this.pp()
                        } else {
                            "\tAmount: ${currencyCode.formatAmount(amount)}\n" +
                                "\tDescription: $description" +
                                "\tMemo: $comment"
                        }
                    }
            }
        }

        return false
    }
}
