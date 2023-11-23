package io.github.smaugfm.monobudget.common.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.mp.KoinPlatformTools

inline fun <reified T : Any> KoinComponent.injectAll(
    crossinline postProcess: (List<T>) -> List<T> = { it },
): Lazy<List<T>> =
    lazy(KoinPlatformTools.defaultLazyMode()) {
        if (this is KoinScopeComponent) {
            scope.getAll<T>(T::class).let(postProcess)
        } else {
            getKoin().getAll<T>().let(postProcess)
        }
    }

inline fun <reified T : Any, K> KoinComponent.injectAllMap(crossinline mapper: (T) -> K): Lazy<List<K>> =
    lazy(KoinPlatformTools.defaultLazyMode()) {
        if (this is KoinScopeComponent) {
            scope.getAll<T>(T::class).map(mapper)
        } else {
            getKoin().getAll<T>().map(mapper)
        }
    }
