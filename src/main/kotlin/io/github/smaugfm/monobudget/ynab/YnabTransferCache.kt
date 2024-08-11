package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.TransferCache
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Single
class YnabTransferCache(expirationDuration: Duration = 1.minutes) :
    TransferCache.Expiring<YnabTransactionDetail>(expirationDuration)
