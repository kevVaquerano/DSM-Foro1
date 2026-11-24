package com.dreamteam.bookmaster.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.rotate
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.dreamteam.bookmaster.data.LocalProfilePhotoStore
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import com.dreamteam.bookmaster.R
import com.google.firebase.firestore.Source
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val AdminBlue = Color(0xFF2864B4)
private val AdminNavy = Color(0xFF173B73)
private val AdminBackground = Color(0xFFF1F6FD)

/** Dashboard inicial: las cinco funciones tendrán sus propias pantallas en la siguiente fase. */
@Composable
fun AdminDashboardScreen(onLogout: () -> Unit, onAddBook: () -> Unit, onEditBook: () -> Unit) {
    var adminTab by remember { mutableStateOf(0) } // 0 = Inicio, 1 = Perfil
    var salesOpen by remember { mutableStateOf(false) }
    var bookListOpen by remember { mutableStateOf(false) }
    var stockAlertsOpen by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("Administrador") }
    var notice by remember { mutableStateOf<String?>(null) }
    val uid = Firebase.auth.currentUser?.uid
    // Hora local del dispositivo, sin depender de la hora del servidor.
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Buenos días"
        in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    LaunchedEffect(uid) {
        if (uid != null) {
            Firebase.firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (Firebase.auth.currentUser?.uid == uid && doc.getString("role") == "admin") {
                        firstName = doc.getString("firstName")?.takeIf { it.isNotBlank() } ?: "Administrador"
                    }
                }
        }
    }

    if (salesOpen) {
        AdminSalesReport(onBack = { salesOpen = false })
        return
    }
    if (bookListOpen) {
        AdminBookList(onBack = { bookListOpen = false })
        return
    }

    if (stockAlertsOpen) {
        AdminStockAlerts(onBack = { stockAlertsOpen = false })
        return
    }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        // La barra es exclusiva del administrador. No usa rutas ni membresías del cliente.
        if (adminTab == 0) {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BookMaster", color = AdminNavy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Administración", color = AdminBlue, fontSize = 13.sp,
                        modifier = Modifier.offset(y = 4.dp))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("¡$greeting, $firstName!", color = AdminNavy,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("¿Qué deseás hacer hoy?", color = AdminNavy,
                    fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AdminTile("Agregar un nuevo libro", R.drawable.library_add,
                        Modifier.weight(1f), onClick = onAddBook)
                    AdminTile("Editar un libro", R.drawable.edit_square,
                        Modifier.weight(1f), onClick = onEditBook)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AdminTile("Información de ventas", R.drawable.analytics,
                        Modifier.weight(1f)) { salesOpen = true }
                    AdminTile("Listado de libros", R.drawable.collections_bookmark,
                        Modifier.weight(1f)) { bookListOpen = true }
                }
                // Se reutiliza la campana que ya utiliza el módulo de consumidor.
                AdminTile("Alertas de stock", R.drawable.ic_nav_notifications,
                    Modifier.width(165.dp)) { stockAlertsOpen = true }
                notice?.let { Text(it, color = AdminBlue, textAlign = TextAlign.Center) }
            }
        } else {
            AdminProfileTab(
                modifier = Modifier.weight(1f),
                onLogout = onLogout,
                onNamesSaved = { firstName = it }
            )
        }
        NavigationBar(
            containerColor = AdminNavy,
            tonalElevation = 3.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            NavigationBarItem(
                selected = adminTab == 0,
                onClick = { adminTab = 0 },
                icon = { Icon(painterResource(R.drawable.ic_nav_home), contentDescription = "Inicio", tint = Color.White, modifier = Modifier.size(32.dp)) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AdminBlue,
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White
                )
            )
            NavigationBarItem(
                selected = adminTab == 1,
                onClick = { adminTab = 1 },
                icon = { Icon(painterResource(R.drawable.ic_person_profile), contentDescription = "Perfil", tint = Color.White, modifier = Modifier.size(32.dp)) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AdminBlue,
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White
                )
            )
        }
    }
}

