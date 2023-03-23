package io.github.smaugfm.monobudget.service.verification

interface ApplicationStartupVerifier {
    suspend fun verify()
}
