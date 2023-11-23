package io.github.smaugfm.monobudget.common.lifecycle

import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope

class StatementProcessingScopeComponent(val ctx: StatementProcessingContext) : KoinScopeComponent {
    override val scope =
        createScope().also {
            it.declare(ctx)
        }
}
