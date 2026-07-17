package com.example.finnhubwatch.data.remote

import com.example.finnhubwatch.domain.model.BackendEvent
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface FinancialBackend {
    val mode: BackendMode

    suspend fun search(query: String): List<Instrument>

    suspend fun quote(symbol: String): Quote?

    fun stream(symbols: Set<String>): Flow<BackendEvent>
}
