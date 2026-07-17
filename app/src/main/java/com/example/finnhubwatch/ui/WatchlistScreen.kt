package com.example.finnhubwatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finnhubwatch.domain.model.PriceSource
import com.example.finnhubwatch.theme.FinnhubWatchTheme

@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel = hiltViewModel()) {
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
        Box {
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
}

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
