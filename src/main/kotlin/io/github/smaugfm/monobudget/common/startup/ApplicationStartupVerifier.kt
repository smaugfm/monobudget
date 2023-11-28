package io.github.smaugfm.monobudget.common.startup

interface ApplicationStartupVerifier {
    suspend fun verify()
}
