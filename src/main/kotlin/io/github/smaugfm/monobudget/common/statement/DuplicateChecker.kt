package io.github.smaugfm.monobudget.common.statement

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.misc.SimpleCache
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class DuplicateChecker {
    private val idsCache = SimpleCache<String, Unit> {}

    fun isDuplicate(item: StatementItem): Boolean {
        if (!idsCache.checkAndPutKey(item.id, Unit)) {
            log.info { "Skipping duplicate statement $item" }
            return true
        }

        return false
    }
}
