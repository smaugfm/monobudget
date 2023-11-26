package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.TransferCache
import org.koin.core.annotation.Single

@Single
class LunchmoneyTransferCache : TransferCache<LunchmoneyTransaction>()
