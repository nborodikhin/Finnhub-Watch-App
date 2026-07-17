package com.example.finnhubwatch.ui

import com.example.finnhubwatch.data.model.Instrument
import com.example.finnhubwatch.data.model.LivePrice
import com.example.finnhubwatch.data.model.PriceSource
import com.example.finnhubwatch.data.model.WatchlistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistRowsTest {
    private val items =
        listOf(
            WatchlistItem(Instrument("AAPL", "Apple Inc."), 327.0),
            WatchlistItem(Instrument("DOCS", "Doximity Inc."), null),
            WatchlistItem(Instrument("NVDA", "NVIDIA Corp."), 212.0),
        )

    @Test
    fun priceSortKeepsMissingPricesLastInBothDirections() {
        val descending = buildWatchlistRows(items, emptyMap(), "", WatchlistSort.PRICE, false)
        val ascending = buildWatchlistRows(items, emptyMap(), "", WatchlistSort.PRICE, true)

        assertEquals(listOf("AAPL", "NVDA", "DOCS"), descending.map { it.symbol })
        assertEquals(listOf("NVDA", "AAPL", "DOCS"), ascending.map { it.symbol })
    }

    @Test
    fun livePriceOverridesCachedValueAndSource() {
        val rows =
            buildWatchlistRows(
                items,
                mapOf("AAPL" to LivePrice(330.0, 1L)),
                "apple",
                WatchlistSort.SYMBOL,
                true,
            )

        assertEquals(330.0, rows.single().price)
        assertEquals(PriceSource.LIVE, rows.single().source)
    }
}
