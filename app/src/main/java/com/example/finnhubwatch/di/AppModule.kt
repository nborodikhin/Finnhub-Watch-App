package com.example.finnhubwatch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Room
import com.example.finnhubwatch.data.RoomWatchlistRepository
import com.example.finnhubwatch.data.WatchlistRepository
import com.example.finnhubwatch.data.local.WatchlistDao
import com.example.finnhubwatch.data.local.WatchlistDatabase
import com.example.finnhubwatch.data.remote.DemoClock
import com.example.finnhubwatch.data.remote.DemoRandom
import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.data.settings.EncryptedApiKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): WatchlistDatabase =
        Room
            .databaseBuilder(context, WatchlistDatabase::class.java, "watchlist.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideWatchlistDao(database: WatchlistDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    @Singleton
    fun provideWatchlistRepository(dao: WatchlistDao): WatchlistRepository = RoomWatchlistRepository(dao)

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings.preferences_pb") }

    @Provides
    @Singleton
    fun provideApiKeyStore(dataStore: DataStore<Preferences>): ApiKeyStore = EncryptedApiKeyStore(dataStore)

    @Provides
    fun provideDemoRandom(): DemoRandom = DemoRandom { from, until -> Random.nextDouble(from, until) }

    @Provides
    fun provideDemoClock(): DemoClock = DemoClock { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun provideProcessLifecycleOwner(): LifecycleOwner = ProcessLifecycleOwner.get()
}
