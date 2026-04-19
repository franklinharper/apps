package com.franklinharper.concentra.browser.web

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject

class PasskeyManager(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    fun isSupported(): Boolean = sdkInt >= 23

    companion object {
        const val REGISTER_REQUEST_CODE = 1001
        const val SIGN_REQUEST_CODE = 1002

        fun handleResult(data: android.content.Intent): PasskeyResult {
            val errorBytes = data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
            if (errorBytes != null) {
                val error = AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
                return when (error.errorCode) {
                    ErrorCode.NOT_ALLOWED_ERR -> PasskeyResult.Cancelled
                    else -> PasskeyResult.Failure(
                        errorType = error.errorCode.name,
                        message = error.errorMessage ?: error.errorCode.name,
                    )
                }
            }

            val credBytes = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
                ?: return PasskeyResult.Failure("error", "No credential data in result")

            val credential = PublicKeyCredential.deserializeFromBytes(credBytes)
            return when (val response = credential.response) {
                is AuthenticatorAttestationResponse -> PasskeyResult.Success(buildRegistrationJson(credential, response))
                is AuthenticatorAssertionResponse -> PasskeyResult.Success(credential.toJson())
                else -> PasskeyResult.Failure("error", "Unexpected response type")
            }
        }

        private fun buildRegistrationJson(
            credential: PublicKeyCredential,
            response: AuthenticatorAttestationResponse,
        ): String {
            val flags = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            val clientDataJSON = android.util.Base64.encodeToString(response.clientDataJSON, flags)
            val attestationObject = android.util.Base64.encodeToString(response.attestationObject, flags)
            val rawId = android.util.Base64.encodeToString(credential.rawId, flags)
            val transports = response.transports?.toList() ?: emptyList()

            return JSONObject().apply {
                put("id", credential.id)
                put("rawId", rawId)
                put("type", credential.type)
                put("response", JSONObject().apply {
                    put("clientDataJSON", clientDataJSON)
                    put("attestationObject", attestationObject)
                    put("transports", org.json.JSONArray(transports))
                })
                put("getClientExtensionResults", JSONObject())
            }.toString()
        }
    }

    fun create(context: Context, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        return try {
            val options = PublicKeyCredentialCreationOptions(requestJson)
            val browserOptions = BrowserPublicKeyCredentialCreationOptions.Builder()
                .setPublicKeyCredentialCreationOptions(options)
                .setOrigin(Uri.parse(origin))
                .build()
            val client = Fido.getFido2PrivilegedApiClient(context)
            val pendingIntent = Tasks.await(client.getRegisterPendingIntent(browserOptions))
            PasskeyResult.LaunchIntent(pendingIntent, REGISTER_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("PasskeyManager", "create failed", e)
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }

    fun get(context: Context, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        return try {
            val json = JSONObject(requestJson)
            val allowList = json.optJSONArray("allowCredentials")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val c = arr.getJSONObject(i)
                    PublicKeyCredentialDescriptor(
                        c.optString("type", PublicKeyCredentialType.PUBLIC_KEY.toString()),
                        android.util.Base64.decode(c.getString("id"), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP),
                        emptyList(),
                    )
                }
            }
            val requestOptions = PublicKeyCredentialRequestOptions.Builder()
                .setChallenge(android.util.Base64.decode(json.getString("challenge"), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP))
                .setRpId(json.optString("rpId", null))
                .setAllowList(allowList ?: emptyList())
                .setTimeoutSeconds(json.optDouble("timeout", 60.0))
                .build()
            val browserOptions = BrowserPublicKeyCredentialRequestOptions.Builder()
                .setPublicKeyCredentialRequestOptions(requestOptions)
                .setOrigin(Uri.parse(origin))
                .build()
            val client = Fido.getFido2PrivilegedApiClient(context)
            val pendingIntent = Tasks.await(client.getSignPendingIntent(browserOptions))
            PasskeyResult.LaunchIntent(pendingIntent, SIGN_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("PasskeyManager", "get failed", e)
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }
}
