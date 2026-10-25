package com.dreamteam.bookmaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Configuración de la tipografía de BookMaster.
 *
 * Define el tamaño, el grosor y el espaciado
 * de los textos utilizados en la aplicación.
 */
val Typography = Typography(

    // Estilo principal para los textos de contenido.
    bodyLarge = TextStyle(

        // Utiliza la fuente predeterminada del sistema.
        fontFamily = FontFamily.Default,

        // Mantiene un grosor normal para facilitar la lectura.
        fontWeight = FontWeight.Normal,

        // Tamaño de la letra.
        fontSize = 16.sp,

        // Espacio vertical entre las líneas del texto.
        lineHeight = 24.sp,

        // Separación adicional entre caracteres.
        letterSpacing = 0.5.sp
    )
)