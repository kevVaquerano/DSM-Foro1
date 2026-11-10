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

/**
 * Pantalla que muestra la información de un libro seleccionado.
 *
 * Permite consultar sus detalles, elegir una cantidad disponible
 * y agregar los ejemplares al carrito de compras.
 */
@Composable
fun BookDetailScreen(
    book: Book,
    onBackClick: () -> Unit,
    onAddToCart: (Book, Int) -> Unit = { _, _ -> }
) {
    // Colores principales utilizados en esta pantalla.
    val darkBlueText = Color(0xFF473071)
    val backgroundGray = Color(0xFFFCFAFF)
    val confirmationGreen = Color(0xFF2E7D32)

    // Guarda la cantidad seleccionada y la reinicia al cambiar de libro o stock.
    var quantity by remember(book.id, book.stock) {
        mutableStateOf(1)
    }

    // Controla los mensajes de confirmación que aparecen en pantalla.
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Calcula el precio según la cantidad seleccionada.
    val totalPrice = book.price * quantity

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = confirmationGreen,
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

            // Permite regresar a la pantalla anterior.
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .shadow(2.dp, RoundedCornerShape(12.dp))
            ) {
                Text(text = "⬅️", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contenedor principal con los detalles del libro.
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
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

                        // Representación visual de la portada del libro.
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

                        // Muestra el título y el autor del libro.
                        Text(
                            text = book.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlueText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Por ${book.author}",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Presenta la categoría y la calificación del libro.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFEDE5F8),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = book.category,
                                    fontSize = 12.sp,
                                    color = Color(0xFF7952B5),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⭐ ",
                                    fontSize = 14.sp
                                )

                                Text(
                                    text = book.rating.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Muestra el precio unitario y las existencias.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Precio",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = "$${String.format("%.2f", book.price)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = confirmationGreen
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Disponibilidad",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = if (book.stock > 0) {
                                        "${book.stock} disponibles"
                                    } else {
                                        "Agotado"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (book.stock > 0) {
                                        darkBlueText
                                    } else {
                                        Color.Red
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // El selector solo aparece cuando hay ejemplares disponibles.
                        if (book.stock > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(
                                        Color(0xFFF5F0FA),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                            ) {

                                // Disminuye la cantidad sin bajar de una unidad.
                                OutlinedButton(
                                    onClick = { quantity-- },
                                    enabled = quantity > 1,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(
                                        text = "-",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Cantidad actual seleccionada.
                                Text(
                                    text = "$quantity",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp
                                    )
                                )

                                // Aumenta la cantidad sin superar el stock.
                                OutlinedButton(
                                    onClick = { quantity++ },
                                    enabled = quantity < book.stock,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Agrega los ejemplares al carrito y muestra una confirmación.
                    Button(
                        onClick = {
                            val selectedQuantity = quantity

                            onAddToCart(book, selectedQuantity)

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "✅ ¡$selectedQuantity ejemplar(es) agregado(s) al carrito!",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        enabled = book.stock > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7952B5)
                        )
                    ) {
                        Text(
                            text = if (book.stock > 0) {
                                "🛒 Agregar al Carrito ($${String.format("%.2f", totalPrice)})"
                            } else {
                                "Agotado"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}