package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Single

@Single
class YnabMonoTransferBetweenAccountsDetector :
    TransferBetweenAccountsDetector<YnabTransactionDetail>()
