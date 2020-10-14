package com.github.smaugfm.events

interface IEventDispatcher<T> {
    suspend fun dispatch(event: T)
}
