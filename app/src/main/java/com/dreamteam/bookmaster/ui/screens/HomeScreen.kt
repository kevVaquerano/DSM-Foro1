package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modelo que almacena la información de cada libro.
 *
 * Incluye sus datos principales, precio, existencias
 * y el color utilizado para representar su portada.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val rating: Double,
    val price: Double,
    val stock: Int,
    val colorBackground: Color
)

/**
 * Representa un producto agregado al carrito.
 *
 * Relaciona un libro con la cantidad de ejemplares
 * seleccionados por el usuario.
 */
data class CartItem(
    val book: Book,
    var quantity: Int
)

/**
 * Pantalla principal de BookMaster.
 *
 * Permite explorar el catálogo, buscar libros por título
 * o autor, filtrar categorías y acceder al carrito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    booksList: List<Book>,
    onLogout: () -> Unit,
    onBookClick: (Book) -> Unit,
    onCartClick: () -> Unit,
    cartItemCount: Int
) {
    // Colores principales de la interfaz.
    val primaryBlue = Color(0xFF7952B5)
    val darkBlueText = Color(0xFF473071)
    val backgroundGray = Color(0xFFFCFAFF)

    // Guarda el texto que escribe el usuario en la búsqueda.
    var searchQuery by remember {
        mutableStateOf("")
    }

    // Controla la categoría seleccionada en los filtros.
    var selectedCategory by remember {
        mutableStateOf("Todos")
    }

    // Categorías disponibles para organizar el catálogo.
    val categories = remember {
        listOf(
            "Todos",
            "Ficción",
            "Desarrollo",
            "Historia",
            "Novelas"
        )
    }

    // Filtra los libros según el texto y la categoría elegida.
    // La búsqueda no distingue entre mayúsculas y minúsculas.
    val filteredBooks = booksList.filter { book ->

        val matchesSearch =
            book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)

        val matchesCategory =
            selectedCategory == "Todos" ||
                    book.category == selectedCategory

        // Solo muestra los libros que cumplen ambas condiciones.
        matchesSearch && matchesCategory
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Encabezado principal con el nombre de la aplicación.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "¡Hola de nuevo! 👋",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "BookMaster",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkBlueText
                    )
                }

                // Agrupa los accesos al carrito y al cierre de sesión.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Muestra la cantidad de productos del carrito.
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$cartItemCount"
                                    )
                                }
                            }
                        }
                    ) {

                        // Abre la pantalla del carrito de compras.
                        IconButton(
                            onClick = onCartClick,
                            modifier = Modifier
                                .background(
                                    Color.White,
                                    RoundedCornerShape(12.dp)
                                )
                                .shadow(
                                    2.dp,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                text = "🛒",
                                fontSize = 20.sp
                            )
                        }
                    }

                    // Permite cerrar la sesión actual.
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Salir 🚪",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campo para buscar libros por nombre o autor.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        2.dp,
                        RoundedCornerShape(16.dp)
                    )
                    .background(
                        Color.White,
                        RoundedCornerShape(16.dp)
                    ),
                placeholder = {
                    Text("Buscar libro o autor...")
                },
                leadingIcon = {
                    Text("🔍")
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Lista horizontal para seleccionar categorías.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->

                    // Identifica si esta categoría está seleccionada.
                    val isSelected = category == selectedCategory

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = category
                        },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = darkBlueText
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actualiza el encabezado según la categoría seleccionada.
            Text(
                text = if (selectedCategory == "Todos") {
                    "Catálogo de Libros"
                } else {
                    "Categoría: $selectedCategory"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlueText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Comprueba si existen libros que coincidan con los filtros.
            if (filteredBooks.isEmpty()) {

                // Informa cuando la búsqueda no encuentra resultados.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron libros",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }

            } else {

                // Organiza los libros en una cuadrícula de dos columnas.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {

                    // Crea una tarjeta por cada libro disponible en el filtro.
                    items(
                        items = filteredBooks,
                        key = { it.id }
                    ) { book ->

                        BookCard(
                            book = book,
                            onClick = {
                                onBookClick(book)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta que representa un libro dentro del catálogo.
 *
 * Muestra su portada, título, autor, calificación,
 * precio y disponibilidad.
 */
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit
) {
    // Colores usados para destacar la información del libro.
    val darkBlueText = Color(0xFF473071)
    val priceGreen = Color(0xFF2E7D32)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable {
                onClick()
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            // Portada representativa con el color asignado al libro.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(book.colorBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    fontSize = 38.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Muestra el título y limita su extensión a una línea.
            Text(
                text = book.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = darkBlueText
            )

            // Nombre del autor del libro.
            Text(
                text = book.author,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Calificación asignada al libro.
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐ ",
                    fontSize = 11.sp
                )

                Text(
                    text = book.rating.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Presenta el precio y el número de ejemplares disponibles.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Precio unitario del libro.
                Text(
                    text = "$${String.format("%.2f", book.price)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = priceGreen
                )

                // Indica las existencias o si el libro está agotado.
                Text(
                    text = if (book.stock > 0) {
                        "Stock: ${book.stock}"
                    } else {
                        "Agotado"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (book.stock > 0) {
                        Color.Gray
                    } else {
                        Color.Red
                    }
                )
            }
        }
    }
}