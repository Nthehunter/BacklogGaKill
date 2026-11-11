package com.manito.backlogquest.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class CatalogProvider(val label: String) {
    RAWG("RAWG"),
    THE_GAMES_DB("TheGamesDB")
}

data class CatalogGame(
    val id: Int,
    val title: String,
    val platforms: List<String>,
    val genres: List<String>,
    val estimatedHours: Double?,
    val imageUrl: String?,
    val provider: CatalogProvider
)

enum class CatalogErrorKind {
    MISSING_API_KEY,
    INVALID_API_KEY,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    NETWORK,
    REMOTE_ERROR
}

class GameCatalogException(
    val kind: CatalogErrorKind,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class GameCatalogService(private val apiKey: String) {
    suspend fun search(query: String): List<CatalogGame> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw GameCatalogException(
                CatalogErrorKind.MISSING_API_KEY,
                "Falta configurar la clave de RAWG"
            )
        }
        val url = Uri.parse("https://api.rawg.io/api/games").buildUpon()
            .appendQueryParameter("key", apiKey)
            .appendQueryParameter("search", query.trim())
            .appendQueryParameter("search_precise", "true")
            .appendQueryParameter("page_size", "8")
            .build().toString()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")
            if (connection.responseCode !in 200..299) {
                throw responseError(connection.responseCode)
            }
            val root = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val results = root.getJSONArray("results")
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.getJSONObject(index)
                    val platforms = item.optJSONArray("platforms")
                    val genres = item.optJSONArray("genres")
                    add(CatalogGame(
                        id = item.getInt("id"),
                        title = item.getString("name"),
                        platforms = buildList {
                            if (platforms != null) for (i in 0 until platforms.length()) {
                                add(platforms.getJSONObject(i).getJSONObject("platform").getString("name"))
                            }
                        },
                        genres = buildList {
                            if (genres != null) for (i in 0 until genres.length()) {
                                add(genres.getJSONObject(i).getString("name"))
                            }
                        },
                        estimatedHours = item.optDouble("playtime")
                            .takeIf { !it.isNaN() && it > 0 },
                        imageUrl = item.optString("background_image")
                            .takeIf { it.startsWith("http") },
                        provider = CatalogProvider.RAWG
                    ))
                }
            }
        } catch (error: IOException) {
            throw GameCatalogException(
                CatalogErrorKind.NETWORK,
                "No se pudo conectar con RAWG",
                error
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun getEstimatedHours(gameId: Int): Double? = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Falta configurar la clave de RAWG" }
        val url = Uri.parse("https://api.rawg.io/api/games/$gameId").buildUpon()
            .appendQueryParameter("key", apiKey)
            .build().toString()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return@withContext null
            val game = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            game.optDouble("playtime").takeIf { !it.isNaN() && it > 0 }
        } finally {
            connection.disconnect()
        }
    }

    private fun responseError(responseCode: Int): GameCatalogException {
        val kind = when (responseCode) {
            401, 403 -> CatalogErrorKind.INVALID_API_KEY
            429 -> CatalogErrorKind.RATE_LIMITED
            408, 425, in 500..599 -> CatalogErrorKind.SERVICE_UNAVAILABLE
            else -> CatalogErrorKind.REMOTE_ERROR
        }
        return GameCatalogException(kind, "RAWG respondió con código $responseCode")
    }
}
