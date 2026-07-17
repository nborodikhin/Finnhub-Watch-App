package com.example.finnhubwatch.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

interface ApiKeyStore {
    val apiKey: Flow<String>

    suspend fun saveApiKey(value: String)
}

class EncryptedApiKeyStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ApiKeyStore {
        override val apiKey: Flow<String> =
            dataStore.data
                .map { preferences -> preferences[API_KEY] }
                .map { encoded -> encoded?.let(::decrypt).orEmpty() }
                .catch { emit("") }

        override suspend fun saveApiKey(value: String) {
            dataStore.edit { preferences ->
                preferences[API_KEY] = encrypt(value)
            }
        }

        private fun encrypt(value: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val payload = cipher.iv + encrypted
            return Base64.encodeToString(payload, Base64.NO_WRAP)
        }

        private fun decrypt(value: String): String {
            val payload = Base64.decode(value, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv))
            return cipher.doFinal(encrypted).toString(StandardCharsets.UTF_8)
        }

        private fun key(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                    .apply {
                        init(
                            KeyGenParameterSpec
                                .Builder(
                                    KEY_ALIAS,
                                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .build(),
                        )
                    }.generateKey()
            }
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val KEY_ALIAS = "finnhub_watch_api_key"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_IV_LENGTH = 12
            const val GCM_TAG_LENGTH = 128
            val API_KEY = stringPreferencesKey("encrypted_api_key")
        }
    }
