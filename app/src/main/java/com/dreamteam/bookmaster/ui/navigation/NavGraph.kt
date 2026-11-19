package com.dreamteam.bookmaster.ui.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dreamteam.bookmaster.ui.screens.*
import com.dreamteam.bookmaster.ui.viewmodels.ConnectBD
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.ui.theme.BookMasterDarkPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterLightPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterBackground

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object BookDetail : Screen("book_detail")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object ReceiptPreview : Screen("receipt_preview")
    object History : Screen("history_screen")
    object Profile : Screen("profile_screen")
    object CompleteProfile : Screen("complete_profile")
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

    // Catálogo real: los IDs son los documentos de Firestore (libro_001 ... libro_018).
    val booksList = remember { mutableStateListOf<Book>() }
    var edadUsuario by remember { mutableStateOf<Int?>(null) }
    var cargandoCatalogo by remember { mutableStateOf(false) }
    var errorCatalogo by remember { mutableStateOf<String?>(null) }

    val cartItems = remember { mutableStateListOf<CartItem>() }
    var receiptPreview by remember { mutableStateOf<OrderReceiptPreview?>(null) }

    // Para este prototipo, la edad limita la visibilidad y el carrito en el cliente.
    // No sustituye validaciones seguras del servidor para compras reales.

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
                if (libro != null && edadUsuario != null && libro.minimumAge <= edadUsuario!!) {
                    cartItems.add(CartItem(libro, producto.cantidad))
                }
            }
            usuarioDelCarrito = uid
        }
    }

    fun calcularEdad(fecha: String): Int? = try {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formato.isLenient = false
        val nacimiento = formato.parse(fecha) ?: throw IllegalArgumentException("Fecha inválida")
        val n = Calendar.getInstance().apply { time = nacimiento }
        val hoy = Calendar.getInstance()
        if (n.after(hoy)) null else {
            var edad = hoy.get(Calendar.YEAR) - n.get(Calendar.YEAR)
            val mesActual = hoy.get(Calendar.MONTH)
            val diaActual = hoy.get(Calendar.DAY_OF_MONTH)
            val mesNacimiento = n.get(Calendar.MONTH)
            val diaNacimiento = n.get(Calendar.DAY_OF_MONTH)
            if (mesActual < mesNacimiento ||
                (mesActual == mesNacimiento && diaActual < diaNacimiento)) edad--
            edad.takeIf { it in 0..120 }
        }
    } catch (_: Exception) { null }

    fun cargarCatalogoDeUsuario() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        booksList.clear()
        edadUsuario = null
        errorCatalogo = null
        cargandoCatalogo = true
        Firebase.firestore.collection("users").document(uid).get()
            .addOnSuccessListener { perfil ->
                if (Firebase.auth.currentUser?.uid != uid) return@addOnSuccessListener
                val edad = calcularEdad(perfil.getString("birthDate").orEmpty())
                edadUsuario = edad
                if (edad == null) {
                    cargandoCatalogo = false
                    errorCatalogo = "No pudimos verificar tu fecha de nacimiento. Revisá tu perfil."
                    return@addOnSuccessListener
                }
                Firebase.firestore.collection("books").get()
                    .addOnSuccessListener { snapshot ->
                        if (Firebase.auth.currentUser?.uid != uid) return@addOnSuccessListener
                        val libros = snapshot.documents.mapNotNull { doc ->
                            val minimo = doc.getLong("minimumAge")?.toInt() ?: return@mapNotNull null
                            val precio = doc.getDouble("price") ?: return@mapNotNull null
                            val stock = doc.getLong("stock")?.toInt() ?: return@mapNotNull null
                            if (minimo > edad || minimo < 0 || precio < 0 || stock < 0) return@mapNotNull null
                            Book(
                                id = doc.id,
                                title = doc.getString("title").orEmpty(),
                                author = doc.getString("author").orEmpty(),
                                category = doc.getString("category").orEmpty(),
                                rating = doc.getDouble("rating") ?: 0.0,
                                price = precio,
                                stock = stock,
                                colorBackground = BookMasterLightPurple,
                                minimumAge = minimo,
                                publicationYear = doc.getLong("publicationYear")?.toInt() ?: 0,
                                description = doc.getString("description").orEmpty(),
                                imageUrl = doc.getString("imageUrl").orEmpty()
                            )
                        }.filter { it.title.isNotBlank() }.sortedBy { it.id }
                        booksList.clear()
                        booksList.addAll(libros)
                        cargandoCatalogo = false
                        // Recuperar carrito cuando ya conocemos los IDs y libros permitidos.
                        cargarCarritoDeUsuario()
                    }
                    .addOnFailureListener { error ->
                        if (Firebase.auth.currentUser?.uid == uid) {
                            cargandoCatalogo = false
                            errorCatalogo = "No se pudo cargar el catálogo: ${error.localizedMessage}"
                        }
                    }
            }
            .addOnFailureListener { error ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    cargandoCatalogo = false
                    errorCatalogo = "No se pudo verificar tu perfil: ${error.localizedMessage}"
                }
            }
    }

    // Guardar PRIMERO en Firestore y luego actualizar la interfaz: si falla,
    // la selección local permanece intacta y se puede volver a intentar.
    fun cambiarCantidad(libroId: String, cantidadNueva: Int) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null || uid != usuarioDelCarrito || cargandoCarrito || cargandoCatalogo || guardandoCambio) {
            avisar("Esperá a que se cargue tu carrito")
            return
        }
        if (cantidadNueva < 0) return
        if (cantidadNueva > 0 && (edadUsuario == null ||
                    booksList.none { it.id == libroId && it.minimumAge <= edadUsuario!! })) {
            avisar("Este libro no está disponible para tu edad")
            return
        }
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

    // La barra inferior pertenece únicamente al módulo cliente.
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val rutasPrincipales = setOf(Screen.Home.route, Screen.History.route, Screen.Profile.route)
    val mostrarBarra = rutaActual in rutasPrincipales
    val moradoOscuro = BookMasterDarkPurple
    // La posición se conserva al navegar entre Inicio y Detalles del libro.
    var carritoX by remember { mutableFloatStateOf(-1f) }
    var carritoY by remember { mutableFloatStateOf(-1f) }
    var arrastrandoCarrito by remember { mutableStateOf(false) }
    val cantidadCarrito = cartItems.sumOf { it.quantity }
    val escalaCarrito = remember { Animatable(1f) }
    var cantidadAnterior by remember { mutableStateOf<Int?>(null) }

    // El contador solo aumenta después de que cambiarCantidad confirmó el guardado.
    LaunchedEffect(cantidadCarrito) {
        val anterior = cantidadAnterior
        cantidadAnterior = cantidadCarrito
        if (anterior != null && cantidadCarrito > anterior) {
            escalaCarrito.animateTo(1.18f, spring(dampingRatio = 0.5f, stiffness = 700f))
            escalaCarrito.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
        }
    }


    Scaffold(
        containerColor = BookMasterBackground,
        // El Login y Registro ya aplican sus propios márgenes de barras del sistema.
        contentWindowInsets = if (mostrarBarra) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(containerColor = moradoOscuro) {
                    listOf(
                        Screen.History.route to "bell",
                        Screen.Home.route to "home",
                        Screen.Profile.route to "person"
                    ).forEach { (ruta, dibujo) ->
                        NavigationBarItem(
                            selected = rutaActual == ruta,
                            onClick = {
                                if (rutaActual != ruta) {
                                    navController.navigate(ruta) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                ClienteNavigationIcon(tipo = dibujo, color = Color.White)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White,
                                indicatorColor = BookMasterPurple
                            )
                        )
                    }
                }
            }
        },
    ) { paddingPantalla ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingPantalla)
        ) {
            val density = LocalDensity.current
            val anchoArea = with(density) { maxWidth.toPx() }
            val altoArea = with(density) { maxHeight.toPx() }
            val ladoToque = with(density) { 88.dp.toPx() }
            val maxX = (anchoArea - ladoToque).coerceAtLeast(0f)
            val maxY = (altoArea - ladoToque).coerceAtLeast(0f)
            val posicionX = (if (carritoX < 0f) maxX
            else carritoX).coerceIn(0f, maxX)
            val posicionY = (if (carritoY < 0f) maxY * 0.72f
            else carritoY).coerceIn(0f, maxY)
            // Mientras se arrastra sigue el dedo; al soltarlo se desliza al borde más cercano.
            val posicionXVisual by animateFloatAsState(
                targetValue = posicionX,
                animationSpec = if (arrastrandoCarrito) snap() else tween(durationMillis = 220),
                label = "CarritoPegadoAlBorde"
            )

            NavHost(
                navController = navController,
                startDestination = Screen.Welcome.route,
                modifier = Modifier.fillMaxSize()
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
                            cargarCatalogoDeUsuario()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                        onCompleteProfileNeeded = {
                            navController.navigate(Screen.CompleteProfile.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.CompleteProfile.route) {
                    CompleteProfileScreen(onComplete = {
                        cargarCatalogoDeUsuario()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                        }
                    })
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            cargarCatalogoDeUsuario()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        },
                        onCompleteProfileNeeded = {
                            navController.navigate(Screen.CompleteProfile.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    if (cargandoCatalogo) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BookMasterPurple)
                        }
                    } else if (errorCatalogo != null) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(errorCatalogo.orEmpty(), color = BookMasterDarkPurple)
                        }
                    } else HomeScreen(
                        booksList = booksList,
                        onBookClick = { book ->
                            selectedBookId = book.id
                            navController.navigate(Screen.BookDetail.route)
                        }
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
                        onClearCart = {
                            val uid = Firebase.auth.currentUser?.uid
                            if (uid == null || uid != usuarioDelCarrito || cargandoCarrito || cargandoCatalogo || guardandoCambio) {
                                avisar("Esperá a que tu carrito esté listo")
                            } else {
                                guardandoCambio = true
                                val collection = Firebase.firestore.collection("users").document(uid).collection("cart")
                                collection.get()
                                    .addOnSuccessListener { snapshot ->
                                        if (Firebase.auth.currentUser?.uid != uid) {
                                            guardandoCambio = false
                                        } else if (snapshot.size() > 500) {
                                            guardandoCambio = false
                                            avisar("El carrito excede el límite de vaciado; eliminá algunos libros primero")
                                        } else if (snapshot.documents.isEmpty()) {
                                            cartItems.clear()
                                            guardandoCambio = false
                                        } else {
                                            val batch = Firebase.firestore.batch()
                                            snapshot.documents.forEach { batch.delete(it.reference) }
                                            batch.commit()
                                                .addOnSuccessListener {
                                                    guardandoCambio = false
                                                    if (Firebase.auth.currentUser?.uid == uid) {
                                                        cartItems.clear()
                                                        avisar("Carrito vaciado correctamente")
                                                    }
                                                }
                                                .addOnFailureListener { error ->
                                                    guardandoCambio = false
                                                    avisar("No se pudo vaciar el carrito: ${error.localizedMessage}")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        guardandoCambio = false
                                        avisar("No se pudo consultar el carrito: ${error.localizedMessage}")
                                    }
                            }
                        },
                        onBackClick = { navController.popBackStack() },
                        onCheckoutClick = {
                            val uid = Firebase.auth.currentUser?.uid
                            if (uid == null || uid != usuarioDelCarrito || cargandoCarrito || cargandoCatalogo || guardandoCambio) {
                                avisar("Esperá a que tu carrito esté listo")
                            } else if (cartItems.isEmpty()) {
                                avisar("El carrito está vacío")
                            } else {
                                // Etapas 1 y 2: navegar al Checkout SIN registrar ni vaciar el pedido.
                                navController.navigate(Screen.Checkout.route) { launchSingleTop = true }
                            }
                        }
                    )
                }


                // Checkout de preparación: aún no genera pedidos, cobros ni modifica el carrito.
                composable(Screen.Checkout.route) {
                    CheckoutScreen(
                        cartItems = cartItems,
                        onBackClick = { navController.popBackStack() },
                        onPreviewReceipt = { preview ->
                            // Firestore ya confirmó el pedido y vació los documentos del carrito.
                            cartItems.clear()
                            // Refrescar el catálogo para mostrar las existencias ya descontadas.
                            // No se recarga el carrito aquí porque ya se vació en la transacción.
                            val uidActual = Firebase.auth.currentUser?.uid
                            Firebase.firestore.collection("books").get()
                                .addOnSuccessListener { snapshot ->
                                    if (Firebase.auth.currentUser?.uid != uidActual) return@addOnSuccessListener
                                    val nuevosStocks = snapshot.documents.mapNotNull { doc ->
                                        doc.getLong("stock")?.toInt()?.let { doc.id to it }
                                    }.toMap()
                                    for (i in booksList.indices) {
                                        val libro = booksList[i]
                                        nuevosStocks[libro.id]?.let { nuevoStock ->
                                            booksList[i] = libro.copy(stock = nuevoStock)
                                        }
                                    }
                                }
                            receiptPreview = preview
                            navController.navigate(Screen.ReceiptPreview.route) { launchSingleTop = true }
                        }
                    )
                }

                // ETAPA 1: recibo de vista previa sin escritura de pedidos ni cobro.
                composable(Screen.ReceiptPreview.route) {
                    val preview = receiptPreview
                    if (preview == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        OrderReceiptScreen(
                            receipt = preview,
                            onBackClick = { navController.popBackStack() },
                            onHomeClick = {
                                receiptPreview = null
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                composable(Screen.History.route) {
                    HistoryScreen(onBackClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } })
                }

                // Perfil del cliente: fotografía y nombres editables; nacimiento solo lectura.
                composable(Screen.Profile.route) {
                    ProfileScreen(onLogout = {
                        Firebase.auth.signOut()
                        cartItems.clear() // No elimina el carrito guardado en Firestore.
                        receiptPreview = null
                        usuarioDelCarrito = null
                        selectedBookId = null
                        edadUsuario = null
                        booksList.clear()
                        errorCatalogo = null
                        cargandoCatalogo = false
                        cargandoCarrito = false
                        guardandoCambio = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
                }
            }

            // Carrito visible en Inicio y Detalles. Se acopla al lado izquierdo o derecho.
            if (rutaActual == Screen.Home.route || rutaActual == Screen.BookDetail.route) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(posicionXVisual.roundToInt(), posicionY.roundToInt()) }
                        .size(88.dp)
                        .zIndex(5f)
                        .pointerInput(maxX, maxY) {
                            detectDragGestures(
                                onDragStart = {
                                    carritoX = posicionXVisual
                                    arrastrandoCarrito = true
                                },
                                onDragEnd = {
                                    arrastrandoCarrito = false
                                    carritoX = if (carritoX < maxX / 2f) 0f else maxX
                                },
                                onDragCancel = {
                                    arrastrandoCarrito = false
                                    carritoX = if (carritoX < maxX / 2f) 0f else maxX
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    carritoX = (carritoX + drag.x).coerceIn(0f, maxX)
                                    carritoY = (if (carritoY < 0f) posicionY else carritoY)
                                        .plus(drag.y).coerceIn(0f, maxY)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Escalamos círculo y contador juntos cuando se confirma un libro nuevo.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = escalaCarrito.value
                                scaleY = escalaCarrito.value
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(BookMasterLightPurple)
                                .border(5.dp, moradoOscuro, CircleShape)
                                .clickable { navController.navigate(Screen.Cart.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_cart),
                                contentDescription = "Abrir carrito",
                                tint = moradoOscuro,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // La insignia ahora se monta SOBRE el círculo, en la esquina superior derecha.
                        if (cantidadCarrito > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-3).dp, y = 3.dp)
                                    .size(30.dp)
                                    .background(Color(0xFFF28C28), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cantidadCarrito.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    } // Scaffold

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


/**
 * Íconos Material Symbols para la navegación del cliente.
 */
@Composable
private fun ClienteNavigationIcon(
    tipo: String,
    color: Color
) {
    val icono = when (tipo) {
        "bell" -> R.drawable.ic_nav_notifications
        "home" -> R.drawable.ic_nav_home
        "person" -> R.drawable.ic_person_profile
        "cart" -> R.drawable.ic_nav_cart
        else -> R.drawable.ic_nav_home
    }

    Icon(
        painter = painterResource(id = icono),
        contentDescription = when (tipo) {
            "bell" -> "Notificaciones"
            "home" -> "Inicio"
            "person" -> "Perfil"
            "cart" -> "Carrito"
            else -> null
        },
        tint = color,
        modifier = Modifier.size(32.dp)
    )
}