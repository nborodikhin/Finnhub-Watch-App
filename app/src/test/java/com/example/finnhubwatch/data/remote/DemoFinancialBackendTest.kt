package com.example.finnhubwatch.data.remote

import app.cash.turbine.test
import com.example.finnhubwatch.data.model.BackendEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFinancialBackendTest {
    private val backend =
        DemoFinancialBackend(
            random = DemoRandom { _, _ -> 1_000.0 },
            clock = DemoClock { 123L },
        )

    @Test
    fun searchReturnsPredefinedInstrument() =
        runTest {
            val result = backend.search("aapl")

            assertEquals(listOf("AAPL"), result.map { it.symbol })
        }

    @Test
    fun quoteReturnsFixedBasePrice() =
        runTest {
            val quote = backend.quote("DOCS")

            assertEquals(230.0, quote?.price)
        }

    @Test
    fun streamStartsConnected() =
        runTest {
            backend.stream(setOf("AAPL")).test {
                assertEquals(BackendEvent.Connected, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun streamUsesInjectedClockAndBoundedPrice() =
        runTest {
            backend.stream(setOf("AAPL")).test {
                awaitItem()
                val trade = awaitItem() as BackendEvent.Trade

                assertEquals(123L, trade.timestamp)
                assertTrue(trade.price in 327.0 * 0.9..327.0 * 1.1)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
