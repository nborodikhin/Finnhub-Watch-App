package com.example.finnhubwatch.data

import com.example.finnhubwatch.data.local.WatchlistDao
import com.example.finnhubwatch.data.local.WatchlistEntity
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface WatchlistRepository {
    val items: Flow<List<WatchlistItem>>

    suspend fun upsert(
        instrument: Instrument,
        cachedPrice: Double?,
    )

    suspend fun remove(symbol: String)
}

class RoomWatchlistRepository
    @Inject
    constructor(
        private val dao: WatchlistDao,
    ) : WatchlistRepository {
        override val items: Flow<List<WatchlistItem>> =
            dao.observeAll().map { entities ->
                entities.map { entity ->
                    WatchlistItem(
                        instrument = Instrument(entity.symbol, entity.name),
                        cachedPrice = entity.cachedPrice,
                    )
                }
            }

        override suspend fun upsert(
            instrument: Instrument,
            cachedPrice: Double?,
        ) {
            dao.upsert(
                WatchlistEntity(
                    symbol = instrument.symbol,
                    name = instrument.name,
                    cachedPrice = cachedPrice,
                ),
            )
        }

        override suspend fun remove(symbol: String) {
            dao.delete(symbol)
        }
    }
