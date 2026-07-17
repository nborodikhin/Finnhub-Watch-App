package com.example.finnhubwatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.finnhubwatch.SearchRoute
import com.example.finnhubwatch.WatchlistRoute
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistAppScaffold(
    appViewModel: AppViewModel = hiltViewModel(),
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
                singleLine = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
