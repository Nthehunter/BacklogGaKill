package com.manito.backlogquest.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import coil.compose.AsyncImage
import com.manito.backlogquest.R
import com.manito.backlogquest.BacklogQuestApplication
import com.manito.backlogquest.data.GameEntity
import com.manito.backlogquest.data.GameRecommender
import com.manito.backlogquest.data.GameStatus
import com.manito.backlogquest.data.PlaySessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Destination("home", "Inicio", Icons.Default.Home)
    data object Library : Destination("library", "Biblioteca", Icons.Default.LibraryBooks)
    data object Wishlist : Destination("wishlist", "Deseos", Icons.Default.FavoriteBorder)
    data object Achievements : Destination("achievements", "Logros", Icons.Default.EmojiEvents)
    data object Stats : Destination("stats", "Resumen", Icons.Default.BarChart)
    data object Profile : Destination("profile", "Perfil", Icons.Default.Person)
    data object Settings : Destination("settings", "Ajustes", Icons.Default.Settings)
}

private val topDestinations = listOf(
    Destination.Home,
    Destination.Library,
    Destination.Wishlist,
    Destination.Achievements,
    Destination.Stats
)

private val PlatinumColor = Color(0xFFE5E4E2)
private val AchievementGold = Color(0xFFFFC857)

private enum class RecognitionType {
    PLAYSTATION_PLATINUM,
    PC_ALL_ACHIEVEMENTS
}

private fun isPlayStationPlatform(platform: String): Boolean {
    val value = platform.lowercase(Locale.ROOT)
    return "playstation" in value || "ps5" in value || "ps4" in value ||
        "ps3" in value || "vita" in value
}

private fun isPcPlatform(platform: String): Boolean {
    val value = platform.lowercase(Locale.ROOT)
    return "steam" in value || "pc" in value || "windows" in value
}

