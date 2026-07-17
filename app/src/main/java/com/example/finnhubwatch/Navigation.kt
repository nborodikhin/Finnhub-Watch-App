package com.example.finnhubwatch

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.finnhubwatch.ui.AppViewModel
import com.example.finnhubwatch.ui.SearchScreen
import com.example.finnhubwatch.ui.SearchViewModel
import com.example.finnhubwatch.ui.WatchlistAppScaffold
import com.example.finnhubwatch.ui.WatchlistScreen
import com.example.finnhubwatch.ui.WatchlistViewModel

@Composable
fun MainNavigation() {
    val appViewModel: AppViewModel = hiltViewModel()
    val watchlistViewModel: WatchlistViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
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
