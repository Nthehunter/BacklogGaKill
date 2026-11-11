package com.manito.backlogquest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8CFF),
    onPrimary = Color(0xFF16005C),
    primaryContainer = Color(0xFF302477),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFF62D7FF),
    onSecondary = Color(0xFF003545),
    secondaryContainer = Color(0xFF064A5E),
    onSecondaryContainer = Color(0xFFC3F0FF),
    tertiary = Color(0xFFFFB86B),
    background = Color(0xFF070B18),
    onBackground = Color(0xFFF1F3FF),
    surface = Color(0xFF101729),
    onSurface = Color(0xFFF1F3FF),
    surfaceVariant = Color(0xFF1B243B),
    onSurfaceVariant = Color(0xFFC5CCE0),
    outline = Color(0xFF68728E),
    error = Color(0xFFFFB4AB)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun BacklogQuestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = AppShapes,
        content = content
    )
}
