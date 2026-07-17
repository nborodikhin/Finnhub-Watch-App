package com.example.finnhubwatch.data

import com.example.finnhubwatch.data.remote.DemoFinancialBackend
import com.example.finnhubwatch.data.remote.FinancialBackend
import com.example.finnhubwatch.data.remote.FinnhubFinancialBackend
import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.domain.model.BackendEvent
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.ConnectionStatus
import com.example.finnhubwatch.domain.model.FinancialException
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.LivePrice
import com.example.finnhubwatch.domain.model.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class FinancialRepository
    @Inject
    constructor(
        private val apiKeyStore: ApiKeyStore,
        private val watchlistRepository: WatchlistRepository,
        private val demoBackend: DemoFinancialBackend,
        private val realBackend: FinnhubFinancialBackend,
        private val foregroundMonitor: ForegroundMonitor,
    ) : InstrumentSearchRepository {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val _livePrices = MutableStateFlow<Map<String, LivePrice>>(emptyMap())
        private val _connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Inactive)
        private val _mode = MutableStateFlow(BackendMode.DEMO)
        private val reconnectToken = MutableStateFlow(0L)

        val livePrices: StateFlow<Map<String, LivePrice>> = _livePrices.asStateFlow()
        val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()
        val mode: StateFlow<BackendMode> = _mode.asStateFlow()

        init {
            val keyFlow = apiKeyStore.apiKey.distinctUntilChanged()
            scope.launch {
                keyFlow.collectLatest { key -> _mode.value = if (key.isBlank()) BackendMode.DEMO else BackendMode.REAL }
            }
            scope.launch {
                combine(
                    keyFlow,
                    watchlistRepository.items.map { items -> items.map { it.instrument.symbol }.toSet() },
                    foregroundMonitor.isForeground,
                    reconnectToken,
                ) { key, symbols, foreground, _ ->
                    StreamConfig(key, symbols, foreground)
                }.distinctUntilChanged()
                    .collectLatest(::runStream)
            }
        }

        override suspend fun search(query: String): Result<List<Instrument>> =
            try {
                val backend = activeBackend()
                Result.success(backend.search(query))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FinancialException) {
                if (exception.unauthorized) _connection.value = ConnectionStatus.Unauthorized
                Result.failure(exception)
            } catch (exception: Exception) {
                Result.failure(exception)
            }

        override suspend fun quote(symbol: String): Result<Quote?> =
            try {
                Result.success(activeBackend().quote(symbol))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FinancialException) {
                if (exception.unauthorized) _connection.value = ConnectionStatus.Unauthorized
                Result.failure(exception)
            } catch (exception: Exception) {
                Result.failure(exception)
            }

        fun reconnect() {
            reconnectToken.value += 1
        }

        private suspend fun activeBackend(): FinancialBackend = if (apiKeyStore.apiKey.first().isBlank()) demoBackend else realBackend

        private suspend fun runStream(config: StreamConfig) {
            if (!config.foreground || config.symbols.isEmpty()) {
                clearLivePrices()
                _connection.value = ConnectionStatus.Inactive
                return
            }

            val backend = if (config.key.isBlank()) demoBackend else realBackend
            var attempt = 0
            while (true) {
                try {
                    _connection.value = ConnectionStatus.Connecting
                    backend.stream(config.symbols).collect { event ->
                        when (event) {
                            BackendEvent.Connected -> _connection.value = ConnectionStatus.Live
                            is BackendEvent.Trade -> onTrade(event)
                            is BackendEvent.Failed -> throw event.exception
                        }
                    }
                    if (backend.mode == BackendMode.DEMO) return
                    throw FinancialException.Network("WebSocket closed")
                } catch (exception: CancellationException) {
                    clearLivePrices()
                    throw exception
                } catch (exception: FinancialException) {
                    clearLivePrices()
                    if (exception.unauthorized) {
                        _connection.value = ConnectionStatus.Unauthorized
                        return
                    }
                    if (backend.mode == BackendMode.DEMO) {
                        _connection.value = ConnectionStatus.Disconnected
                        return
                    }
                    attempt += 1
                    if (attempt > RETRY_DELAYS_SECONDS.size) {
                        _connection.value = ConnectionStatus.Disconnected
                        return
                    }
                    val delaySeconds = RETRY_DELAYS_SECONDS[attempt - 1]
                    _connection.value = ConnectionStatus.Retrying(attempt, delaySeconds)
                    delay(delaySeconds * 1_000L)
                }
            }
        }

        private suspend fun onTrade(event: BackendEvent.Trade) {
            val livePrice = LivePrice(event.price, event.timestamp)
            _livePrices.value = _livePrices.value + (event.symbol to livePrice)
            watchlistRepository.items.first().firstOrNull { it.instrument.symbol == event.symbol }?.let { item ->
                watchlistRepository.upsert(item.instrument, event.price)
            }
        }

        private fun clearLivePrices() {
            _livePrices.value = emptyMap()
        }

        private data class StreamConfig(
            val key: String,
            val symbols: Set<String>,
            val foreground: Boolean,
        )

        private companion object {
            val RETRY_DELAYS_SECONDS = intArrayOf(1, 2, 4, 8)
        }
    }

interface InstrumentSearchRepository {
    suspend fun search(query: String): Result<List<Instrument>>

    suspend fun quote(symbol: String): Result<Quote?>
}
