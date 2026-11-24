package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import com.dreamteam.bookmaster.ui.theme.BookMasterPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterDarkPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterBackground

/**
 * Modelo que almacena la información de cada libro.
 * Incluye datos bibliográficos, precio, existencias y datos asociados a la portada.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val rating: Double,
    val price: Double,
    val stock: Int,
    val colorBackground: Color,
    val minimumAge: Int = 0,
    val publicationYear: Int = 0,
    val description: String = "",
    val imageUrl: String = ""
)

/**
 * Representa un producto agregado al carrito.
 * Relaciona un libro con la cantidad de ejemplares seleccionados por el usuario.
 */
data class CartItem(
    val book: Book,
    var quantity: Int
)

/**
 * Pantalla principal de BookMaster.
 * Recibe el catálogo, permite buscar por título o autor y filtrar por categoría.
 * Delega la apertura del detalle al callback onBookClick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    booksList: List<Book>,
    onBookClick: (Book) -> Unit
) {
    // Colores principales de la interfaz.
    val primaryBlue = BookMasterPurple
    val darkBlueText = BookMasterDarkPurple
    val backgroundGray = BookMasterBackground

    // Guarda el texto que escribe el usuario en la búsqueda.
    var searchQuery by remember {
        mutableStateOf("")
    }

    // Controla la categoría seleccionada en los filtros.
    var selectedCategory by remember {
        mutableStateOf("Todos")
    }

    // Categorías fijas del filtro; no se generan a partir del catálogo recibido.
    val categories = remember {
        listOf(
            "Todos",
            "Infantil",
            "Desarrollo Personal",
            "Ciencia Ficción",
            "Terror",
            "Romance",
            "Historia"
        )
    }

    // Saludo según la hora local del dispositivo.
    val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val saludo = when (horaActual) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    val contexto = LocalContext.current
    // Busca el recurso del logo; solo se muestra si existe.
    val logoId = remember(contexto.packageName) {
        contexto.resources.getIdentifier("bookmaster_icon1", "drawable", contexto.packageName)
    }
    // Estado del carrusel y ámbito de corrutinas para desplazar los filtros.
    val carouselState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

            // Encabezado con saludo y logo.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = saludo,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlueText
                )
                if (logoId != 0) {
                    Image(
                        painter = painterResource(logoId),
                        contentDescription = "Logo de BookMaster",
                        modifier = Modifier.size(52.dp),
                        contentScale = ContentScale.Fit
                    )
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
                    Text(
                        text = "Buscar libro o autor...",
                        color = Color(0xFFB0A8BA),
                        fontSize = 16.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_home_search),
                        contentDescription = "Buscar",
                        tint = BookMasterDarkPurple,
                        modifier = Modifier.size(21.dp)
                    )
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
                state = carouselState,
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
                            scope.launch {
                                // Desplaza el carrusel suavemente para acompañar la elección.
                                val index = categories.indexOf(category)
                                carouselState.animateScrollToItem((index - 1).coerceAtLeast(0))
                            }
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
                    "Catálogo de libros"
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
                    contentPadding = PaddingValues(bottom = 88.dp),
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
 * Muestra su portada, título, autor, calificación, precio y disponibilidad.
 */
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit
) {
    // Colores usados para destacar la información del libro.
    val darkBlueText = BookMasterDarkPurple
    val priceGreen = BookMasterDarkPurple

    // Busca la portada incluida en res/drawable mediante el ID del libro.
    // Ejemplo: books/libro_001 -> drawable/libro_001.jpg (o png/webp).
    val context = LocalContext.current
    val coverResource = remember(book.id) {
        context.resources.getIdentifier(book.id, "drawable", context.packageName)
    }

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
            // Vista previa de la portada: Crop conserva las proporciones y TopCenter prioriza la parte superior.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F3F6)),
                contentAlignment = Alignment.Center
            ) {
                if (coverResource != 0) {
                    Image(
                        painter = painterResource(id = coverResource),
                        contentDescription = "Portada de ${book.title}",
                        modifier = Modifier.fillMaxSize().padding(7.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                } else {
                    // Si falta alguna imagen, la tarjeta sigue funcionando.
                    Text(text = "📖", fontSize = 38.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Título y calificación; los textos largos se limitan con puntos suspensivos.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = book.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = darkBlueText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_rating_star),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", book.rating),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = darkBlueText
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = book.author,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Precio con dos decimales y disponibilidad según las existencias.
            Spacer(Modifier.height(17.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", book.price)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = priceGreen
                )
                Text(
                    text = if (book.stock > 0) "Stock: ${book.stock}" else "Agotado",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Rojo: menos de 10; amarillo: 10; verde: más de 10.
                    color = when {
                        book.stock < 10 -> Color(0xFFC62828)
                        book.stock == 10 -> Color(0xFF9A6700)
                        else -> Color(0xFF2E7D32)
                    }
                )
            }
        }
    }
}