/** Perfil administrativo con la distribución del perfil cliente, sin datos de compras. */
@Composable
private fun AdminProfileTab(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onNamesSaved: (String) -> Unit
) {
    val user = Firebase.auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstName by remember(user?.uid) { mutableStateOf("") }
    var lastName by remember(user?.uid) { mutableStateOf("") }
    var savedFirstName by remember(user?.uid) { mutableStateOf("") }
    var savedLastName by remember(user?.uid) { mutableStateOf("") }
    var loading by remember(user?.uid) { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var error by remember(user?.uid) { mutableStateOf<String?>(null) }
    var photo by remember(user?.uid) { mutableStateOf<Bitmap?>(null) }
    var editingPhoto by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            loading = false
            error = "Iniciá sesión para ver tu perfil."
            return@LaunchedEffect
        }
        photo = withContext(Dispatchers.IO) { LocalProfilePhotoStore.load(context, user.uid) }
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (Firebase.auth.currentUser?.uid != user.uid) return@addOnSuccessListener
                if (doc.getString("role") != "admin" || doc.getString("uid") != user.uid) {
                    error = "Esta cuenta no tiene perfil administrativo."
                } else {
                    firstName = doc.getString("firstName").orEmpty()
                    lastName = doc.getString("lastName").orEmpty()
                    savedFirstName = firstName
                    savedLastName = lastName
                    error = null
                }
                loading = false
            }
            .addOnFailureListener { ex ->
                if (Firebase.auth.currentUser?.uid == user.uid) {
                    error = "No se pudo consultar el perfil: ${ex.localizedMessage}"
                    loading = false
                }
            }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && user != null && !editingPhoto && !saving) {
            editingPhoto = true
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    LocalProfilePhotoStore.decodeSelectedPhoto(context, uri)
                }
                val saved = if (decoded != null) withContext(Dispatchers.IO) {
                    LocalProfilePhotoStore.save(context, user.uid, decoded)
                } else false
                if (saved) {
                    photo = decoded
                    error = null
                } else error = "No se pudo guardar la fotografía."
                editingPhoto = false
            }
        }
    }

    val cardBorder = BorderStroke(1.dp, AdminNavy.copy(alpha = 0.65f))
    val pale = Color(0xFFDCEBFA)
    Column(
        modifier = modifier.fillMaxWidth().background(AdminBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mi perfil", color = AdminNavy, fontSize = 27.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = cardBorder,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(104.dp)) {
                    Box(
                        modifier = Modifier.size(94.dp).align(Alignment.TopStart)
                            .clip(CircleShape).background(pale)
                            .clickable(enabled = !editingPhoto && !saving && user != null) {
                                photoPicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            editingPhoto -> CircularProgressIndicator(
                                color = AdminBlue, modifier = Modifier.size(30.dp))
                            photo != null -> Image(
                                bitmap = photo!!.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            else -> Icon(painterResource(R.drawable.ic_person_profile),
                                contentDescription = "Foto de perfil", tint = AdminNavy,
                                modifier = Modifier.size(48.dp))
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(34.dp)
                            .border(2.dp, Color.White, CircleShape).clip(CircleShape)
                            .background(AdminNavy)
                            .clickable(enabled = !editingPhoto && !saving && user != null) {
                                if (photo == null) photoPicker.launch("image/*")
                                else scope.launch {
                                    editingPhoto = true
                                    val removed = withContext(Dispatchers.IO) {
                                        LocalProfilePhotoStore.delete(context, user!!.uid)
                                    }
                                    if (removed) { photo = null; error = null }
                                    else error = "No se pudo quitar la fotografía."
                                    editingPhoto = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(if (photo == null) R.drawable.ic_camera_profile
                            else R.drawable.ic_delete_profile),
                            contentDescription = if (photo == null) "Agregar foto" else "Eliminar foto",
                            tint = Color.White, modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (loading) CircularProgressIndicator(color = AdminBlue)
                Text(
                    listOf(firstName, lastName).filter { it.isNotBlank() }
                        .joinToString(" ").ifBlank { "Administrador" },
                    fontSize = 19.sp, fontWeight = FontWeight.Bold,
                    color = AdminNavy, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (editing) {
                            firstName = savedFirstName
                            lastName = savedLastName
                        }
                        editing = !editing
                        error = null
                    },
                    enabled = !loading && !saving && user != null,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AdminBlue)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_profile_arrow),
                        contentDescription = null, tint = AdminBlue,
                        modifier = Modifier.size(20.dp).rotate(if (editing) 90f else 0f))
                    Spacer(Modifier.width(8.dp))
                    Text(if (editing) "Cancelar edición" else "Editar mis datos", color = AdminNavy)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = cardBorder,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Datos personales", color = AdminNavy,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (editing) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { if (it.length <= 60) { firstName = it; error = null } },
                        label = { Text("Nombres") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        isError = !adminValidName(firstName),
                        supportingText = if (!adminValidName(firstName)) {
                            { Text("Ingresá de 2 a 60 caracteres: letras, tildes y espacios.") }
                        } else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminBlue, focusedLabelColor = AdminBlue,
                            cursorColor = AdminBlue, focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { if (it.length <= 60) { lastName = it; error = null } },
                        label = { Text("Apellidos") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        isError = !adminValidName(lastName),
                        supportingText = if (!adminValidName(lastName)) {
                            { Text("Ingresá de 2 a 60 caracteres: letras, tildes y espacios.") }
                        } else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminBlue, focusedLabelColor = AdminBlue,
                            cursorColor = AdminBlue, focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White)
                    )
                    Button(
                        onClick = {
                            val activeUser = user ?: return@Button
                            if (!adminValidName(firstName) || !adminValidName(lastName)) {
                                error = "Ingresá nombres y apellidos válidos."
                                return@Button
                            }
                            if (Firebase.auth.currentUser?.uid != activeUser.uid) return@Button
                            saving = true
                            error = null
                            val newFirst = firstName.trim()
                            val newLast = lastName.trim()
                            // Solo se actualizan los nombres, sin modificar rol, correo ni otros datos.
                            Firebase.firestore.collection("users").document(activeUser.uid)
                                .update(mapOf("firstName" to newFirst, "lastName" to newLast))
                                .addOnSuccessListener {
                                    if (Firebase.auth.currentUser?.uid == activeUser.uid) {
                                        savedFirstName = newFirst
                                        savedLastName = newLast
                                        firstName = newFirst
                                        lastName = newLast
                                        editing = false
                                        saving = false
                                        onNamesSaved(newFirst)
                                        Toast.makeText(context, "Datos actualizados correctamente.",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { ex ->
                                    if (Firebase.auth.currentUser?.uid == activeUser.uid) {
                                        saving = false
                                        error = "No se pudieron guardar los nombres: ${ex.localizedMessage}"
                                    }
                                }
                        },
                        enabled = !saving && !loading && user != null &&
                                adminValidName(firstName) && adminValidName(lastName),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                        else Text("Guardar cambios", color = Color.White)
                    }
                } else {
                    Column {
                        Text("Nombres", fontSize = 12.sp, color = Color.Gray)
                        Text(firstName.ifBlank { "No disponible" },
                            fontWeight = FontWeight.Medium, color = AdminNavy)
                    }
                    HorizontalDivider(color = pale)
                    Column {
                        Text("Apellidos", fontSize = 12.sp, color = Color.Gray)
                        Text(lastName.ifBlank { "No disponible" },
                            fontWeight = FontWeight.Medium, color = AdminNavy)
                    }
                }
            }
        }

        error?.let { Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB44141))
        ) {
            Icon(painterResource(R.drawable.ic_profile_logout), null,
                tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text("¿Cerrar sesión?", color = AdminNavy,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            text = {
                Text("¿Seguro que querés cerrar sesión y volver al inicio de sesión?",
                    color = AdminNavy, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { confirmLogout = false },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminBlue)
                    ) {
                        Text("Cancelar", color = AdminNavy)
                    }

                    Spacer(Modifier.width(32.dp))

                    Button(
                        onClick = {
                            confirmLogout = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB44141)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar sesión", color = Color.White)
                    }
                }
            },
            dismissButton = {}
        )
    }
}

