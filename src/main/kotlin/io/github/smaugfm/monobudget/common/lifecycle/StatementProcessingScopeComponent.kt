package io.github.smaugfm.monobudget.common.lifecycle

import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope

class StatementProcessingScopeComponent(val statementItem: StatementItem) : KoinScopeComponent {
    override val scope = createScope().also {
        it.declare(statementItem)
        it.declare(StatementProcessingContext(statementItem))
    }
}
