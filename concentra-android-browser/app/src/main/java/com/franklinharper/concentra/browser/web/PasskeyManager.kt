package com.franklinharper.concentra.browser.web

import android.app.Activity
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialCancellationException

class PasskeyManager(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    fun isSupported(): Boolean = sdkInt >= 26

    suspend fun create(activity: Activity?, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        activity ?: return PasskeyResult.Failure("error", "No activity context")
        return try {
            val request = CreatePublicKeyCredentialRequest(
                requestJson = requestJson,
                clientDataHash = ByteArray(0),
                preferImmediatelyAvailableCredentials = false,
                origin = origin,
            )
            val credentialManager = CredentialManager.create(activity)
            val response = credentialManager.createCredential(activity, request)
            val json = (response as CreatePublicKeyCredentialResponse).registrationResponseJson
            PasskeyResult.Success(json)
        } catch (e: CreateCredentialCancellationException) {
            PasskeyResult.Cancelled
        } catch (e: CreateCredentialInterruptedException) {
            PasskeyResult.Failure("interrupted", e.message ?: "Interrupted")
        } catch (e: CreateCredentialProviderConfigurationException) {
            PasskeyResult.Failure("no_provider", e.message ?: "No passkey provider configured")
        } catch (e: Exception) {
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }

    suspend fun get(activity: Activity?, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        activity ?: return PasskeyResult.Failure("error", "No activity context")
        return try {
            val option = GetPublicKeyCredentialOption(requestJson = requestJson)
            val request = GetCredentialRequest(listOf(option), origin)
            val credentialManager = CredentialManager.create(activity)
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential as PublicKeyCredential
            PasskeyResult.Success(credential.authenticationResponseJson)
        } catch (e: GetCredentialCancellationException) {
            PasskeyResult.Cancelled
        } catch (e: Exception) {
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }
}
