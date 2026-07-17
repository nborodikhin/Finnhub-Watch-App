package com.example.finnhubwatch

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.finnhubwatch.ui.SearchScreen
import com.example.finnhubwatch.ui.WatchlistAppScaffold
import com.example.finnhubwatch.ui.WatchlistScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(WatchlistRoute)

    WatchlistAppScaffold(
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
                        entry<WatchlistRoute> { WatchlistScreen() }
                        entry<SearchRoute> { SearchScreen() }
                    },
            )
        },
    )
}
