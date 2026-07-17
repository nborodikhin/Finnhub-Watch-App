package com.example.finnhubwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.domain.model.FinancialException
import com.example.finnhubwatch.domain.model.SearchResult
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

data class SearchResultUi(
    val result: SearchResult,
    val isInWatchlist: Boolean,
)

class SearchViewModel(
    private val financialRepository: FinancialRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    private var searchJob: Job? = null
    private var lastResults: List<SearchResult> = emptyList()

    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        watchlistRepository.items
            .onEach { items ->
                val symbols = items.map { it.instrument.symbol }.toSet()
                _uiState.update { state ->
                    state.copy(results = lastResults.map { SearchResultUi(it, it.instrument.symbol in symbols) })
                }
            }.launchIn(viewModelScope)
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        if (value.isBlank()) {
            lastResults = emptyList()
            _uiState.update { it.copy(status = SearchStatus.Idle, results = emptyList()) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(300)
                search(value)
            }
    }

    fun clearQuery() {
        setQuery("")
    }

    fun retry() {
        setQuery(_uiState.value.query)
    }

    fun toggleMembership(result: SearchResultUi) {
        viewModelScope.launch {
            if (result.isInWatchlist) {
                watchlistRepository.remove(result.result.instrument.symbol)
            } else {
                watchlistRepository.upsert(result.result.instrument, result.result.quote?.price)
            }
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(status = SearchStatus.Loading) }
        financialRepository.search(query).fold(
            onSuccess = { results ->
                lastResults = results
                _uiState.update {
                    it.copy(
                        status = if (results.isEmpty()) SearchStatus.Empty else SearchStatus.Results,
                        results = results.map { result -> SearchResultUi(result, false) },
                    )
                }
                refreshMembership()
            },
            onFailure = { error ->
                val code = (error as? FinancialException)?.code ?: "network"
                _uiState.update { it.copy(status = SearchStatus.Error(code), results = emptyList()) }
            },
        )
    }

    private suspend fun refreshMembership() {
        val symbols =
            watchlistRepository.items
                .first()
                .map { it.instrument.symbol }
                .toSet()
        _uiState.update { state ->
            state.copy(results = lastResults.map { SearchResultUi(it, it.instrument.symbol in symbols) })
        }
    }
}
