package io.github.smaugfm.monobudget.common.verify

interface ApplicationStartupVerifier {
    suspend fun verify()
}
