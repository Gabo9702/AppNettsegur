package com.cgro.appnettsegur.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Solo tema claro y sin color dinámico, para respetar la identidad visual de Nettsegur.
private val EsquemaClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Blanco,
    primaryContainer = AzulPrimarioContenedor,
    onPrimaryContainer = AzulPrimario,
    secondary = AzulSecundario,
    onSecondary = NeutroOscuro,
    secondaryContainer = AzulSecundarioContenedor,
    onSecondaryContainer = NeutroOscuro,
    background = NeutroClaro,
    onBackground = NeutroOscuro,
    surface = Blanco,
    onSurface = NeutroOscuro,
    surfaceVariant = GrisVariante,
    onSurfaceVariant = GrisTextoSecundario,
    surfaceContainer = NeutroClaro,
    surfaceContainerLow = Blanco,
    surfaceContainerHigh = Blanco,
    surfaceContainerHighest = GrisVariante,
    outline = GrisBorde,
    outlineVariant = GrisBorde,
)

val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun AppNettsegurTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
