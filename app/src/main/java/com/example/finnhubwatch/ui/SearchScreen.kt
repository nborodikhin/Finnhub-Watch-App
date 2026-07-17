package com.example.finnhubwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import com.example.finnhubwatch.domain.model.SearchResult
import com.example.finnhubwatch.theme.FinnhubWatchTheme

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        state = state,
        onQueryChanged = viewModel::setQuery,
        onClear = viewModel::clearQuery,
        onToggle = viewModel::toggleMembership,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun SearchContent(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    onToggle: (SearchResultUi) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
            placeholder = { Text("Search stocks, crypto, forex") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.semantics { contentDescription = "Clear search" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        when (val status = state.status) {
            SearchStatus.Idle -> EmptyState("Search for an instrument", "Try AAPL or Apple.")
            SearchStatus.Loading -> LoadingState()
            SearchStatus.Empty -> EmptyState("No results", "Try a different symbol or name.")
            is SearchStatus.Error -> ErrorState(status.code, onRetry)
            SearchStatus.Results ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.results, key = { it.result.instrument.symbol }) { result -> SearchRow(result, onToggle) }
                }
        }
    }
}

@Composable
private fun SearchRow(
    result: SearchResultUi,
    onToggle: (SearchResultUi) -> Unit,
) {
    val instrument = result.result.instrument
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(instrument.symbol, style = MaterialTheme.typography.titleMedium)
            Text(instrument.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatPrice(result.result.quote?.price), style = MaterialTheme.typography.bodyLarge)
        Checkbox(
            checked = result.isInWatchlist,
            onCheckedChange = { onToggle(result) },
            modifier = Modifier.semantics { contentDescription = "Add ${instrument.symbol} to watchlist" },
        )
    }
}

@Composable
private fun ErrorState(
    code: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Spacer(Modifier.size(12.dp))
        Text("API Error: $code", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(12.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SearchPreview() {
    FinnhubWatchTheme {
        SearchContent(
            SearchUiState(
                query = "a",
                status = SearchStatus.Results,
                results = listOf(SearchResultUi(SearchResult(Instrument("AAPL", "Apple Inc."), Quote(327.10)), true)),
            ),
            {},
            {},
            {},
            {},
        )
    }
}