@Composable
private fun RecognitionEmblem(
    type: RecognitionType,
    size: Int = 32,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val platinum = type == RecognitionType.PLAYSTATION_PLATINUM
    val accent = if (platinum) PlatinumColor else AchievementGold
    val background = if (platinum) Color(0xFF17234A) else Color(0xFF123C35)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (platinum) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size((size * 0.62f).dp)
        )
        if (platinum) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size((size * 0.34f).dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

private enum class LibrarySort(val label: String) {
    SMART("Recomendación"),
    RECENT("Más recientes"),
    TITLE("Título A–Z"),
    PLATFORM("Plataforma"),
    PRIORITY("Mayor prioridad"),
    PROGRESS("Mayor progreso"),
    PLAYTIME("Más jugados")
}

private data class PlatformVisual(
    val key: String,
    val label: String,
    val shortLabel: String,
    val color: Color
)

private fun platformVisual(platform: String): PlatformVisual {
    val value = platform.lowercase(Locale.ROOT)
    return when {
        "steam" in value -> PlatformVisual("steam", "Steam", "STEAM", Color(0xFF1A9FFF))
        "playstation" in value || "ps5" in value || "ps4" in value ||
            "ps3" in value || "vita" in value -> {
            val short = when {
                "ps5" in value -> "PS5"
                "ps4" in value -> "PS4"
                "ps3" in value -> "PS3"
                "vita" in value -> "PS VITA"
                else -> "PLAYSTATION"
            }
            PlatformVisual("playstation", "PlayStation", short, Color(0xFF1671D9))
        }
        "switch" in value || "nintendo" in value || "wii" in value ||
            "3ds" in value -> {
            val short = when {
                "switch" in value -> "SWITCH"
                "3ds" in value -> "3DS"
                "wii u" in value -> "WII U"
                "wii" in value -> "WII"
                else -> "NINTENDO"
            }
            PlatformVisual("nintendo", "Nintendo", short, Color(0xFFE60012))
        }
        "xbox" in value -> PlatformVisual(
            "xbox",
            "Xbox",
            if ("series" in value) "XBOX SERIES" else "XBOX",
            Color(0xFF107C10)
        )
        "pc" in value || "windows" in value ->
            PlatformVisual("pc", "PC", "PC", Color(0xFF8B5CF6))
        else -> PlatformVisual(
            key = value.ifBlank { "other" },
            label = platform.ifBlank { "Otra" },
            shortLabel = platform.ifBlank { "OTRA" }.uppercase(Locale.ROOT).take(12),
            color = Color(0xFF607D8B)
        )
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    val visual = platformVisual(platform)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(visual.color.copy(alpha = 0.2f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(visual.color)
        )
        Text(
            visual.shortLabel,
            color = visual.color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusBadge(
    status: GameStatus,
    onClick: (() -> Unit)? = null
) {
    val color = when (status) {
        GameStatus.PLAYING -> Color(0xFF4ADE80)
        GameStatus.COMPLETED -> Color(0xFF62D7FF)
        GameStatus.PAUSED -> Color(0xFFFFB86B)
        GameStatus.ONGOING -> Color(0xFF2DD4BF)
        GameStatus.ABANDONED -> Color(0xFFFF7A90)
        GameStatus.WISHLIST -> Color(0xFFF472B6)
        GameStatus.REPLAY -> Color(0xFFB69CFF)
        GameStatus.BACKLOG -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = status.label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (onClick != null) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Cambiar estado",
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun BacklogQuestApp() {
    val application = LocalContext.current.applicationContext as BacklogQuestApplication
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(application))
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BacklogBottomBar(navController, currentRoute)
            }
        }
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(outerPadding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenGame = { navController.navigate("game/$it") },
                    onOpenProfile = { navController.navigate(Destination.Profile.route) }
                )
            }
            composable(Destination.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onAddGame = { navController.navigate("game/new") },
                    onOpenGame = { navController.navigate("game/$it") }
                )
            }
            composable(Destination.Wishlist.route) {
                WishlistScreen(
                    viewModel = viewModel,
                    onAddGame = { navController.navigate("game/new/wishlist") },
                    onOpenGame = { navController.navigate("game/$it") }
                )
            }
            composable(Destination.Achievements.route) {
                AchievementsScreen(
                    viewModel = viewModel,
                    onOpenGame = { navController.navigate("game/$it") }
                )
            }
            composable(Destination.Stats.route) {
                StatsScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(Destination.Settings.route) }
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack
                )
            }
            composable(Destination.Profile.route) {
                LocalProfileScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack
                )
            }
            composable("game/new") {
                GameFormScreen(
                    viewModel = viewModel,
                    gameId = null,
                    onBack = navController::popBackStack,
                    onSaved = { id ->
                        navController.navigate("game/$id") {
                            popUpTo("game/new") { inclusive = true }
                        }
                    }
                )
            }
            composable("game/new/wishlist") {
                GameFormScreen(
                    viewModel = viewModel,
                    gameId = null,
                    forcedStatus = GameStatus.WISHLIST,
                    onBack = navController::popBackStack,
                    onSaved = {
                        navController.popBackStack(
                            Destination.Wishlist.route,
                            inclusive = false
                        )
                    }
                )
            }
            composable(
                route = "game/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { entry ->
                val gameId = entry.arguments?.getLong("gameId") ?: return@composable
                GameDetailScreen(
                    viewModel = viewModel,
                    gameId = gameId,
                    onBack = navController::popBackStack,
                    onEdit = { navController.navigate("game/$gameId/edit") }
                )
            }
            composable(
                route = "game/{gameId}/edit",
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { entry ->
                val gameId = entry.arguments?.getLong("gameId") ?: return@composable
                GameFormScreen(
                    viewModel = viewModel,
                    gameId = gameId,
                    onBack = navController::popBackStack,
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BacklogBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 0.dp
    ) {
        topDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: AppViewModel,
    onOpenGame: (Long) -> Unit,
    onOpenProfile: () -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val profile by viewModel.localProfile.collectAsStateWithLifecycle()
    val recommendation = remember(games) { GameRecommender.recommend(games) }
    val playing = remember(games) {
        games.filter { it.status == GameStatus.PLAYING }
    }
    val completed = remember(games) { games.count { it.status == GameStatus.COMPLETED } }

    StarfieldBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "BACKLOG QUEST",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(onClick = onOpenProfile)
                        ) {
                            LocalProfileAvatar(
                                name = profile.displayName,
                                avatarUri = profile.avatarUri,
                                size = 48
                            )
                            Text(
                                "Mi perfil",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Bienvenido, ${profile.displayName}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Es hora de cazar juegos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard("En backlog", games.count { it.status == GameStatus.BACKLOG }, Modifier.weight(1f))
                        MetricCard("Jugando", playing.size, Modifier.weight(1f))
                        MetricCard("Terminados", completed, Modifier.weight(1f))
                    }
                }

                item {
                    Text(
                        text = "Recomendación",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (recommendation == null) {
                        EmptyCard(
                            title = "Tu backlog está vacío",
                            body = "Añade tu primer juego para que la app pueda recomendarte qué jugar."
                        )
                    } else {
                        RecommendationCard(recommendation, onOpenGame)
                    }
                }

                item {
                    Text(
                        text = "Jugando ahora",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (playing.isEmpty()) {
                    item {
                        EmptyCard(
                            title = "No tienes ningún juego activo",
                            body = "Abre un juego y registra una sesión para marcarlo como jugando."
                        )
                    }
                } else {
                    items(playing, key = { it.id }) { game ->
                        GameCard(
                            game = game,
                            onClick = { onOpenGame(game.id) },
                            onStatusChange = { viewModel.updateGameStatus(game, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementShowcase(
    platinumGames: List<GameEntity>,
    pcAchievementGames: List<GameEntity>,
    onOpenGame: (Long) -> Unit
) {
    val highlighted = remember(platinumGames, pcAchievementGames) {
        (platinumGames.map { it to RecognitionType.PLAYSTATION_PLATINUM } +
            pcAchievementGames.map { it to RecognitionType.PC_ALL_ACHIEVEMENTS })
            .sortedByDescending { it.first.updatedAt }
            .take(8)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                MaterialTheme.shapes.extraLarge
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Text(
                    "Sala de trofeos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tus grandes logros, separados por plataforma.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RecognitionSummary(
                    type = RecognitionType.PLAYSTATION_PLATINUM,
                    value = platinumGames.size,
                    title = "Platinos",
                    subtitle = "PlayStation",
                    modifier = Modifier.weight(1f)
                )
                RecognitionSummary(
                    type = RecognitionType.PC_ALL_ACHIEVEMENTS,
                    value = pcAchievementGames.size,
                    title = "100 % logros",
                    subtitle = "PC / Steam",
                    modifier = Modifier.weight(1f)
                )
            }
            if (highlighted.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(highlighted, key = { "${it.first.id}-${it.second}" }) { item ->
                        RecognitionGameTile(
                            game = item.first,
                            type = item.second,
                            onClick = { onOpenGame(item.first.id) }
                        )
                    }
                }
            } else {
                Text(
                    "Marca tus primeros logros desde la ficha de un juego.",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecognitionSummary(
    type: RecognitionType,
    value: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val platinum = type == RecognitionType.PLAYSTATION_PLATINUM
    val color = if (platinum) Color(0xFF8EA8FF) else Color(0xFF55D6A7)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RecognitionEmblem(type = type, size = 38)
        Column {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun RecognitionGameTile(
    game: GameEntity,
    type: RecognitionType,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (game.imageUrl.isNullOrBlank()) {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                )
            } else {
                AsyncImage(
                    model = game.imageUrl,
                    contentDescription = "Portada de ${game.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            RecognitionEmblem(
                type = type,
                size = 34,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Text(
            game.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RecommendationCard(game: GameEntity, onOpenGame: (Long) -> Unit) {
    val platform = platformVisual(game.platform)
    Card(
        onClick = { onOpenGame(game.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(platform.color.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.imageUrl != null) {
                    AsyncImage(
                        model = game.imageUrl,
                        contentDescription = "Portada de ${game.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Recomendado",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        game.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlatformBadge(game.platform)
                        StatusBadge(game.status)
                    }
                    Text(
                        "Prioridad ${game.priority}/5",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            GameProgress(game)
            Text(
                text = "La recomendación valora prioridad, antigüedad, progreso y duración restante.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
    viewModel: AppViewModel,
    onAddGame: () -> Unit,
    onOpenGame: (Long) -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    var selectedStatus by rememberSaveable { mutableStateOf<GameStatus?>(null) }
    var selectedPlatform by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(LibrarySort.SMART) }
    var sortExpanded by remember { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var platformMenuExpanded by remember { mutableStateOf(false) }

    val libraryGames = remember(games) { games.filter { it.status != GameStatus.WISHLIST } }
    val platforms = remember(libraryGames) {
        libraryGames
            .map { platformVisual(it.platform) }
            .distinctBy { it.key }
            .sortedBy { it.label }
    }

    val filtered = remember(
        libraryGames,
        selectedStatus,
        selectedPlatform,
        search,
        selectedSort
    ) {
        val matching = libraryGames.filter { game ->
                (selectedStatus == null || game.status == selectedStatus) &&
                (selectedPlatform == null || platformVisual(game.platform).key == selectedPlatform) &&
                (search.isBlank() || game.title.contains(search, ignoreCase = true) ||
                    game.platform.contains(search, ignoreCase = true))
        }
        when (selectedSort) {
            LibrarySort.SMART -> matching
            LibrarySort.RECENT -> matching.sortedByDescending { it.addedAt }
            LibrarySort.TITLE -> matching.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            LibrarySort.PLATFORM -> matching.sortedWith(
                compareBy<GameEntity> { platformVisual(it.platform).label }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            LibrarySort.PRIORITY -> matching.sortedWith(
                compareByDescending<GameEntity> { it.priority }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            LibrarySort.PROGRESS -> matching.sortedWith(
                compareByDescending<GameEntity> { it.progressPercent }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            LibrarySort.PLAYTIME -> matching.sortedWith(
                compareByDescending<GameEntity> { it.playedMinutes }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        }
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Filtrar biblioteca") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Muestra solo lo que buscas sin llenar la pantalla de opciones.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { statusMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                selectedStatus?.let { "Estado: ${it.label}" }
                                    ?: "Estado: todos"
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos los estados") },
                                onClick = {
                                    selectedStatus = null
                                    statusMenuExpanded = false
                                }
                            )
                            GameStatus.entries
                                .filterNot { it == GameStatus.WISHLIST }
                                .forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.label) },
                                        onClick = {
                                            selectedStatus = status
                                            statusMenuExpanded = false
                                        }
                                    )
                                }
                        }
                    }
                    if (platforms.size > 1) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { platformMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    platforms.firstOrNull { it.key == selectedPlatform }
                                        ?.let { "Plataforma: ${it.label}" }
                                        ?: "Plataforma: todas"
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = platformMenuExpanded,
                                onDismissRequest = { platformMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas las plataformas") },
                                    onClick = {
                                        selectedPlatform = null
                                        platformMenuExpanded = false
                                    }
                                )
                                platforms.forEach { platform ->
                                    DropdownMenuItem(
                                        text = { Text(platform.label) },
                                        onClick = {
                                            selectedPlatform = platform.key
                                            platformMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilters = false }) { Text("Listo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedStatus = null
                    selectedPlatform = null
                }) { Text("Limpiar") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.library_starfield),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xC7070B18))
        )

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddGame,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Añadir juego") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Mi biblioteca",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${filtered.size} de ${libraryGames.size} juegos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar por título o plataforma") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterCount = listOfNotNull(selectedStatus, selectedPlatform).size
                    OutlinedButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (filterCount == 0) "Filtros" else "Filtros ($filterCount)")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { sortExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                selectedSort.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            LibrarySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        selectedSort = option
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedStatus != null || selectedPlatform != null) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedStatus?.let { status ->
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedStatus = null },
                                    label = { Text("${status.label} ×") }
                                )
                            }
                        }
                        selectedPlatform?.let { platformKey ->
                            platforms.firstOrNull { it.key == platformKey }?.let { platform ->
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { selectedPlatform = null },
                                        label = { Text("${platform.label} ×") }
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            EmptyCard(
                                title = "No hay juegos",
                                body = "Cambia el filtro o añade un juego nuevo."
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { game ->
                            GameCard(
                                game = game,
                                onClick = { onOpenGame(game.id) },
                                onStatusChange = { viewModel.updateGameStatus(game, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistScreen(
    viewModel: AppViewModel,
    onAddGame: () -> Unit,
    onOpenGame: (Long) -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val wishlist = remember(games) { games.filter { it.status == GameStatus.WISHLIST } }

    StarfieldBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddGame,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Añadir deseo") }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Lista de deseos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${wishlist.size} juegos que tienes en el radar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (wishlist.isEmpty()) {
                    item {
                        EmptyCard(
                            title = "Tu wishlist está vacía",
                            body = "Al crear o editar un juego, selecciona el estado Wishlist."
                        )
                    }
                } else {
                    items(wishlist, key = { it.id }) { game ->
                        GameCard(
                            game = game,
                            onClick = { onOpenGame(game.id) },
                            onStatusChange = { viewModel.updateGameStatus(game, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameEntity,
    onClick: () -> Unit,
    onStatusChange: (GameStatus) -> Unit
) {
    val platform = platformVisual(game.platform)
    var statusExpanded by rememberSaveable(game.id) { mutableStateOf(false) }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(platform.color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(92.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(platform.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.imageUrl != null) {
                        AsyncImage(
                            model = game.imageUrl,
                            contentDescription = "Imagen de ${game.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = platform.color,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (game.platinum) {
                            Spacer(Modifier.size(8.dp))
                            RecognitionEmblem(
                                type = RecognitionType.PLAYSTATION_PLATINUM,
                                contentDescription = "Platino de PlayStation"
                            )
                        }
                        if (game.allAchievements) {
                            Spacer(Modifier.size(8.dp))
                            RecognitionEmblem(
                                type = RecognitionType.PC_ALL_ACHIEVEMENTS,
                                contentDescription = "100 % de logros de PC"
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        PlatformBadge(game.platform)
                        Box {
                            StatusBadge(
                                status = game.status,
                                onClick = { statusExpanded = true }
                            )
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                GameStatus.entries.forEach { status ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                status.label,
                                                fontWeight = if (status == game.status) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                            )
                                        },
                                        onClick = {
                                            statusExpanded = false
                                            if (status != game.status) onStatusChange(status)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (game.genre.isNotBlank()) {
                        Text(
                            text = game.genre,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                GameProgress(game)
            }
        }
    }
}

@Composable
private fun GameProgress(game: GameEntity) {
    if (game.status == GameStatus.ONGOING) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Juego sin final",
                color = Color(0xFF2DD4BF),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(formatPlayedTime(game.playedMinutes), style = MaterialTheme.typography.labelMedium)
        }
        return
    }
    LinearProgressIndicator(
        progress = game.progressPercent / 100f,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${game.progressPercent}%", style = MaterialTheme.typography.labelMedium)
        Text(formatPlayedTime(game.playedMinutes), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameDetailScreen(
    viewModel: AppViewModel,
    gameId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val game by viewModel.observeGame(gameId).collectAsStateWithLifecycle(initialValue = null)
    val sessions by viewModel.observeSessions(gameId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var showSessionDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Juego") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    val currentGame = game
                    if (currentGame != null && isPlayStationPlatform(currentGame.platform)) {
                        IconButton(onClick = { viewModel.togglePlatinum(currentGame) }) {
                            RecognitionEmblem(
                                type = RecognitionType.PLAYSTATION_PLATINUM,
                                size = 36,
                                contentDescription = if (currentGame.platinum) {
                                    "Quitar platino de PlayStation"
                                } else {
                                    "Marcar platino de PlayStation"
                                },
                                modifier = Modifier.then(
                                    if (currentGame.platinum) Modifier else Modifier.alpha(0.4f)
                                )
                            )
                        }
                    } else if (currentGame != null && isPcPlatform(currentGame.platform)) {
                        IconButton(onClick = { viewModel.toggleAllAchievements(currentGame) }) {
                            RecognitionEmblem(
                                type = RecognitionType.PC_ALL_ACHIEVEMENTS,
                                size = 36,
                                contentDescription = if (currentGame.allAchievements) {
                                    "Quitar 100 % de logros"
                                } else {
                                    "Marcar 100 % de logros"
                                },
                                modifier = Modifier.then(
                                    if (currentGame.allAchievements) Modifier else Modifier.alpha(0.4f)
                                )
                            )
                        }
                    }
                    IconButton(onClick = onEdit, enabled = game != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showDeleteDialog = true }, enabled = game != null) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (game != null && game?.status != GameStatus.COMPLETED) {
                FloatingActionButton(onClick = { showSessionDialog = true }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Registrar sesión")
                }
            }
        }
    ) { padding ->
        val currentGame = game
        if (currentGame == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando juego…")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (currentGame.imageUrl != null) {
                                AsyncImage(
                                    model = currentGame.imageUrl,
                                    contentDescription = "Imagen de ${currentGame.title}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                            }
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                            Text(
                                currentGame.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlatformBadge(currentGame.platform)
                                Box {
                                    StatusBadge(
                                        currentGame.status,
                                        onClick = { statusExpanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = statusExpanded,
                                        onDismissRequest = { statusExpanded = false }
                                    ) {
                                        GameStatus.entries.forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status.label) },
                                                onClick = {
                                                    statusExpanded = false
                                                    viewModel.updateGameStatus(currentGame, status)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            GameProgress(currentGame)
                            DetailRow("Prioridad", "${currentGame.priority}/5")
                            DetailRow(
                                "Duración estimada",
                                currentGame.estimatedHours?.let { "${formatDecimal(it)} h" } ?: "Sin estimar"
                            )
                            if (currentGame.genre.isNotBlank()) {
                                DetailRow("Género", currentGame.genre)
                            }
                            if (currentGame.notes.isNotBlank()) {
                                HorizontalDivider()
                                Text(currentGame.notes)
                            }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Sesiones de juego",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        EmptyCard(
                            title = "Aún no hay sesiones",
                            body = "Registra cuánto has jugado y el progreso alcanzado."
                        )
                    }
                } else {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            showProgress = currentGame.status != GameStatus.ONGOING
                        )
                    }
                }
            }
        }
    }

    val currentGame = game
    if (showSessionDialog && currentGame != null) {
        RegisterSessionDialog(
            game = currentGame,
            onDismiss = { showSessionDialog = false },
            onSave = { minutes, progress, notes ->
                viewModel.registerSession(
                    gameId = currentGame.id,
                    durationMinutes = minutes,
                    progressAfter = progress,
                    notes = notes,
                    onSaved = { showSessionDialog = false }
                )
            }
        )
    }

    if (showDeleteDialog && currentGame != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar juego") },
            text = { Text("Se eliminarán también todas sus sesiones. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGame(currentGame, onDeleted = onBack)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SessionCard(session: PlaySessionEntity, showProgress: Boolean = true) {
    Card {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDate(session.playedAt),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (showProgress) {
                        "${session.durationMinutes} min · ${session.progressAfter}%"
                    } else {
                        "${session.durationMinutes} min"
                    }
                )
            }
            if (session.notes.isNotBlank()) {
                Text(session.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RegisterSessionDialog(
    game: GameEntity,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, progress: Int, notes: String) -> Unit
) {
    var duration by rememberSaveable { mutableStateOf("60") }
    var progress by rememberSaveable { mutableStateOf(game.progressPercent.toString()) }
    var notes by rememberSaveable { mutableStateOf("") }
    val ongoing = game.status == GameStatus.ONGOING
    val durationNumber = duration.toIntOrNull()
    val progressNumber = if (ongoing) game.progressPercent else progress.toIntOrNull()
    val valid = durationNumber != null && durationNumber > 0 &&
        progressNumber != null && progressNumber in game.progressPercent..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit) },
                    label = { Text("Duración en minutos") },
                    singleLine = true
                )
                if (ongoing) {
                    Text(
                        "Este juego no necesita porcentaje de progreso.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    OutlinedTextField(
                        value = progress,
                        onValueChange = { progress = it.filter(Char::isDigit).take(3) },
                        label = { Text("Progreso final (%)") },
                        supportingText = { Text("Mínimo actual: ${game.progressPercent}%") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(durationNumber!!, progressNumber!!, notes) },
                enabled = valid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameFormScreen(
    viewModel: AppViewModel,
    gameId: Long?,
    forcedStatus: GameStatus? = null,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val existingGame by if (gameId == null) {
        remember { mutableStateOf<GameEntity?>(null) }
    } else {
        viewModel.observeGame(gameId).collectAsStateWithLifecycle(initialValue = null)
    }

    var title by rememberSaveable(existingGame?.id) { mutableStateOf(existingGame?.title.orEmpty()) }
    var platform by rememberSaveable(existingGame?.id) { mutableStateOf(existingGame?.platform.orEmpty()) }
    var genre by rememberSaveable(existingGame?.id) { mutableStateOf(existingGame?.genre.orEmpty()) }
    var estimatedHours by rememberSaveable(existingGame?.id) {
        mutableStateOf(existingGame?.estimatedHours?.let(::formatDecimal).orEmpty())
    }
    var durationLoading by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable(existingGame?.id) { mutableStateOf(existingGame?.notes.orEmpty()) }
    var imageUrl by rememberSaveable(existingGame?.id) { mutableStateOf(existingGame?.imageUrl) }
    var status by rememberSaveable(existingGame?.id, forcedStatus) {
        mutableStateOf(existingGame?.status ?: forcedStatus ?: GameStatus.BACKLOG)
    }
    var priority by rememberSaveable(existingGame?.id) {
        mutableStateOf((existingGame?.priority ?: 3).toFloat())
    }
    var progress by rememberSaveable(existingGame?.id) {
        mutableStateOf((existingGame?.progressPercent ?: 0).toFloat())
    }
    var statusExpanded by remember { mutableStateOf(false) }
    val catalogSearch by viewModel.catalogSearch.collectAsStateWithLifecycle()

    val valid = title.isNotBlank() && platform.isNotBlank() &&
        (estimatedHours.isBlank() || estimatedHours.replace(',', '.').toDoubleOrNull() != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gameId == null) "Añadir juego" else "Editar juego") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (gameId == null) viewModel.searchCatalog(it)
                },
                label = { Text("Título *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (gameId == null && catalogSearch.loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("Buscando en catálogos…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            catalogSearch.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (gameId == null && catalogSearch.results.isNotEmpty()) {
                Text("Resultados del catálogo", fontWeight = FontWeight.SemiBold)
                catalogSearch.results.forEach { result ->
                    Card(
                        onClick = {
                            title = result.title
                            platform = result.platforms.firstOrNull().orEmpty()
                            genre = result.genres.joinToString()
                            estimatedHours = result.estimatedHours?.let(::formatDecimal).orEmpty()
                            imageUrl = result.imageUrl
                            viewModel.clearCatalogSearch()
                            if (result.estimatedHours == null) {
                                durationLoading = true
                                viewModel.loadEstimatedHours(result) { hours ->
                                    if (estimatedHours.isBlank()) {
                                        estimatedHours = hours?.let(::formatDecimal).orEmpty()
                                    }
                                    durationLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (result.imageUrl != null) {
                                    AsyncImage(
                                        model = result.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column {
                                    Text(result.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        result.platforms.take(3).joinToString()
                                            .ifBlank { "Plataforma sin especificar" },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    result.estimatedHours?.let { hours ->
                                        Text(
                                            "Duración media: ${formatDecimal(hours)} h",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Datos proporcionados por ${catalogSearch.results.first().provider.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("Plataforma *") },
                placeholder = { Text("PC, PS5, Switch…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Acceso rápido",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("PC (Steam)", "PS5", "Nintendo Switch", "Xbox Series")) { option ->
                    FilterChip(
                        selected = platform.equals(option, ignoreCase = true),
                        onClick = { platform = option },
                        label = { Text(platformVisual(option).label) }
                    )
                }
            }
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Género") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = estimatedHours,
                onValueChange = { value ->
                    estimatedHours = value.filter { it.isDigit() || it == ',' || it == '.' }
                },
                label = { Text("Duración estimada en horas") },
                supportingText = {
                    Text("Se completa automáticamente cuando está disponible; puedes corregirla.")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (durationLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        "Consultando la ficha completa en RAWG…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = {
                    if (forcedStatus == null) statusExpanded = !statusExpanded
                }
            ) {
                OutlinedTextField(
                    value = status.label,
                    onValueChange = {},
                    readOnly = true,
                    enabled = forcedStatus == null,
                    label = { Text("Estado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    GameStatus.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                status = option
                                if (option == GameStatus.COMPLETED) progress = 100f
                                if (option == GameStatus.ONGOING) progress = 0f
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Prioridad: ${priority.roundToInt()}/5", fontWeight = FontWeight.SemiBold)
            Slider(
                value = priority,
                onValueChange = { priority = it },
                valueRange = 1f..5f,
                steps = 3
            )

            if (status == GameStatus.ONGOING) {
                Text(
                    "Los juegos sin final registran tiempo, pero no porcentaje de progreso.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Progreso: ${progress.roundToInt()}%", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                        if (it >= 100f) status = GameStatus.COMPLETED
                        else if (status == GameStatus.COMPLETED) status = GameStatus.PLAYING
                    },
                    valueRange = 0f..100f,
                    steps = 99
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val base = existingGame ?: GameEntity(
                        title = title,
                        platform = platform
                    )
                    viewModel.saveGame(
                        base.copy(
                            title = title,
                            platform = platform,
                            genre = genre,
                            estimatedHours = estimatedHours
                                .replace(',', '.')
                                .toDoubleOrNull(),
                            notes = notes,
                            imageUrl = imageUrl,
                            status = forcedStatus ?: status,
                            priority = priority.roundToInt(),
                            progressPercent = progress.roundToInt()
                        ),
                        onSaved = onSaved
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Guardar juego")
            }
        }
    }
}

@Composable
private fun LocalProfileAvatar(
    name: String,
    avatarUri: String?,
    size: Int,
    modifier: Modifier = Modifier
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "J" }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
            .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri.isNullOrBlank()) {
            Text(
                initials,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = if (size >= 96) {
                    MaterialTheme.typography.headlineLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Black
            )
        } else {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementsScreen(
    viewModel: AppViewModel,
    onOpenGame: (Long) -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val platinumGames = remember(games) {
        games.filter { it.platinum && isPlayStationPlatform(it.platform) }
            .sortedByDescending { it.updatedAt }
    }
    val pcAchievementGames = remember(games) {
        games.filter { it.allAchievements && isPcPlatform(it.platform) }
            .sortedByDescending { it.updatedAt }
    }

    StarfieldBackground {
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Tus logros",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Una vitrina para lo más difícil de tu colección.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    AchievementShowcase(
                        platinumGames = platinumGames,
                        pcAchievementGames = pcAchievementGames,
                        onOpenGame = onOpenGame
                    )
                }
                if (platinumGames.isNotEmpty()) {
                    item {
                        RecognitionShelf(
                            title = "Platinos de PlayStation",
                            games = platinumGames,
                            type = RecognitionType.PLAYSTATION_PLATINUM,
                            onOpenGame = onOpenGame
                        )
                    }
                }
                if (pcAchievementGames.isNotEmpty()) {
                    item {
                        RecognitionShelf(
                            title = "100 % de logros en PC",
                            games = pcAchievementGames,
                            type = RecognitionType.PC_ALL_ACHIEVEMENTS,
                            onOpenGame = onOpenGame
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognitionShelf(
    title: String,
    games: List<GameEntity>,
    type: RecognitionType,
    onOpenGame: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { game ->
                RecognitionGameTile(
                    game = game,
                    type = type,
                    onClick = { onOpenGame(game.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalProfileScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.localProfile.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var displayName by rememberSaveable(profile.displayName) {
        mutableStateOf(profile.displayName)
    }
    var bio by rememberSaveable(profile.bio) { mutableStateOf(profile.bio) }
    var avatarUri by rememberSaveable(profile.avatarUri) {
        mutableStateOf(profile.avatarUri)
    }
    var saved by rememberSaveable { mutableStateOf(false) }
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Algunos proveedores ya conceden acceso persistente sin requerir esta llamada.
            }
            avatarUri = uri.toString()
            saved = false
        }
    }
    val libraryGames = remember(games) { games.filter { it.status != GameStatus.WISHLIST } }
    val completed = remember(libraryGames) {
        libraryGames.count { it.status == GameStatus.COMPLETED }
    }
    val platinum = remember(libraryGames) {
        libraryGames.count { it.platinum && isPlayStationPlatform(it.platform) }
    }
    val allAchievements = remember(libraryGames) {
        libraryGames.count { it.allAchievements && isPcPlatform(it.platform) }
    }
    val favoritePlatform = remember(libraryGames) {
        libraryGames
            .groupingBy { it.platform.ifBlank { "Otra" } }
            .eachCount()
            .maxByOrNull { it.value }
    }

    StarfieldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Mi perfil") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                MaterialTheme.shapes.extraLarge
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box {
                                LocalProfileAvatar(
                                    name = displayName,
                                    avatarUri = avatarUri,
                                    size = 116,
                                    modifier = Modifier.clickable {
                                        photoLauncher.launch(arrayOf("image/*"))
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            photoLauncher.launch(arrayOf("image/*"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Cambiar foto",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                displayName.ifBlank { "Jugador" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "PERFIL LOCAL",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sin cuenta ni servidor. Tu identidad se guarda en este móvil.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Tu identidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = {
                                    displayName = it.take(24)
                                    saved = false
                                },
                                label = { Text("Nombre") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = bio,
                                onValueChange = {
                                    bio = it.take(120)
                                    saved = false
                                },
                                label = { Text("Frase de perfil") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (avatarUri != null) {
                                TextButton(
                                    onClick = {
                                        avatarUri = null
                                        saved = false
                                    }
                                ) {
                                    Text("Quitar foto")
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.saveLocalProfile(displayName, bio, avatarUri)
                                    saved = true
                                },
                                enabled = displayName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(if (saved) "Perfil guardado" else "Guardar perfil")
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Tu vitrina",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard("Juegos", libraryGames.size, Modifier.weight(1f))
                        MetricCard("Terminados", completed, Modifier.weight(1f))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecognitionSummary(
                            RecognitionType.PLAYSTATION_PLATINUM,
                            platinum,
                            "Platinos",
                            "PlayStation",
                            Modifier.weight(1f)
                        )
                        RecognitionSummary(
                            RecognitionType.PC_ALL_ACHIEVEMENTS,
                            allAchievements,
                            "100 % logros",
                            "PC / Steam",
                            Modifier.weight(1f)
                        )
                    }
                }
                favoritePlatform?.let { platform ->
                    item {
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        "Plataforma principal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PlatformBadge(platform.key)
                                }
                                Text(
                                    "${platform.value} juegos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (bio.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                    .copy(alpha = 0.55f)
                            )
                        ) {
                            Text(
                                "“$bio”",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val libraryGames = remember(games) { games.filter { it.status != GameStatus.WISHLIST } }
    val completableGames = remember(libraryGames) {
        libraryGames.filter { it.status != GameStatus.ONGOING }
    }
    val completed = completableGames.count { it.status == GameStatus.COMPLETED }
    val active = libraryGames.count { it.status == GameStatus.PLAYING }
    val totalMinutes = libraryGames.sumOf { it.playedMinutes }

    val completionRate = if (completableGames.isEmpty()) {
        0
    } else {
        completed * 100 / completableGames.size
    }
    val averageProgress = if (completableGames.isEmpty()) {
        0
    } else {
        completableGames.map { it.progressPercent }.average().roundToInt()
    }
    val byPlatform = libraryGames.groupingBy { it.platform.ifBlank { "Sin plataforma" } }
        .eachCount()
        .entries
        .sortedByDescending { it.value }

    StarfieldBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tu resumen",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Abrir ajustes")
                        }
                    }
                    Text(
                        "Progreso, tiempo y distribución de tu colección.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard("Total", libraryGames.size, Modifier.weight(1f))
                        MetricCard("Activos", active, Modifier.weight(1f))
                        MetricCard("Terminados", completed, Modifier.weight(1f))
                    }
                }
                item {
                    StatsCard("Tasa de finalización", "$completionRate%")
                }
                item {
                    StatsCard("Progreso medio", "$averageProgress%")
                }
                item {
                    StatsCard("Tiempo jugado", formatPlayedTime(totalMinutes))
                }
                item {
                    Text(
                        "Juegos por plataforma",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (byPlatform.isEmpty()) {
                    item {
                        EmptyCard("Sin datos", "Añade juegos para ver estadísticas por plataforma.")
                    }
                } else {
                    items(byPlatform, key = { it.key }) { entry ->
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PlatformBadge(entry.key)
                                    Text(entry.key)
                                }
                                Text(entry.value.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nintendoState by viewModel.nintendoConnection.collectAsStateWithLifecycle()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupFailed by remember { mutableStateOf(false) }
    var steamBusy by remember { mutableStateOf(false) }
    var steamMessage by remember { mutableStateOf<String?>(null) }
    var steamFailed by remember { mutableStateOf(false) }
    var playStationBusy by remember { mutableStateOf(false) }
    var playStationMessage by remember { mutableStateOf<String?>(null) }
    var playStationFailed by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            backupMessage = null
            viewModel.exportBackup(uri) { result ->
                backupBusy = false
                backupFailed = result.isFailure
                backupMessage = result.fold(
                    onSuccess = { "Copia exportada correctamente." },
                    onFailure = { it.message ?: "No se pudo exportar la copia." }
                )
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImportUri = uri }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Importar copia") },
            text = {
                Text(
                    "Se sustituirán todos los juegos y sesiones de este móvil por los de la " +
                        "copia. Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    backupBusy = true
                    backupMessage = null
                    viewModel.importBackup(uri) { result ->
                        backupBusy = false
                        backupFailed = result.isFailure
                        backupMessage = result.fold(
                            onSuccess = {
                                "Copia importada: ${it.games} juegos y ${it.sessions} sesiones."
                            },
                            onFailure = { it.message ?: "No se pudo importar la copia." }
                        )
                    }
                }) { Text("Sustituir datos") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancelar") }
            }
        )
    }

    StarfieldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Ajustes") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Conexiones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Actualiza tu colección desde tus plataformas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    SettingsActionCard(
                        title = "Steam",
                        body = if (viewModel.steamConfigured) {
                            "Importa juegos, horas y comprueba automáticamente el 100 % de logros."
                        } else {
                            "Configura steam.api.key y steam.id en local.properties."
                        },
                        buttonLabel = "Actualizar desde Steam",
                        busyLabel = "Consultando Steam…",
                        busy = steamBusy,
                        message = steamMessage,
                        failed = steamFailed,
                        onClick = {
                            steamBusy = true
                            steamMessage = null
                            viewModel.importSteamLibrary { result ->
                                steamBusy = false
                                steamFailed = result.isFailure
                                steamMessage = result.fold(
                                    onSuccess = {
                                        "${it.found} encontrados, ${it.added} añadidos, " +
                                            "${it.alreadyPresent} existentes y " +
                                            "${it.perfectGames} con el 100 % de logros."
                                    },
                                    onFailure = {
                                        it.message ?: "No se pudo actualizar Steam."
                                    }
                                )
                            }
                        }
                    )
                }
                item {
                    SettingsActionCard(
                        title = "PlayStation Network",
                        body = if (viewModel.playStationConfigured) {
                            "Importa juegos con trofeos, progreso y platinos de PS3, PS4, PS5 y Vita."
                        } else {
                            "Configura psn.npsso en local.properties."
                        },
                        note = "Solo aparecen juegos que hayan sincronizado trofeos.",
                        buttonLabel = "Actualizar desde PlayStation",
                        busyLabel = "Consultando PlayStation…",
                        busy = playStationBusy,
                        message = playStationMessage,
                        failed = playStationFailed,
                        onClick = {
                            playStationBusy = true
                            playStationMessage = null
                            viewModel.importPlayStationLibrary { result ->
                                playStationBusy = false
                                playStationFailed = result.isFailure
                                playStationMessage = result.fold(
                                    onSuccess = {
                                        "${it.found} encontrados, ${it.added} añadidos, " +
                                            "${it.alreadyPresent} existentes y " +
                                            "${it.platinumGames} con platino."
                                    },
                                    onFailure = {
                                        it.message ?: "No se pudo actualizar PlayStation."
                                    }
                                )
                            }
                        }
                    )
                }
                item {
                    SettingsActionCard(
                        title = "Nintendo Switch",
                        body = if (nintendoState.connected) {
                            "Importa los juegos jugados en Nintendo Switch y Switch 2 con sus horas."
                        } else {
                            "Conecta tu cuenta de Nintendo desde el navegador. No necesitas copiar ninguna clave."
                        },
                        note = "Nintendo solo muestra juegos que hayas abierto; las compras sin jugar no aparecen.",
                        buttonLabel = if (nintendoState.connected) {
                            "Actualizar desde Nintendo"
                        } else {
                            "Conectar Nintendo"
                        },
                        busyLabel = "Consultando Nintendo…",
                        busy = nintendoState.busy,
                        message = nintendoState.message,
                        failed = nintendoState.failed,
                        secondaryLabel = if (nintendoState.connected) "Desconectar cuenta" else null,
                        onSecondaryClick = viewModel::disconnectNintendo,
                        onClick = {
                            if (nintendoState.connected) {
                                viewModel.importNintendoLibrary()
                            } else {
                                viewModel.startNintendoLogin().onSuccess { url ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    )
                                }
                            }
                        }
                    )
                }
                item {
                    Text(
                        "Copias de seguridad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Guarda tu colección o llévala a otro móvil.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = {
                                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        .format(Date())
                                    exportLauncher.launch("backlog-quest-$date.json")
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Exportar copia")
                            }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream"
                                    ))
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Importar copia")
                            }
                            if (backupBusy) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Procesando copia…")
                                }
                            }
                            backupMessage?.let { message ->
                                Text(
                                    message,
                                    color = if (backupFailed) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    body: String,
    buttonLabel: String,
    busyLabel: String,
    busy: Boolean,
    message: String?,
    failed: Boolean,
    note: String? = null,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            note?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onClick,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(buttonLabel)
            }
            if (secondaryLabel != null && onSecondaryClick != null) {
                TextButton(
                    onClick = onSecondaryClick,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryLabel)
                }
            }
            if (busy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(busyLabel)
                }
            }
            message?.let {
                Text(
                    it,
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StarfieldBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.library_starfield),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xA6070B18),
                            Color(0x70101729),
                            Color(0xB8070B18)
                        )
                    )
                )
        )
        content()
    }
}

@Composable
private fun StatsCard(label: String, value: String) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatPlayedTime(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours == 0 -> "$remaining min"
        remaining == 0 -> "$hours h"
        else -> "$hours h $remaining min"
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
