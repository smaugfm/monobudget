package com.github.smaugfm.events

interface IEventDispatcher {
    suspend fun dispatch(event: Event)
}