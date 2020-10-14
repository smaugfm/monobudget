package com.github.smaugfm.ynab

import io.ktor.client.features.ResponseException

class YnabApiException(
    val e: ResponseException,
    val errorResponse: YnabErrorResponse,
) : Exception(e)
