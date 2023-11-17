package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class YnabStatementItemProcessor : StatementItemProcessor<YnabTransactionDetail, YnabSaveTransaction>()
