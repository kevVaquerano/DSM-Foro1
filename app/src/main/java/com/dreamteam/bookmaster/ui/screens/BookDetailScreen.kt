package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun BookDetailScreen(
    book: Book,
    onBackClick: () -> Unit,
    onAddToCart: (Book, Int) -> Unit = { _, _ -> }
) {
    val darkBlueText = Color(0xFF0D1B2A)
    val backgroundGray = Color(0xFFF5F6F8)

    // Estado local para la cantidad seleccionada
    var quantity by remember { mutableStateOf(1) }

    // Estado del Snackbar y alcance para corrutinas
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2E7D32), // Verde confirmación
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = backgroundGray
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            // Botón de regresar
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .shadow(2.dp, RoundedCornerShape(12.dp))
            ) {
                Text(text = "⬅️", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta principal con detalles del libro
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Portada grande del libro
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(book.colorBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 70.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Título
                        Text(
                            text = book.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlueText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Autor
                        Text(
                            text = "Por ${book.author}",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Categoría y Calificación
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = book.category,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⭐ ", fontSize = 14.sp)
                                Text(
                                    text = book.rating.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Precio y Stock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Precio", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = "$${String.format("%.2f", book.price)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Disponibilidad", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = if (book.stock > 0) "${book.stock} disponibles" else "Agotado",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (book.stock > 0) darkBlueText else Color.Red
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Selector de cantidad (+ / -)
                        if (book.stock > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(Color(0xFFF0F4F8), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "$quantity",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                OutlinedButton(
                                    onClick = { if (quantity < book.stock) quantity++ },
                                    enabled = quantity < book.stock,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // BOTÓN CON CONFIRMACIÓN SNACKBAR
                    Button(
                        onClick = {
                            onAddToCart(book, quantity)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "✅ ¡$quantity ejemplar(es) agregado(s) al carrito!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        enabled = book.stock > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                    ) {
                        Text(
                            text = if (book.stock > 0) "🛒 Agregar al Carrito ($${String.format("%.2f", book.price * quantity)})" else "Agotado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}