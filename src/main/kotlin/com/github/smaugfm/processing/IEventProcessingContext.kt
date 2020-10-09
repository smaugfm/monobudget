package com.github.smaugfm.processing

import com.github.kotlintelegrambot.Bot
import com.github.smaugfm.ynab.YnabApi

interface IEventProcessingContext {
    val ynab: YnabApi
}