package com.example.finnhubwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finnhubwatch.data.FinancialRepository
import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.ConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val mode: BackendMode = BackendMode.DEMO,
    val connection: ConnectionStatus = ConnectionStatus.Inactive,
    val settingsOpen: Boolean = false,
    val apiKeyDraft: String = "",
    val showConnectionNotice: Boolean = false,
)

class AppViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val financialRepository: FinancialRepository,
) : ViewModel() {
    private val settingsOpen = MutableStateFlow(false)
    private val apiKeyDraft = MutableStateFlow("")
    private val storedApiKey = apiKeyStore.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val showConnectionNotice = MutableStateFlow(false)
    private var connectionNoticeJob: Job? = null

    val uiState: StateFlow<AppUiState> =
        combine(
            financialRepository.mode,
            financialRepository.connection,
            settingsOpen,
            apiKeyDraft,
            showConnectionNotice,
        ) { mode, connection, open, draft, showNotice ->
            AppUiState(mode, connection, open, draft, showNotice)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    init {
        financialRepository.connection
            .onEach { status ->
                connectionNoticeJob?.cancel()
                showConnectionNotice.value = false
                if (status is ConnectionStatus.Connecting || status is ConnectionStatus.Retrying) {
                    connectionNoticeJob =
                        viewModelScope.launch {
                            delay(2_000)
                            if (financialRepository.connection.value == status) showConnectionNotice.value = true
                        }
                }
            }.launchIn(viewModelScope)
    }

    fun openSettings() {
        apiKeyDraft.value = storedApiKey.value
        settingsOpen.value = true
    }

    fun updateApiKeyDraft(value: String) {
        apiKeyDraft.value = value
    }

    fun cancelSettings() {
        settingsOpen.value = false
    }

    fun saveSettings() {
        viewModelScope.launch {
            apiKeyStore.saveApiKey(apiKeyDraft.value.trim())
            settingsOpen.value = false
            financialRepository.reconnect()
        }
    }

    fun reconnect() {
        financialRepository.reconnect()
    }
}
