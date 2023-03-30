package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.mono.MonoTransferBetweenAccountsDetector

class LunchmoneyMonoTransferBetweenAccountsDetector :
    MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>()
