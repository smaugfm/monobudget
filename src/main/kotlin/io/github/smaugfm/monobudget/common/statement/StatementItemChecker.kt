package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.misc.SimpleCache
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

open class StatementItemChecker {
    private val webhookResponsesCache = SimpleCache<String, Unit> {}

    open suspend fun check(item: StatementItem): Boolean {
        if (!webhookResponsesCache.checkAndPutKey(item.id, Unit)) {
            log.info { "Duplicate ${MonoWebhookResponseData::class.simpleName} $item" }
            return false
        }

        return true
    }
}
