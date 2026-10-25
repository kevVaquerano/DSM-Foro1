package com.dreamteam.bookmaster.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dreamteam.bookmaster.ui.screens.*

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object BookDetail : Screen("book_detail")
    object Cart : Screen("cart")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    var selectedBookId by remember { mutableStateOf<String?>(null) }

    // Lista global de libros reactiva (mantiene el stock actualizado)
    val booksList = remember {
        mutableStateListOf(
            Book("1", "El Principito", "Antoine de Saint-Exupéry", "Ficción", 4.8, 12.99, 15, Color(0xFF81BEF7)),
            Book("2", "Hábitos Atómicos", "James Clear", "Desarrollo", 4.9, 18.50, 8, Color(0xFFFFB74D)),
            Book("3", "Cien Años de Soledad", "Gabriel García Márquez", "Novelas", 4.7, 15.00, 12, Color(0xFFA1887F)),
            Book("4", "Sapiens", "Yuval Noah Harari", "Historia", 4.6, 22.00, 5, Color(0xFF81C784)),
            Book("5", "1984", "George Orwell", "Ficción", 4.8, 14.25, 0, Color(0xFFE57373)),
            Book("6", "El Monje que Vendió su Ferrari", "Robin Sharma", "Desarrollo", 4.5, 16.80, 20, Color(0xFFBA68C8))
        )
    }

    // Lista reactiva para almacenar los items del carrito
    val cartItems = remember { mutableStateListOf<CartItem>() }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) {
                            inclusive = true
                        }
                    }
                },
                onRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Welcome.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                booksList = booksList,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBookClick = { book ->
                    selectedBookId = book.id
                    navController.navigate(Screen.BookDetail.route)
                },
                onCartClick = {
                    navController.navigate(Screen.Cart.route)
                },
                cartItemCount = cartItems.sumOf { it.quantity }
            )
        }

        composable(Screen.BookDetail.route) {
            val currentBook = booksList.find { it.id == selectedBookId }
            currentBook?.let { book ->
                BookDetailScreen(
                    book = book,
                    onBackClick = { navController.popBackStack() },
                    onAddToCart = { addedBook, quantity ->
                        val existingItem = cartItems.find { it.book.id == addedBook.id }
                        if (existingItem != null) {
                            existingItem.quantity += quantity
                        } else {
                            cartItems.add(CartItem(addedBook, quantity))
                        }
                    }
                )
            }
        }

        composable(Screen.Cart.route) {
            CartScreen(
                cartItems = cartItems,
                onIncreaseQuantity = { item ->
                    val index = cartItems.indexOf(item)
                    val bookInCatalog = booksList.find { it.id == item.book.id }
                    if (index != -1 && bookInCatalog != null && item.quantity < bookInCatalog.stock) {
                        cartItems[index] = item.copy(quantity = item.quantity + 1)
                    }
                },
                onDecreaseQuantity = { item ->
                    val index = cartItems.indexOf(item)
                    if (index != -1) {
                        if (item.quantity > 1) {
                            cartItems[index] = item.copy(quantity = item.quantity - 1)
                        } else {
                            cartItems.removeAt(index)
                        }
                    }
                },
                onRemoveItem = { item ->
                    cartItems.remove(item)
                },
                onBackClick = { navController.popBackStack() },
                onCheckoutClick = {
                    // DESCONTAR EL STOCK DEL CATÁLOGO REAL
                    cartItems.forEach { cartItem ->
                        val index = booksList.indexOfFirst { it.id == cartItem.book.id }
                        if (index != -1) {
                            val currentBook = booksList[index]
                            val newStock = (currentBook.stock - cartItem.quantity).coerceAtLeast(0)
                            booksList[index] = currentBook.copy(stock = newStock)
                        }
                    }

                    // Limpiar el carrito y regresar al inicio
                    cartItems.clear()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}