package com.manito.backlogquest.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TheGamesDbCatalogService(private val apiKey: String) {
    private var genreNames: Map<Int, String>? = null

    suspend fun search(query: String): List<CatalogGame> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw GameCatalogException(
                CatalogErrorKind.MISSING_API_KEY,
                "Falta configurar la clave de TheGamesDB"
            )
        }

        val url = Uri.parse("https://api.thegamesdb.net/v1.1/Games/ByGameName").buildUpon()
            .appendQueryParameter("apikey", apiKey)
            .appendQueryParameter("name", query.trim())
            .appendQueryParameter("fields", "genres,platform")
            .appendQueryParameter("include", "boxart,platform")
            .build().toString()
        val root = requestJson(url)
        val games = root.optJSONObject("data")?.optJSONArray("games")
            ?: return@withContext emptyList()
        val include = root.optJSONObject("include")
        val platformData = include?.optJSONObject("platform")?.optJSONObject("data")
        val boxArt = include?.optJSONObject("boxart")
        val imageBaseUrl = boxArt?.optJSONObject("base_url")?.optString("medium").orEmpty()
        val imagesByGame = boxArt?.optJSONObject("data")
        val genresById = loadGenreNames()

        buildList {
            for (index in 0 until minOf(games.length(), 8)) {
                val game = games.getJSONObject(index)
                val gameId = game.getInt("id")
                val platformId = game.optInt("platform", -1)
                val platform = platformData?.optJSONObject(platformId.toString())
                    ?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                val genreIds = game.optJSONArray("genres")
                val imageUrl = imagesByGame?.optJSONArray(gameId.toString())
                    ?.let { images ->
                        (0 until images.length())
                            .asSequence()
                            .map { images.getJSONObject(it) }
                            .firstOrNull {
                                it.optString("type") == "boxart" &&
                                    it.optString("side") == "front"
                            }
                            ?.optString("filename")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { imageBaseUrl + it }
                    }

                add(CatalogGame(
                    id = gameId,
                    title = game.getString("game_title"),
                    platforms = listOfNotNull(platform),
                    genres = buildList {
                        if (genreIds != null) for (genreIndex in 0 until genreIds.length()) {
                            genresById[genreIds.getInt(genreIndex)]?.let(::add)
                        }
                    },
                    estimatedHours = null,
                    imageUrl = imageUrl,
                    provider = CatalogProvider.THE_GAMES_DB
                ))
            }
        }
    }

    private fun loadGenreNames(): Map<Int, String> {
        genreNames?.let { return it }
        val url = Uri.parse("https://api.thegamesdb.net/v1/Genres").buildUpon()
            .appendQueryParameter("apikey", apiKey)
            .build().toString()
        val genres = requestJson(url).optJSONObject("data")?.optJSONObject("genres")
            ?: return emptyMap()
        val result = buildMap {
            val keys = genres.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = genres.optJSONObject(key) ?: continue
                put(item.optInt("id", key.toIntOrNull() ?: -1), item.optString("name"))
            }
        }
        genreNames = result
        return result
    }

    private fun requestJson(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val kind = when (responseCode) {
                    401, 403 -> CatalogErrorKind.INVALID_API_KEY
                    429 -> CatalogErrorKind.RATE_LIMITED
                    408, 425, in 500..599 -> CatalogErrorKind.SERVICE_UNAVAILABLE
                    else -> CatalogErrorKind.REMOTE_ERROR
                }
                throw GameCatalogException(
                    kind,
                    "TheGamesDB respondió con código $responseCode"
                )
            }
            return connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } catch (error: IOException) {
            throw GameCatalogException(
                CatalogErrorKind.NETWORK,
                "No se pudo conectar con TheGamesDB",
                error
            )
        } finally {
            connection.disconnect()
        }
    }
}
