package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.mono.TransferBetweenAccountsDetector
import org.koin.core.annotation.Single

@Single
class LunchmoneyMonoTransferBetweenAccountsDetector :
    TransferBetweenAccountsDetector<LunchmoneyTransaction>()