private fun adminValidName(value: String): Boolean {
    val clean = value.trim()
    return clean.length in 2..60 &&
            Regex("""^[\p{L}\p{M}]+(?:[ ][\p{L}\p{M}]+)*$""").matches(clean)
}

@Composable
private fun AdminTile(
    label: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(122.dp),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFFDCEBFA),
        contentColor = AdminNavy,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, AdminBlue.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = AdminNavy,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = label,
                color = AdminNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private data class SalesSlice(val category: String, val units: Int, val color: Color)

/** Reporte de pedidos de prueba no cancelados, por cantidad de ejemplares. */
@Composable
private fun AdminSalesReport(onBack: () -> Unit) {
    val adminUid = Firebase.auth.currentUser?.uid
    var loading by remember(adminUid) { mutableStateOf(true) }
    var error by remember(adminUid) { mutableStateOf<String?>(null) }
    var sales by remember(adminUid) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var countedOrders by remember(adminUid) { mutableIntStateOf(0) }

    fun refresh() {
        if (adminUid == null || Firebase.auth.currentUser?.uid != adminUid) {
            error = "Iniciá sesión como administrador para consultar el reporte."
            loading = false
            return
        }
        loading = true
        error = null
        val db = Firebase.firestore
        // Verificar el rol en el cliente; las reglas de Firestore también deben autorizar
        // la lectura collectionGroup(orders) para administradores.
        db.collection("users").document(adminUid).get(Source.SERVER)
            .addOnSuccessListener { profile ->
                if (Firebase.auth.currentUser?.uid != adminUid) return@addOnSuccessListener
                if (profile.getString("role") != "admin" || profile.getString("uid") != adminUid) {
                    loading = false
                    error = "Esta cuenta no tiene acceso administrativo al reporte."
                    return@addOnSuccessListener
                }
                db.collection("books").get(Source.SERVER)
                    .addOnSuccessListener { books ->
                        if (Firebase.auth.currentUser?.uid != adminUid) return@addOnSuccessListener
                        val categories = books.documents.associate { doc ->
                            doc.id to doc.getString("category").orEmpty().trim().ifBlank { "Sin categoría" }
                        }
                        db.collectionGroup("orders").get(Source.SERVER)
                            .addOnSuccessListener { orders ->
                                if (Firebase.auth.currentUser?.uid != adminUid) return@addOnSuccessListener
                                val totals = mutableMapOf<String, Int>()
                                var count = 0
                                for (order in orders.documents) {
                                    // Solo pedidos creados en users/{uid}/orders.
                                    val parent = order.reference.parent.parent
                                    if (parent?.parent?.id != "users") continue
                                    val status = order.getString("status")?.uppercase(Locale.ROOT).orEmpty()
                                    if (status in setOf("CANCELADO", "CANCELLED", "CANCELED")) continue
                                    if (status.isBlank()) continue
                                    val lines = order.get("items") as? List<*> ?: continue
                                    var hasUnits = false
                                    for (raw in lines) {
                                        val item = raw as? Map<*, *> ?: continue
                                        val id = item["bookId"] as? String ?: continue
                                        val amount = (item["quantity"] as? Number)?.toInt() ?: continue
                                        if (amount <= 0) continue
                                        val category = categories[id] ?: "Categoría no disponible"
                                        totals[category] = (totals[category] ?: 0) + amount
                                        hasUnits = true
                                    }
                                    if (hasUnits) count++
                                }
                                sales = totals
                                countedOrders = count
                                loading = false
                            }
                            .addOnFailureListener { e ->
                                loading = false
                                error = "No se pudieron consultar los pedidos de todos los clientes: ${e.localizedMessage}. Revisá los permisos de collectionGroup(orders) en Firestore."
                            }
                    }
                    .addOnFailureListener { e ->
                        loading = false
                        error = "No se pudo consultar el catálogo: ${e.localizedMessage}"
                    }
            }
            .addOnFailureListener { e ->
                loading = false
                error = "No se pudo verificar el administrador: ${e.localizedMessage}"
            }
    }

    LaunchedEffect(adminUid) { refresh() }
    val palette = listOf(
        Color(0xFF02A8D1), Color(0xFF3E8DDF), Color(0xFFFFA000),
        Color(0xFF86C900), Color(0xFF708080), Color(0xFF9874D8),
        Color(0xFFE56B6F), Color(0xFF1DAD91), Color(0xFFB48B59)
    )
    val slices = sales.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .mapIndexed { i, e -> SalesSlice(e.key, e.value, palette[i % palette.size]) }
    val total = slices.sumOf { it.units }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Surface(color = AdminNavy, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.width(54.dp)) {
                    Icon(painterResource(R.drawable.ic_book_detail_back), "Regresar", tint = Color.White)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Información de ventas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(54.dp))
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Pedidos realizados en BookMaster - por categorías", fontSize = 12.sp, color = AdminNavy, textAlign = TextAlign.Center)
            when {
                loading -> CircularProgressIndicator(color = AdminBlue)
                error != null -> {
                    Text(error.orEmpty(), color = Color(0xFFB42332), fontSize = 13.sp)
                    OutlinedButton(onClick = { refresh() }) { Text("Reintentar") }
                }
                total == 0 -> {
                    Text("Todavía no hay ejemplares registrados en pedidos no cancelados.", color = AdminNavy, textAlign = TextAlign.Center)
                    Button(
                        onClick = { refresh() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                    ) { Text("Actualizar reporte", fontWeight = FontWeight.Bold) }
                }
                else -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$total ejemplares · $countedOrders pedidos", fontWeight = FontWeight.SemiBold, color = AdminNavy)
                                Spacer(Modifier.height(18.dp))
                                Canvas(Modifier.size(265.dp)) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.minDimension / 2f
                                    var startAngle = -90f

                                    // Porcentajes en el interior, sin amontonar los segmentos pequeños.
                                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 15.sp.toPx()
                                        textAlign = Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.create(
                                            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
                                        )
                                    }
                                    val outlinePaint = Paint(textPaint).apply {
                                        color = android.graphics.Color.rgb(23, 59, 115)
                                        style = Paint.Style.STROKE
                                        strokeWidth = 2.5.dp.toPx()
                                        strokeJoin = Paint.Join.ROUND
                                    }
                                    slices.forEach { slice ->
                                        val percent = slice.units.toFloat() / total
                                        val sweep = percent * 360f
                                        drawArc(
                                            color = slice.color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = true
                                        )
                                        if (percent >= 0.04f) {
                                            val mid = Math.toRadians((startAngle + sweep / 2f).toDouble())
                                            val distance = radius * 0.62f
                                            val x = center.x + cos(mid).toFloat() * distance
                                            val y = center.y + sin(mid).toFloat() * distance -
                                                    (textPaint.ascent() + textPaint.descent()) / 2f
                                            val label = String.format(Locale.US, "%.0f%%", percent * 100f)
                                            drawIntoCanvas { canvas ->
                                                canvas.nativeCanvas.drawText(label, x, y, outlinePaint)
                                                canvas.nativeCanvas.drawText(label, x, y, textPaint)
                                            }
                                        }
                                        startAngle += sweep
                                    }
                                    // Líneas blancas de 2 dp entre las porciones, incluso en el centro.
                                    if (slices.size > 1) {
                                        var boundary = -90f
                                        slices.forEach { slice ->
                                            val radians = Math.toRadians(boundary.toDouble())
                                            drawLine(
                                                color = Color.White,
                                                start = center,
                                                end = Offset(
                                                    center.x + cos(radians).toFloat() * radius,
                                                    center.y + sin(radians).toFloat() * radius
                                                ),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                            boundary += slice.units.toFloat() / total * 360f
                                        }
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                            }
                            // Separador fuera del padding: alcanza ambos bordes de la tarjeta.
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = AdminBlue.copy(alpha = 0.30f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Categorías", color = AdminNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(12.dp))
                                slices.forEach { slice ->
                                    val pct = slice.units * 100.0 / total
                                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(16.dp).background(slice.color, RoundedCornerShape(3.dp)))
                                        Spacer(Modifier.width(10.dp))
                                        Text(slice.category, modifier = Modifier.weight(1f), color = AdminNavy, fontSize = 13.sp)
                                        Text(String.format(Locale.US, "%.1f%%", pct), color = AdminNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("(${slice.units})", color = AdminNavy, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { refresh() },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Actualizar reporte", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Mismos rangos que BookDetail: 0-9 rojo, 10 amarillo, 11+ verde.
private fun adminStockColor(stock: Long): Color = when {
    stock < 10L -> Color(0xFFC62828)
    stock == 10L -> Color(0xFF9A6700)
    else -> Color(0xFF2E7D32)
}

private data class AdminListedBook(
    val id: String,
    val title: String,
    val author: String,
    val stock: Long,
    val imageUrl: String
)

/** Portada local (si no está oculta), URL HTTPS, drawable original, marcador vacío. */
@Composable
private fun AdminListedBookCover(book: AdminListedBook) {
    val context = LocalContext.current
    val originalResource = remember(book.id) {
        context.resources.getIdentifier(book.id, "drawable", context.packageName)
    }
    var cover by remember(book.id, book.imageUrl) { mutableStateOf<Bitmap?>(null) }
    var hidden by remember(book.id, book.imageUrl) { mutableStateOf(false) }
    var checking by remember(book.id, book.imageUrl) { mutableStateOf(true) }

    LaunchedEffect(book.id, book.imageUrl) {
        checking = true
        cover = null
        val result = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "book_covers")
            val safeId = book.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(dir, "$safeId.jpg")
            val isHidden = File(file.absolutePath + ".hidden").exists()
            if (isHidden) return@withContext Pair(true, null)
            val local = try {
                if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
            } catch (_: Exception) { null }
            if (local != null) return@withContext Pair(false, local)
            val address = book.imageUrl.trim()
            if (!address.startsWith("https://", true)) return@withContext Pair(false, null)
            val remote = try {
                val conn = URL(address).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.instanceFollowRedirects = true
                try {
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else null
                } finally { conn.disconnect() }
            } catch (_: Exception) { null }
            Pair(false, remote)
        }
        hidden = result.first
        cover = result.second
        checking = false
    }

    Box(
        modifier = Modifier.size(width = 70.dp, height = 96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF0F3F8))
            .border(1.dp, AdminBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            cover != null -> Image(
                bitmap = cover!!.asImageBitmap(),
                contentDescription = "Portada de ${book.title}",
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
            !checking && !hidden && originalResource != 0 -> Image(
                painter = painterResource(originalResource),
                contentDescription = "Portada de ${book.title}",
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
            checking -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp), color = AdminBlue, strokeWidth = 2.dp
            )
            else -> Text("📖", fontSize = 26.sp)
        }
    }
}

@Composable
private fun AdminBookList(onBack: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid
    var books by remember(uid) { mutableStateOf<List<AdminListedBook>>(emptyList()) }
    var loading by remember(uid) { mutableStateOf(true) }
    var failure by remember(uid) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid, reloadKey) {
        if (uid == null) {
            loading = false
            failure = "Iniciá sesión para consultar el catálogo."
            return@LaunchedEffect
        }
        loading = true
        failure = null
        Firebase.firestore.collection("books").get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    books = snapshot.documents
                        .filter { it.getBoolean("isActive") != false }
                        .map { doc ->
                            AdminListedBook(
                                id = doc.id,
                                title = doc.getString("title").orEmpty().ifBlank { "Sin título" },
                                author = doc.getString("author").orEmpty().ifBlank { "Autor no disponible" },
                                stock = doc.getLong("stock") ?: 0L,
                                imageUrl = doc.getString("imageUrl").orEmpty()
                            )
                        }
                        .sortedBy { it.title.lowercase(Locale.ROOT) }
                    loading = false
                }
            }
            .addOnFailureListener { ex ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    loading = false
                    failure = "No se pudo cargar el listado: ${ex.localizedMessage}"
                }
            }
    }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Surface(color = AdminNavy, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(62.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.width(54.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_detail_back),
                        contentDescription = "Regresar", tint = Color.White
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Listado de libros", color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(54.dp))
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminBlue)
                }
                failure != null -> {
                    Text(failure.orEmpty(), color = Color(0xFFC62828), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { reloadKey++ }, colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)) {
                        Text("Reintentar")
                    }
                }
                else -> {
                    // Un solo marco exterior y divisiones finas, como en el boceto.
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(3f), contentAlignment = Alignment.Center) {
                                    Text("Libros", color = AdminNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Box(Modifier.width(1.dp).fillMaxHeight().background(AdminBlue.copy(alpha = 0.32f)))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text("Stock", color = AdminNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            HorizontalDivider(color = AdminBlue.copy(alpha = 0.32f), thickness = 1.dp)
                            if (books.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No hay libros activos registrados.", color = AdminNavy)
                                }
                            } else {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(books, key = { it.id }) { book ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(3f).padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                AdminListedBookCover(book)
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(book.title, color = AdminNavy,
                                                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(book.author, color = Color(0xFF626C7D), fontSize = 12.sp)
                                                }
                                            }
                                            Box(Modifier.width(1.dp).height(116.dp).background(AdminBlue.copy(alpha = 0.32f)))
                                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                                Text(book.stock.toString(), color = adminStockColor(book.stock),
                                                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                            }
                                        }
                                        HorizontalDivider(color = AdminBlue.copy(alpha = 0.24f), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Información sobre el stock", color = AdminNavy,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                            AdminStockLegend("0 a 9 ejemplares", Color(0xFFC62828))
                            AdminStockLegend("10 ejemplares", Color(0xFF9A6700))
                            AdminStockLegend("11 o más ejemplares", Color(0xFF2E7D32))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { reloadKey++ },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Actualizar listado", color = Color.White,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStockLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(13.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = AdminNavy, fontSize = 14.sp)
    }
}


/** Pedidos simulados guardados solo en este dispositivo, separados por administrador. */
private object LocalRestockOrders {
    private const val FILE = "bookmaster_restock_demo"

    fun read(context: android.content.Context, uid: String): Map<String, Int> {
        return try {
            val prefs = context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            val json = JSONObject(prefs.getString(uid, "{}") ?: "{}")
            val result = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val quantity = json.optInt(id, 0)
                if (quantity in 1..100000) result[id] = quantity
            }
            result
        } catch (_: Exception) { emptyMap() }
    }

    fun write(context: android.content.Context, uid: String, orders: Map<String, Int>) {
        val json = JSONObject()
        orders.forEach { (id, quantity) -> json.put(id, quantity) }
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .edit().putString(uid, json.toString()).apply()
    }
}

