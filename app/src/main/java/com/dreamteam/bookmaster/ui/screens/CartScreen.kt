package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla del carrito de compras.
 *
 * Muestra los libros agregados, permite modificar sus cantidades,
 * eliminar productos y consultar el total antes de pagar.
 */
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onIncreaseQuantity: (CartItem) -> Unit,
    onDecreaseQuantity: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onBackClick: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    // Colores utilizados en la pantalla del carrito.
    val darkBlueText = Color(0xFF0D1B2A)
    val backgroundGray = Color(0xFFF5F6F8)
    val priceGreen = Color(0xFF2E7D32)

    // Calcula el importe total y la cantidad de ejemplares.
    // Los valores se actualizan cuando cambia el contenido del carrito.
    val totalAmount = cartItems.sumOf {
        it.book.price * it.quantity
    }

    val totalBooksCount = cartItems.sumOf {
        it.quantity
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Encabezado con el título y el botón para regresar.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(
                            Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "⬅️",
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Carrito de Compras 🛒",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlueText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comprueba si el carrito tiene libros agregados.
            if (cartItems.isEmpty()) {

                // Muestra un mensaje cuando todavía no hay productos.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🛒",
                            fontSize = 60.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tu carrito está vacío",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }

            } else {

                // Presenta los productos en una lista desplazable.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(cartItems) { item ->

                        // Cada tarjeta administra las acciones de su libro.
                        CartItemCard(
                            item = item,
                            onIncrease = {
                                onIncreaseQuantity(item)
                            },
                            onDecrease = {
                                onDecreaseQuantity(item)
                            },
                            onRemove = {
                                onRemoveItem(item)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tarjeta con el resumen general de la compra.
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        // Cantidad total de ejemplares agregados.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cantidad total:",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )

                            Text(
                                text = "$totalBooksCount libros",
                                fontWeight = FontWeight.Bold,
                                color = darkBlueText,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Importe final calculado a partir de los productos.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total a pagar:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = darkBlueText
                            )

                            Text(
                                text = "$${String.format("%.2f", totalAmount)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = priceGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Continúa hacia el proceso de pago.
                        Button(
                            onClick = onCheckoutClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF42A5F5)
                            )
                        ) {
                            Text(
                                text = "Proceder al Pago 💳",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual para cada libro del carrito.
 *
 * Muestra su portada, nombre, precio, cantidad y subtotal.
 * También permite aumentar, disminuir o eliminar el producto.
 */
@Composable
fun CartItemCard(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    // Datos utilizados para calcular y mostrar el subtotal.
    val book = item.book
    val subtotal = book.price * item.quantity
    val darkBlueText = Color(0xFF0D1B2A)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Miniatura que representa la portada del libro.
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(book.colorBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del producto y controles de cantidad.
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = darkBlueText
                )

                Text(
                    text = "$${String.format("%.2f", book.price)} c/u",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Permite cambiar la cantidad del producto.
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Solicita disminuir una unidad del carrito.
                    OutlinedButton(
                        onClick = onDecrease,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "-",
                            fontSize = 14.sp
                        )
                    }

                    // Cantidad de ejemplares que hay actualmente.
                    Text(
                        text = "${item.quantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Solo permite aumentar si hay más existencias.
                    OutlinedButton(
                        onClick = onIncrease,
                        enabled = item.quantity < book.stock,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Agrupa la opción de eliminar y el subtotal del libro.
            Column(
                horizontalAlignment = Alignment.End
            ) {

                // Elimina el producto seleccionado del carrito.
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "🗑️",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Precio acumulado según la cantidad del producto.
                Text(
                    text = "$${String.format("%.2f", subtotal)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = darkBlueText
                )
            }
        }
    }
}