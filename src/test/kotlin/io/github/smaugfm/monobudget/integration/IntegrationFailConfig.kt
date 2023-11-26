package io.github.smaugfm.monobudget.integration

sealed class IntegrationFailConfig(val attemptFailRange: IntRange) {
    class Update(attemptFailRange: IntRange) :
        IntegrationFailConfig(attemptFailRange)

    class Insert(attemptFailRange: IntRange) :
        IntegrationFailConfig(attemptFailRange)

    class GetSingle(attemptFailRange: IntRange) :
        IntegrationFailConfig(attemptFailRange)

    class CreateTransactionGroup(attemptFailRange: IntRange) :
        IntegrationFailConfig(attemptFailRange)
}
