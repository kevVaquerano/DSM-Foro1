package com.dreamteam.bookmaster.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private val AddBlue = Color(0xFF2864B4)
private val AddNavy = Color(0xFF173B73)
private val AddBackground = Color(0xFFF1F6FD)
private val AddLine = Color(0xFFDCE6F4)
private val AddMenuBackground = Color(0xFFE7F3FF)

// Se admiten letras Unicode (á, é, í, ó, ú, ü, ñ), espacios y números solo en títulos.
// Sin signos, emojis, espacios repetidos ni cadenas formadas solo por números.
private val addTitlePattern = Regex("^[\\p{L}\\p{M}0-9]+(?: [\\p{L}\\p{M}0-9]+)*$")
private val addAuthorPattern = Regex("^[\\p{L}\\p{M}]+(?: [\\p{L}\\p{M}]+)*$")
private fun addTitleIsValid(text: String): Boolean {
    val value = text.trim()
    return value.length in 2..150 && addTitlePattern.matches(value) && value.any { it.isLetter() || it.isDigit() }
}
private fun addAuthorIsValid(text: String): Boolean {
    val value = text.trim()
    return value.length in 2..120 && addAuthorPattern.matches(value) && value.any { it.isLetter() }
}


private fun addHttpsValid(value: String): Boolean = value.isBlank() || try {
    val parsed = java.net.URI(value.trim())
    parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null && value.trim().length <= 1000
} catch (_: Exception) { false }
private val allowedNewCategories = setOf(
    "Romance", "Ciencia Ficción", "Ciencias", "Literatura", "Psicología y Superación Personal",
    "Historia", "Fantasía", "Infantil", "Otros"
)


/** Mismo almacenamiento privado de portadas que usa la pantalla Editar libro. */
private object AddLocalCoverStorage {
    suspend fun decodeSelected(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1600) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } catch (_: Exception) { null }
    }

    suspend fun save(context: Context, id: String, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val folder = File(context.filesDir, "book_covers")
            if (!folder.exists()) folder.mkdirs()
            val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(folder, "$safeId.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            File(file.absolutePath + ".hidden").delete()
            true
        } catch (_: Exception) { false }
    }
}

/** Etiquetas uniformes arriba del borde. Asterisco rojo solo para datos obligatorios. */
@Composable
private fun AddBookField(
    name: String, value: String, onValueChange: (String) -> Unit,
    type: KeyboardType = KeyboardType.Text, lines: Int = 1, errorText: String? = null
) {
    val required = name.removeSuffix(":") !in setOf(
        "URL HTTPS de portada", "Descripción"
    )
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = null,
                isError = errorText != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = if (lines == 1) 68.dp else 118.dp),
                singleLine = lines == 1,
                minLines = lines,
                maxLines = if (lines == 1) 1 else 5,
                keyboardOptions = KeyboardOptions(keyboardType = type),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AddBlue,
                    unfocusedBorderColor = AddBlue.copy(alpha = 0.75f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = AddNavy,
                    unfocusedTextColor = AddNavy,
                    cursorColor = AddBlue
                )
            )
            Row(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = 13.dp, y = (-8).dp)
                    .background(AddBackground)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = AddNavy, fontSize = 13.sp)
                if (required) {
                    Text(" *", color = Color(0xFFD32F2F), fontSize = 15.sp)
                }
            }
        }
        if (errorText != null) Text(errorText, color = Color(0xFFB42332), fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp))
    }
}

