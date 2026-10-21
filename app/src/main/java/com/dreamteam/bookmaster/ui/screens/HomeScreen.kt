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

// Modelo de datos con Precio y Stock agregados
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

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val primaryBlue = Color(0xFF42A5F5)
    val darkBlueText = Color(0xFF0D1B2A)
    val backgroundGray = Color(0xFFF5F6F8)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = listOf("Todos", "Ficción", "Desarrollo", "Historia", "Novelas")

    // Lista de libros locales con precio y stock
    val booksList = remember {
        listOf(
            Book("1", "El Principito", "Antoine de Saint-Exupéry", "Ficción", 4.8, 12.99, 15, Color(0xFF81BEF7)),
            Book("2", "Hábitos Atómicos", "James Clear", "Desarrollo", 4.9, 18.50, 8, Color(0xFFFFB74D)),
            Book("3", "Cien Años de Soledad", "Gabriel García Márquez", "Novelas", 4.7, 15.00, 12, Color(0xFFA1887F)),
            Book("4", "Sapiens", "Yuval Noah Harari", "Historia", 4.6, 22.00, 5, Color(0xFF81C784)),
            Book("5", "1984", "George Orwell", "Ficción", 4.8, 14.25, 0, Color(0xFFE57373)), // Sin stock
            Book("6", "El Monje que Vendió su Ferrari", "Robin Sharma", "Desarrollo", 4.5, 16.80, 20, Color(0xFFBA68C8))
        )
    }

    val filteredBooks = booksList.filter { book ->
        val matchesSearch = book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Todos" || book.category == selectedCategory
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

            // Encabezado
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

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de Búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp)),
                placeholder = { Text("Buscar libro o autor...") },
                leadingIcon = { Text("🔍") },
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

            // Filtro por Categorías
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

            Text(
                text = if (selectedCategory == "Todos") "Catálogo de Libros" else "Categoría: $selectedCategory",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlueText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rejilla de Libros
            if (filteredBooks.isEmpty()) {
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBooks) { book ->
                        BookCard(book = book)
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(book: Book) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { /* Detalle del libro opcional */ }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Portada simulada
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

            // Título
            Text(
                text = book.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF0D1B2A)
            )

            // Autor
            Text(
                text = book.author,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Calificación
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

            // Precio y Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Precio
                Text(
                    text = "$${String.format("%.2f", book.price)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32) // Verde precio
                )

                // Stock indicador
                Text(
                    text = if (book.stock > 0) "Stock: ${book.stock}" else "Agotado",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (book.stock > 0) Color.Gray else Color.Red
                )
            }
        }
    }
}