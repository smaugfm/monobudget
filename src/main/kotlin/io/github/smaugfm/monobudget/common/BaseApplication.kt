package io.github.smaugfm.monobudget.common

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.exception.BudgetBackendException
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementEvents
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

abstract class BaseApplication<TTransaction, TNewTransaction> : KoinComponent {
    protected abstract val statementSources: List<StatementSource>
    private val statementEvents by inject<StatementEvents>()

    open suspend fun run() {
        beforeStart()

        statementSources.forEach { it.prepare() }

        afterSourcesPrepare()

        log.info { "Started application" }

        statementSources.asFlow()
            .flatMapMerge { it.statements() }
            .filter(statementEvents::onNewStatement)
            .map(::StatementProcessingScopeComponent)
            .onEach {
                with(it) {
                    try {
                        scope.get<StatementItemProcessor<TTransaction, TNewTransaction>>()
                            .process()
                        statementEvents.onStatementEnd(ctx)
                    } catch (e: BudgetBackendException) {
                        statementEvents.onStatementRetry(ctx, e)
                    } catch (e: Throwable) {
                        statementEvents.onStatementError(ctx, e)
                    } finally {
                        scope.close()
                    }
                }
            }
            .collect()
    }

    protected open suspend fun beforeStart() {
    }

    protected open suspend fun afterSourcesPrepare() {
    }
}
