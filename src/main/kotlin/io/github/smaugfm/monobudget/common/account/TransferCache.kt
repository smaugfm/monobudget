package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.Deferred
import kotlin.time.Duration.Companion.minutes

abstract class TransferCache<TTransaction> :
    ConcurrentExpiringMap<StatementItem, Deferred<TTransaction>>(1.minutes)
