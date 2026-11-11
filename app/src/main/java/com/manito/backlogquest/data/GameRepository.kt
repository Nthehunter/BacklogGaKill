package com.manito.backlogquest.data

import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.max

data class SteamImportSummary(
    val found: Int,
    val added: Int,
    val alreadyPresent: Int,
    val perfectGames: Int
)

data class PlayStationImportSummary(
    val found: Int,
    val added: Int,
    val alreadyPresent: Int,
    val platinumGames: Int
)

data class NintendoImportSummary(
    val found: Int,
    val added: Int,
    val alreadyPresent: Int,
    val totalPlayedMinutes: Int
)

class GameRepository(private val dao: GameDao) {
    val games: Flow<List<GameEntity>> = dao.observeGames()

    fun observeGame(gameId: Long): Flow<GameEntity?> = dao.observeGame(gameId)

    fun observeSessions(gameId: Long): Flow<List<PlaySessionEntity>> =
        dao.observeSessions(gameId)

    suspend fun saveGame(game: GameEntity): Long {
        val now = System.currentTimeMillis()
        val normalized = game.copy(
            title = game.title.trim(),
            platform = game.platform.trim(),
            genre = game.genre.trim(),
            progressPercent = game.progressPercent.coerceIn(0, 100),
            priority = game.priority.coerceIn(1, 5),
            updatedAt = now,
            completedAt = when {
                game.status == GameStatus.COMPLETED && game.completedAt == null -> now
                game.status != GameStatus.COMPLETED -> null
                else -> game.completedAt
            }
        )

        return if (normalized.id == 0L) {
            dao.insertGame(normalized)
        } else {
            dao.updateGame(normalized)
            normalized.id
        }
    }

    suspend fun deleteGame(game: GameEntity) = dao.deleteGame(game)

    suspend fun createBackup(): String = BackupCodec.encode(
        games = dao.getAllGames(),
        sessions = dao.getAllSessions()
    )

    suspend fun restoreBackup(json: String): BackupSummary {
        val backup = BackupCodec.decode(json)
        dao.replaceAllData(backup.games, backup.sessions)
        return BackupSummary(backup.games.size, backup.sessions.size)
    }

    suspend fun importSteamGames(games: List<SteamOwnedGame>): SteamImportSummary {
        val existingGames = dao.getAllGames()
        val existingByKey = existingGames.associateByTo(mutableMapOf()) { game ->
            importKey(game.title, game.platform)
        }
        val uniqueGames = games.distinctBy { it.appId }
        val now = System.currentTimeMillis()
        val newGames = uniqueGames.mapNotNull { steamGame ->
            val key = importKey(steamGame.name, STEAM_PLATFORM)
            val existing = existingByKey[key]
            if (existing != null) {
                val refreshed = existing.copy(
                    playedMinutes = max(existing.playedMinutes, steamGame.playedMinutes),
                    imageUrl = existing.imageUrl ?: steamGame.imageUrl,
                    allAchievements = steamGame.allAchievements
                        ?: existing.allAchievements,
                    updatedAt = if (
                        steamGame.allAchievements != null &&
                        steamGame.allAchievements != existing.allAchievements
                    ) now else existing.updatedAt
                )
                if (refreshed != existing) dao.updateGame(refreshed)
                return@mapNotNull null
            }
            GameEntity(
                title = steamGame.name,
                platform = STEAM_PLATFORM,
                status = GameStatus.BACKLOG,
                playedMinutes = steamGame.playedMinutes,
                imageUrl = steamGame.imageUrl,
                allAchievements = steamGame.allAchievements == true,
                addedAt = now,
                updatedAt = now
            )
        }
        if (newGames.isNotEmpty()) dao.insertGames(newGames)

        return SteamImportSummary(
            found = uniqueGames.size,
            added = newGames.size,
            alreadyPresent = uniqueGames.size - newGames.size,
            perfectGames = uniqueGames.count { it.allAchievements == true }
        )
    }

    suspend fun importPlayStationGames(
        games: List<PlayStationTrophyGame>
    ): PlayStationImportSummary {
        val existingKeys = dao.getAllGames()
            .mapTo(mutableSetOf()) { game -> importKey(game.title, game.platform) }
        val uniqueGames = games.distinctBy { it.communicationId }
        val now = System.currentTimeMillis()
        val newGames = uniqueGames.mapNotNull { psnGame ->
            val key = importKey(psnGame.name, psnGame.platform)
            if (!existingKeys.add(key)) return@mapNotNull null
            val completed = psnGame.progressPercent >= 100
            GameEntity(
                title = psnGame.name,
                platform = psnGame.platform,
                status = if (completed) GameStatus.COMPLETED else GameStatus.BACKLOG,
                progressPercent = psnGame.progressPercent,
                imageUrl = psnGame.imageUrl,
                platinum = psnGame.hasPlatinum,
                notes = "Progreso importado de los trofeos de PlayStation Network.",
                addedAt = now,
                updatedAt = now,
                completedAt = if (completed) now else null
            )
        }
        if (newGames.isNotEmpty()) dao.insertGames(newGames)

        return PlayStationImportSummary(
            found = uniqueGames.size,
            added = newGames.size,
            alreadyPresent = uniqueGames.size - newGames.size,
            platinumGames = uniqueGames.count { it.hasPlatinum }
        )
    }

