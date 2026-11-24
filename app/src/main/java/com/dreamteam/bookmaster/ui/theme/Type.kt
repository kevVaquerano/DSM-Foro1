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

    // Estilo bodyLarge de Material 3 para textos de contenido.
    bodyLarge = TextStyle(

        // Utiliza la fuente predeterminada del sistema.
        fontFamily = FontFamily.Default,

        // Mantiene un grosor normal para facilitar la lectura.
        fontWeight = FontWeight.Normal,

        // Tamaño del texto: 16 sp, adaptable a la escala de fuente del usuario.
        fontSize = 16.sp,

        // Altura de línea de 24 sp.
        lineHeight = 24.sp,

        // Espaciado entre caracteres de 0.5 sp.
        letterSpacing = 0.5.sp
    )
)