/** Alertas con los mismos límites que BookDetail: rojo 0–9, amarillo 10. */
@Composable
private fun AdminStockAlerts(onBack: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    var books by remember(uid) { mutableStateOf<List<AdminListedBook>>(emptyList()) }
    var loading by remember(uid) { mutableStateOf(true) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var info by remember(uid) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var pending by remember(uid) {
        mutableStateOf(if (uid == null) emptyMap() else LocalRestockOrders.read(context, uid))
    }
    var receivingId by remember(uid) { mutableStateOf<String?>(null) }

    fun savePending(next: Map<String, Int>) {
        val activeUid = uid ?: return
        pending = next
        LocalRestockOrders.write(context, activeUid, next)
    }

    fun receive(book: AdminListedBook) {
        val activeUid = uid ?: return
        val amount = pending[book.id] ?: return
        if (receivingId != null || Firebase.auth.currentUser?.uid != activeUid) return
        receivingId = book.id
        error = null
        info = null
        val bookRef = Firebase.firestore.collection("books").document(book.id)
        Firebase.firestore.runTransaction { transaction ->
            val current = transaction.get(bookRef)
            if (!current.exists() || current.getBoolean("isActive") == false) {
                throw IllegalStateException("Este libro ya no está disponible.")
            }
            val original = current.getLong("stock") ?: 0L
            if (original < 0L || original > 100000L || original + amount > 100000L) {
                throw IllegalStateException("El stock resultante supera el máximo permitido.")
            }
            transaction.update(bookRef, "stock", original + amount)
            original + amount
        }.addOnSuccessListener { updatedStock ->
            if (Firebase.auth.currentUser?.uid == activeUid) {
                savePending(pending - book.id)
                info = "Cargamento recibido: +$amount ejemplares de «${book.title}». Stock actual: $updatedStock."
                receivingId = null
                reloadKey++
            }
        }.addOnFailureListener { ex ->
            if (Firebase.auth.currentUser?.uid == activeUid) {
                error = "No se pudo confirmar la recepción: ${ex.localizedMessage}. La solicitud sigue pendiente."
                receivingId = null
            }
        }
    }

    LaunchedEffect(uid, reloadKey) {
        if (uid == null) {
            loading = false
            error = "Iniciá sesión para consultar las alertas."
            return@LaunchedEffect
        }
        loading = true
        error = null
        Firebase.firestore.collection("books").get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    books = snapshot.documents
                        .filter { it.getBoolean("isActive") != false }
                        .map { doc ->
                            AdminListedBook(
                                id = doc.id,
                                title = doc.getString("title").orEmpty().ifBlank { "Sin título" },
                                author = doc.getString("author").orEmpty(),
                                stock = doc.getLong("stock") ?: 0L,
                                imageUrl = doc.getString("imageUrl").orEmpty()
                            )
                        }
                        .sortedBy { it.title.lowercase(Locale.ROOT) }
                    loading = false
                }
            }
            .addOnFailureListener { failure ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    error = "No se pudieron cargar las alertas: ${failure.localizedMessage}"
                    loading = false
                }
            }
    }

    val yellow = books.filter { it.stock == 10L }
    val red = books.filter { it.stock in 0L..9L }
    val pendingOther = books.filter { it.id in pending && it.stock > 9L }

    Column(Modifier.fillMaxSize().background(AdminBackground)) {
        Surface(color = AdminNavy, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(62.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.width(54.dp)) {
                    Icon(painterResource(R.drawable.ic_book_detail_back),
                        contentDescription = "Regresar", tint = Color.White)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Alertas de stock", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(54.dp))
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Libros con cantidad baja de stock",
                modifier = Modifier.fillMaxWidth(),
                color = AdminNavy, fontSize = 21.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            info?.let {
                Text(it, color = Color(0xFF2E7D32), fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth())
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminBlue)
                }
                else -> {
                    error?.let {
                        Text(it, color = Color(0xFFC62828), fontSize = 13.sp)
                    }
                    AdminAlertTable("Alerta amarilla · 10 ejemplares", yellow,
                        Color(0xFF9A6700), pending, receivingId,
                        onRequest = { id, amount ->
                            if (uid != null && Firebase.auth.currentUser?.uid == uid && id !in pending) {
                                savePending(pending + (id to amount))
                                info = "Solicitud registrada. Confirmá la recepción cuando llegue el cargamento."
                            }
                        },
                        onReceive = ::receive,
                        onCancel = { id -> savePending(pending - id) }
                    )
                    AdminAlertTable("Alerta roja · 0 a 9 ejemplares", red,
                        Color(0xFFC62828), pending, receivingId,
                        onRequest = { id, amount ->
                            if (uid != null && Firebase.auth.currentUser?.uid == uid && id !in pending) {
                                savePending(pending + (id to amount))
                                info = "Solicitud registrada. Confirmá la recepción cuando llegue el cargamento."
                            }
                        },
                        onReceive = ::receive,
                        onCancel = { id -> savePending(pending - id) }
                    )
                    if (pendingOther.isNotEmpty()) {
                        AdminAlertTable("Cargamentos pendientes", pendingOther,
                            Color(0xFF2E7D32), pending, receivingId,
                            onRequest = { _, _ -> },
                            onReceive = ::receive,
                            onCancel = { id -> savePending(pending - id) }
                        )
                    }
                    Text(
                        "Las solicitudes simuladas se guardan en este dispositivo; " +
                                "el stock se actualiza en Firestore al recibir el cargamento.",
                        fontSize = 12.sp, color = Color(0xFF626C7D),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { reloadKey++ },
                        enabled = !loading && receivingId == null,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                    ) {
                        Icon(painterResource(R.drawable.update),
                            contentDescription = null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Actualizar alertas", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAlertTable(
    heading: String,
    books: List<AdminListedBook>,
    stockColor: Color,
    pending: Map<String, Int>,
    receivingId: String?,
    onRequest: (String, Int) -> Unit,
    onReceive: (AdminListedBook) -> Unit,
    onCancel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                Text(heading, color = AdminNavy, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)
            }
            HorizontalDivider(color = AdminBlue.copy(alpha = 0.38f))
            Row(Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(3f), contentAlignment = Alignment.Center) {
                    Text("Libros", color = AdminNavy, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(AdminBlue.copy(alpha = 0.35f)))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Stock", color = AdminNavy, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = AdminBlue.copy(alpha = 0.38f))
            if (books.isEmpty()) {
                Text("No hay libros en esta alerta.",
                    color = Color(0xFF626C7D), fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    textAlign = TextAlign.Center)
            } else {
                books.forEachIndexed { index, book ->
                    var expanded by remember(book.id) { mutableStateOf(false) }
                    var quantity by remember(book.id) { mutableStateOf("15") }
                    val requested = pending[book.id]
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(3f).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AdminListedBookCover(book)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(book.title, color = AdminNavy,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(book.author.ifBlank { "Autor no disponible" },
                                        color = Color(0xFF626C7D), fontSize = 12.sp)
                                }
                            }
                            Box(Modifier.width(1.dp).height(116.dp)
                                .background(AdminBlue.copy(alpha = 0.32f)))
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(book.stock.toString(), color = stockColor,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                        }
                        // Panel animado de reabastecimiento para stock crítico o solicitudes pendientes.
                        AnimatedVisibility(
                            visible = book.stock in 0L..5L || requested != null,
                            enter = slideInVertically { -it / 3 } + fadeIn(),
                            exit = slideOutVertically { -it / 3 } + fadeOut()
                        ) {
                            val maxRequest = (100000L - book.stock).coerceIn(0L, 100000L).toInt()
                            val requestAmount = quantity.toIntOrNull()
                            val validAmount = requestAmount != null && requestAmount in 1..maxRequest

                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .border(1.dp, AdminBlue, RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF1F6FD), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 15.dp, vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (requested != null) {
                                        Text(
                                            "Cargamento solicitado: $requested ejemplares",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            color = AdminNavy,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Pendiente de recepción",
                                            color = Color(0xFF9A6700),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { onReceive(book) },
                                                enabled = receivingId == null,
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                                            ) {
                                                if (receivingId == book.id) {
                                                    CircularProgressIndicator(
                                                        Modifier.size(18.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else Text("Confirmar llegada", fontSize = 12.sp, textAlign = TextAlign.Center)
                                            }
                                            OutlinedButton(
                                                onClick = { onCancel(book.id) },
                                                enabled = receivingId == null,
                                                modifier = Modifier.heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AdminBlue),
                                                contentPadding = PaddingValues(horizontal = 12.dp)
                                            ) { Text("Cancelar", color = AdminNavy, fontSize = 12.sp) }
                                        }
                                    } else {
                                        Text(
                                            "¡Stock crítico!",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "¿Querés reabastecer este libro?",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            color = AdminNavy,
                                            fontSize = 13.sp
                                        )
                                        if (!expanded) {
                                            Button(
                                                onClick = { expanded = true },
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.delivery_truck_speed),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                                Spacer(Modifier.width(9.dp))
                                                Text("Solicitar cargamento", fontSize = 14.sp)
                                            }
                                        }
                                        AnimatedVisibility(
                                            visible = expanded,
                                            enter = slideInVertically { -it / 4 } + fadeIn(),
                                            exit = slideOutVertically { -it / 4 } + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = quantity,
                                                    onValueChange = { input ->
                                                        // Rechaza letras, símbolos, negativos y cadenas de más de 6 dígitos.
                                                        if (input.length <= 6 && input.all { it in '0'..'9' }) {
                                                            quantity = input
                                                        }
                                                    },
                                                    label = { Text("Cantidad de ejemplares") },
                                                    placeholder = { Text("De 1 a $maxRequest") },
                                                    singleLine = true,
                                                    isError = quantity.isNotEmpty() && !validAmount,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = AdminBlue,
                                                        unfocusedBorderColor = AdminBlue.copy(alpha = 0.65f),
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White,
                                                        focusedTextColor = AdminNavy,
                                                        unfocusedTextColor = AdminNavy
                                                    )
                                                )
                                                Text(
                                                    "Cantidad permitida: 1 a $maxRequest ejemplares.",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    fontSize = 12.sp,
                                                    color = if (quantity.isNotEmpty() && !validAmount)
                                                        Color(0xFFC62828) else AdminNavy
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val amount = quantity.toIntOrNull() ?: return@Button
                                                            if (amount in 1..maxRequest) {
                                                                onRequest(book.id, amount)
                                                                expanded = false
                                                            }
                                                        },
                                                        enabled = validAmount,
                                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 9.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                                                    ) {
                                                        // Espacio reservado para el ícono XML elegido por el usuario.
                                                        Text("Confirmar solicitud", fontSize = 12.sp, textAlign = TextAlign.Center)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { expanded = false },
                                                        modifier = Modifier.heightIn(min = 48.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminBlue),
                                                        contentPadding = PaddingValues(horizontal = 14.dp)
                                                    ) { Text("Volver", color = AdminNavy, fontSize = 12.sp) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (index < books.lastIndex) {
                        HorizontalDivider(color = AdminBlue.copy(alpha = 0.24f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}