    suspend fun importNintendoGames(
        games: List<NintendoPlayedGame>
    ): NintendoImportSummary {
        normalizeNintendoPlatformCodes()
        val existingByKey = dao.getAllGames().associateByTo(mutableMapOf()) { game ->
            importKey(game.title, game.platform)
        }
        val uniqueGames = games.distinctBy { "${it.titleId}|${it.platform}" }
        val now = System.currentTimeMillis()
        val newGames = uniqueGames.mapNotNull { nintendoGame ->
            val platform = normalizeNintendoPlatform(nintendoGame.platform)
            val key = importKey(nintendoGame.name, platform)
            val existing = existingByKey[key]
            if (existing != null) {
                val refreshed = existing.copy(
                    playedMinutes = max(existing.playedMinutes, nintendoGame.playedMinutes),
                    imageUrl = existing.imageUrl ?: nintendoGame.imageUrl,
                    updatedAt = if (
                        nintendoGame.playedMinutes > existing.playedMinutes ||
                        (existing.imageUrl == null && nintendoGame.imageUrl != null)
                    ) now else existing.updatedAt
                )
                if (refreshed != existing) dao.updateGame(refreshed)
                return@mapNotNull null
            }
            GameEntity(
                title = nintendoGame.name,
                platform = platform,
                status = GameStatus.BACKLOG,
                playedMinutes = nintendoGame.playedMinutes,
                imageUrl = nintendoGame.imageUrl,
                notes = "Horas importadas del historial de juego de Nintendo.",
                addedAt = now,
                updatedAt = now
            )
        }
        if (newGames.isNotEmpty()) dao.insertGames(newGames)

        return NintendoImportSummary(
            found = uniqueGames.size,
            added = newGames.size,
            alreadyPresent = uniqueGames.size - newGames.size,
            totalPlayedMinutes = uniqueGames.sumOf { it.playedMinutes }
        )
    }

    suspend fun normalizeNintendoPlatformCodes() {
        val now = System.currentTimeMillis()
        dao.getAllGames().forEach { game ->
            val normalized = when (game.platform.trim().uppercase(Locale.ROOT)) {
                "BEE", "BEE-001" -> "Nintendo Switch 2"
                "HAC", "HAC-001", "HEG", "HEG-001", "HDH", "HDH-001" ->
                    NINTENDO_SWITCH_PLATFORM
                else -> game.platform
            }
            if (normalized != game.platform) {
                dao.updateGame(game.copy(platform = normalized, updatedAt = now))
            }
        }
    }

    suspend fun registerSession(
        gameId: Long,
        durationMinutes: Int,
        progressAfter: Int,
        notes: String
    ) {
        val game = dao.getGame(gameId) ?: return
        val safeDuration = max(durationMinutes, 1)
        val safeProgress = progressAfter.coerceIn(game.progressPercent, 100)
        val ongoing = game.status == GameStatus.ONGOING
        val completed = !ongoing && safeProgress >= 100
        val now = System.currentTimeMillis()

        dao.insertSession(
            PlaySessionEntity(
                gameId = gameId,
                durationMinutes = safeDuration,
                progressAfter = safeProgress,
                notes = notes.trim(),
                playedAt = now
            )
        )

        dao.updateGame(
            game.copy(
                playedMinutes = game.playedMinutes + safeDuration,
                progressPercent = safeProgress,
                status = when {
                    ongoing -> GameStatus.ONGOING
                    completed -> GameStatus.COMPLETED
                    else -> GameStatus.PLAYING
                },
                updatedAt = now,
                completedAt = if (completed) now else null
            )
        )
    }

    private fun importKey(title: String, platform: String): String =
        "${title.trim().lowercase(Locale.ROOT)}|${platform.trim().lowercase(Locale.ROOT)}"

    private companion object {
        const val STEAM_PLATFORM = "PC (Steam)"
        const val NINTENDO_SWITCH_PLATFORM = "Nintendo Switch"
    }
}

object GameRecommender {
    fun recommend(games: List<GameEntity>): GameEntity? =
        games
            .asSequence()
            .filter {
                it.status != GameStatus.COMPLETED &&
                    it.status != GameStatus.ABANDONED &&
                    it.status != GameStatus.WISHLIST
            }
            .maxByOrNull(::score)

    private fun score(game: GameEntity): Double {
        val daysInBacklog = (System.currentTimeMillis() - game.addedAt)
            .coerceAtLeast(0L) / 86_400_000.0
        val remainingHours = game.estimatedHours?.let { estimate ->
            estimate * (100 - game.progressPercent) / 100.0
        } ?: 20.0

        return game.priority * 20.0 +
            (if (game.status == GameStatus.PLAYING) 35.0 else 0.0) +
            daysInBacklog.coerceAtMost(60.0) / 2.0 +
            (25.0 - remainingHours.coerceAtMost(25.0))
    }
}
