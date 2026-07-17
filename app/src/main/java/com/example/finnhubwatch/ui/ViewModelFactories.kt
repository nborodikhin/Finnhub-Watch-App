package com.example.finnhubwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.data.settings.ApiKeyStore

class AppViewModelFactory(
    private val apiKeyStore: ApiKeyStore,
    private val financialRepository: FinancialRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(apiKeyStore, financialRepository) as T
}

class WatchlistViewModelFactory(
    private val watchlistRepository: WatchlistRepository,
    private val financialRepository: FinancialRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WatchlistViewModel(watchlistRepository, financialRepository) as T
}

class SearchViewModelFactory(
    private val financialRepository: FinancialRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(financialRepository, watchlistRepository) as T
}
