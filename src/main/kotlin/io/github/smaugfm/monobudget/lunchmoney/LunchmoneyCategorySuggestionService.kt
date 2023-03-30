package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.suggestion.CategorySuggestionService
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(createdAtStart = true)
class LunchmoneyCategorySuggestionService : CategorySuggestionService() {
    private val periodicFetcherFactory: PeriodicFetcherFactory by inject()
    private val api: LunchmoneyApi by inject()

    private val categoriesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) {
        api.getAllCategories().awaitSingle()
    }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> = categoriesFetcher.getData().map {
        it.id.toString() to it.name
    }

    override suspend fun categoryNameById(categoryId: String?): String? {
        if (categoryId == null) {
            return null
        }
        val idLong = categoryId.toLong()
        return categoriesFetcher.getData().find { it.id == idLong }?.name
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
        ?.toString()
}
