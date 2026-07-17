package com.example.finnhubwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.theme.FinnhubWatchTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var apiKeyStore: ApiKeyStore

    @Inject lateinit var financialRepository: FinancialRepository

    @Inject lateinit var watchlistRepository: WatchlistRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinnhubWatchTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation(apiKeyStore, financialRepository, watchlistRepository)
                }
            }
        }
    }
}
