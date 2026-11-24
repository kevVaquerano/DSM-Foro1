package com.dreamteam.bookmaster.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.util.Locale

// Paleta de colores del módulo administrativo.
private val AdminBlue = Color(0xFF2864B4)
private val AdminNavy = Color(0xFF173B73)
private val AdminPale = Color(0xFFF1F6FD)
private val AdminLine = Color(0xFFDCE6F4)
private val AdminRed = Color(0xFFD64545)

// Se admiten letras Unicode (á, é, í, ó, ú, ü, ñ), espacios y números solo en títulos.
// No se admiten signos, emojis ni espacios internos repetidos. El título puede contener solo números.
private val editTitlePattern = Regex("^[\\p{L}\\p{M}0-9]+(?: [\\p{L}\\p{M}0-9]+)*$")
private val editAuthorPattern = Regex("^[\\p{L}\\p{M}]+(?: [\\p{L}\\p{M}]+)*$")
private fun editTitleIsValid(text: String): Boolean {
    val value = text.trim()
    return value.length in 2..150 && editTitlePattern.matches(value) && value.any { it.isLetter() || it.isDigit() }
}
private fun editAuthorIsValid(text: String): Boolean {
    val value = text.trim()
    return value.length in 2..120 && editAuthorPattern.matches(value) && value.any { it.isLetter() }
}


// La URL es opcional; si existe, debe cumplir las condiciones HTTPS.
private fun editHttpsValid(value: String): Boolean = value.isBlank() || try {
    val parsed = java.net.URI(value.trim())
    parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null && value.trim().length <= 1000
} catch (_: Exception) { false }


// Datos necesarios para identificar y mostrar cada libro en el listado.
private data class EditableBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val isbn: String,
    val imageUrl: String
)

private fun safeBookTitle(book: EditableBook): String =
    if (book.title.startsWith("https://", true) || book.title.startsWith("http://", true))
        "⚠ Revisar título" else book.title.ifBlank { "Sin título" }

/** Portadas de demostración guardadas en el dispositivo, sin Firebase Storage. */
private object AdminLocalCoverStorage {
    private fun coverFile(context: Context, id: String): File {
        val folder = File(context.filesDir, "book_covers")
        if (!folder.exists()) folder.mkdirs()
        val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(folder, "$safeId.jpg")
    }

    // Reduce las dimensiones de la imagen durante la lectura para limitar el uso de memoria.
    suspend fun decodeSelected(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1600) {
                    sample *= 2
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null,
                        BitmapFactory.Options().apply { inSampleSize = sample })
                }
            } catch (_: Exception) { null }
        }

    // El archivo marcador impide volver a mostrar una portada retirada localmente.
    private fun hiddenFile(context: Context, id: String): File =
        File(coverFile(context, id).absolutePath + ".hidden")

    suspend fun isHidden(context: Context, id: String): Boolean =
        withContext(Dispatchers.IO) { hiddenFile(context, id).exists() }

    suspend fun remove(context: Context, id: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val cover = coverFile(context, id)
                val deleted = !cover.exists() || cover.delete()
                val marker = hiddenFile(context, id)
                deleted && (marker.exists() || marker.createNewFile())
            } catch (_: Exception) { false }
        }

    suspend fun load(context: Context, id: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = coverFile(context, id)
            if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (_: Exception) { null }
    }

    suspend fun save(context: Context, id: String, bitmap: Bitmap): Boolean =
        withContext(Dispatchers.IO) {
            try {
                hiddenFile(context, id).delete()
                coverFile(context, id).outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                }
                true
            } catch (_: Exception) { false }
        }
}

