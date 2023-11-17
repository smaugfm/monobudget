package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class LunchmoneyStatementItemProcessor :
    StatementItemProcessor<LunchmoneyTransaction, LunchmoneyInsertTransaction>()
