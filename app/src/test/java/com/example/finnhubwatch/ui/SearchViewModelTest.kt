package com.example.finnhubwatch.ui

import com.example.finnhubwatch.data.InstrumentSearchRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.domain.model.FinancialException
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import com.example.finnhubwatch.domain.model.WatchlistItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun publishesResultsBeforeHydratingEveryQuote() =
        runTest(dispatcher) {
            val repository = FakeInstrumentSearchRepository()
            val watchlist = FakeWatchlistRepository()
            val viewModel = SearchViewModel(repository, watchlist)

            viewModel.setQuery("a")
            advanceTimeBy(300)
            runCurrent()

            assertEquals(SearchStatus.Results, viewModel.uiState.value.status)
            val initialState = viewModel.uiState.value
            assertTrue(
                initialState.results.all { it.quoteState == SearchQuoteState.Pending },
            )
            assertEquals(listOf("AAPL"), repository.quoteRequests)

            repository.completeQuote("AAPL", Result.success(Quote(327.0)))
            runCurrent()
            val availableState = viewModel.uiState.value
            assertTrue(
                availableState.results.first().quoteState is SearchQuoteState.Available,
            )
            assertEquals(listOf("AAPL", "APPL.TO"), repository.quoteRequests)

            repository.completeQuote("APPL.TO", Result.failure(FinancialException.Api("403", "forbidden")))
            runCurrent()
            val unavailableState = viewModel.uiState.value
            assertTrue(
                unavailableState.results[1].quoteState is SearchQuoteState.Unavailable,
            )
            assertEquals(listOf("AAPL", "APPL.TO", "DOCS"), repository.quoteRequests)

            repository.completeQuote("DOCS", Result.success(Quote(230.0)))
            runCurrent()
            val finalState = viewModel.uiState.value
            val priced = finalState.results.first()
            viewModel.toggleMembership(priced)
            runCurrent()

            assertEquals(listOf("AAPL"), watchlist.items.value.map { it.instrument.symbol })
            viewModel.toggleMembership(viewModel.uiState.value.results[1])
            runCurrent()
            assertEquals(listOf("AAPL"), watchlist.items.value.map { it.instrument.symbol })
        }

    @Test
    fun retryableQuoteCanBeRetriedWithoutRepeatingSearch() =
        runTest(dispatcher) {
            val repository = FakeInstrumentSearchRepository()
            val viewModel = SearchViewModel(repository, FakeWatchlistRepository())

            viewModel.setQuery("a")
            advanceTimeBy(300)
            runCurrent()
            repository.completeQuote("AAPL", Result.failure(FinancialException.Api("503", "server")))
            runCurrent()
            val retryableState = viewModel.uiState.value
            assertTrue(retryableState.results.first().quoteState is SearchQuoteState.Retryable)
            repository.completeQuote("APPL.TO", Result.failure(FinancialException.Api("403", "forbidden")))
            runCurrent()
            repository.completeQuote("DOCS", Result.success(Quote(230.0)))
            runCurrent()

            repository.resetQuote("AAPL")
            viewModel.retryQuote("AAPL")
            runCurrent()
            assertEquals(1, repository.searchRequests)
            repository.completeQuote("AAPL", Result.success(Quote(327.0)))
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(
                state.results.first().quoteState is SearchQuoteState.Available,
            )
            assertEquals(1, repository.searchRequests)
        }

    @Test
    fun newQuerySupersedesPendingQuoteHydration() =
        runTest(dispatcher) {
            val repository = FakeInstrumentSearchRepository()
            val viewModel = SearchViewModel(repository, FakeWatchlistRepository())

            viewModel.setQuery("a")
            advanceTimeBy(300)
            runCurrent()
            viewModel.setQuery("docs")
            advanceTimeBy(300)
            runCurrent()
            repository.completeQuote("DOCS", Result.success(Quote(230.0)))
            runCurrent()

            assertEquals("docs", viewModel.uiState.value.query)
            val state = viewModel.uiState.value
            assertEquals(
                listOf("DOCS"),
                state.results.map { it.instrument.symbol },
            )
        }
}

private class FakeInstrumentSearchRepository : InstrumentSearchRepository {
    private val pendingQuotes = mutableMapOf<String, CompletableDeferred<Result<Quote?>>>()
    val quoteRequests = mutableListOf<String>()
    var searchRequests = 0
        private set

    override suspend fun search(query: String): Result<List<Instrument>> {
        searchRequests += 1
        return if (query == "docs") {
            Result.success(listOf(Instrument("DOCS", "Doximity Inc.")))
        } else {
            Result.success(
                listOf(
                    Instrument("AAPL", "Apple Inc."),
                    Instrument("APPL.TO", "Apple Inc."),
                    Instrument("DOCS", "Doximity Inc."),
                ),
            )
        }
    }

    override suspend fun quote(symbol: String): Result<Quote?> {
        quoteRequests += symbol
        return pendingQuotes.getOrPut(symbol) { CompletableDeferred() }.await()
    }

    fun completeQuote(
        symbol: String,
        result: Result<Quote?>,
    ) {
        pendingQuotes.getValue(symbol).complete(result)
    }

    fun resetQuote(symbol: String) {
        pendingQuotes[symbol] = CompletableDeferred()
    }
}

private class FakeWatchlistRepository : WatchlistRepository {
    override val items = MutableStateFlow<List<WatchlistItem>>(emptyList())

    override suspend fun upsert(
        instrument: Instrument,
        cachedPrice: Double?,
    ) {
        items.value = items.value.filterNot { it.instrument.symbol == instrument.symbol } + WatchlistItem(instrument, cachedPrice)
    }

    override suspend fun remove(symbol: String) {
        items.value = items.value.filterNot { it.instrument.symbol == symbol }
    }
}
