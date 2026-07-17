package com.example.finnhubwatch.data.remote

import com.example.finnhubwatch.data.settings.ApiKeyStore
import com.example.finnhubwatch.domain.model.FinancialException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinnhubFinancialBackendTest {
    @Test
    fun quoteForbiddenIsSymbolLevelAndPreservesStatus() =
        runTest {
            val exception =
                runCatching { backend(403).quote("APPL.TO") }.exceptionOrNull()

            assertTrue(exception is FinancialException.Api)
            assertEquals("403", (exception as FinancialException.Api).code)
            assertFalse(exception.retryable)
        }

    @Test
    fun searchForbiddenIsAuthorization() =
        runTest {
            val exception =
                runCatching { backend(403).search("aapl") }.exceptionOrNull()

            assertTrue(exception is FinancialException.Authorization)
            assertEquals("403", (exception as FinancialException.Authorization).code)
            assertTrue(exception.unauthorized)
        }

    @Test
    fun quoteWithoutCurrentPriceIsSuccessfulButUnavailable() =
        runTest {
            val quote = backend(200, "{\"c\":0,\"pc\":100.0}").quote("APPL.TO")

            assertEquals(null, quote?.price)
            assertEquals(100.0, quote?.previousClose)
        }

    @Test
    fun rateLimitAndServerErrorsAreRetryable() {
        assertTrue(FinancialException.Api("408", "timeout").retryable)
        assertTrue(FinancialException.Api("429", "rate limited").retryable)
        assertTrue(FinancialException.Api("503", "server").retryable)
        assertTrue(FinancialException.Network("offline").retryable)
        assertFalse(FinancialException.Api("404", "missing").retryable)
    }

    private fun backend(
        code: Int,
        body: String = "{}",
    ): FinnhubFinancialBackend =
        FinnhubFinancialBackend(
            client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(StubResponseInterceptor(code, body))
                    .build(),
            json = Json,
            apiKeyStore = FakeApiKeyStore,
        )
}

private class StubResponseInterceptor(
    private val code: Int,
    private val body: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        Response
            .Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("stub")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
}

private object FakeApiKeyStore : ApiKeyStore {
    override val apiKey: Flow<String> = flowOf("test-key")

    override suspend fun saveApiKey(value: String) = Unit
}
