package com.example.finnhubwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.domain.model.LivePrice
import com.example.finnhubwatch.domain.model.PriceSource
import com.example.finnhubwatch.domain.model.WatchlistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WatchlistSort {
    SYMBOL,
    PRICE,
}

data class WatchlistRowUi(
    val symbol: String,
    val name: String,
    val price: Double?,
    val source: PriceSource,
    val changePercent: Double?,
)

data class WatchlistUiState(
    val filter: String = "",
    val sort: WatchlistSort = WatchlistSort.SYMBOL,
    val ascending: Boolean = true,
    val rows: List<WatchlistRowUi> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class WatchlistViewModel
    @Inject
    constructor(
        private val watchlistRepository: WatchlistRepository,
        financialRepository: FinancialRepository,
    ) : ViewModel() {
        private val filter = MutableStateFlow("")
        private val sort = MutableStateFlow(WatchlistSort.SYMBOL)
        private val ascending = MutableStateFlow(true)

        val uiState: StateFlow<WatchlistUiState> =
            combine(
                watchlistRepository.items,
                financialRepository.livePrices,
                filter,
                sort,
                ascending,
            ) { items, livePrices, filterText, sortField, isAscending ->
                val ordered = buildWatchlistRows(items, livePrices, filterText, sortField, isAscending)
                WatchlistUiState(
                    filter = filterText,
                    sort = sortField,
                    ascending = isAscending,
                    rows = ordered,
                    isLoading = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

        fun setFilter(value: String) {
            filter.value = value
        }

        fun selectSort(value: WatchlistSort) {
            if (sort.value == value) {
                ascending.value = !ascending.value
            } else {
                sort.value = value
                ascending.value = true
            }
        }

        fun remove(symbol: String) {
            viewModelScope.launch { watchlistRepository.remove(symbol) }
        }
    }

internal fun buildWatchlistRows(
    items: List<WatchlistItem>,
    livePrices: Map<String, LivePrice>,
    filterText: String,
    sortField: WatchlistSort,
    isAscending: Boolean,
): List<WatchlistRowUi> {
    val filtered =
        items
            .map { item ->
                val live = livePrices[item.instrument.symbol]
                WatchlistRowUi(
                    symbol = item.instrument.symbol,
                    name = item.instrument.name,
                    price = live?.price ?: item.cachedPrice,
                    source = if (live == null) PriceSource.CACHED else PriceSource.LIVE,
                    changePercent = null,
                )
            }.filter { row ->
                filterText.isBlank() ||
                    row.symbol.contains(filterText, ignoreCase = true) ||
                    row.name.contains(filterText, ignoreCase = true)
            }

    val sorted =
        when (sortField) {
            WatchlistSort.SYMBOL -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.symbol })
            WatchlistSort.PRICE -> {
                val (priced, missing) = filtered.partition { it.price != null }
                priced.sortedBy { it.price } + missing
            }
        }
    return if (sortField == WatchlistSort.PRICE && !isAscending) {
        val (priced, missing) = sorted.partition { it.price != null }
        priced.asReversed() + missing
    } else if (isAscending) {
        sorted
    } else {
        sorted.asReversed()
    }
}
