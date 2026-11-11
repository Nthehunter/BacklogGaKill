package com.manito.backlogquest.data

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class NintendoPlayedGame(
    val titleId: String,
    val name: String,
    val platform: String,
    val playedMinutes: Int,
    val imageUrl: String?
)

class NintendoLibraryException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Receives the custom-scheme redirect even if Android recreates the activity. */
object NintendoAuthCallbackBus {
    private val callbackChannel = Channel<String>(Channel.BUFFERED)
    val callbacks = callbackChannel.receiveAsFlow()

    fun publish(callbackUrl: String?) {
        if (!callbackUrl.isNullOrBlank() && callbackUrl.startsWith(REDIRECT_URI)) {
            callbackChannel.trySend(callbackUrl)
        }
    }

    private const val REDIRECT_URI = "npf5c38e31cd085304b://auth"
}

class NintendoLibraryService(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val secretStore = NintendoSecretStore(preferences)

    val isConfigured: Boolean
        get() = secretStore.readSessionToken() != null

    fun createLoginUrl(): String {
        val state = randomBase64(36)
        val verifier = randomBase64(32)
        val challenge = Base64.encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(StandardCharsets.UTF_8)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        preferences.edit()
            .putString(KEY_PENDING_STATE, state)
            .putString(KEY_PENDING_VERIFIER, verifier)
            .apply()

        return Uri.parse("$ACCOUNTS_URL/connect/1.0.0/authorize").buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "session_token_code")
            .appendQueryParameter("scope", "openid user user.mii user.email user.links[].id")
            .appendQueryParameter("session_token_code_challenge", challenge)
            .appendQueryParameter("session_token_code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("theme", "login_form")
            .build()
            .toString()
    }

    fun completeLogin(callbackUrl: String) {
        val callback = Uri.parse(callbackUrl)
        if (callback.scheme != REDIRECT_SCHEME || callback.host != "auth") {
            throw NintendoLibraryException("Nintendo devolvió un enlace de acceso no válido.")
        }
        val fragmentParameters = Uri.parse("https://callback.invalid/?${callback.fragment.orEmpty()}")
        val code = fragmentParameters.getQueryParameter("session_token_code")
            ?.takeIf(String::isNotBlank)
            ?: throw NintendoLibraryException("Nintendo no devolvió el código de acceso.")
        val returnedState = fragmentParameters.getQueryParameter("state")
        val expectedState = preferences.getString(KEY_PENDING_STATE, null)
        val verifier = preferences.getString(KEY_PENDING_VERIFIER, null)
        if (expectedState.isNullOrBlank() || verifier.isNullOrBlank() || returnedState != expectedState) {
            throw NintendoLibraryException("El acceso de Nintendo ha caducado. Inténtalo de nuevo.")
        }

        val response = postForm(
            "$ACCOUNTS_URL/connect/1.0.0/api/session_token",
            mapOf(
                "client_id" to CLIENT_ID,
                "session_token_code" to code,
                "session_token_code_verifier" to verifier
            )
        )
        val sessionToken = JSONObject(response).optString("session_token").takeIf(String::isNotBlank)
            ?: throw NintendoLibraryException("Nintendo no pudo completar el acceso.")
        secretStore.writeSessionToken(sessionToken)
        preferences.edit().remove(KEY_PENDING_STATE).remove(KEY_PENDING_VERIFIER).apply()
    }

    fun getPlayHistory(): List<NintendoPlayedGame> {
        val sessionToken = secretStore.readSessionToken()
            ?: throw NintendoLibraryException("Conecta primero tu cuenta de Nintendo.")
        val tokenResponse = try {
            postJson(
                "$ACCOUNTS_URL/connect/1.0.0/api/token",
                JSONObject()
                    .put("client_id", CLIENT_ID)
                    .put("session_token", sessionToken)
                    .put(
                        "grant_type",
                        "urn:ietf:params:oauth:grant-type:jwt-bearer-session-token"
                    )
                    .toString()
            )
        } catch (error: NintendoLibraryException) {
            if (error.message?.contains("HTTP 400") == true ||
                error.message?.contains("HTTP 401") == true
            ) {
                secretStore.clearSessionToken()
                throw NintendoLibraryException(
                    "La sesión de Nintendo ha caducado. Vuelve a conectar la cuenta.",
                    error
                )
            }
            throw error
        }
        val tokens = JSONObject(tokenResponse)
        val accessToken = tokens.optString("access_token").takeIf(String::isNotBlank)
            ?: throw NintendoLibraryException("Nintendo no devolvió un token de acceso.")
        val idToken = tokens.optString("id_token").takeIf(String::isNotBlank)

        val historyResponse = try {
            getHistory(accessToken)
        } catch (firstError: NintendoLibraryException) {
            if (idToken == null) throw firstError
            getHistory(idToken)
        }
        return parsePlayHistory(historyResponse)
    }

    fun disconnect() {
        secretStore.clearSessionToken()
        preferences.edit().remove(KEY_PENDING_STATE).remove(KEY_PENDING_VERIFIER).apply()
    }

    private fun getHistory(token: String): String = request(
        url = "$APP_URL/api/v2.0/users/me/play_histories",
        method = "GET",
        headers = mapOf(
            "Authorization" to "Bearer $token",
            "Gentry-Locale" to "es-ES",
            "User-Agent" to USER_AGENT
        )
    )

    private fun parsePlayHistory(json: String): List<NintendoPlayedGame> {
        val histories = JSONObject(json).optJSONArray("playHistories")
            ?: throw NintendoLibraryException("Nintendo devolvió un historial vacío.")
        return buildList {
            for (index in 0 until histories.length()) {
                val item = histories.optJSONObject(index) ?: continue
                val id = item.optString("titleId").trim()
                val name = item.optString("titleName").trim()
                if (id.isBlank() || name.isBlank()) continue
                val platform = normalizeNintendoPlatform(item.optString("platform")
                    .ifBlank { item.optString("deviceType") }
                    .ifBlank { "Nintendo Switch" })
                add(
                    NintendoPlayedGame(
                        titleId = id,
                        name = name,
                        platform = platform,
                        playedMinutes = item.optInt("totalPlayedMinutes", 0).coerceAtLeast(0),
                        imageUrl = item.optString("imageUrl").takeIf(String::isNotBlank)
                    )
                )
            }
        }
    }

    private fun postForm(url: String, values: Map<String, String>): String {
        val body = values.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return request(
            url = url,
            method = "POST",
            body = body,
            contentType = "application/x-www-form-urlencoded"
        )
    }

    private fun postJson(url: String, body: String): String = request(
        url = url,
        method = "POST",
        body = body,
        contentType = "application/json"
    )

    private fun request(
        url: String,
        method: String,
        body: String? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            headers.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(body) }
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw NintendoLibraryException("Nintendo devolvió el error HTTP $status.")
            }
            return response
        } catch (error: NintendoLibraryException) {
            throw error
        } catch (error: IOException) {
            throw NintendoLibraryException(
                "No se pudo conectar con Nintendo. Revisa la conexión e inténtalo de nuevo.",
                error
            )
        } catch (error: Exception) {
            throw NintendoLibraryException("Nintendo devolvió una respuesta no válida.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun randomBase64(byteCount: Int): String {
        val bytes = ByteArray(byteCount).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val CLIENT_ID = "5c38e31cd085304b"
        const val REDIRECT_SCHEME = "npf5c38e31cd085304b"
        const val REDIRECT_URI = "$REDIRECT_SCHEME://auth"
        const val ACCOUNTS_URL = "https://accounts.nintendo.com"
        const val APP_URL = "https://app-api.znej.nintendo.com"
        const val USER_AGENT = "com.nintendo.znej/3.0.3 (iOS/26.0.1)"
        const val PREFERENCES = "nintendo_connection"
        const val KEY_PENDING_STATE = "pending_state"
        const val KEY_PENDING_VERIFIER = "pending_verifier"
    }
}

fun normalizeNintendoPlatform(platform: String): String {
    val value = platform.trim()
    return when (value.uppercase()) {
        "BEE", "BEE-001", "NINTENDO SWITCH 2" -> "Nintendo Switch 2"
        "HAC", "HAC-001", "HEG", "HEG-001", "HDH", "HDH-001",
        "NINTENDO SWITCH" -> "Nintendo Switch"
        else -> value.ifBlank { "Nintendo Switch" }
    }
}

private class NintendoSecretStore(
    private val preferences: android.content.SharedPreferences
) {
    fun writeSessionToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))
        preferences.edit()
            .putString(KEY_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun readSessionToken(): String? {
        val encrypted = preferences.getString(KEY_TOKEN, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)),
                StandardCharsets.UTF_8
            )
        } catch (_: Exception) {
            clearSessionToken()
            null
        }
    }

    fun clearSessionToken() {
        preferences.edit().remove(KEY_TOKEN).remove(KEY_IV).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "backlog_quest_nintendo_session"
        const val KEY_TOKEN = "session_token"
        const val KEY_IV = "session_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
