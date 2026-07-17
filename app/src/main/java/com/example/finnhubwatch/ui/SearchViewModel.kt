package com.example.finnhubwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finnhubwatch.data.InstrumentSearchRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.domain.model.FinancialException
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed interface SearchStatus {
    data object Idle : SearchStatus

    data object Loading : SearchStatus

    data object Results : SearchStatus

    data object Empty : SearchStatus

    data class Error(
        val code: String,
    ) : SearchStatus
}

data class SearchUiState(
    val query: String = "",
    val status: SearchStatus = SearchStatus.Idle,
    val results: List<SearchResultUi> = emptyList(),
)

sealed interface SearchQuoteState {
    data object Pending : SearchQuoteState

    data class Available(
        val quote: Quote,
    ) : SearchQuoteState

    data class Unavailable(
        val code: String? = null,
    ) : SearchQuoteState

    data class Retryable(
        val code: String,
    ) : SearchQuoteState
}

data class SearchResultUi(
    val instrument: Instrument,
    val quoteState: SearchQuoteState,
    val isInWatchlist: Boolean,
) {
    val quote: Quote?
        get() = (quoteState as? SearchQuoteState.Available)?.quote

    val canAdd: Boolean
        get() = !isInWatchlist && quote?.price != null
}

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val financialRepository: InstrumentSearchRepository,
        private val watchlistRepository: WatchlistRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchUiState())
        private var searchJob: Job? = null
        private val quoteRetryJobs = mutableMapOf<String, Job>()
        private val quoteMutex = Mutex()
        private var queryGeneration = 0L
        private var lastResults: List<SearchResultUi> = emptyList()

        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        init {
            watchlistRepository.items
                .onEach { items ->
                    val symbols = items.map { it.instrument.symbol }.toSet()
                    updateResults(queryGeneration) { result ->
                        result.copy(isInWatchlist = result.instrument.symbol in symbols)
                    }
                }.launchIn(viewModelScope)
        }

        fun setQuery(value: String) {
            queryGeneration += 1
            val generation = queryGeneration
            _uiState.update { it.copy(query = value) }
            searchJob?.cancel()
            quoteRetryJobs.values.forEach(Job::cancel)
            quoteRetryJobs.clear()
            if (value.isBlank()) {
                lastResults = emptyList()
                _uiState.update { it.copy(status = SearchStatus.Idle, results = emptyList()) }
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(300)
                    search(value, generation)
                }
        }

        fun clearQuery() {
            setQuery("")
        }

        fun retry() {
            setQuery(_uiState.value.query)
        }

        fun retryQuote(symbol: String) {
            val generation = queryGeneration
            val result = lastResults.firstOrNull { it.instrument.symbol == symbol } ?: return
            if (result.quoteState !is SearchQuoteState.Retryable) return

            updateResults(generation) {
                if (it.instrument.symbol == symbol) it.copy(quoteState = SearchQuoteState.Pending) else it
            }
            quoteRetryJobs[symbol]?.cancel()
            quoteRetryJobs[symbol] =
                viewModelScope.launch {
                    hydrateQuote(result.instrument, generation)
                }
        }

        fun toggleMembership(result: SearchResultUi) {
            viewModelScope.launch {
                if (result.isInWatchlist) {
                    watchlistRepository.remove(result.instrument.symbol)
                    return@launch
                }
                if (!result.canAdd) return@launch
                watchlistRepository.upsert(result.instrument, result.quote?.price ?: return@launch)
            }
        }

        private suspend fun search(
            query: String,
            generation: Long,
        ) {
            if (generation != queryGeneration) return
            _uiState.update { it.copy(status = SearchStatus.Loading) }
            val result = financialRepository.search(query)
            if (generation != queryGeneration) return
            if (result.isFailure) {
                val code = (result.exceptionOrNull() as? FinancialException)?.code ?: "network"
                _uiState.update { it.copy(status = SearchStatus.Error(code), results = emptyList()) }
                lastResults = emptyList()
                return
            }

            val rows =
                result
                    .getOrThrow()
                    .distinctBy { it.symbol }
                    .map { instrument ->
                        SearchResultUi(instrument, SearchQuoteState.Pending, false)
                    }
            lastResults = rows
            _uiState.update {
                it.copy(
                    status = if (rows.isEmpty()) SearchStatus.Empty else SearchStatus.Results,
                    results = rows,
                )
            }
            refreshMembership(generation)
            hydrateQuotes(rows, generation)
        }

        private suspend fun hydrateQuotes(
            rows: List<SearchResultUi>,
            generation: Long,
        ) {
            rows.forEach { result ->
                if (generation != queryGeneration) return
                hydrateQuote(result.instrument, generation)
            }
        }

        private suspend fun hydrateQuote(
            instrument: Instrument,
            generation: Long,
        ) {
            val quoteResult = quoteMutex.withLock { financialRepository.quote(instrument.symbol) }
            if (generation != queryGeneration) return
            updateResults(generation) {
                if (it.instrument.symbol == instrument.symbol) {
                    it.copy(quoteState = quoteState(quoteResult))
                } else {
                    it
                }
            }
        }

        private fun quoteState(result: Result<Quote?>): SearchQuoteState =
            if (result.isSuccess) {
                val quote = result.getOrNull()
                if (quote?.price != null) SearchQuoteState.Available(quote) else SearchQuoteState.Unavailable()
            } else {
                val exception = result.exceptionOrNull() as? FinancialException
                if (exception?.retryable == true) {
                    SearchQuoteState.Retryable(exception.code)
                } else {
                    SearchQuoteState.Unavailable(exception?.code)
                }
            }

        private suspend fun refreshMembership(generation: Long) {
            val symbols =
                watchlistRepository.items
                    .first()
                    .map { it.instrument.symbol }
                    .toSet()
            updateResults(generation) { result ->
                result.copy(isInWatchlist = result.instrument.symbol in symbols)
            }
        }

        private fun updateResults(
            generation: Long,
            transform: (SearchResultUi) -> SearchResultUi,
        ) {
            if (generation != queryGeneration) return
            lastResults = lastResults.map(transform)
            _uiState.update { state -> state.copy(results = lastResults) }
        }
    }
