package com.dreamteam.bookmaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Define los esquemas de colores de BookMaster.
 *
 * El modo claro se aplica por defecto; el modo oscuro se selecciona mediante darkTheme.
 */

// Esquema oscuro con la paleta fija de BookMaster.
private val DarkColorScheme = darkColorScheme(
    primary = BookMasterPurple,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = BookMasterLightPurple,
    onSecondary = BookMasterDarkPurple,
    background = BookMasterDarkPurple,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = BookMasterDarkPurple,
    onSurface = androidx.compose.ui.graphics.Color.White
)

// Esquema claro utilizado de forma predeterminada.
private val LightColorScheme = lightColorScheme(
    primary = BookMasterPurple,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = BookMasterLightPurple,
    onSecondary = BookMasterDarkPurple,
    tertiary = BookMasterDarkPurple,
    background = BookMasterBackground,
    onBackground = BookMasterDarkPurple,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = BookMasterDarkPurple,
    surfaceVariant = BookMasterLightPurple,
    onSurfaceVariant = BookMasterMutedText,
    outline = BookMasterPurple
)

/**
 * Aplica el tema visual general de la aplicación.
 *
 * @param darkTheme Indica si debe utilizarse el modo oscuro.
 * @param dynamicColor Parámetro conservado por compatibilidad; no activa colores dinámicos.
 * @param content Contenido de la aplicación al que se aplica el tema.
 */
@Composable
fun DSM_LibreriaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Selecciona el esquema según darkTheme, sin consultar el modo del dispositivo.
    // dynamicColor se conserva en la firma por compatibilidad, pero no se aplica:
    // una paleta dinámica volvería a alterar la identidad visual morada.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Aplica los colores y la tipografía a todos los componentes hijos.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}