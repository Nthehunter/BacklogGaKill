package com.manito.backlogquest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PlayStationTrophyGame(
    val communicationId: String,
    val name: String,
    val platform: String,
    val progressPercent: Int,
    val imageUrl: String,
    val hasPlatinum: Boolean
)

class PlayStationLibraryException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class PlayStationLibraryService(private val npsso: String) {
    val isConfigured: Boolean
        get() = npsso.isNotBlank()

    suspend fun getTrophyGames(): List<PlayStationTrophyGame> = withContext(Dispatchers.IO) {
        if (npsso.isBlank()) {
            throw PlayStationLibraryException(
                "Configura psn.npsso en local.properties."
            )
        }

        try {
            val accessCode = exchangeNpssoForAccessCode(npsso.trim())
            val accessToken = exchangeAccessCodeForToken(accessCode)
            fetchAllTrophyGames(accessToken)
        } catch (error: PlayStationLibraryException) {
            throw error
        } catch (error: IOException) {
            throw PlayStationLibraryException(
                "No se pudo conectar con PlayStation Network. Revisa la conexión.",
                error
            )
        } catch (error: Exception) {
            throw PlayStationLibraryException(
                "PlayStation Network devolvió una respuesta no válida.",
                error
            )
        }
    }

    private fun exchangeNpssoForAccessCode(token: String): String {
        val query = listOf(
            "access_type" to "offline",
            "client_id" to CLIENT_ID,
            "redirect_uri" to REDIRECT_URI,
            "response_type" to "code",
            "scope" to "psn:mobile.v2.core psn:clientapp"
        ).joinToString("&") { (key, value) ->
            "$key=${encode(value)}"
        }
        val connection = URI("$AUTH_BASE_URL/authorize?$query")
            .toURL()
            .openConnection() as HttpURLConnection

        return try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Cookie", "npsso=$token")
            connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")

            val status = connection.responseCode
            val location = connection.getHeaderField("Location").orEmpty()
            if (status !in 300..399 || location.isBlank()) {
                throw PlayStationLibraryException(
                    "El NPSSO no es válido o ha caducado. Genera uno nuevo desde PlayStation."
                )
            }
            extractQueryParameter(location, "code")
                ?: throw PlayStationLibraryException(
                    "Sony no devolvió el código de acceso esperado."
                )
        } finally {
            connection.disconnect()
        }
    }

    private fun exchangeAccessCodeForToken(accessCode: String): String {
        val body = listOf(
            "code" to accessCode,
            "redirect_uri" to REDIRECT_URI,
            "grant_type" to "authorization_code",
            "token_format" to "jwt"
        ).joinToString("&") { (key, value) -> "$key=${encode(value)}" }
        val connection = URI("$AUTH_BASE_URL/token")
            .toURL()
            .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            connection.setRequestProperty("Authorization", "Basic $MOBILE_CLIENT_AUTH")
            connection.outputStream.bufferedWriter().use { it.write(body) }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED ||
                status == HttpURLConnection.HTTP_FORBIDDEN
            ) {
                throw PlayStationLibraryException(
                    "PlayStation rechazó la sesión. Genera un NPSSO nuevo."
                )
            }
            if (status !in 200..299) {
                throw PlayStationLibraryException(
                    "PlayStation devolvió el error HTTP $status al iniciar sesión."
                )
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(json).optString("access_token").takeIf { it.isNotBlank() }
                ?: throw PlayStationLibraryException(
                    "Sony no devolvió un token de acceso."
                )
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchAllTrophyGames(accessToken: String): List<PlayStationTrophyGame> {
        val result = mutableListOf<PlayStationTrophyGame>()
        var offset = 0

        while (true) {
            val connection = URI(
                "$TROPHY_BASE_URL/v1/users/me/trophyTitles?limit=$PAGE_SIZE&offset=$offset"
            ).toURL().openConnection() as HttpURLConnection
            val page = try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15_000
                connection.readTimeout = 25_000
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Accept-Language", "es-ES")
                connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_UNAUTHORIZED ||
                    status == HttpURLConnection.HTTP_FORBIDDEN
                ) {
                    throw PlayStationLibraryException(
                        "PlayStation rechazó el acceso a los trofeos."
                    )
                }
                if (status == 429) {
                    throw PlayStationLibraryException(
                        "PlayStation ha limitado temporalmente las consultas. Inténtalo más tarde."
                    )
                }
                if (status !in 200..299) {
                    throw PlayStationLibraryException(
                        "PlayStation devolvió el error HTTP $status al leer los juegos."
                    )
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } finally {
                connection.disconnect()
            }

            val titles = page.optJSONArray("trophyTitles")
                ?: throw PlayStationLibraryException(
                    "La lista de trofeos de PlayStation está vacía o no es visible."
                )
            for (index in 0 until titles.length()) {
                val item = titles.optJSONObject(index) ?: continue
                val communicationId = item.optString("npCommunicationId").trim()
                val name = item.optString("trophyTitleName").trim()
                if (communicationId.isBlank() || name.isBlank()) continue
                val earned = item.optJSONObject("earnedTrophies")
                result += PlayStationTrophyGame(
                    communicationId = communicationId,
                    name = name,
                    platform = normalizePlatform(item.optString("trophyTitlePlatform")),
                    progressPercent = item.optInt("progress", 0).coerceIn(0, 100),
                    imageUrl = item.optString("trophyTitleIconUrl").trim(),
                    hasPlatinum = earned?.optInt("platinum", 0)?.let { it > 0 } == true
                )
            }

            val nextOffset = if (page.has("nextOffset")) page.optInt("nextOffset", -1) else -1
            if (nextOffset < 0 || nextOffset <= offset || titles.length() == 0) break
            offset = nextOffset
        }

        return result.distinctBy { it.communicationId }
    }

    private fun normalizePlatform(value: String): String {
        val platforms = value.split(',')
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .map { platform ->
                when (platform) {
                    "PS5" -> "PlayStation 5"
                    "PS4" -> "PlayStation 4"
                    "PS3" -> "PlayStation 3"
                    "PSVITA", "VITA" -> "PlayStation Vita"
                    "PC" -> "PC (PlayStation)"
                    else -> platform
                }
            }
            .distinct()
        return platforms.joinToString(" / ").ifBlank { "PlayStation" }
    }

    private fun extractQueryParameter(url: String, name: String): String? {
        val query = url.substringAfter('?', "").substringBefore('#')
        return query.split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', "")
                val value = part.substringAfter('=', "")
                if (key == name && value.isNotBlank()) {
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                } else {
                    null
                }
            }
            .firstOrNull()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val AUTH_BASE_URL = "https://ca.account.sony.com/api/authz/v3/oauth"
        const val TROPHY_BASE_URL = "https://m.np.playstation.com/api/trophy"
        const val CLIENT_ID = "09515159-7237-4370-9b40-3806e67c0891"
        const val REDIRECT_URI = "com.scee.psxandroid.scecompcall://redirect"
        const val MOBILE_CLIENT_AUTH =
            "MDk1MTUxNTktNzIzNy00MzcwLTliNDAtMzgwNmU2N2MwODkxOnVjUGprYTV0bnRCMktxc1A="
        const val PAGE_SIZE = 800
    }
}
