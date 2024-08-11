package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.TransferCache
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Single(binds = [TransferCache::class])
class LunchmoneyTransferCache(expirationDuration: Duration = 1.minutes) :
    TransferCache.Expiring<LunchmoneyTransaction>(expirationDuration)