/** Conserva la firma llamada desde NavGraph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddBookScreen(onBackClick: () -> Unit, onBookSaved: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var titleTouched by remember { mutableStateOf(false) }
    var authorTouched by remember { mutableStateOf(false) }
    var isbn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var catalogCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var categoriesLoading by remember { mutableStateOf(true) }
    var categoriesError by remember { mutableStateOf<String?>(null) }

    // Las mismas categorías reales que muestra Editar libro: libros activos en Firestore.
    LaunchedEffect(uid) {
        categoriesLoading = true
        Firebase.firestore.collection("books").get()
            .addOnSuccessListener { snapshot ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    catalogCategories = snapshot.documents
                        .filter { it.getBoolean("isActive") != false }
                        .map { it.getString("category").orEmpty().trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sortedWith(String.CASE_INSENSITIVE_ORDER)
                    categoriesError = if (catalogCategories.isEmpty())
                        "No hay categorías registradas en el catálogo." else null
                    categoriesLoading = false
                }
            }
            .addOnFailureListener { e ->
                if (Firebase.auth.currentUser?.uid == uid) {
                    categoriesLoading = false
                    categoriesError = "No se pudieron cargar las categorías: ${e.localizedMessage}"
                }
            }
    }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var minAge by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedCover by remember { mutableStateOf<Bitmap?>(null) }
    var remoteCover by remember { mutableStateOf<Bitmap?>(null) }
    var readingCover by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            readingCover = true
            scope.launch {
                val photo = AddLocalCoverStorage.decodeSelected(context, uri)
                if (photo != null) {
                    selectedCover = photo
                    failure = null
                } else failure = "No se pudo abrir la portada seleccionada."
                readingCover = false
            }
        }
    }

    LaunchedEffect(imageUrl) {
        remoteCover = null
        val url = imageUrl.trim()
        if (url.startsWith("https://", ignoreCase = true) && url.length <= 1000) {
            remoteCover = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.instanceFollowRedirects = true
                    try {
                        if (connection.responseCode in 200..299)
                            connection.inputStream.use { BitmapFactory.decodeStream(it) }
                        else null
                    } finally { connection.disconnect() }
                } catch (_: Exception) { null }
            }
        }
    }

    val numericPrice = price.replace(',', '.').toDoubleOrNull()
    val numericStock = stock.toLongOrNull()
    val numericRating = rating.replace(',', '.').toDoubleOrNull()
    val numericAge = minAge.toLongOrNull()
    val numericYear = year.toLongOrNull()
    val cleanIsbn = isbn.filter(Char::isDigit)
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    // Se permiten tildes, eñes y otros caracteres Unicode en título y autor.
    // No aplicar filtros ASCII ni eliminar acentos al guardar en Firestore.
    val valid = addTitleIsValid(name) && addAuthorIsValid(author) &&
            (cleanIsbn.length in listOf(10, 13) &&
                    isbn.all { it.isDigit() || it == '-' || it == ' ' }) &&
            category in catalogCategories && category in allowedNewCategories && numericPrice != null && numericPrice.isFinite() &&
            numericPrice > 0 && numericPrice <= 100000 && numericStock != null &&
            numericStock in 0L..100000L && numericRating != null && numericRating.isFinite() &&
            numericRating in 0.0..5.0 && numericAge != null && numericAge in 0L..120L &&
            numericYear != null && numericYear in 1450L..currentYear.toLong() && description.length <= 2000 && addHttpsValid(imageUrl)

    Column(Modifier.fillMaxSize().background(AddBackground)) {
        Surface(color = AddNavy, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, enabled = !loading, modifier = Modifier.width(54.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_detail_back),
                        contentDescription = "Regresar", tint = Color.White, modifier = Modifier.size(25.dp)
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Agregar libro", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(54.dp))
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier.fillMaxWidth().height(285.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF0F3F8))
                        .border(BorderStroke(1.dp, AddBlue.copy(alpha = 0.35f)), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedCover != null -> Image(
                            bitmap = selectedCover!!.asImageBitmap(),
                            contentDescription = "Portada seleccionada",
                            modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit
                        )
                        remoteCover != null -> Image(
                            bitmap = remoteCover!!.asImageBitmap(),
                            contentDescription = "Portada por URL",
                            modifier = Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit
                        )
                        else -> Text("📖", fontSize = 35.sp)
                    }
                }
                val hasCover = selectedCover != null || remoteCover != null
                Surface(
                    onClick = {
                        if (!loading && !readingCover) {
                            if (selectedCover != null) selectedCover = null
                            else if (remoteCover != null) imageUrl = ""
                            else photoPicker.launch("image/*")
                        }
                    },
                    shape = CircleShape, color = AddBlue, shadowElevation = 6.dp,
                    modifier = Modifier.offset(y = 22.dp).size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (readingCover) CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                        ) else Icon(
                            painter = painterResource(
                                if (hasCover) R.drawable.ic_delete_profile else R.drawable.ic_checkout_edit
                            ),
                            contentDescription = if (hasCover) "Quitar portada" else "Elegir portada",
                            tint = Color.White, modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.height(17.dp))
            HorizontalDivider(color = AddLine)
            Spacer(Modifier.height(24.dp))
            Text(
                "Agregar información", modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center, fontSize = 20.sp,
                color = AddNavy, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(22.dp))
            AddBookField("URL HTTPS de portada:", imageUrl, { if (it.length <= 1000) imageUrl = it }, KeyboardType.Uri, errorText = if (imageUrl.isNotBlank() && !addHttpsValid(imageUrl)) "Ingresá una URL HTTPS válida." else null)
            Spacer(Modifier.height(12.dp))
            AddBookField("Título:", name, { titleTouched = true; if (it.length <= 150) name = it }, errorText = if ((titleTouched || name.isNotEmpty()) && !addTitleIsValid(name)) "Ingresá un título de 2 a 150 caracteres. Solo letras, tildes, espacios y números; sin símbolos ni espacios dobles." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Autor:", author, { authorTouched = true; if (it.length <= 120) author = it }, errorText = if ((authorTouched || author.isNotEmpty()) && !addAuthorIsValid(author)) "Ingresá un autor de 2 a 120 caracteres. Solo letras, tildes, ñ y espacios; sin números ni símbolos." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("ISBN:", isbn, { if (it.length <= 20) isbn = it }, errorText = if (isbn.isNotBlank() && !(cleanIsbn.length in listOf(10, 13) && isbn.all { it.isDigit() || it == '-' || it == ' ' })) "Debe tener 10 o 13 dígitos." else null)
            Spacer(Modifier.height(11.dp))
            // Selector visual con flecha independiente y categorías reales de Firestore.
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = {
                        if (!loading && !categoriesLoading && catalogCategories.isNotEmpty())
                            categoryExpanded = it
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, AddBlue, RoundedCornerShape(14.dp))
                            .menuAnchor(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.ifBlank {
                                if (categoriesLoading) "Cargando categorías..."
                                else "Seleccioná una categoría"
                            },
                            color = if (category.isBlank()) Color(0xFF626C7D) else AddNavy,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(start = 15.dp, end = 8.dp)
                        )
                        Box(Modifier.width(1.dp).fillMaxHeight().background(AddBlue))
                        Box(Modifier.width(51.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text("▼", color = AddBlue, fontSize = 24.sp)
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(AddMenuBackground)
                    ) {
                        catalogCategories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = AddNavy, fontSize = 14.sp) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                    failure = null
                                },
                                colors = MenuDefaults.itemColors(textColor = AddNavy)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.TopStart)
                        .offset(x = 13.dp, y = (-8).dp)
                        .background(AddBackground)
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categoría:", color = AddNavy, fontSize = 13.sp)
                    Text(" *", color = Color(0xFFD32F2F), fontSize = 15.sp)
                }
            }
            if (category.isNotBlank() && category !in allowedNewCategories) {
                Text("Firestore todavía no autoriza crear libros con esta categoría. Hay que ajustar la lista permitida en las reglas.",
                    color = Color(0xFFB42332), fontSize = 12.sp)
            }
            categoriesError?.let {
                Text(it, color = Color(0xFFB42332), fontSize = 12.sp)
            }
            Spacer(Modifier.height(11.dp))
            AddBookField("Calificación (0 a 5):", rating, { if (it.length <= 8) rating = it }, KeyboardType.Decimal, errorText = if (rating.isNotBlank() && (numericRating == null || !numericRating.isFinite() || numericRating !in 0.0..5.0)) "La calificación debe estar entre 0 y 5." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Precio ($):", price, { if (it.length <= 12) price = it }, KeyboardType.Decimal, errorText = if (price.isNotBlank() && (numericPrice == null || !numericPrice.isFinite() || numericPrice <= 0.0 || numericPrice > 100000.0)) "Ingresá un precio mayor que 0 y hasta $100,000." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Stock:", stock, { if (it.length <= 8) stock = it }, KeyboardType.Number, errorText = if (stock.isNotBlank() && (numericStock == null || numericStock !in 0L..100000L)) "Stock válido: de 0 a 100,000." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Edad mínima:", minAge, { if (it.length <= 3) minAge = it }, KeyboardType.Number, errorText = if (minAge.isNotBlank() && (numericAge == null || numericAge !in 0L..120L)) "Edad válida: de 0 a 120." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Año de publicación:", year, { if (it.length <= 4) year = it }, KeyboardType.Number, errorText = if (year.isNotBlank() && (numericYear == null || numericYear !in 1450L..currentYear.toLong())) "Año válido: de 1450 a $currentYear." else null)
            Spacer(Modifier.height(11.dp))
            AddBookField("Descripción:", description, { if (it.length <= 2000) description = it }, lines = 3)
            Spacer(Modifier.height(12.dp))
            if (!valid && !loading) {
                Text("Completá los campos obligatorios (*) con valores válidos. ISBN debe tener 10 o 13 dígitos.",
                    color = AddNavy, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            failure?.let {
                Text(it, color = Color(0xFFB42332), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }
            Button(
                enabled = !loading && !readingCover && valid && uid != null,
                onClick = {
                    if (loading || !valid || uid == null || Firebase.auth.currentUser?.uid != uid) return@Button
                    loading = true
                    failure = null
                    val fields: Map<String, Any> = mapOf(
                        "title" to name.trim(), "author" to author.trim(),
                        "isbn" to cleanIsbn, "category" to category.trim(),
                        "price" to numericPrice!!, "stock" to numericStock!!,
                        "minimumAge" to numericAge!!, "publicationYear" to numericYear!!,
                        "description" to description.trim(), "imageUrl" to imageUrl.trim(),
                        "rating" to numericRating!!, "createdAt" to FieldValue.serverTimestamp()
                    )
                    Firebase.firestore.collection("books").add(fields)
                        .addOnSuccessListener { doc ->
                            val cover = selectedCover
                            if (cover == null) {
                                loading = false
                                onBookSaved()
                            } else scope.launch {
                                val saved = AddLocalCoverStorage.save(context, doc.id, cover)
                                loading = false
                                if (!saved) {
                                    failure = "El libro se agregó a Firestore, pero no se pudo guardar la portada local."
                                } else onBookSaved()
                            }
                        }
                        .addOnFailureListener { err ->
                            loading = false
                            failure = "No se pudo agregar el libro: ${err.localizedMessage ?: "error de Firebase"}"
                        }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AddBlue)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White)
                else Text("Agregar nuevo libro", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}