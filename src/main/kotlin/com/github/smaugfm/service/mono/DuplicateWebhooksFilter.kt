package com.github.smaugfm.service.mono

import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.util.SimpleCache
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class DuplicateWebhooksFilter(private val mappings: Mappings) {

    private val webhookResponsesCache = SimpleCache<MonoWebhookResponseData, Unit> {}

    fun checkIsDuplicate(webhookResponseData: MonoWebhookResponseData): Boolean {

        if (!webhookResponsesCache.alreadyHasKey(webhookResponseData, Unit)) {
            logger.info { "Duplicate ${MonoWebhookResponseData::class.simpleName} $webhookResponseData" }
            return true
        }

        with(webhookResponseData) {
            logger.info {
                "Incoming transaction from ${mappings.getMonoAccAlias(account)}'s account.\n" +
                    with(statementItem) {
                        "\tAmount: ${amount}${currencyCode}\n" +
                            "\tMemo: $comment"
                    }
            }
        }

        return false
    }
}
