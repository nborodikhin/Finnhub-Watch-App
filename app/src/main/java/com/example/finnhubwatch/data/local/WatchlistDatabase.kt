package com.example.finnhubwatch.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "watchlist_items",
    primaryKeys = ["symbol"],
)
data class WatchlistEntity(
    val symbol: String,
    val name: String,
    val cachedPrice: Double?,
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY symbol ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist_items WHERE symbol = :symbol")
    suspend fun delete(symbol: String)
}

@Database(
    entities = [WatchlistEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WatchlistDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
