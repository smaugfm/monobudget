package io.github.smaugfm.monobudget.components.verification

interface ApplicationStartupVerifier {
    suspend fun verify()
}
