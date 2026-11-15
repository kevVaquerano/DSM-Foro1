package com.dreamteam.bookmaster.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import com.dreamteam.bookmaster.data.LocalProfilePhotoStore
import com.dreamteam.bookmaster.data.UserProfileRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Completa un perfil antiguo o de Google sin volver a pedir correo ni contraseña. */
@Composable
fun CompleteProfileScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val user = remember { Firebase.auth.currentUser }
    val db = remember { Firebase.firestore }
    val scope = rememberCoroutineScope()

    val morado = Color(0xFF7952B5)
    val moradoOscuro = Color(0xFF473071)
    val fondoLila = Color(0xFFD8C9F0)
    val fondoCampo = Color(0xFFE7E4EB)

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var loadingPhoto by remember { mutableStateOf(false) }
    var loadingProfile by remember { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Si se elimina la foto, no se vuelve a cargar la de Google durante esta pantalla.
    var photoRemoved by remember { mutableStateOf(false) }

    // Precarga nombres de Google y respeta los campos existentes de Firestore.
    LaunchedEffect(user?.uid) {
        if (user == null) {
            error = "No hay ninguna sesión iniciada."
            loadingProfile = false
            return@LaunchedEffect
        }

        val nombreGoogle = user.displayName.orEmpty().trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        firstName = nombreGoogle.firstOrNull().orEmpty()
        lastName = nombreGoogle.drop(1).joinToString(" ")

        // La foto de Google es opcional. Si no puede descargarse, se muestra el avatar.
        val fotoGoogle = user.photoUrl?.toString()
        if (!fotoGoogle.isNullOrBlank() && !photoRemoved && selectedPhoto == null) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(fotoGoogle).openConnection().apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
            if (!photoRemoved && selectedPhoto == null) selectedPhoto = bitmap
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                firstName = doc.getString("firstName")?.takeIf { it.isNotBlank() } ?: firstName
                lastName = doc.getString("lastName")?.takeIf { it.isNotBlank() } ?: lastName
                birthDate = doc.getString("birthDate").orEmpty()
                loadingProfile = false
            }
            .addOnFailureListener {
                error = "No se pudo consultar el perfil: ${it.localizedMessage}"
                loadingProfile = false
            }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            loadingPhoto = true
            scope.launch {
                val image = withContext(Dispatchers.IO) {
                    LocalProfilePhotoStore.decodeSelectedPhoto(context, uri)
                }
                loadingPhoto = false
                if (image == null) error = "No se pudo leer la fotografía seleccionada."
                else {
                    selectedPhoto = image
                    photoRemoved = false
                    error = null
                }
            }
        }
    }

    // Último día permitido: ayer (no hoy ni fechas futuras).
    val datePicker = remember(context) {
        val ayer = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        DatePickerDialog(context, { _, year, month, day ->
            birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            error = null
        }, 2000, 0, 1).apply {
            datePicker.maxDate = ayer
        }
    }

    // Respetamos las barras de estado y navegación para que la tarjeta no las invada.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoLila)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        val alturaDisponible = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = alturaDisponible)
                    .background(Color(0xFFFCFAFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "BookMaster", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 4.5.sp, color = moradoOscuro
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    "Completar perfil:", fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, color = moradoOscuro
                )
                Spacer(Modifier.height(42.dp))

                // Mismo avatar editable de RegisterScreen (cámara / basurero).
                Box(Modifier.size(100.dp)) {
                    Box(
                        modifier = Modifier.size(92.dp).align(Alignment.TopStart)
                            .clip(CircleShape).background(Color(0xFFE7DCF5))
                            .clickable(enabled = !working && !loadingPhoto && !loadingProfile) {
                                photoPicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            loadingPhoto -> CircularProgressIndicator(Modifier.size(30.dp), color = morado)
                            selectedPhoto != null -> Image(
                                bitmap = selectedPhoto!!.asImageBitmap(),
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            else -> Icon(
                                painterResource(R.drawable.ic_person_profile),
                                contentDescription = "Agregar foto de perfil",
                                tint = morado, modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                    if (!loadingPhoto) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                                .border(2.dp, Color.White, CircleShape).clip(CircleShape)
                                .background(moradoOscuro)
                                .clickable(enabled = !working && !loadingProfile) {
                                    if (selectedPhoto == null) photoPicker.launch("image/*")
                                    else {
                                        selectedPhoto = null
                                        photoRemoved = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(
                                    if (selectedPhoto == null) R.drawable.ic_camera_profile
                                    else R.drawable.ic_delete_profile
                                ),
                                contentDescription = if (selectedPhoto == null) "Agregar foto" else "Quitar foto",
                                tint = Color.White, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Foto de perfil (opcional)", fontSize = 13.sp, color = moradoOscuro)
                Spacer(Modifier.height(31.dp))

                @Composable
                fun field(label: String, hint: String, value: String, onChange: (String) -> Unit) {
                    Text(label, fontWeight = FontWeight.SemiBold, color = moradoOscuro,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(
                        value = value, onValueChange = { onChange(it); error = null },
                        placeholder = { Text(hint, fontSize = 14.sp) },
                        enabled = !working && !loadingProfile, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fondoCampo,
                            unfocusedContainerColor = fondoCampo,
                            disabledContainerColor = fondoCampo,
                            focusedBorderColor = morado,
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }

                field("Nombres:", "Ingresa tus nombres", firstName) { firstName = it }
                field("Apellidos:", "Ingresa tus apellidos", lastName) { lastName = it }

                Text("Fecha de nacimiento:", fontWeight = FontWeight.SemiBold,
                    color = moradoOscuro, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(5.dp))
                OutlinedButton(
                    onClick = { datePicker.show() },
                    enabled = !working && !loadingProfile,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = fondoCampo, contentColor = moradoOscuro
                    ),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Text(
                        text = if (birthDate.isBlank()) "Selecciona tu fecha de nacimiento"
                        else birthDate.split("-").let { p ->
                            if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else birthDate
                        },
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Start,
                        fontSize = 14.sp, fontWeight = FontWeight.Normal,
                        color = if (birthDate.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else moradoOscuro
                    )
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_calendar_purple),
                            contentDescription = "Abrir calendario",
                            tint = morado, modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(34.dp))
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                }
                Button(
                    onClick = {
                        if (user == null) {
                            error = "Sesión no iniciada."
                            return@Button
                        }
                        val hoy = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val nacimientoValido = try {
                            val partes = birthDate.split("-").map { it.toInt() }
                            val nacimiento = Calendar.getInstance().apply {
                                isLenient = false
                                set(partes[0], partes[1] - 1, partes[2], 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            nacimiento.timeInMillis < hoy.timeInMillis
                        } catch (_: Exception) { false }
                        if (firstName.isBlank() || lastName.isBlank() ||
                            !UserProfileRepository.isValidBirthDate(birthDate) || !nacimientoValido
                        ) {
                            error = "Revisá nombres, apellidos y fecha de nacimiento (debe ser anterior a hoy)."
                            return@Button
                        }
                        working = true
                        error = null
                        UserProfileRepository.savePersonalInfo(
                            user, firstName, lastName, birthDate
                        ) { ok, message ->
                            if (!ok) {
                                working = false
                                error = message ?: "No se pudo guardar el perfil."
                            } else {
                                scope.launch {
                                    // Guarda la fotografía opcional en este dispositivo.
                                    val foto = selectedPhoto
                                    val saved = if (foto == null) true else withContext(Dispatchers.IO) {
                                        LocalProfilePhotoStore.save(context, user.uid, foto)
                                    }
                                    working = false
                                    if (!saved) Toast.makeText(
                                        context, "Perfil guardado, pero no se pudo guardar la fotografía.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onComplete()
                                }
                            }
                        }
                    },
                    enabled = !working && !loadingPhoto && !loadingProfile,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = morado),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    if (working || loadingProfile) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Guardar y continuar", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}