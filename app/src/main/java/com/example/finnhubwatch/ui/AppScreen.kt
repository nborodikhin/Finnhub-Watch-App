package com.example.finnhubwatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.finnhubwatch.SearchRoute
import com.example.finnhubwatch.WatchlistRoute
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.ConnectionStatus
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.PriceSource
import com.example.finnhubwatch.domain.model.Quote
import com.example.finnhubwatch.domain.model.SearchResult
import com.example.finnhubwatch.theme.FinnhubWatchTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistAppScaffold(
    appViewModel: AppViewModel,
    currentRoute: NavKey?,
    onRouteSelected: (NavKey) -> Unit,
    content: @Composable () -> Unit,
) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()
    val isSearch = currentRoute == SearchRoute

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (isSearch) "Search" else "Watchlist") },
                    actions = {
                        IconButton(
                            onClick = appViewModel::openSettings,
                            modifier = Modifier.semantics { contentDescription = "Open settings" },
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    },
                )
                ConnectionBanner(
                    mode = state.mode,
                    connection = state.connection,
                    showConnectionNotice = state.showConnectionNotice,
                    onReconnect = appViewModel::reconnect,
                )
                TabRow(selectedTabIndex = if (isSearch) 1 else 0) {
                    Tab(
                        selected = !isSearch,
                        onClick = { onRouteSelected(WatchlistRoute) },
                        modifier = Modifier.semantics { contentDescription = "Watchlist tab" },
                        text = { Text("Watchlist") },
                    )
                    Tab(
                        selected = isSearch,
                        onClick = { onRouteSelected(SearchRoute) },
                        modifier = Modifier.semantics { contentDescription = "Search tab" },
                        text = { Text("Search") },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
        ) {
            content()
        }
    }

    if (state.settingsOpen) {
        SettingsDialog(
            value = state.apiKeyDraft,
            onValueChange = appViewModel::updateApiKeyDraft,
            onSave = appViewModel::saveSettings,
            onCancel = appViewModel::cancelSettings,
        )
    }
}

@Composable
private fun ConnectionBanner(
    mode: BackendMode,
    connection: ConnectionStatus,
    showConnectionNotice: Boolean,
    onReconnect: () -> Unit,
) {
    val banner =
        when {
            connection == ConnectionStatus.Unauthorized -> BannerModel("API key error, update in settings", Icons.Default.Error, false)
            mode == BackendMode.DEMO -> BannerModel("Demo mode, set api key in settings", Icons.Default.Warning, false)
            connection == ConnectionStatus.Disconnected ->
                BannerModel(
                    "Live updates paused. Tap to reconnect",
                    Icons.Default.CloudOff,
                    true,
                )
            showConnectionNotice && (connection is ConnectionStatus.Connecting || connection is ConnectionStatus.Retrying) ->
                BannerModel(
                    "Connecting...",
                    Icons.Default.Refresh,
                    false,
                )
            else -> null
        }
    banner ?: return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = banner.action, onClick = onReconnect)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(banner.icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(banner.text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (banner.action) {
            TextButton(onClick = onReconnect) { Text("RECONNECT") }
        }
    }
}

private data class BannerModel(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: Boolean,
)

@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WatchlistContent(
        state = state,
        onFilterChanged = viewModel::setFilter,
        onClearFilter = { viewModel.setFilter("") },
        onSortSelected = viewModel::selectSort,
        onRemove = viewModel::remove,
    )
}

@Composable
internal fun WatchlistContent(
    state: WatchlistUiState,
    onFilterChanged: (String) -> Unit,
    onClearFilter: () -> Unit,
    onSortSelected: (WatchlistSort) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterBar(
            value = state.filter,
            onValueChange = onFilterChanged,
            onClear = onClearFilter,
            sort = state.sort,
            ascending = state.ascending,
            onSortSelected = onSortSelected,
        )
        when {
            state.isLoading -> LoadingState()
            state.rows.isEmpty() -> EmptyState("No results", "Add instruments from the Search tab.")
            else ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.symbol }) { row -> WatchlistRow(row, onRemove) }
                }
        }
    }
}

@Composable
private fun FilterBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    sort: WatchlistSort,
    ascending: Boolean,
    onSortSelected: (WatchlistSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Filter") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.semantics { contentDescription = "Clear filter" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        SortButton("Symbol", sort == WatchlistSort.SYMBOL, ascending, { onSortSelected(WatchlistSort.SYMBOL) })
        SortButton("Price", sort == WatchlistSort.PRICE, ascending, { onSortSelected(WatchlistSort.PRICE) })
    }
}

@Composable
private fun SortButton(
    label: String,
    selected: Boolean,
    ascending: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier.semantics {
                contentDescription = "Sort by $label ${if (ascending) "ascending" else "descending"}"
                role = Role.Button
            },
    ) {
        Text(label)
        if (selected) {
            Icon(
                if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun WatchlistRow(
    row: WatchlistRowUi,
    onRemove: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { menuOpen = true }
                    .semantics {
                        contentDescription = "Actions for ${row.symbol}"
                        role = Role.Button
                    }.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.symbol, style = MaterialTheme.typography.titleMedium)
                Text(
                    row.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        row.source.name,
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (row.source ==
                                PriceSource.LIVE
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatPrice(row.price), style = MaterialTheme.typography.titleMedium)
                }
                IconButton(
                    onClick = { menuOpen = true },
                    modifier =
                        Modifier.size(28.dp).semantics {
                            contentDescription =
                                "More actions for ${row.symbol}"
                        },
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Remove from watchlist") },
                onClick = {
                    menuOpen = false
                    onRemove(row.symbol)
                },
            )
        }
    }
}

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
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
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
internal fun SettingsDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Settings") },
        text = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("finnhub.io API key (leave empty for demo mode)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

private fun formatPrice(price: Double?): String = price?.let { "${'$'}%.2f".format(Locale.US, it) } ?: "--"

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun WatchlistPreview() {
    FinnhubWatchTheme {
        WatchlistContent(
            WatchlistUiState(rows = listOf(WatchlistRowUi("AAPL", "Apple Inc.", 327.10, PriceSource.CACHED, null)), isLoading = false),
            {},
            {},
            {},
            {},
        )
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
