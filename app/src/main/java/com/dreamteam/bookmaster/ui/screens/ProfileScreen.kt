package com.dreamteam.bookmaster.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.data.LocalProfilePhotoStore
import com.dreamteam.bookmaster.data.UserProfileRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Perfil del cliente. Nunca modifica birthDate, role, email ni información de compras. */
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    // Misma familia de colores de Login, Registro y Completar perfil.
    val navy = Color(0xFF473071)
    val purple = Color(0xFF7952B5)
    val background = Color(0xFFFCFAFF)
    val pale = Color(0xFFEDE5F8)
    val locked = Color(0xFFB9AEC9)
    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val scope = rememberCoroutineScope()

    // Estados del formulario y copias de los datos guardados para cancelar la edición.
    var firstName by remember(user?.uid) { mutableStateOf("") }
    var lastName by remember(user?.uid) { mutableStateOf("") }
    var birthDate by remember(user?.uid) { mutableStateOf("") }
    var address by remember(user?.uid) { mutableStateOf("") }
    var savedFirstName by remember(user?.uid) { mutableStateOf("") }
    var savedLastName by remember(user?.uid) { mutableStateOf("") }
    var savedAddress by remember(user?.uid) { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var photo by remember(user?.uid) { mutableStateOf<Bitmap?>(null) }
    var photoLoading by remember { mutableStateOf(false) }
    // Controla la visibilidad del diálogo de confirmación de cierre de sesión.
    var confirmLogout by remember { mutableStateOf(false) }

    // Estado verificado de Firestore, compartido con carrito y Checkout.
    val membership = rememberMembershipStatus()
    val puntos = membership.points ?: 0L
    val nivel = membership.level
    val minimoPlata = 200L
    val minimoOro = 500L
    // Calcula el avance dentro del nivel actual, limitado al rango de la barra.
    val progreso = when (nivel) {
        "BRONCE" -> (puntos.toFloat() / minimoPlata).coerceIn(0f, 1f)
        "PLATA" -> ((puntos - minimoPlata).toFloat() / (minimoOro - minimoPlata)).coerceIn(0f, 1f)
        "ORO" -> 1f
        else -> 0f
    }
    val siguienteNivel = when (nivel) {
        "BRONCE" -> "Plata"
        "PLATA" -> "Oro"
        else -> null
    }
    val puntosRestantes = when (nivel) {
        "BRONCE" -> (minimoPlata - puntos).coerceAtLeast(0L)
        "PLATA" -> (minimoOro - puntos).coerceAtLeast(0L)
        else -> 0L
    }

    // Recupera la fotografía local y los datos personales al cambiar de cuenta.
    LaunchedEffect(user?.uid) {
        if (user == null) {
            error = "No hay una sesión iniciada."
            loading = false
            return@LaunchedEffect
        }
        photo = withContext(Dispatchers.IO) { LocalProfilePhotoStore.load(context, user.uid) }
        Firebase.firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                firstName = doc.getString("firstName").orEmpty()
                lastName = doc.getString("lastName").orEmpty()
                birthDate = doc.getString("birthDate").orEmpty()
                address = doc.getString("homeAddress").orEmpty()
                savedFirstName = firstName
                savedLastName = lastName
                savedAddress = address
                loading = false
            }
            .addOnFailureListener { e ->
                error = "No se pudo cargar el perfil: ${e.localizedMessage}"
                loading = false
            }
    }

    // Selecciona y guarda la fotografía en el almacenamiento privado del dispositivo.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && user != null) {
            photoLoading = true
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    LocalProfilePhotoStore.decodeSelectedPhoto(context, uri)
                }
                if (decoded == null) {
                    error = "No se pudo leer la fotografía."
                } else {
                    val saved = withContext(Dispatchers.IO) {
                        LocalProfilePhotoStore.save(context, user.uid, decoded)
                    }
                    if (saved) {
                        photo = decoded
                        error = null
                        Toast.makeText(context, "Fotografía actualizada en este dispositivo.", Toast.LENGTH_SHORT).show()
                    } else error = "No se pudo guardar la fotografía."
                }
                photoLoading = false
            }
        }
    }

    // Estilo compartido por las tarjetas del perfil.
    val cardColors = CardDefaults.cardColors(containerColor = Color.White)
    val cardBorder = BorderStroke(1.dp, navy.copy(alpha = 0.65f))
    Column(
        modifier = Modifier.fillMaxSize().background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Mi perfil", fontSize = 27.sp, fontWeight = FontWeight.Bold,
            color = navy, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        Card(colors = cardColors, border = cardBorder, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(104.dp)) {
                    Box(
                        modifier = Modifier.size(94.dp).align(Alignment.TopStart)
                            .clip(CircleShape).background(pale)
                            .clickable(enabled = !photoLoading && !saving && user != null) { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoLoading) CircularProgressIndicator(color = navy, modifier = Modifier.size(30.dp))
                        else if (photo != null) Image(
                            bitmap = photo!!.asImageBitmap(), contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        else Icon(painterResource(R.drawable.ic_person_profile), "Foto de perfil", tint = navy, modifier = Modifier.size(48.dp))
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(34.dp)
                            .border(2.dp, Color.White, CircleShape).clip(CircleShape)
                            .background(navy).clickable(enabled = !photoLoading && user != null) {
                                if (photo == null) photoPicker.launch("image/*")
                                else scope.launch {
                                    val removed = withContext(Dispatchers.IO) {
                                        LocalProfilePhotoStore.delete(context, user!!.uid)
                                    }
                                    if (removed) { photo = null; Toast.makeText(context, "Foto eliminada de este dispositivo.", Toast.LENGTH_SHORT).show() }
                                    else error = "No se pudo eliminar la fotografía."
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(if (photo == null) R.drawable.ic_camera_profile else R.drawable.ic_delete_profile),
                            contentDescription = if (photo == null) "Agregar foto" else "Eliminar foto",
                            tint = Color.White, modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Usuario de BookMaster" },
                    fontSize = 19.sp, fontWeight = FontWeight.Bold, color = navy, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(user?.email.orEmpty(), fontSize = 13.sp, color = Color(0xFF6C617B), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                // Al cancelar la edición, restaura los últimos datos guardados.
                OutlinedButton(onClick = {
                    if (editing) {
                        firstName = savedFirstName
                        lastName = savedLastName
                        address = savedAddress
                    }
                    editing = !editing
                    error = null
                }, enabled = !loading && !saving,
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_arrow),
                        contentDescription = null,
                        tint = purple,
                        modifier = Modifier.size(20.dp).rotate(if (editing) 90f else 0f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (editing) "Cancelar edición" else "Editar mis datos", color = navy)
                }
            }
        }

        Card(colors = cardColors, border = cardBorder, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Datos personales", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = navy,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (editing) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { if (it.length <= 60) { firstName = it; error = null } },
                        label = { Text("Nombres") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = firstName.isNotBlank() && !validarNombrePerfil(firstName),
                        supportingText = if (firstName.isNotBlank() && !validarNombrePerfil(firstName)) {
                            { Text("Usá de 2 a 60 caracteres: letras, espacios, guiones o apóstrofes.") }
                        } else null,
                        colors = profileFieldColors(purple),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { if (it.length <= 60) { lastName = it; error = null } },
                        label = { Text("Apellidos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = lastName.isNotBlank() && !validarNombrePerfil(lastName),
                        supportingText = if (lastName.isNotBlank() && !validarNombrePerfil(lastName)) {
                            { Text("Usá de 2 a 60 caracteres: letras, espacios, guiones o apóstrofes.") }
                        } else null,
                        colors = profileFieldColors(purple),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )
                } else {
                    ProfileDataLine("Nombres", firstName.ifBlank { "No disponible" })
                    ProfileDataLine("Apellidos", lastName.ifBlank { "No disponible" })
                }
                HorizontalDivider(color = pale)
                // Dirección de casa opcional: se usa únicamente si el cliente solicita envío a domicilio.
                if (editing) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { if (it.length <= 300) { address = it; error = null } },
                        label = { Text("Dirección de casa (opcional)") },
                        placeholder = { Text("Departamento, municipio, colonia, calle, casa y referencia") },
                        supportingText = { Text("Para entregas a domicilio. Máximo 300 caracteres.") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = profileFieldColors(purple),
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                } else {
                    ProfileDataLine("Dirección de casa", address.ifBlank { "Sin registrar" })
                }
                HorizontalDivider(color = pale)
                // Guardar inmediatamente después de la dirección y ANTES de los datos protegidos.
                if (editing) Button(
                    onClick = {
                        val currentUser = user ?: return@Button
                        if (!validarNombrePerfil(firstName) || !validarNombrePerfil(lastName)) {
                            error = "Los nombres y apellidos son obligatorios: 2 a 60 caracteres, solo letras, espacios, guiones o apóstrofes."
                            return@Button
                        }
                        saving = true; error = null
                        // Actualiza únicamente los nombres y la dirección mediante el repositorio.
                        UserProfileRepository.updateNamesAndAddress(currentUser, firstName, lastName, address) { ok, message ->
                            saving = false
                            if (ok) {
                                savedFirstName = firstName.trim()
                                savedLastName = lastName.trim()
                                savedAddress = address.trim()
                                firstName = savedFirstName
                                lastName = savedLastName
                                address = savedAddress
                                editing = false
                                Toast.makeText(context, "Datos actualizados correctamente.", Toast.LENGTH_SHORT).show()
                            }
                            else error = message ?: "No se pudieron guardar los cambios."
                        }
                    }, enabled = !saving && !loading && user != null,
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = purple)
                ) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Guardar cambios") }
                HorizontalDivider(color = pale)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_profile_email), null, tint = purple, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Correo electrónico", fontSize = 12.sp, color = Color.Gray)
                        Text(user?.email.orEmpty(), color = Color(0xFF80738C))
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_profile_lock),
                        contentDescription = "Correo no editable",
                        tint = locked,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(color = pale)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_calendar_purple), null, tint = purple, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fecha de nacimiento (no editable)", fontSize = 12.sp, color = Color.Gray)
                        Text(birthDate.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else birthDate }.ifBlank { "No disponible" }, color = Color(0xFF80738C))
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_profile_lock),
                        contentDescription = "Fecha de nacimiento no editable",
                        tint = locked,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Este bloque va ANTES de «Mi membresía», conservando el resto del perfil.
        Card(colors = cardColors, border = cardBorder, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_checkout_cash), contentDescription = null, tint = navy, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Mis métodos de pago", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = navy)
                        Text("Tarjetas para tus compras", fontSize = 13.sp, color = Color(0xFF6C617B))
                    }
                }
                HorizontalDivider(color = pale)
                DemoPaymentMethods(manage = true)
            }
        }

        // Membresía real: se consulta desde Firestore, sin editarla desde el perfil.
        Card(colors = cardColors, border = cardBorder, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_crown_award),
                        contentDescription = null,
                        tint = navy,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Mi membresía", fontWeight = FontWeight.Bold, color = navy, fontSize = 17.sp)
                        Text(
                            "Tu progreso y beneficios",
                            color = Color(0xFF6C617B), fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = pale)
                Surface(
                    color = pale,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (membership.loading) "CONSULTANDO MEMBRESÍA" else "NIVEL ${nivel ?: "SIN CONFIGURAR"}", color = navy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(if (membership.loading || membership.error) "—" else puntos.toString(), color = navy, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp)
                                Text("puntos acumulados", color = Color(0xFF6C617B), fontSize = 12.sp)
                                if (nivel != null) Text(
                                    "Descuento activo: ${MembershipPricing.label(nivel)}",
                                    color = navy, fontSize = 12.sp
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_profile_award),
                                contentDescription = "Membresía ${nivel ?: "sin configurar"}",
                                tint = when (membership.level) {
                                    "BRONCE" -> Color(0xFFCD7F32)
                                    "PLATA" -> Color(0xFFC0C0C0)
                                    "ORO" -> Color(0xFFFFD700)
                                    else -> Color(0xFF8794A3)
                                },
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Text(
                            when {
                                membership.loading -> "Consultando tus puntos…"
                                membership.error || nivel == null -> "No se pudo verificar tu membresía. Revisá Firestore."
                                siguienteNivel == null -> "¡Alcanzaste el nivel máximo!"
                                else -> "$puntosRestantes puntos para llegar a $siguienteNivel"
                            },
                            color = navy,
                            fontSize = 12.sp
                        )
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0xFFD7C5ED))) {
                            Box(Modifier.fillMaxWidth(progreso).fillMaxHeight().clip(CircleShape).background(navy))
                        }
                    }
                }
            }
        }

        // Muestra los errores de carga, edición o gestión de la fotografía.
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB44141))
        ) {
            Icon(painterResource(R.drawable.ic_profile_logout), null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Solicita confirmación antes de cerrar la sesión; cancelar mantiene la cuenta activa.
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    "¿Cerrar sesión?", color = navy,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "¿Seguro que querés cerrar sesión y volver al inicio de sesión?",
                    color = navy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        border = BorderStroke(1.dp, purple)
                    ) {
                        Text("Cancelar", color = navy)
                    }

                    Spacer(Modifier.width(24.dp))

                    Button(
                        onClick = {
                            confirmLogout = false
                            // Delega el cierre de sesión y la navegación al callback recibido.
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB44141)),
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

@Composable
private fun ProfileDataLine(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, color = Color(0xFF473071), fontWeight = FontWeight.Medium)
    }
}

/** Paleta morada reutilizada únicamente en los campos editables del cliente. */
@Composable
private fun profileFieldColors(purple: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = purple,
    focusedLabelColor = purple,
    cursorColor = purple,
    unfocusedBorderColor = Color(0xFFCAC4D0),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
/** Validación de interfaz; el repositorio también debe validar antes de guardar en Firestore. */
private fun validarNombrePerfil(valor: String): Boolean {
    val limpio = valor.trim()
    if (limpio.length !in 2..60) return false
    return Regex("""^[\p{L}\p{M}]+(?:[ '\-][\p{L}\p{M}]+)*$""").matches(limpio)
}