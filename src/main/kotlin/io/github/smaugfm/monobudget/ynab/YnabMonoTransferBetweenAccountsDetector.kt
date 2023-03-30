package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.common.mono.MonoTransferBetweenAccountsDetector

class YnabMonoTransferBetweenAccountsDetector :
    MonoTransferBetweenAccountsDetector<YnabTransactionDetail>()
