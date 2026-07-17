package com.example.finnhubwatch.data.remote

import com.example.finnhubwatch.domain.model.BackendEvent
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

fun interface DemoRandom {
    fun nextDouble(
        from: Double,
        until: Double,
    ): Double
}

fun interface DemoClock {
    fun now(): Long
}

class DemoFinancialBackend
    @Inject
    constructor(
        private val random: DemoRandom,
        private val clock: DemoClock,
    ) : FinancialBackend {
        override val mode: BackendMode = BackendMode.DEMO

        override suspend fun search(query: String): List<Instrument> {
            val normalized = query.trim()
            return catalog.filter { instrument ->
                instrument.symbol.contains(normalized, ignoreCase = true) ||
                    instrument.name.contains(normalized, ignoreCase = true)
            }
        }

        override suspend fun quote(symbol: String): Quote? =
            catalog.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.let { instrument ->
                Quote(basePrices.getValue(instrument.symbol), previousClose = basePrices.getValue(instrument.symbol))
            }

        override fun stream(symbols: Set<String>): Flow<BackendEvent> =
            flow {
                emit(BackendEvent.Connected)
                val values = symbols.associateWith { basePrices[it] ?: return@associateWith 0.0 }.toMutableMap()
                while (currentCoroutineContext().isActive) {
                    delay(5_000)
                    values.forEach { (symbol, current) ->
                        val base = basePrices.getValue(symbol)
                        val next = (current + random.nextDouble(-10.0, 10.0)).coerceIn(base * 0.9, base * 1.1)
                        values[symbol] = next
                        emit(BackendEvent.Trade(symbol, next, clock.now()))
                    }
                }
            }

        private companion object {
            val catalog =
                listOf(
                    Instrument("DOCS", "Doximity Inc."),
                    Instrument("NVDA", "NVIDIA Corp."),
                    Instrument("AAPL", "Apple Inc."),
                    Instrument("AMZN", "Amazon.com Inc."),
                    Instrument("MSFT", "Microsoft Corp."),
                )
            val basePrices =
                mapOf(
                    "DOCS" to 230.0,
                    "NVDA" to 212.0,
                    "AAPL" to 327.0,
                    "AMZN" to 255.0,
                    "MSFT" to 395.0,
                )
        }
    }
