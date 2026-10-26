package com.dreamteam.bookmaster.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dreamteam.bookmaster.ui.screens.*

/**
 * Define las rutas disponibles dentro de BookMaster.
 *
 * Cada pantalla tiene un identificador único que permite
 * realizar la navegación sin repetir nombres de rutas.
 */
sealed class Screen(val route: String) {

    // Pantalla inicial de bienvenida.
    object Welcome : Screen("welcome")

    // Pantallas de autenticación.
    object Login : Screen("login")
    object Register : Screen("register")

    // Pantalla principal y catálogo de libros.
    object Home : Screen("home")

    // Detalles de un libro seleccionado.
    object BookDetail : Screen("book_detail")

    // Carrito de compras.
    object Cart : Screen("cart")
}

/**
 * Administra la navegación principal de BookMaster.
 *
 * Conecta las pantallas de la aplicación y controla
 * temporalmente el catálogo, el carrito de compras
 * y la actualización de las existencias.
 */
@Composable
fun NavGraph() {

    // Controlador que permite cambiar entre pantallas.
    val navController = rememberNavController()

    // Guarda el identificador del libro seleccionado.
    var selectedBookId by remember {
        mutableStateOf<String?>(null)
    }

    /**
     * Catálogo de libros disponibles en BookMaster.
     *
     * mutableStateListOf permite que Compose actualice
     * la interfaz cuando cambian las existencias.
     */
    val booksList = remember {
        mutableStateListOf(

            Book(
                "1",
                "El Principito",
                "Antoine de Saint-Exupéry",
                "Ficción",
                4.8,
                12.99,
                15,
                Color(0xFF81BEF7)
            ),

            Book(
                "2",
                "Hábitos Atómicos",
                "James Clear",
                "Desarrollo",
                4.9,
                18.50,
                8,
                Color(0xFFFFB74D)
            ),

            Book(
                "3",
                "Cien Años de Soledad",
                "Gabriel García Márquez",
                "Novelas",
                4.7,
                15.00,
                12,
                Color(0xFFA1887F)
            ),

            Book(
                "4",
                "Sapiens",
                "Yuval Noah Harari",
                "Historia",
                4.6,
                22.00,
                5,
                Color(0xFF81C784)
            ),

            Book(
                "5",
                "1984",
                "George Orwell",
                "Ficción",
                4.8,
                14.25,
                0,
                Color(0xFFE57373)
            ),

            Book(
                "6",
                "El Monje que Vendió su Ferrari",
                "Robin Sharma",
                "Desarrollo",
                4.5,
                16.80,
                20,
                Color(0xFFBA68C8)
            )
        )
    }

    /**
     * Lista de productos agregados al carrito.
     *
     * Al utilizar una lista de estado, Compose puede
     * detectar las modificaciones y actualizar los totales.
     */
    val cartItems = remember {
        mutableStateListOf<CartItem>()
    }

    /**
     * NavHost contiene las rutas de navegación.
     *
     * La pantalla Welcome se establece como punto
     * de inicio cuando se abre la aplicación.
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {

        // PANTALLA DE BIENVENIDA
        composable(Screen.Welcome.route) {

            WelcomeScreen(

                // Abre el inicio de sesión y retira Welcome del historial.
                onLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) {
                            inclusive = true
                        }
                    }
                },

                // Permite acceder directamente al registro.
                onRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Welcome.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // PANTALLA DE INICIO DE SESIÓN
        composable(Screen.Login.route) {

            LoginScreen(

                // Si la autenticación es correcta, abre el catálogo.
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },

                // Abre el formulario para crear una cuenta nueva.
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // PANTALLA DE REGISTRO
        composable(Screen.Register.route) {

            RegisterScreen(

                // Después del registro, dirige al usuario al catálogo.
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },

                // Regresa a la pantalla anterior.
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // PANTALLA PRINCIPAL
        composable(Screen.Home.route) {

            HomeScreen(
                booksList = booksList,

                // Cierra la vista actual y regresa al inicio de sesión.
                onLogout = {
                    // Cierra la sesión real de Firebase antes de volver al login.
                    FirebaseAuth.getInstance().signOut()
                    selectedBookId = null
                    cartItems.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                },

                // Guarda el libro seleccionado antes de abrir sus detalles.
                onBookClick = { book ->
                    selectedBookId = book.id

                    navController.navigate(
                        Screen.BookDetail.route
                    )
                },

                // Abre el carrito de compras.
                onCartClick = {
                    navController.navigate(Screen.Cart.route)
                },

                // Calcula la cantidad total de ejemplares en el carrito.
                cartItemCount = cartItems.sumOf {
                    it.quantity
                }
            )
        }

        // PANTALLA DE DETALLES DEL LIBRO
        composable(Screen.BookDetail.route) {

            // Busca el libro seleccionado dentro del catálogo.
            val currentBook = booksList.find {
                it.id == selectedBookId
            }

            currentBook?.let { book ->

                BookDetailScreen(
                    book = book,

                    // Regresa a la pantalla anterior.
                    onBackClick = {
                        navController.popBackStack()
                    },

                    /**
                     * Agrega un libro al carrito.
                     *
                     * Si ya existe, aumenta su cantidad.
                     * Si no existe, crea un nuevo producto.
                     */
                    onAddToCart = { addedBook, quantity ->

                        // Consulta las existencias actuales del catálogo.
                        val availableStock = booksList.find {
                            it.id == addedBook.id
                        }?.stock ?: 0

                        // Busca si el libro ya fue agregado al carrito.
                        val existingIndex = cartItems.indexOfFirst {
                            it.book.id == addedBook.id
                        }

                        if (existingIndex != -1) {

                            val existingItem = cartItems[existingIndex]

                            // Calcula la cantidad sin superar las existencias.
                            val newQuantity = (
                                    existingItem.quantity + quantity
                                    ).coerceAtMost(availableStock)

                            // Reemplaza el elemento para actualizar la interfaz.
                            cartItems[existingIndex] = existingItem.copy(
                                quantity = newQuantity
                            )

                        } else if (quantity > 0 && availableStock > 0) {

                            // Agrega un nuevo libro con una cantidad válida.
                            cartItems.add(
                                CartItem(
                                    addedBook,
                                    quantity.coerceAtMost(availableStock)
                                )
                            )
                        }
                    }
                )
            }
        }

        // PANTALLA DEL CARRITO DE COMPRAS
        composable(Screen.Cart.route) {

            CartScreen(
                cartItems = cartItems,

                /**
                 * Aumenta la cantidad de un producto.
                 * Comprueba que todavía existan ejemplares disponibles.
                 */
                onIncreaseQuantity = { item ->

                    val index = cartItems.indexOf(item)

                    val bookInCatalog = booksList.find {
                        it.id == item.book.id
                    }

                    if (
                        index != -1 &&
                        bookInCatalog != null &&
                        item.quantity < bookInCatalog.stock
                    ) {
                        cartItems[index] = item.copy(
                            quantity = item.quantity + 1
                        )
                    }
                },

                /**
                 * Disminuye la cantidad seleccionada.
                 *
                 * Si queda una sola unidad y se disminuye,
                 * el producto se elimina del carrito.
                 */
                onDecreaseQuantity = { item ->

                    val index = cartItems.indexOf(item)

                    if (index != -1) {

                        if (item.quantity > 1) {

                            cartItems[index] = item.copy(
                                quantity = item.quantity - 1
                            )

                        } else {

                            cartItems.removeAt(index)
                        }
                    }
                },

                // Elimina completamente un libro del carrito.
                onRemoveItem = { item ->
                    cartItems.remove(item)
                },

                // Regresa a la pantalla anterior.
                onBackClick = {
                    navController.popBackStack()
                },

                /**
                 * Procesa la compra de los libros seleccionados.
                 *
                 * Descuenta las cantidades del catálogo,
                 * vacía el carrito y regresa a la pantalla principal.
                 */
                onCheckoutClick = {

                    // Recorre todos los productos del carrito.
                    cartItems.forEach { cartItem ->

                        // Busca la posición del libro en el catálogo.
                        val index = booksList.indexOfFirst {
                            it.id == cartItem.book.id
                        }

                        if (index != -1) {

                            val currentBook = booksList[index]

                            // Calcula las existencias después de la compra.
                            val newStock = (
                                    currentBook.stock - cartItem.quantity
                                    ).coerceAtLeast(0)

                            // Actualiza el libro con su nueva cantidad.
                            booksList[index] = currentBook.copy(
                                stock = newStock
                            )
                        }
                    }

                    // Vacía el carrito después de procesar la compra.
                    cartItems.clear()

                    // Regresa al catálogo y limpia la navegación anterior.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}