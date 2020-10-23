package com.github.smaugfm.events

interface EventHandlerCreator<T> {
    fun create(dispatch: GenericDispatch<T>): IGenericEventHandler<T>
}

interface IGenericEventHandler<T> {
    val name: String
    suspend fun handle(event: T): Boolean
}
typealias EventHandler = IGenericEventHandler<Event>
typealias GenericDispatch<T> = suspend (T) -> Unit
typealias Dispatch = GenericDispatch<Event>
