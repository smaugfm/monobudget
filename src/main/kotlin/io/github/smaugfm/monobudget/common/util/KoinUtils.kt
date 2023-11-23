package io.github.smaugfm.monobudget.common.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.mp.KoinPlatformTools

inline fun <reified T : Any> KoinComponent.injectAll(
    crossinline postProcess: (List<T>) -> List<T> = { it },
): Lazy<List<T>> = lazy(KoinPlatformTools.defaultLazyMode()) {
    if (this is KoinScopeComponent) {
        scope.getAll<T>(T::class).let(postProcess)
    } else {
        getKoin().getAll<T>()
    }
}
