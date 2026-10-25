package com.dreamteam.bookmaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Define los esquemas de colores de BookMaster.
 *
 * Permite adaptar la apariencia de los componentes
 * al modo claro u oscuro del dispositivo.
 */

// Colores principales utilizados cuando está activo el modo oscuro.
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Colores principales utilizados cuando está activo el modo claro.
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Aplica el tema visual general de la aplicación.
 *
 * @param darkTheme Indica si debe utilizarse el modo oscuro.
 * @param dynamicColor Permite utilizar colores dinámicos
 * en dispositivos compatibles con Android 12 o superior.
 * @param content Contenido de la aplicación al que se aplica el tema.
 */
@Composable
fun DSM_LibreriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Comprueba qué esquema de colores debe utilizarse.
    val colorScheme = when {

        // Utiliza los colores dinámicos del sistema si están disponibles.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        // Aplica los colores definidos para el modo oscuro.
        darkTheme -> DarkColorScheme

        // Utiliza los colores del modo claro en los demás casos.
        else -> LightColorScheme
    }

    // Aplica los colores y la tipografía a todos los componentes hijos.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}