package com.dreamteam.bookmaster.ui.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamteam.bookmaster.ui.screens.*
import com.dreamteam.bookmaster.ui.viewmodels.ConnectBD
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object BookDetail : Screen("book_detail")
    object Cart : Screen("cart")
    object History : Screen("history_screen")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val bd = remember { ConnectBD() }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    var cargandoCarrito by remember { mutableStateOf(false) }
    var guardandoCambio by remember { mutableStateOf(false) }
    var usuarioDelCarrito by remember { mutableStateOf<String?>(null) }

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

    val cartItems = remember { mutableStateListOf<CartItem>() }

    // Inventario temporal local: no dependemos de la colección books.
    // PENDIENTE: definir la colección y permisos con el administrador Firebase.

    fun avisar(mensaje: String) {
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
    }

    // Al entrar con una cuenta, recuperamos exclusivamente SU carrito remoto.
    // Mientras se carga, el diálogo impide modificar o vaciar accidentalmente el carrito.
    fun cargarCarritoDeUsuario() {
        val uid = Firebase.auth.currentUser?.uid
        cartItems.clear()
        usuarioDelCarrito = null
        if (uid == null) {
            avisar("Iniciá sesión para recuperar tu carrito")
            return
        }

        cargandoCarrito = true
        bd.obtenerCarrito { productos, error ->
            // Impide que una respuesta tardía de la cuenta anterior contamine la nueva.
            if (Firebase.auth.currentUser?.uid != uid) return@obtenerCarrito
            cargandoCarrito = false
            if (productos == null) {
                avisar("No se pudo recuperar el carrito: ${error ?: "error desconocido"}. Volvé a iniciar sesión para reintentar.")
                return@obtenerCarrito
            }
            cartItems.clear()
            productos.forEach { producto ->
                val libro = booksList.find { it.id == producto.libroId }
                if (libro != null) {
                    cartItems.add(CartItem(libro, producto.cantidad))
                }
            }
            usuarioDelCarrito = uid
        }
    }

    // Guardar PRIMERO en Firestore y luego actualizar la interfaz: si falla,
    // la selección local permanece intacta y se puede volver a intentar.
    fun cambiarCantidad(libroId: String, cantidadNueva: Int) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null || uid != usuarioDelCarrito || cargandoCarrito || guardandoCambio) {
            avisar("Esperá a que se cargue tu carrito")
            return
        }
        if (cantidadNueva < 0) return
        val stock = booksList.find { it.id == libroId }?.stock ?: 0
        if (cantidadNueva > stock) {
            avisar("No hay suficientes existencias")
            return
        }
        guardandoCambio = true
        val onResultado: (Boolean, String?) -> Unit = { correcto, error ->
            guardandoCambio = false
            if (Firebase.auth.currentUser?.uid == uid && usuarioDelCarrito == uid) {
                if (correcto) {
                    val index = cartItems.indexOfFirst { it.book.id == libroId }
                    if (cantidadNueva == 0) {
                        if (index >= 0) cartItems.removeAt(index)
                    } else {
                        val libro = booksList.find { it.id == libroId }
                        if (libro != null) {
                            if (index >= 0) {
                                cartItems[index] = cartItems[index].copy(quantity = cantidadNueva)
                            } else {
                                cartItems.add(CartItem(libro, cantidadNueva))
                            }
                        }
                    }
                } else {
                    avisar("No se guardó el cambio: ${error ?: "error de conexión"}")
                }
            }
        }
        if (cantidadNueva == 0) {
            bd.eliminarProductoCarrito(libroId, onResultado)
        } else {
            bd.guardarProductoCarrito(libroId, cantidadNueva, onResultado)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                } },
                onRegister = { navController.navigate(Screen.Register.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                } }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    cargarCarritoDeUsuario()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    cargarCarritoDeUsuario()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                booksList = booksList,
                onLogout = {
                    Firebase.auth.signOut()
                    cartItems.clear() // Solo memoria local: NO se elimina el carrito remoto.
                    usuarioDelCarrito = null
                    selectedBookId = null
                    cargandoCarrito = false
                    guardandoCambio = false
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBookClick = { book ->
                    selectedBookId = book.id
                    navController.navigate(Screen.BookDetail.route)
                },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                cartItemCount = cartItems.sumOf { it.quantity }
            )
        }

        composable(Screen.BookDetail.route) {
            booksList.find { it.id == selectedBookId }?.let { book ->
                BookDetailScreen(
                    book = book,
                    onBackClick = { navController.popBackStack() },
                    onAddToCart = { addedBook, quantity ->
                        val actual = cartItems.find { it.book.id == addedBook.id }?.quantity ?: 0
                        val nueva = actual + quantity

                        when {
                            quantity <= 0 -> avisar("Seleccioná una cantidad válida")
                            nueva > addedBook.stock -> avisar(
                                "Solo hay ${addedBook.stock} ejemplares disponibles. Ya tenés $actual en el carrito."
                            )
                            else -> cambiarCantidad(addedBook.id, nueva)
                        }
                    }
                )
            }
        }

        composable(Screen.Cart.route) {
            CartScreen(
                cartItems = cartItems,
                onIncreaseQuantity = { item ->
                    val stock = booksList.find { it.id == item.book.id }?.stock ?: 0
                    if (item.quantity < stock) cambiarCantidad(item.book.id, item.quantity + 1)
                    else avisar("No hay más existencias")
                },
                onDecreaseQuantity = { item ->
                    cambiarCantidad(item.book.id, (item.quantity - 1).coerceAtLeast(0))
                },
                onRemoveItem = { item -> cambiarCantidad(item.book.id, 0) },
                onBackClick = { navController.popBackStack() },
                onCheckoutClick = {
                    val uid = Firebase.auth.currentUser?.uid
                    if (uid == null || uid != usuarioDelCarrito || cargandoCarrito || guardandoCambio) {
                        avisar("Esperá a que tu carrito esté listo")
                    } else if (cartItems.isEmpty()) {
                        avisar("El carrito está vacío")
                    } else {
                        // Capturamos el estado local ANTES de iniciar una transacción, que
                        // Firestore puede ejecutar varias veces ante compras concurrentes.
                        val usuarioCompra = uid
                        val copiaDeCompra = cartItems.map { it.copy() }
                        guardandoCambio = true
                        val db = Firebase.firestore
                        val orden = db.collection("users").document(uid)
                            .collection("orders").document()
                        val referenciasCarrito = copiaDeCompra.map { item ->
                            db.collection("users").document(uid)
                                .collection("cart").document(item.book.id)
                        }

                        // Se confirma el pedido y se vacía el carrito en un solo lote.
                        // El stock sigue siendo local: esta versión no controla inventario compartido.
                        val total = copiaDeCompra.sumOf { it.book.price * it.quantity }
                        val unidades = copiaDeCompra.sumOf { it.quantity }
                        val resumen = copiaDeCompra.joinToString(", ") {
                            "${it.quantity}x ${it.book.title}"
                        }
                        val batch = db.batch()
                        batch.set(orden, mapOf(
                            "totalAmount" to total,
                            "itemsCount" to unidades,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "summary" to resumen
                        ))
                        referenciasCarrito.forEach { batch.delete(it) }
                        batch.commit()
                            .addOnSuccessListener {
                                guardandoCambio = false
                                if (Firebase.auth.currentUser?.uid == usuarioCompra &&
                                    usuarioDelCarrito == usuarioCompra) {
                                    copiaDeCompra.forEach { item ->
                                        val index = booksList.indexOfFirst { it.id == item.book.id }
                                        if (index >= 0) {
                                            val book = booksList[index]
                                            booksList[index] = book.copy(
                                                stock = (book.stock - item.quantity).coerceAtLeast(0)
                                            )
                                        }
                                    }
                                    cartItems.clear()
                                    avisar("Compra registrada correctamente")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                guardandoCambio = false
                                Log.e("NavGraph", "No se completó la compra", e)
                                avisar("Compra no realizada: ${e.localizedMessage ?: "error de Firebase"}. Tu carrito se conserva.")
                            }
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(onBackClick = { navController.popBackStack() })
        }
    }

    // Diálogo modal: impide cambios mientras se lee/escribe en Firestore,
    // evitando operaciones concurrentes que dejen cantidades desincronizadas.
    if (cargandoCarrito || guardandoCambio) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface {
                Box(modifier = Modifier.fillMaxSize(0.35f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}