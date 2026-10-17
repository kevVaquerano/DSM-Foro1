package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    // Definición de colores basados en la interfaz
    val skyBlueColor = Color(0xFF81BEF7)
    val buttonBlueColor = Color(0xFF42A5F5)
    val shadowBlueColor = Color(0xFF1E88E5)
    val darkBlueText = Color(0xFF0D1B2A)
    val lightBackgroundColor = Color(0xFFF5F5F7)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Tarjeta contenedora principal con fondo claro
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = lightBackgroundColor),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- SECCIÓN SUPERIOR: Frase inspiradora ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(skyBlueColor)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Comillas superiores
                        Text(
                            text = "“",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlueText,
                            lineHeight = 40.sp
                        )

                        // Texto principal de la frase
                        Text(
                            text = "Donde cada\nlibro deja una\nenseñanza...",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlueText,
                            lineHeight = 38.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Comillas inferiores
                        Text(
                            text = "”",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlueText,
                            lineHeight = 40.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }

                // --- SECCIÓN INFERIOR: Marca, ilustración y botón ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge del Nombre de la App ("BookMaster")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.LightGray),
                        modifier = Modifier
                            .shadow(4.dp, shape = RoundedCornerShape(50))
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "BookMaster",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF1A365D),
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                        )
                    }

                    // Recuadro ilustrativo con la imagen del gato (ic_cat_books)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = skyBlueColor),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(130.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_cat_books),
                                contentDescription = "Gato sobre libros",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // --- BOTÓN "¡Comienza ahora!" con efecto de sombra superpuesta ---
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(0.85f)
                    ) {
                        // Sombra azul oscura desplazada para efecto 3D
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = (-6).dp, y = 6.dp)
                                .background(shadowBlueColor, shape = RoundedCornerShape(12.dp))
                        )

                        // Botón principal
                        Button(
                            onClick = onStartClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonBlueColor),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "¡Comienza ahora!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = darkBlueText,
                                textDecoration = TextDecoration.Underline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}