/** Muestra la portada local, remota o drawable, salvo que esté marcada como oculta. */
@Composable
private fun AdminBookCover(
    bookId: String, imageUrl: String, modifier: Modifier = Modifier, reloadKey: Int = 0
) {
    val ctx = LocalContext.current
    val drawableId = remember(bookId) { ctx.resources.getIdentifier(bookId, "drawable", ctx.packageName) }
    var localCover by remember(bookId, reloadKey) { mutableStateOf<Bitmap?>(null) }
    var isHidden by remember(bookId, reloadKey) { mutableStateOf(false) }
    var remoteCover by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(bookId, reloadKey) {
        isHidden = AdminLocalCoverStorage.isHidden(ctx, bookId)
        localCover = if (isHidden) null else AdminLocalCoverStorage.load(ctx, bookId)
    }
    LaunchedEffect(imageUrl) {
        remoteCover = null
        if (imageUrl.startsWith("https://", ignoreCase = true)) {
            remoteCover = withContext(Dispatchers.IO) {
                try {
                    val conn = URL(imageUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.instanceFollowRedirects = true
                    try {
                        if (conn.responseCode in 200..299) {
                            conn.inputStream.use { BitmapFactory.decodeStream(it) }
                        } else null
                    } finally { conn.disconnect() }
                } catch (_: Exception) { null }
            }
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFF0F3F8))
            .border(BorderStroke(1.dp, AdminBlue.copy(alpha = 0.35f)), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isHidden -> Text("📖", fontSize = 35.sp)
            localCover != null -> Image(
                bitmap = localCover!!.asImageBitmap(), contentDescription = "Portada local",
                modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit
            )
            remoteCover != null -> Image(
                bitmap = remoteCover!!.asImageBitmap(), contentDescription = "Portada por URL",
                modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit
            )
            drawableId != 0 -> Image(
                painter = painterResource(drawableId), contentDescription = "Portada original",
                modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit
            )
            else -> Text("📖", fontSize = 35.sp)
        }
    }
}

@Composable
private fun AdminEditField(
    name: String, value: String, onValueChange: (String) -> Unit,
    type: KeyboardType = KeyboardType.Text, lines: Int = 1, errorText: String? = null
) {
    // Los campos opcionales pueden estar vacíos. Mantenemos sus etiquetas
    // arriba del borde, igual que las etiquetas de los campos con contenido.
    val floatingLabel = true

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = null,
                placeholder = {
                    val example = when (name.removeSuffix(":")) {
                        "URL HTTPS de portada" -> "Ingresar enlace HTTPS (opcional)"
                        "Título" -> "Ingresar título del libro"
                        "Autor" -> "Ingresar nombre del autor"
                        "ISBN (opcional)" -> "Ingresar ISBN (opcional)"
                        "Categoría" -> "Ingresar categoría"
                        "Calificación (0 a 5)" -> "Ingresar calificación de 0 a 5"
                        "Precio ($)" -> "Ingresar precio"
                        "Stock" -> "Ingresar cantidad disponible"
                        "Edad mínima" -> "Ingresar edad mínima"
                        "Año de publicación" -> "Ingresar año de publicación"
                        "Descripción" -> "Ingresar descripción (opcional)"
                        else -> "Ingresar información"
                    }
                    Text(example, fontSize = 14.sp, color = Color(0xFF9299A8))
                },
                isError = errorText != null,
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = if (lines == 1) 68.dp else 118.dp),
                singleLine = lines == 1,
                minLines = lines,
                maxLines = if (lines == 1) 1 else 5,
                keyboardOptions = KeyboardOptions(keyboardType = type),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminBlue,
                    unfocusedBorderColor = AdminBlue.copy(alpha = 0.75f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = AdminNavy,
                    unfocusedLabelColor = AdminNavy,
                    focusedTextColor = AdminNavy,
                    unfocusedTextColor = AdminNavy,
                    cursorColor = AdminBlue
                )
            )

            if (floatingLabel) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    color = AdminNavy,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 13.dp, y = (-8).dp)
                        .background(AdminPale)
                        .padding(horizontal = 5.dp)
                )
            }
        }
        if (errorText != null) Text(errorText, color = Color(0xFFB42332), fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp))
    }
}

