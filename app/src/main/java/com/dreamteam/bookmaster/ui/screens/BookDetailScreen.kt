package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.ui.theme.BookMasterPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterDarkPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterBackground
import java.util.Locale

/**
 * Detalle de un libro.
 * Mantiene intacta la firma usada desde NavGraph y la lógica de añadir al carrito.
 * Material Symbols requeridos en drawable:
 * - ic_book_detail_back.xml: arrow_back (NUEVO)
 * - ic_book_rating_star.xml: usar la variante Filled de Material Symbols para ver la estrella amarilla
 * - ic_nav_cart.xml: ya existente, se reutiliza.
 */
@Composable
fun BookDetailScreen(
    book: Book,
    onBackClick: () -> Unit,
    onAddToCart: (Book, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    // Mantener la barra del sistema del mismo color que el encabezado.
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(71, 48, 113)
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
    }
    val coverResource = remember(book.id) {
        context.resources.getIdentifier(book.id, "drawable", context.packageName)
    }
    var quantity by remember(book.id, book.stock) { mutableIntStateOf(1) }
    val available = book.stock > 0
    val total = book.price * quantity
    val softPurple = Color(0xFFEDE5F8)
    val muted = Color(0xFF766D80)
    val stockColor = when {
        book.stock < 10 -> Color(0xFFC62828) // 0-9: rojo
        book.stock == 10 -> Color(0xFF9A6700) // 10: amarillo oscuro, legible sobre blanco
        else -> Color(0xFF2E7D32) // 11+: verde
    }

    Scaffold(
        containerColor = BookMasterBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total", color = muted, fontSize = 12.sp)
                            Text(
                                "$${String.format(Locale.US, "%.2f", total)}",
                                color = BookMasterPurple,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (available) {
                            // Un solo control segmentado; las líneas dividen cada sector.
                            Row(
                                modifier = Modifier
                                    .height(42.dp)
                                    .border(1.dp, Color(0xFFB99CD6), RoundedCornerShape(11.dp))
                                    .clip(RoundedCornerShape(11.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(37.dp).fillMaxHeight()
                                        .clickable(enabled = quantity > 1) { quantity-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "−", fontSize = 21.sp, fontWeight = FontWeight.Bold,
                                        color = if (quantity > 1) BookMasterDarkPurple else Color(0xFFBFB5C7)
                                    )
                                }
                                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFB99CD6)))
                                Box(modifier = Modifier.width(36.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Text(quantity.toString(), fontWeight = FontWeight.Bold, color = BookMasterDarkPurple)
                                }
                                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFB99CD6)))
                                Box(
                                    modifier = Modifier.width(37.dp).fillMaxHeight()
                                        .clickable(enabled = quantity < book.stock) { quantity++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "+", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                        color = if (quantity < book.stock) BookMasterDarkPurple else Color(0xFFBFB5C7)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onAddToCart(book, quantity) },
                        enabled = available,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BookMasterPurple)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_book_detail_add_cart),
                            contentDescription = null,
                            modifier = Modifier.size(23.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (available) "Agregar al carrito" else "Agotado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Encabezado oscuro; la flecha vuelve a ocupar el lugar de «Volver».
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(BookMasterDarkPurple)
                    .statusBarsPadding()
                    .height(62.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_detail_back),
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    "Detalles del libro",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                // Portada completa y de mayor tamaño. Fit conserva toda la imagen,
                // incluso si el libro tiene una portada muy vertical u horizontal.
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .height(330.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF1F3F6)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverResource != 0) {
                        Image(
                            painter = painterResource(coverResource),
                            contentDescription = "Portada de ${book.title}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        )
                    } else {
                        Text("Portada no disponible", color = BookMasterDarkPurple)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
                    // Título a la izquierda y categoría a su derecha, en cápsula.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = book.title,
                            color = BookMasterDarkPurple,
                            fontSize = 23.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = softPurple,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = book.category,
                                color = BookMasterDarkPurple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (book.publicationYear > 0) "${book.author} · ${book.publicationYear}" else book.author,
                        color = muted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(13.dp))

                    // Calificación sin cápsula, y edad recomendada en el extremo derecho.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_book_rating_star),
                                contentDescription = "Calificación",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", book.rating),
                                color = BookMasterDarkPurple,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        }
                        Surface(color = softPurple, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "${book.minimumAge}+",
                                color = BookMasterDarkPurple,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFFD4C4E8))
                    Spacer(Modifier.height(17.dp))
                    Text("Descripción", color = BookMasterDarkPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        book.description.ifBlank { "Aún no hay una descripción disponible." },
                        color = Color(0xFF605A67), fontSize = 14.sp, lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Precio unitario", color = muted, fontSize = 12.sp)
                            Text(
                                "$${String.format(Locale.US, "%.2f", book.price)}",
                                color = BookMasterPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Disponibilidad", color = muted, fontSize = 12.sp)
                            Text(
                                if (available) "${book.stock} disponibles" else "Agotado",
                                color = stockColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}