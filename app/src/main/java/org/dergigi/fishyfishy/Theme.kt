package org.dergigi.fishyfishy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF14796F), onPrimary = Color.White,
    primaryContainer = Color(0xFFBCECE0), onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF193E3D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8DF), onSecondaryContainer = Color(0xFF193E3D),
    tertiary = Color(0xFF765A18), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0C86C), onTertiaryContainer = Color(0xFF302300),
    background = Color(0xFFF7F6F0), onBackground = Color(0xFF193E3D),
    surface = Color(0xFFF7F6F0), onSurface = Color(0xFF193E3D),
    surfaceVariant = Color(0xFFE3EEE8), onSurfaceVariant = Color(0xFF617573),
    surfaceDim = Color(0xFFD7DCD5), surfaceBright = Color(0xFFF7F6F0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEEF1EA), surfaceContainerHigh = Color(0xFFE7ECE5),
    surfaceContainerHighest = Color(0xFFE0E6DF),
    outline = Color(0xFF718581), outlineVariant = Color(0xFFC0CDC7),
    inverseSurface = Color(0xFF263B37), inverseOnSurface = Color(0xFFF0F3EB),
    inversePrimary = Color(0xFF88D5C5), surfaceTint = Color(0xFF14796F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF88D5C5), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF145047), onPrimaryContainer = Color(0xFFBCECE0),
    secondary = Color(0xFFB4CFC3), onSecondary = Color(0xFF203A32),
    secondaryContainer = Color(0xFF354F46), onSecondaryContainer = Color(0xFFD3E8DF),
    tertiary = Color(0xFFF0C86C), onTertiary = Color(0xFF403000),
    tertiaryContainer = Color(0xFF594414), onTertiaryContainer = Color(0xFFFFE1A0),
    background = Color(0xFF101C1A), onBackground = Color(0xFFE0EAE3),
    surface = Color(0xFF101C1A), onSurface = Color(0xFFE0EAE3),
    surfaceVariant = Color(0xFF293E38), onSurfaceVariant = Color(0xFFB8CCC3),
    surfaceDim = Color(0xFF101C1A), surfaceBright = Color(0xFF35433E),
    surfaceContainerLowest = Color(0xFF0B1614), surfaceContainerLow = Color(0xFF192823),
    surfaceContainer = Color(0xFF20302A), surfaceContainerHigh = Color(0xFF293A33),
    surfaceContainerHighest = Color(0xFF34463E),
    outline = Color(0xFF899E94), outlineVariant = Color(0xFF40574C),
    inverseSurface = Color(0xFFE0EAE3), inverseOnSurface = Color(0xFF263B37),
    inversePrimary = Color(0xFF14796F), surfaceTint = Color(0xFF88D5C5),
)

@Composable
fun FishyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
