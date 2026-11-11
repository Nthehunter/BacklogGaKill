package com.manito.backlogquest.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manito.backlogquest.BacklogQuestApplication
import com.manito.backlogquest.BuildConfig
import com.manito.backlogquest.data.CatalogErrorKind
import com.manito.backlogquest.data.CatalogGame
import com.manito.backlogquest.data.CatalogProvider
import com.manito.backlogquest.data.BackupSummary
import com.manito.backlogquest.data.GameCatalogService
import com.manito.backlogquest.data.GameCatalogException
import com.manito.backlogquest.data.GameEntity
import com.manito.backlogquest.data.GameRepository
import com.manito.backlogquest.data.GameStatus
import com.manito.backlogquest.data.LocalProfile
import com.manito.backlogquest.data.LocalProfileRepository
import com.manito.backlogquest.data.NintendoAuthCallbackBus
import com.manito.backlogquest.data.NintendoImportSummary
import com.manito.backlogquest.data.NintendoLibraryService
import com.manito.backlogquest.data.PlaySessionEntity
import com.manito.backlogquest.data.PlayStationImportSummary
import com.manito.backlogquest.data.PlayStationLibraryService
import com.manito.backlogquest.data.TheGamesDbCatalogService
import com.manito.backlogquest.data.SteamImportSummary
import com.manito.backlogquest.data.SteamLibraryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class CatalogSearchState(
    val results: List<CatalogGame> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

data class NintendoConnectionState(
    val connected: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val failed: Boolean = false
)

class AppViewModel(
    application: Application,
    private val repository: GameRepository,
    private val profileRepository: LocalProfileRepository,
    private val nintendoLibraryService: NintendoLibraryService
) : AndroidViewModel(application) {
    private val catalogService = GameCatalogService(BuildConfig.RAWG_API_KEY)
    private val secondaryCatalogService =
        TheGamesDbCatalogService(BuildConfig.THEGAMESDB_API_KEY)
    private val steamLibraryService =
        SteamLibraryService(BuildConfig.STEAM_API_KEY, BuildConfig.STEAM_ID)
    private val playStationLibraryService =
        PlayStationLibraryService(BuildConfig.PSN_NPSSO)
    val steamConfigured: Boolean
        get() = steamLibraryService.isConfigured
    val playStationConfigured: Boolean
        get() = playStationLibraryService.isConfigured
    private var searchJob: Job? = null
    private val _catalogSearch = MutableStateFlow(CatalogSearchState())
    val catalogSearch: StateFlow<CatalogSearchState> = _catalogSearch
    private val _nintendoConnection = MutableStateFlow(
        NintendoConnectionState(connected = nintendoLibraryService.isConfigured)
    )
    val nintendoConnection: StateFlow<NintendoConnectionState> = _nintendoConnection
    val localProfile: StateFlow<LocalProfile> = profileRepository.profile

    init {
        viewModelScope.launch {
            repository.normalizeNintendoPlatformCodes()
        }
        viewModelScope.launch {
            NintendoAuthCallbackBus.callbacks.collect(::completeNintendoLogin)
        }
    }

    val games: StateFlow<List<GameEntity>> = repository.games.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun observeGame(gameId: Long): Flow<GameEntity?> = repository.observeGame(gameId)

    fun observeSessions(gameId: Long): Flow<List<PlaySessionEntity>> =
        repository.observeSessions(gameId)

    fun searchCatalog(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _catalogSearch.value = CatalogSearchState()
            return
        }
        searchJob = viewModelScope.launch {
            delay(450)
            _catalogSearch.value = CatalogSearchState(loading = true)
            _catalogSearch.value = try {
                CatalogSearchState(results = searchCatalogWithRetry(query))
            } catch (error: CancellationException) {
                throw error
            } catch (error: GameCatalogException) {
                CatalogSearchState(
                    error = when (error.kind) {
                        CatalogErrorKind.MISSING_API_KEY ->
                            "Configura rawg.api.key o thegamesdb.api.key en local.properties."
                        CatalogErrorKind.INVALID_API_KEY ->
                            "El catálogo rechazó la clave de API. Revisa local.properties."
                        CatalogErrorKind.RATE_LIMITED ->
                            "Se alcanzó el límite de consultas del catálogo. Inténtalo más tarde."
                        CatalogErrorKind.SERVICE_UNAVAILABLE ->
                            "Los catálogos no están disponibles ahora mismo. Puedes completar el juego manualmente."
                        CatalogErrorKind.NETWORK ->
                            "No se pudo conectar. Revisa tu conexión e inténtalo de nuevo."
                        CatalogErrorKind.REMOTE_ERROR ->
                            "El catálogo devolvió un error. Puedes completar el juego manualmente."
                    }
                )
            } catch (_: Exception) {
                CatalogSearchState(
                    error = "La respuesta del catálogo no se pudo procesar. Puedes completar el juego manualmente."
                )
            }
        }
    }

    private suspend fun searchCatalogWithRetry(query: String): List<CatalogGame> {
        var rawgError: GameCatalogException? = null
        val rawgAttempts = if (BuildConfig.THEGAMESDB_API_KEY.isBlank()) 2 else 1
        for (attempt in 0 until rawgAttempts) {
            try {
                return catalogService.search(query)
            } catch (error: GameCatalogException) {
                rawgError = error
                val retryable = error.kind == CatalogErrorKind.SERVICE_UNAVAILABLE ||
                    error.kind == CatalogErrorKind.NETWORK
                if (!retryable || attempt == rawgAttempts - 1) break
                delay(500)
            }
        }

        if (BuildConfig.THEGAMESDB_API_KEY.isBlank()) {
            throw rawgError ?: GameCatalogException(
                CatalogErrorKind.MISSING_API_KEY,
                "No hay ningún catálogo configurado"
            )
        }
        return secondaryCatalogService.search(query)
    }

    fun clearCatalogSearch() {
        searchJob?.cancel()
        _catalogSearch.value = CatalogSearchState()
    }

    fun loadEstimatedHours(game: CatalogGame, onLoaded: (Double?) -> Unit) {
        if (game.provider != CatalogProvider.RAWG) {
            onLoaded(null)
            return
        }
        viewModelScope.launch {
            val hours = try {
                catalogService.getEstimatedHours(game.id)
            } catch (_: Exception) {
                null
            }
            onLoaded(hours)
        }
    }

    fun exportBackup(uri: Uri, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val json = repository.createBackup()
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: error("No se pudo abrir el archivo de destino")
                }
                Result.success(Unit)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            onComplete(result)
        }
    }

    fun importBackup(uri: Uri, onComplete: (Result<BackupSummary>) -> Unit) {
        viewModelScope.launch {
            val result = try {
                val summary = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val json = resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val content = StringBuilder()
                        val buffer = CharArray(4_096)
                        while (true) {
                            val read = reader.read(buffer)
                            if (read < 0) break
                            content.append(buffer, 0, read)
                            require(content.length <= MAX_BACKUP_SIZE_CHARS) {
                                "La copia es demasiado grande"
                            }
                        }
                        content.toString()
                    } ?: error("No se pudo abrir la copia")
                    repository.restoreBackup(json)
                }
                Result.success(summary)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            onComplete(result)
        }
    }

    fun importSteamLibrary(onComplete: (Result<SteamImportSummary>) -> Unit) {
        viewModelScope.launch {
            val result = try {
                val summary = withContext(Dispatchers.IO) {
                    val ownedGames = steamLibraryService.getOwnedGames()
                    repository.importSteamGames(ownedGames)
                }
                Result.success(summary)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            onComplete(result)
        }
    }

    fun importPlayStationLibrary(
        onComplete: (Result<PlayStationImportSummary>) -> Unit
    ) {
        viewModelScope.launch {
            val result = try {
                val summary = withContext(Dispatchers.IO) {
                    val trophyGames = playStationLibraryService.getTrophyGames()
                    repository.importPlayStationGames(trophyGames)
                }
                Result.success(summary)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
            onComplete(result)
        }
    }

    fun startNintendoLogin(): Result<String> = try {
        _nintendoConnection.value = _nintendoConnection.value.copy(
            busy = false,
            message = "Completa el acceso en Nintendo y volverás automáticamente a la app.",
            failed = false
        )
        Result.success(nintendoLibraryService.createLoginUrl())
    } catch (error: Exception) {
        _nintendoConnection.value = NintendoConnectionState(
            connected = nintendoLibraryService.isConfigured,
            message = error.message ?: "No se pudo iniciar el acceso a Nintendo.",
            failed = true
        )
        Result.failure(error)
    }

    fun importNintendoLibrary() {
        _nintendoConnection.value = _nintendoConnection.value.copy(
            busy = true,
            message = null,
            failed = false
        )
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    val playedGames = nintendoLibraryService.getPlayHistory()
                    repository.importNintendoGames(playedGames)
                }
                _nintendoConnection.value = NintendoConnectionState(
                    connected = true,
                    message = nintendoSummaryMessage(summary)
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _nintendoConnection.value = NintendoConnectionState(
                    connected = nintendoLibraryService.isConfigured,
                    message = error.message ?: "No se pudo actualizar Nintendo Switch.",
                    failed = true
                )
            }
        }
    }

    fun disconnectNintendo() {
        nintendoLibraryService.disconnect()
        _nintendoConnection.value = NintendoConnectionState(
            message = "Cuenta de Nintendo desconectada."
        )
    }

    private fun completeNintendoLogin(callbackUrl: String) {
        _nintendoConnection.value = _nintendoConnection.value.copy(
            busy = true,
            message = "Conectando Nintendo e importando el historial…",
            failed = false
        )
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    nintendoLibraryService.completeLogin(callbackUrl)
                    repository.importNintendoGames(nintendoLibraryService.getPlayHistory())
                }
                _nintendoConnection.value = NintendoConnectionState(
                    connected = true,
                    message = "Nintendo conectada. ${nintendoSummaryMessage(summary)}"
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _nintendoConnection.value = NintendoConnectionState(
                    connected = nintendoLibraryService.isConfigured,
                    message = error.message ?: "No se pudo conectar Nintendo.",
                    failed = true
                )
            }
        }
    }

    private fun nintendoSummaryMessage(summary: NintendoImportSummary): String {
        val hours = summary.totalPlayedMinutes / 60
        return "${summary.found} encontrados, ${summary.added} añadidos, " +
            "${summary.alreadyPresent} existentes y $hours h registradas."
    }

    fun saveGame(game: GameEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onSaved(repository.saveGame(game))
        }
    }

    fun deleteGame(game: GameEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteGame(game)
            onDeleted()
        }
    }

    fun togglePlatinum(game: GameEntity) {
        saveGame(game.copy(platinum = !game.platinum))
    }

    fun toggleAllAchievements(game: GameEntity) {
        saveGame(game.copy(allAchievements = !game.allAchievements))
    }

    fun updateGameStatus(game: GameEntity, status: GameStatus) {
        saveGame(
            game.copy(
                status = status,
                progressPercent = when (status) {
                    GameStatus.COMPLETED -> 100
                    GameStatus.ONGOING -> 0
                    else -> game.progressPercent
                }
            )
        )
    }

    fun saveLocalProfile(displayName: String, bio: String, avatarUri: String?) {
        profileRepository.save(displayName, bio, avatarUri)
    }

    fun registerSession(
        gameId: Long,
        durationMinutes: Int,
        progressAfter: Int,
        notes: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.registerSession(gameId, durationMinutes, progressAfter, notes)
            onSaved()
        }
    }

    companion object {
        private const val MAX_BACKUP_SIZE_CHARS = 5_000_000

        fun factory(application: BacklogQuestApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(
                        application,
                        application.repository,
                        application.profileRepository,
                        application.nintendoLibraryService
                    ) as T
                }
            }
    }
}
