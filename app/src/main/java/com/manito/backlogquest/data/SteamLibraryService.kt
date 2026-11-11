package com.manito.backlogquest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SteamOwnedGame(
    val appId: Long,
    val name: String,
    val playedMinutes: Int,
    val imageUrl: String,
    val allAchievements: Boolean? = null
)

class SteamLibraryException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class SteamLibraryService(
    private val apiKey: String,
    private val steamId: String
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && steamId.isNotBlank()

    suspend fun getOwnedGames(): List<SteamOwnedGame> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || steamId.isBlank()) {
            throw SteamLibraryException(
                "Configura steam.api.key y steam.id en local.properties."
            )
        }
        if (!steamId.all(Char::isDigit) || steamId.length !in 15..20) {
            throw SteamLibraryException(
                "steam.id debe contener tu SteamID64 numérico."
            )
        }

        val query = listOf(
            "key" to apiKey,
            "steamid" to steamId,
            "include_appinfo" to "true",
            "include_played_free_games" to "true",
            "format" to "json"
        ).joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}"
        }
        val connection = URI(
            "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?$query"
        ).toURL().openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED ||
                status == HttpURLConnection.HTTP_FORBIDDEN
            ) {
                throw SteamLibraryException("Steam rechazó la clave de API.")
            }
            if (status !in 200..299) {
                throw SteamLibraryException("Steam devolvió el error HTTP $status.")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            enrichAchievementCompletion(parseOwnedGames(body))
        } catch (error: SteamLibraryException) {
            throw error
        } catch (error: IOException) {
            throw SteamLibraryException(
                "No se pudo conectar con Steam. Revisa la conexión e inténtalo de nuevo.",
                error
            )
        } catch (error: Exception) {
            throw SteamLibraryException("Steam devolvió una respuesta no válida.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseOwnedGames(json: String): List<SteamOwnedGame> {
        val response = JSONObject(json).optJSONObject("response")
            ?: throw SteamLibraryException("Steam devolvió una respuesta vacía.")
        val games = response.optJSONArray("games")
        if (games == null) {
            if (response.optInt("game_count", -1) == 0) return emptyList()
            throw SteamLibraryException(
                "Steam no permite ver la biblioteca. Pon 'Detalles de juegos' en Público."
            )
        }

        return buildList {
            for (index in 0 until games.length()) {
                val item = games.optJSONObject(index) ?: continue
                val appId = item.optLong("appid", 0L)
                val name = item.optString("name").trim()
                if (appId <= 0L || name.isBlank()) continue
                add(
                    SteamOwnedGame(
                        appId = appId,
                        name = name,
                        playedMinutes = item.optInt("playtime_forever", 0)
                            .coerceAtLeast(0),
                        imageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/" +
                            "$appId/header.jpg"
                    )
                )
            }
        }
    }

    private suspend fun enrichAchievementCompletion(
        games: List<SteamOwnedGame>
    ): List<SteamOwnedGame> {
        val completion = mutableMapOf<Long, Boolean?>()
        games.filter { it.playedMinutes > 0 }
            .chunked(6)
            .forEach { chunk ->
                coroutineScope {
                    chunk.map { game ->
                        async(Dispatchers.IO) {
                            game.appId to getAchievementCompletion(game.appId)
                        }
                    }.awaitAll().forEach { (appId, completed) ->
                        completion[appId] = completed
                    }
                }
            }
        return games.map { game ->
            game.copy(allAchievements = completion[game.appId])
        }
    }

    private fun getAchievementCompletion(appId: Long): Boolean? {
        val query = listOf(
            "key" to apiKey,
            "steamid" to steamId,
            "appid" to appId.toString(),
            "l" to "spanish"
        ).joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}"
        }
        val connection = URI(
            "https://api.steampowered.com/ISteamUserStats/" +
                "GetPlayerAchievements/v1/?$query"
        ).toURL().openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BacklogQuest/0.1")
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val playerStats = JSONObject(body).optJSONObject("playerstats") ?: return null
            if (!playerStats.optBoolean("success", false)) return null
            val achievements = playerStats.optJSONArray("achievements") ?: return null
            if (achievements.length() == 0) return null
            (0 until achievements.length()).all { index ->
                achievements.optJSONObject(index)?.optInt("achieved", 0) == 1
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
