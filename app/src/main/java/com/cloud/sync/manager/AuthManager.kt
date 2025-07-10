package com.cloud.sync.manager

import com.cloud.sync.data.local.secure.TokenStorage
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.net.toUri
import com.cloud.sync.manager.interfaces.IAuthManager
import dagger.hilt.android.scopes.ViewModelScoped

@ViewModelScoped
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage
) : IAuthManager {
    private val authService = AuthorizationService(context)

    private val serviceConfig = AuthorizationServiceConfiguration(
        "https://cloudkeycloak.duckdns.org/realms/cloud/protocol/openid-connect/auth".toUri(),
        "https://cloudkeycloak.duckdns.org/realms/cloud/protocol/openid-connect/token".toUri()
    )

    override fun getAuthIntent(): Intent {
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            "cloud-mobile-app",
            ResponseTypeValues.CODE,
            "com.cloud.sync://redirect".toUri()
        ).setScope("openid profile").build()

        return authService.getAuthorizationRequestIntent(authRequest)
    }

    override suspend fun exchangeCodeForToken(intent: Intent) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        ex?.let { throw it }

        if (resp != null) {
            val tokenRequest = resp.createTokenExchangeRequest()
            val tokens = suspendCoroutine<TokenResponse> { continuation ->
                authService.performTokenRequest(tokenRequest) { response, exception ->
                    if (exception != null) {
                        continuation.resumeWithException(exception)
                    } else if (response != null) {
                        continuation.resume(response)
                    }
                }
            }
            tokenStorage.saveTokens(tokens.accessToken!!, tokens.refreshToken!!)
        }
    }
}