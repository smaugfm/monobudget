package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.common.mono.MonoTransferBetweenAccountsDetector
import org.koin.core.annotation.Single

@Single
class YnabMonoTransferBetweenAccountsDetector :
    MonoTransferBetweenAccountsDetector<YnabTransactionDetail>()
