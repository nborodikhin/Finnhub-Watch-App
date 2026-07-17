package com.example.finnhubwatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.ui.AppViewModel
import com.example.finnhubwatch.ui.AppViewModelFactory
import com.example.finnhubwatch.ui.SearchScreen
import com.example.finnhubwatch.ui.SearchViewModel
import com.example.finnhubwatch.ui.SearchViewModelFactory
import com.example.finnhubwatch.ui.WatchlistAppScaffold
import com.example.finnhubwatch.ui.WatchlistScreen
import com.example.finnhubwatch.ui.WatchlistViewModel
import com.example.finnhubwatch.ui.WatchlistViewModelFactory

@Composable
fun MainNavigation(
    apiKeyStore: ApiKeyStore,
    financialRepository: FinancialRepository,
    watchlistRepository: WatchlistRepository,
) {
    val appViewModel: AppViewModel = viewModel(factory = remember { AppViewModelFactory(apiKeyStore, financialRepository) })
    val watchlistViewModel: WatchlistViewModel =
        viewModel(factory = remember { WatchlistViewModelFactory(watchlistRepository, financialRepository) })
    val searchViewModel: SearchViewModel =
        viewModel(factory = remember { SearchViewModelFactory(financialRepository, watchlistRepository) })
    val backStack = rememberNavBackStack(WatchlistRoute)

    WatchlistAppScaffold(
        appViewModel = appViewModel,
        currentRoute = backStack.lastOrNull(),
        onRouteSelected = { route ->
            backStack.clear()
            backStack.add(route)
        },
        content = {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider =
                    entryProvider {
                        entry<WatchlistRoute> { WatchlistScreen(watchlistViewModel) }
                        entry<SearchRoute> { SearchScreen(searchViewModel) }
                    },
            )
        },
    )
}
