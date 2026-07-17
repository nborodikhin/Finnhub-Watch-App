package com.example.finnhubwatch.domain.model

data class Instrument(
    val symbol: String,
    val name: String,
)

data class Quote(
    val price: Double?,
    val previousClose: Double? = null,
    val timestamp: Long? = null,
)

data class SearchResult(
    val instrument: Instrument,
    val quote: Quote?,
)

data class WatchlistItem(
    val instrument: Instrument,
    val cachedPrice: Double?,
)

data class LivePrice(
    val price: Double,
    val timestamp: Long,
)

enum class PriceSource {
    LIVE,
    CACHED,
}

enum class BackendMode {
    DEMO,
    REAL,
}

sealed interface ConnectionStatus {
    data object Inactive : ConnectionStatus

    data object Connecting : ConnectionStatus

    data object Live : ConnectionStatus

    data class Retrying(
        val attempt: Int,
        val delaySeconds: Int,
    ) : ConnectionStatus

    data object Disconnected : ConnectionStatus

    data object Unauthorized : ConnectionStatus
}

sealed class FinancialException(
    message: String,
    val code: String,
    val unauthorized: Boolean = false,
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Api(
        code: String,
        message: String,
    ) : FinancialException(message, code, retryable = code.isRetryableHttpCode())

    class Network(
        message: String,
        cause: Throwable? = null,
    ) : FinancialException(message, "network", retryable = true, cause = cause)

    class Authorization(
        code: String = "401",
        message: String = "API key was rejected",
    ) : FinancialException(message, code, unauthorized = true)
}

private fun String.isRetryableHttpCode(): Boolean =
    toIntOrNull()?.let { code ->
        code == 408 || code == 429 || code >= 500
    } == true

sealed interface BackendEvent {
    data object Connected : BackendEvent

    data class Trade(
        val symbol: String,
        val price: Double,
        val timestamp: Long,
    ) : BackendEvent

    data class Failed(
        val exception: FinancialException,
    ) : BackendEvent
}
