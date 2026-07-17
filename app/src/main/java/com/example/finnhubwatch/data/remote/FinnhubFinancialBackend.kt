package com.example.finnhubwatch.data.remote

import com.example.finnhubwatch.domain.model.BackendEvent
import com.example.finnhubwatch.domain.model.BackendMode
import com.example.finnhubwatch.domain.model.FinancialException
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import javax.inject.Inject

class FinnhubFinancialBackend
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val json: Json,
        private val apiKeyStore: com.example.finnhubwatch.data.settings.ApiKeyStore,
    ) : FinancialBackend {
        override val mode: BackendMode = BackendMode.REAL

        override suspend fun search(query: String): List<Instrument> {
            val response =
                execute(
                    endpoint("search").newBuilder().addQueryParameter("q", query).build(),
                )
            return json.decodeFromString<SearchResponse>(response).result.map { item ->
                Instrument(item.displaySymbol ?: item.symbol, item.description)
            }
        }

        override suspend fun quote(symbol: String): Quote? {
            val response =
                execute(
                    endpoint("quote").newBuilder().addQueryParameter("symbol", symbol).build(),
                )
            val payload = json.decodeFromString<QuoteResponse>(response)
            return Quote(payload.current?.takeIf { it > 0.0 }, payload.previousClose, payload.timestamp)
        }

        override fun stream(symbols: Set<String>): Flow<BackendEvent> =
            callbackFlow {
                val key = apiKeyStore.apiKey.first()
                val url = "wss://ws.finnhub.io?token=$key".toHttpUrl()
                val request = Request.Builder().url(url).build()
                val socket =
                    client.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(
                                webSocket: WebSocket,
                                response: okhttp3.Response,
                            ) {
                                trySend(BackendEvent.Connected)
                                symbols.forEach { symbol ->
                                    webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"$symbol\"}")
                                }
                            }

                            override fun onMessage(
                                webSocket: WebSocket,
                                text: String,
                            ) {
                                try {
                                    val root = json.parseToJsonElement(text).jsonObject
                                    when (root["type"]?.jsonPrimitive?.content) {
                                        "trade" -> {
                                            root["data"]?.jsonArray?.forEach { element ->
                                                val trade = json.decodeFromJsonElement<TradePayload>(element)
                                                if (trade.price != null && trade.symbol != null) {
                                                    trySend(
                                                        BackendEvent.Trade(
                                                            trade.symbol,
                                                            trade.price,
                                                            trade.timestamp ?: System.currentTimeMillis(),
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                        "error" ->
                                            trySend(
                                                BackendEvent.Failed(
                                                    FinancialException.Authorization(root["msg"]?.jsonPrimitive?.content.orEmpty()),
                                                ),
                                            )
                                    }
                                } catch (error: Exception) {
                                    trySend(BackendEvent.Failed(FinancialException.Api("malformed", "Unable to parse stream message")))
                                }
                            }

                            override fun onFailure(
                                webSocket: WebSocket,
                                t: Throwable,
                                response: okhttp3.Response?,
                            ) {
                                val exception =
                                    if (response?.code == 401 || response?.code == 403) {
                                        FinancialException.Authorization()
                                    } else {
                                        FinancialException.Network(t.message ?: "WebSocket connection failed", t)
                                    }
                                trySend(BackendEvent.Failed(exception))
                                close()
                            }

                            override fun onClosed(
                                webSocket: WebSocket,
                                code: Int,
                                reason: String,
                            ) {
                                if (code != 1000) {
                                    trySend(BackendEvent.Failed(FinancialException.Network("WebSocket closed: $code")))
                                }
                                close()
                            }
                        },
                    )
                awaitClose { socket.close(1000, "cancelled") }
            }

        private suspend fun execute(url: okhttp3.HttpUrl): String {
            val key = apiKeyStore.apiKey.first()
            val request = Request.Builder().url(url.newBuilder().addQueryParameter("token", key).build()).build()
            return withContext(Dispatchers.IO) {
                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            if (response.code == 401 || response.code == 403) throw FinancialException.Authorization()
                            throw FinancialException.Api(response.code.toString(), "Finnhub request failed")
                        }
                        response.body?.string() ?: throw FinancialException.Network("Empty Finnhub response")
                    }
                } catch (exception: FinancialException) {
                    throw exception
                } catch (exception: IOException) {
                    throw FinancialException.Network(exception.message ?: "Network request failed", exception)
                }
            }
        }

        private fun endpoint(name: String): okhttp3.HttpUrl = "https://finnhub.io/api/v1/$name".toHttpUrl()

        @Serializable
        private data class SearchResponse(
            val result: List<SearchPayload> = emptyList(),
        )

        @Serializable
        private data class SearchPayload(
            val symbol: String,
            val description: String,
            val displaySymbol: String? = null,
        )

        @Serializable
        private data class QuoteResponse(
            @SerialName("c") val current: Double? = null,
            @SerialName("pc") val previousClose: Double? = null,
            @SerialName("t") val timestamp: Long? = null,
        )

        @Serializable
        private data class TradePayload(
            @SerialName("s") val symbol: String? = null,
            @SerialName("p") val price: Double? = null,
            @SerialName("t") val timestamp: Long? = null,
        )
    }
