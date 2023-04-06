package io.github.smaugfm.monobudget.common.util

import org.koin.core.component.KoinComponent
import org.koin.core.component.KoinScopeComponent
import org.koin.mp.KoinPlatformTools

inline fun <reified T : Any> KoinComponent.injectAll(
    mode: LazyThreadSafetyMode = KoinPlatformTools.defaultLazyMode()
): Lazy<List<T>> = lazy(mode) {
    if (this is KoinScopeComponent) {
        scope.getAll(T::class)
    } else {
        getKoin().getAll<T>()
    }
}
