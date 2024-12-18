package com.jk.networkmodule.network

import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.jk.networkmodule.AuthManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

private const val TIME_OUT = 60_000


val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
val apolloClient = ApolloClient.Builder()
    .serverUrl("https://api.github.com/graphql")
    .okHttpClient(
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    )
    .build()


class AuthorizationInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .apply {
                val authManager = AuthManager
                val tokens = authManager.getAccessToken().toString()
                addHeader("Authorization", "Bearer $tokens")
            }
            .build()
        // Log request details

        return chain.proceed(request)

    }

}

private fun logRequest(request: okhttp3.Request) {
    Log.d("AuthorizationInterceptor", "Request: ${request.method} ${request.url}")
    // Log.d("AuthorizationInterceptor", "Headers: ${request.headers}")
    Log.d("AuthorizationInterceptor", "Body: ${request.body}")
}

private fun logResponse(response: Response) {
    //  Log.d("AuthorizationInterceptor", "Response: ${response.code} ${response.message}")
    // Log.d("AuthorizationInterceptor", "Headers: ${response.headers}")
    Log.d("AuthorizationInterceptor", "Body: ${response.body?.string()}")
}

val ktorHttpClient = HttpClient(Android) {
    followRedirects = false

    install(Auth) {
        bearer {
            // Load tokens function retrieves the access token for each request
            val authManager = AuthManager
            val tokens = authManager.getAccessToken().toString()
            loadTokens {
                BearerTokens(tokens, "")
            }
        }
    }
    defaultRequest {
        url("https://api.github.com/")
    }
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        )

        engine {
            connectTimeout = TIME_OUT
            socketTimeout = TIME_OUT
        }
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Log.v("Logger Ktor =>", message)
            }
        }
        level = LogLevel.ALL
    }

    install(ResponseObserver) {
        onResponse { response ->
            Log.d("HTTP status:", "${response.status.value}")
        }
    }

    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
}
