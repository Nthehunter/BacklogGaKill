package com.manito.backlogquest.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val games: Int,
    val sessions: Int
)

internal data class BackupData(
    val games: List<GameEntity>,
    val sessions: List<PlaySessionEntity>
)

internal object BackupCodec {
    private const val FORMAT_VERSION = 2

    fun encode(games: List<GameEntity>, sessions: List<PlaySessionEntity>): String =
        JSONObject()
            .put("format", "backlog-quest-backup")
            .put("version", FORMAT_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("games", JSONArray().apply {
                games.forEach { game ->
                    put(JSONObject()
                        .put("id", game.id)
                        .put("title", game.title)
                        .put("platform", game.platform)
                        .put("genre", game.genre)
                        .put("status", game.status.name)
                        .put("priority", game.priority)
                        .put("estimatedHours", game.estimatedHours ?: JSONObject.NULL)
                        .put("playedMinutes", game.playedMinutes)
                        .put("progressPercent", game.progressPercent)
                        .put("notes", game.notes)
                        .put("imageUrl", game.imageUrl ?: JSONObject.NULL)
                        .put("platinum", game.platinum)
                        .put("allAchievements", game.allAchievements)
                        .put("addedAt", game.addedAt)
                        .put("updatedAt", game.updatedAt)
                        .put("completedAt", game.completedAt ?: JSONObject.NULL)
                    )
                }
            })
            .put("sessions", JSONArray().apply {
                sessions.forEach { session ->
                    put(JSONObject()
                        .put("id", session.id)
                        .put("gameId", session.gameId)
                        .put("durationMinutes", session.durationMinutes)
                        .put("progressAfter", session.progressAfter)
                        .put("notes", session.notes)
                        .put("playedAt", session.playedAt)
                    )
                }
            })
            .toString(2)

    fun decode(json: String): BackupData {
        val root = JSONObject(json)
        require(root.optString("format") == "backlog-quest-backup") {
            "El archivo no es una copia de Backlog Quest"
        }
        require(root.optInt("version", -1) in 1..FORMAT_VERSION) {
            "La versión de la copia no es compatible"
        }

        val gamesJson = root.getJSONArray("games")
        val games = buildList {
            for (index in 0 until gamesJson.length()) {
                val item = gamesJson.getJSONObject(index)
                add(GameEntity(
                    id = item.getLong("id"),
                    title = item.getString("title"),
                    platform = item.getString("platform"),
                    genre = item.optString("genre"),
                    status = GameStatus.valueOf(item.getString("status")),
                    priority = item.getInt("priority"),
                    estimatedHours = item.nullableDouble("estimatedHours"),
                    playedMinutes = item.getInt("playedMinutes"),
                    progressPercent = item.getInt("progressPercent"),
                    notes = item.optString("notes"),
                    imageUrl = item.nullableString("imageUrl"),
                    platinum = item.optBoolean("platinum", false),
                    allAchievements = item.optBoolean("allAchievements", false),
                    addedAt = item.getLong("addedAt"),
                    updatedAt = item.getLong("updatedAt"),
                    completedAt = item.nullableLong("completedAt")
                ))
            }
        }

        val sessionsJson = root.getJSONArray("sessions")
        val sessions = buildList {
            for (index in 0 until sessionsJson.length()) {
                val item = sessionsJson.getJSONObject(index)
                add(PlaySessionEntity(
                    id = item.getLong("id"),
                    gameId = item.getLong("gameId"),
                    durationMinutes = item.getInt("durationMinutes"),
                    progressAfter = item.getInt("progressAfter"),
                    notes = item.optString("notes"),
                    playedAt = item.getLong("playedAt")
                ))
            }
        }

        validate(games, sessions)
        return BackupData(games, sessions)
    }

    private fun validate(games: List<GameEntity>, sessions: List<PlaySessionEntity>) {
        require(games.all { it.id > 0 && it.title.isNotBlank() && it.platform.isNotBlank() }) {
            "La copia contiene juegos no válidos"
        }
        require(games.map { it.id }.distinct().size == games.size) {
            "La copia contiene juegos duplicados"
        }
        require(games.all {
            it.priority in 1..5 && it.progressPercent in 0..100 && it.playedMinutes >= 0
        }) { "La copia contiene valores de progreso no válidos" }

        val gameIds = games.mapTo(mutableSetOf()) { it.id }
        require(sessions.map { it.id }.distinct().size == sessions.size) {
            "La copia contiene sesiones duplicadas"
        }
        require(sessions.all {
            it.id > 0 && it.gameId in gameIds && it.durationMinutes > 0 &&
                it.progressAfter in 0..100
        }) { "La copia contiene sesiones no válidas" }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (isNull(key)) null else getDouble(key)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key)) null else getLong(key)
}
