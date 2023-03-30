package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.mono.MonoTransferBetweenAccountsDetector
import org.koin.core.annotation.Single

@Single
class LunchmoneyMonoTransferBetweenAccountsDetector :
    MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>()