/** Permite buscar, editar y eliminar libros del catálogo administrativo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditBookScreen(onBackClick: () -> Unit) {
    val db = remember { Firebase.firestore }
    val context = LocalContext.current
    val uid = Firebase.auth.currentUser?.uid
    // Estados del catálogo, búsqueda y selección del libro.
    var books by remember { mutableStateOf<List<EditableBook>>(emptyList()) }
    var loadingList by remember { mutableStateOf(true) }
    var loadingBook by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categoryListState = rememberLazyListState()
    val categoryScope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<EditableBook?>(null) }
    var deleting by remember { mutableStateOf(false) }

    // Datos editables del libro seleccionado.
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var titleTouched by remember { mutableStateOf(false) }
    var authorTouched by remember { mutableStateOf(false) }
    var isbn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var minimumAge by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    val coverScope = rememberCoroutineScope()
    var selectedLocalCover by remember { mutableStateOf<Bitmap?>(null) }
    var coverHidden by remember { mutableStateOf(false) }
    // Solo es una vista previa; no se toca el almacenamiento antes de Guardar cambios.
    var coverAction by remember { mutableStateOf("none") } // none, replace, remove
    var readingCover by remember { mutableStateOf(false) }
    var coverReloadKey by remember { mutableIntStateOf(0) }
    // Abre el selector de imágenes y conserva la selección como vista previa.
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val currentId = selectedId
        if (uri != null && currentId != null && !readingCover && !saving) {
            readingCover = true
            coverScope.launch {
                val photo = AdminLocalCoverStorage.decodeSelected(context, uri)
                if (selectedId == currentId) {
                    if (photo != null) {
                        selectedLocalCover = photo
                        coverHidden = false
                        coverAction = "replace"
                        notice = "Portada seleccionada. Presioná Guardar cambios para confirmar."
                    } else notice = "No se pudo abrir la portada seleccionada."
                }
                readingCover = false
            }
        }
    }

    // Recupera la portada local al seleccionar otro libro o guardar un cambio.
    LaunchedEffect(selectedId, coverReloadKey) {
        val currentId = selectedId ?: return@LaunchedEffect
        val hidden = AdminLocalCoverStorage.isHidden(context, currentId)
        val local = if (hidden) null else AdminLocalCoverStorage.load(context, currentId)
        if (selectedId == currentId) {
            coverHidden = hidden
            selectedLocalCover = local
            coverAction = "none"
        }
    }


    // Carga los datos del documento de Firestore en los campos del formulario.
    fun loadDocument(doc: DocumentSnapshot) {
        title = doc.getString("title").orEmpty()
        author = doc.getString("author").orEmpty()
        titleTouched = false
        authorTouched = false
        isbn = doc.getString("isbn").orEmpty()
        category = doc.getString("category").orEmpty()
        price = (doc.getDouble("price") ?: 0.0).toString()
        rating = (doc.getDouble("rating") ?: 0.0).toString()
        stock = (doc.getLong("stock") ?: 0L).toString()
        minimumAge = (doc.getLong("minimumAge") ?: 0L).toString()
        year = (doc.getLong("publicationYear") ?: 0L).toString()
        description = doc.getString("description").orEmpty()
        imageUrl = doc.getString("imageUrl").orEmpty()
    }

    // Consulta los libros activos y descarta respuestas de una sesión anterior.
    fun refreshBooks() {
        if (uid == null) { loadingList = false; error = "Iniciá sesión como administrador."; return }
        loadingList = true
        db.collection("books").get()
            .addOnSuccessListener { snapshot ->
                if (Firebase.auth.currentUser?.uid != uid) return@addOnSuccessListener
                books = snapshot.documents.filter { it.getBoolean("isActive") != false }.map { doc ->
                    EditableBook(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        author = doc.getString("author").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        isbn = doc.getString("isbn").orEmpty(),
                        imageUrl = doc.getString("imageUrl").orEmpty()
                    )
                }.sortedBy { safeBookTitle(it).lowercase() }
                loadingList = false
                error = null
            }
            .addOnFailureListener { ex ->
                if (Firebase.auth.currentUser?.uid != uid) return@addOnFailureListener
                loadingList = false
                error = "No se pudo cargar el catálogo: ${ex.localizedMessage}"
            }
    }

    LaunchedEffect(uid) { refreshBooks() }

    // Genera las categorías del catálogo y filtra por título, autor o ISBN.
    val categories = listOf("Todos") + books.map { it.category.trim() }
        .filter { it.isNotBlank() }.distinct().sorted()
    val results = books.filter { b ->
        (selectedCategory == "Todos" || b.category == selectedCategory) &&
                (search.isBlank() || safeBookTitle(b).contains(search.trim(), true) ||
                        b.author.contains(search.trim(), true) || b.isbn.contains(search.trim(), true))
    }

    // Normaliza los valores antes de verificar si pueden guardarse.
    val cleanIsbn = isbn.filter(Char::isDigit)
    val p = price.replace(',', '.').toDoubleOrNull()
    val r = rating.replace(',', '.').toDoubleOrNull()
    val s = stock.toLongOrNull()
    val a = minimumAge.toLongOrNull()
    val y = year.toLongOrNull()
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    // Valida formato y longitud de título y autor, además de los demás campos del formulario.
    val valid = editTitleIsValid(title) && editAuthorIsValid(author) &&
            (isbn.isBlank() || (cleanIsbn.length in listOf(10, 13) &&
                    isbn.all { it.isDigit() || it == '-' || it == ' ' })) &&
            category.trim().length in 2..120 && category.trim() in categories.drop(1) && p != null && p.isFinite() && p > 0 && p <= 100000 &&
            r != null && r.isFinite() && r in 0.0..5.0 &&
            s != null && s in 0L..100000L && a != null && a in 0L..120L &&
            y != null && y in 1450L..currentYear.toLong() && description.length <= 2000 &&
            editHttpsValid(imageUrl)

    Column(Modifier.fillMaxSize().background(AdminPale)) {
        Surface(color = AdminNavy, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (selectedId != null) { selectedId = null; notice = null; refreshBooks() }
                    else onBackClick() },
                    enabled = !saving,
                    modifier = Modifier.width(54.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_detail_back),
                        contentDescription = "Regresar",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (selectedId == null) "Editar libro" else "Detalles del libro",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        maxLines = 1)
                }
                Spacer(Modifier.width(54.dp))
            }
        }

        // Alterna entre el listado del catálogo y el formulario de edición.
        if (selectedId == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 17.dp)) {
                Spacer(Modifier.height(17.dp))
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(15.dp)),
                    placeholder = { Text("Buscar por título, autor o ISBN...", fontSize = 14.sp, color = Color(0xFF9299A8)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_home_search), null, tint = AdminBlue,
                            modifier = Modifier.size(21.dp))
                    },
                    singleLine = true, shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AdminBlue, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedPlaceholderColor = Color(0xFF9299A8),
                        unfocusedPlaceholderColor = Color(0xFF9299A8))
                )
                Spacer(Modifier.height(15.dp))
                LazyRow(
                    state = categoryListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                categoryScope.launch {
                                    val index = categories.indexOf(cat)
                                    categoryListState.animateScrollToItem((index - 1).coerceAtLeast(0))
                                }
                            },
                            label = { Text(cat, maxLines = 1) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White, labelColor = AdminNavy
                            ), border = null
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Libros registrados", fontWeight = FontWeight.Bold,
                    color = AdminNavy, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                if (loadingList) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminBlue)
                    }
                }
                error?.let { Text(it, color = Color(0xFFB42332)) }
                if (!loadingList && results.isEmpty() && error == null) {
                    Text("No se encontraron libros.", color = AdminNavy)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(bottom = 22.dp)
                ) {
                    items(results, key = { it.id }) { book ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(17.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AdminBookCover(book.id, book.imageUrl,
                                    Modifier.width(91.dp).height(129.dp), coverReloadKey)
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(safeBookTitle(book), color = AdminNavy,
                                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(5.dp))
                                    Text(book.author, color = Color(0xFF626C7D),
                                        fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (book.title.startsWith("https://", true)) {
                                        Text("Revisá el nombre en Firestore", color = Color(0xFF9A6700), fontSize = 10.sp)
                                    }
                                }
                                Spacer(Modifier.width(7.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Button(
                                        onClick = {
                                            loadingBook = true; error = null; notice = null
                                            db.collection("books").document(book.id).get()
                                                .addOnSuccessListener { doc ->
                                                    if (Firebase.auth.currentUser?.uid == uid && doc.exists()) {
                                                        loadDocument(doc); selectedLocalCover = null; coverHidden = false; coverAction = "none"; selectedId = doc.id
                                                    } else error = "No se encontró el libro."
                                                    loadingBook = false
                                                }
                                                .addOnFailureListener { ex ->
                                                    loadingBook = false
                                                    error = "No se pudo abrir: ${ex.localizedMessage}"
                                                }
                                        },
                                        enabled = !loadingBook && !saving,
                                        modifier = Modifier.width(91.dp).height(43.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_checkout_edit),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp), tint = Color.White
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Editar", fontSize = 12.sp)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { pendingDelete = book },
                                        enabled = !saving && !deleting,
                                        modifier = Modifier.width(91.dp).height(43.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, AdminRed),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminRed)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete_profile),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = AdminRed
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Eliminar", color = AdminRed, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                val currentBookId = selectedId.orEmpty()
                val bundledCoverId = remember(currentBookId) {
                    context.resources.getIdentifier(currentBookId, "drawable", context.packageName)
                }
                val hasCover = selectedLocalCover != null ||
                        (!coverHidden && (imageUrl.trim().startsWith("https://", true) || bundledCoverId != 0))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (selectedLocalCover != null) {
                        Image(
                            bitmap = selectedLocalCover!!.asImageBitmap(),
                            contentDescription = "Portada elegida desde galería",
                            modifier = Modifier.fillMaxWidth().height(285.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (coverHidden) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(285.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFFF0F3F8))
                                .border(BorderStroke(1.dp, AdminBlue.copy(alpha = 0.35f)), RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("📖", fontSize = 35.sp) }
                    } else {
                        AdminBookCover(currentBookId, imageUrl,
                            Modifier.fillMaxWidth().height(285.dp), coverReloadKey)
                    }

                    Surface(
                        onClick = {
                            if (!saving && !readingCover) {
                                if (hasCover) {
                                    selectedLocalCover = null
                                    coverHidden = true
                                    coverAction = "remove"
                                    notice = "Portada retirada de la vista previa. Presioná Guardar cambios para confirmar."
                                } else coverPicker.launch("image/*")
                            }
                        },
                        shape = CircleShape,
                        color = AdminBlue,
                        shadowElevation = 6.dp,
                        modifier = Modifier.offset(y = 22.dp).size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (readingCover) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White, strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (hasCover) R.drawable.ic_delete_profile
                                        else R.drawable.ic_checkout_edit
                                    ),
                                    contentDescription = if (hasCover) "Quitar portada" else "Elegir portada",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "La portada solo cambia al presionar Guardar cambios. " +
                            "Las fotos de galería permanecen en este dispositivo; la URL HTTPS se conserva.",
                    color = AdminNavy, fontSize = 12.sp
                )
                Spacer(Modifier.height(17.dp))
                // Misma distribución que BookDetailScreen, con la paleta azul administrativa.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = title.ifBlank { "Sin título" },
                        color = AdminNavy,
                        fontSize = 23.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (category.isNotBlank()) {
                        Surface(color = Color(0xFFDCEBFA), shape = RoundedCornerShape(50)) {
                            Text(
                                text = category,
                                color = AdminNavy,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if ((year.toIntOrNull() ?: 0) > 0) "$author · $year" else author,
                    color = Color(0xFF766D80),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(13.dp))
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
                            text = String.format(Locale.US, "%.1f", rating.replace(',', '.').toDoubleOrNull() ?: 0.0),
                            color = AdminNavy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    }
                    Surface(color = Color(0xFFDCEBFA), shape = RoundedCornerShape(50)) {
                        Text(
                            text = "${minimumAge}+",
                            color = AdminNavy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = AdminLine)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Editar información",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminNavy
                )
                Spacer(Modifier.height(22.dp))
                if (title.startsWith("http://", true)) {
                    Text("Este registro tiene una URL como título. Corregí el título y verificá la portada.",
                        color = Color(0xFF9A6700), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
                AdminEditField("URL HTTPS de portada:", imageUrl, { if (it.length <= 1000) imageUrl = it }, KeyboardType.Uri, errorText = if (imageUrl.isNotBlank() && !editHttpsValid(imageUrl)) "Ingresá una URL HTTPS válida." else null)
                Spacer(Modifier.height(12.dp))
                AdminEditField("Título:", title, { titleTouched = true; if (it.length <= 150) title = it }, errorText = if ((titleTouched || title.isNotEmpty()) && !editTitleIsValid(title)) "Ingresá un título de 2 a 150 caracteres. Solo letras, tildes, espacios y números; sin símbolos ni espacios dobles." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Autor:", author, { authorTouched = true; if (it.length <= 120) author = it }, errorText = if ((authorTouched || author.isNotEmpty()) && !editAuthorIsValid(author)) "Ingresá un autor de 2 a 120 caracteres. Solo letras, tildes, ñ y espacios; sin números ni símbolos." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("ISBN (opcional):", isbn, { if (it.length <= 20) isbn = it }, errorText = if (isbn.isNotBlank() && !(cleanIsbn.length in listOf(10, 13) && isbn.all { it.isDigit() || it == '-' || it == ' ' })) "Debe tener 10 o 13 dígitos, o quedar vacío." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Categoría:", category, { if (it.length <= 120) category = it }, errorText = if (category.isNotBlank() && category.trim() !in categories.drop(1)) "Usá una categoría existente del catálogo." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Calificación (0 a 5):", rating, { if (it.length <= 8) rating = it }, KeyboardType.Decimal, errorText = if (rating.isNotBlank() && (r == null || !r.isFinite() || r !in 0.0..5.0)) "Calificación válida: de 0 a 5." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Precio ($):", price, { if (it.length <= 12) price = it }, KeyboardType.Decimal, errorText = if (price.isNotBlank() && (p == null || !p.isFinite() || p <= 0.0 || p > 100000.0)) "Precio válido: mayor que 0 y hasta $100,000." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Stock:", stock, { if (it.length <= 8) stock = it }, KeyboardType.Number, errorText = if (stock.isNotBlank() && (s == null || s !in 0L..100000L)) "Stock válido: de 0 a 100,000." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Edad mínima:", minimumAge, { if (it.length <= 3) minimumAge = it }, KeyboardType.Number, errorText = if (minimumAge.isNotBlank() && (a == null || a !in 0L..120L)) "Edad válida: de 0 a 120." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Año de publicación:", year, { if (it.length <= 4) year = it }, KeyboardType.Number, errorText = if (year.isNotBlank() && (y == null || y !in 1450L..currentYear.toLong())) "Año válido: de 1450 a $currentYear." else null)
                Spacer(Modifier.height(11.dp))
                AdminEditField("Descripción:", description, { if (it.length <= 2000) description = it }, lines = 3)
                Spacer(Modifier.height(11.dp))
                if (!valid) Text("Revisá los campos obligatorios y sus valores antes de guardar.",
                    color = AdminNavy, fontSize = 12.sp)
                Text("El ID y los datos de compra del libro se conservan.",
                    fontSize = 12.sp, color = Color(0xFF626C7D))
                Spacer(Modifier.height(13.dp))
                Button(
                    enabled = valid && !saving && uid != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminBlue),
                    onClick = {
                        val id = selectedId ?: return@Button
                        if (!valid || saving || Firebase.auth.currentUser?.uid != uid) return@Button
                        saving = true; notice = null
                        // Actualiza los campos editables sin reemplazar el documento completo.
                        val changes: Map<String, Any> = mapOf(
                            "title" to title.trim(), "author" to author.trim(),
                            "isbn" to cleanIsbn, "category" to category.trim(),
                            "price" to p!!, "rating" to r!!, "stock" to s!!,
                            "minimumAge" to a!!, "publicationYear" to y!!,
                            "description" to description.trim(), "imageUrl" to imageUrl.trim()
                        )
                        db.collection("books").document(id).update(changes)
                            .addOnSuccessListener {
                                if (Firebase.auth.currentUser?.uid == uid) {
                                    books = books.map {
                                        if (it.id == id) it.copy(
                                            title = title.trim(), author = author.trim(),
                                            isbn = cleanIsbn, category = category.trim(),
                                            imageUrl = imageUrl.trim()) else it
                                    }.sortedBy { safeBookTitle(it).lowercase() }
                                }
                                // Aplica el cambio de portada local después de guardar en Firestore.
                                val action = coverAction
                                val pendingBitmap = selectedLocalCover
                                if (action == "none") {
                                    notice = "Libro actualizado correctamente."
                                    saving = false
                                } else {
                                    coverScope.launch {
                                        val imageSaved = when (action) {
                                            "replace" -> pendingBitmap != null &&
                                                    AdminLocalCoverStorage.save(context, id, pendingBitmap)
                                            "remove" -> AdminLocalCoverStorage.remove(context, id)
                                            else -> true
                                        }
                                        if (imageSaved) {
                                            coverAction = "none"
                                            coverReloadKey++
                                            notice = "Libro y portada actualizados correctamente."
                                        } else {
                                            notice = "Los datos se guardaron, pero no se pudo guardar la portada local. Intentá de nuevo."
                                        }
                                        saving = false
                                    }
                                }
                            }
                            .addOnFailureListener { ex ->
                                saving = false
                                notice = "No se guardó: ${ex.localizedMessage}"
                            }
                    }
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White)
                    else Text("Guardar cambios", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                notice?.let { Text(it, color = if (it.startsWith("Libro actualizado")) AdminNavy else Color(0xFFB42332)) }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    // Solicita confirmación antes de eliminar definitivamente un libro.
    pendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { if (!deleting) pendingDelete = null },
            containerColor = Color(0xFFF1F6FD),
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Eliminar libro",
                    color = AdminNavy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "¿Eliminar definitivamente «${safeBookTitle(book)}»? Se borrará su documento de Firestore y desaparecerá del catálogo. Esta acción no se puede deshacer.",
                    color = AdminNavy,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { pendingDelete = null },
                        enabled = !deleting,
                        colors = ButtonDefaults.textButtonColors(contentColor = AdminNavy)
                    ) { Text("Cancelar") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (deleting || Firebase.auth.currentUser?.uid != uid || uid == null) return@Button
                            deleting = true
                            // Elimina definitivamente el documento de la colección books.
                            db.collection("books").document(book.id)
                                .delete()
                                .addOnSuccessListener {
                                    if (Firebase.auth.currentUser?.uid == uid) {
                                        books = books.filterNot { it.id == book.id }
                                        Toast.makeText(context, "Libro eliminado de Firestore", Toast.LENGTH_LONG).show()
                                    }
                                    pendingDelete = null
                                    deleting = false
                                }
                                .addOnFailureListener { ex ->
                                    deleting = false
                                    Toast.makeText(context, "No se pudo eliminar: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        },
                        enabled = !deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (deleting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                        else Text("Eliminar")
                    }
                }
            }
        )
    }
}