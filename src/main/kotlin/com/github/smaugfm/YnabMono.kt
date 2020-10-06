package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.mono.MonoApi
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class YnabMono() : CliktCommand() {
    val token by option().required()

    override fun run() {
        runBlocking {
            val api = MonoApi(token)

            val userInfo = api.fetchUserInfo()

            val statements = api.fetchStatementItems(
                userInfo.accounts.first().id,
                Clock.System.now().minus(2.toDuration(DurationUnit.DAYS))
            )

            println(statements)
        }
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)