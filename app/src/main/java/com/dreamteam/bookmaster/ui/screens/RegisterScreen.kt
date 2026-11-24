package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.util.Patterns
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.data.UserProfileRepository
import com.dreamteam.bookmaster.data.LocalProfilePhotoStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Registro con correo y Google, manteniendo los perfiles existentes de BookMaster. */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onCompleteProfileNeeded: () -> Unit
) {
    // Contexto y servicios de autenticación, base de datos y tareas asíncronas.
    val context = LocalContext.current
    val auth = remember { Firebase.auth }
    val firestore = remember { Firebase.firestore }
    val scope = rememberCoroutineScope()
    var selectedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var loadingPhoto by remember { mutableStateOf(false) }


    // Paleta de colores de la pantalla de registro.
    val morado = Color(0xFF7952B5)
    val moradoOscuro = Color(0xFF473071)
    val fondoLila = Color(0xFFD8C9F0)
    val fondoCampo = Color(0xFFE7E4EB)

    // Datos del formulario y estados de visibilidad, carga y error.
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") } // ISO: yyyy-MM-dd
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // El selector de fotos de Android no requiere permiso para toda la galería.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            loadingPhoto = true
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    LocalProfilePhotoStore.decodeSelectedPhoto(context, uri)
                }
                loadingPhoto = false
                if (bitmap != null) {
                    selectedPhoto = bitmap
                } else {
                    errorMessage = "No se pudo leer la fotografía seleccionada."
                }
            }
        }
    }

    // Selector de fecha de nacimiento con límite anterior al día actual.
    val birthPicker = remember(context) {
        DatePickerDialog(context, { _, year, month, day ->
            birthDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year,
                month + 1,
                day
            )
            errorMessage = null
        }, 2000, 0, 1).apply {

            // Permitir únicamente fechas anteriores a hoy.
            val inicioDeHoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            datePicker.maxDate = inicioDeHoy - 1L
        }
    }

    // Configuración de OAuth para obtener el token de Google.
    val webClientId = remember(context) {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id != 0) context.getString(id) else ""
    }
    val googleOptions = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleClient = remember(googleOptions) { GoogleSignIn.getClient(context, googleOptions) }
    // Recibe el resultado de Google y autentica la cuenta en Firebase.
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            isLoading = false
            errorMessage = "Se canceló el acceso con Google."
        } else {
            try {
                val googleAccount = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = googleAccount.idToken
                if (token.isNullOrBlank()) {
                    isLoading = false
                    errorMessage = "Google no devolvió un token válido."
                } else {
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            if (user == null) {
                                isLoading = false
                                errorMessage = "No se pudo obtener la cuenta de Firebase."
                            } else {
                                // Conserva datos previos; no reemplaza role ni createdAt.
                                UserProfileRepository.prepareProfile(user, "Google") { complete, error ->
                                    isLoading = false
                                    if (error != null) {
                                        errorMessage = "No se pudo preparar el perfil: $error"
                                    } else if (complete) {
                                        onRegisterSuccess()
                                    } else {
                                        onCompleteProfileNeeded()
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            isLoading = false
                            errorMessage = exception.localizedMessage ?: "No se pudo acceder con Google."
                        }
                }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Error de Google Sign-In (${e.statusCode})."
            }
        }
    }

    // Componentes pequeños para que los campos mantengan el mismo estilo.
    @Composable
    fun FormField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        hint: String,
        keyboardType: KeyboardType = KeyboardType.Text,
        isPassword: Boolean = false,
        visible: Boolean = false,
        onToggle: (() -> Unit)? = null
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = moradoOscuro)
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); errorMessage = null },
            placeholder = { Text(hint, fontSize = 14.sp) },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onToggle?.invoke() }, enabled = !isLoading) {
                        Icon(
                            painter = painterResource(
                                if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                            ),
                            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = moradoOscuro,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            } else null,
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

    // Formulario desplazable que se adapta al teclado y a las barras del sistema.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoLila)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFFCFAFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(15.dp))
            Text("BookMaster", fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.5.sp, color = moradoOscuro)
            Spacer(Modifier.height(28.dp))
            Text("Crear cuenta:", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = moradoOscuro)
            Spacer(Modifier.height(28.dp))

            // Avatar con botón superpuesto, inspirado en la foto de perfil de Google.
            // Camarita: elegir foto. Basurero: quitarla y recuperar el avatar.
            Box(modifier = Modifier.size(100.dp)) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(Color(0xFFE7DCF5))
                        .clickable(enabled = !isLoading && !loadingPhoto) {
                            photoPicker.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (loadingPhoto) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = morado
                        )
                    } else if (selectedPhoto != null) {
                        Image(
                            bitmap = selectedPhoto!!.asImageBitmap(),
                            contentDescription = "Fotografía de perfil seleccionada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_person_profile),
                            contentDescription = "Seleccionar foto de perfil",
                            tint = morado,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                if (!loadingPhoto) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(moradoOscuro)
                            .clickable(enabled = !isLoading) {
                                if (selectedPhoto == null) {
                                    photoPicker.launch("image/*")
                                } else {
                                    selectedPhoto = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (selectedPhoto == null) R.drawable.ic_camera_profile
                                else R.drawable.ic_delete_profile
                            ),
                            contentDescription = if (selectedPhoto == null) {
                                "Agregar foto de perfil"
                            } else {
                                "Quitar foto de perfil"
                            },
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // Campos personales y selección de fecha de nacimiento.
            Text("Foto de perfil (opcional)", fontSize = 13.sp, color = moradoOscuro)
            Spacer(Modifier.height(12.dp))

            Box(Modifier.fillMaxWidth()) {
                Column {
                    FormField(firstName, { firstName = it }, "Nombres:", "Ingresa tus nombres")
                    FormField(lastName, { lastName = it }, "Apellidos:", "Ingresa tus apellidos")
                    FormField(email, { email = it }, "Correo electrónico:", "Ingresa tu correo electrónico", KeyboardType.Email)

                    Text("Fecha de nacimiento:", fontWeight = FontWeight.SemiBold, color = moradoOscuro)
                    Spacer(Modifier.height(5.dp))
                    OutlinedButton(
                        onClick = { birthPicker.show() }, enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = fondoCampo, contentColor = moradoOscuro),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Text(
                            text = if (birthDate.isBlank()) {
                                "Selecciona tu fecha de nacimiento"
                            } else {
                                val parts = birthDate.split("-")
                                if (parts.size == 3) {
                                    "${parts[2]}/${parts[1]}/${parts[0]}"
                                } else {
                                    birthDate
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (birthDate.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                moradoOscuro
                            }
                        )
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar_purple),
                                contentDescription = "Abrir calendario",
                                tint = morado,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    FormField(password, { password = it }, "Contraseña:", "Crea una contraseña",
                        KeyboardType.Password, isPassword = true, visible = showPassword,
                        onToggle = { showPassword = !showPassword })
                    FormField(confirmPassword, { confirmPassword = it }, "Confirmar contraseña:", "Repite tu contraseña",
                        KeyboardType.Password, isPassword = true, visible = showConfirmPassword,
                        onToggle = { showConfirmPassword = !showConfirmPassword })
                }
            }

            Spacer(Modifier.height(10.dp))
            // No enlazamos textos legales a páginas inexistentes.
            Text(
                "Al continuar, aceptas los Términos de Uso de BookMaster y reconoces nuestra Política de Privacidad.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                textAlign = TextAlign.Center, color = Color(0xFF736D7A)
            )
            Spacer(Modifier.height(25.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
            }

            Button(
                // Valida los datos antes de crear una cuenta con correo y contraseña.
                onClick = {
                    when {
                        firstName.isBlank() || lastName.isBlank() || email.isBlank() || birthDate.isBlank() ||
                                password.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = "Completá todos los campos obligatorios."
                        }
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            errorMessage = "Ingresá un correo electrónico válido."
                        }
                        !UserProfileRepository.isValidBirthDate(birthDate) -> {
                            errorMessage = "Seleccioná una fecha de nacimiento válida."
                        }
                        password.length < 6 -> {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                        }
                        password != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden."
                        }
                        else -> {
                            isLoading = true
                            errorMessage = null
                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid == null) {
                                        isLoading = false
                                        errorMessage = "No se recibió el UID de la cuenta creada."
                                    } else {
                                        // Inicializa el perfil de cliente con membresía Bronce y cero puntos.
                                        val data = mapOf(
                                            "uid" to uid,
                                            "firstName" to firstName.trim(),
                                            "lastName" to lastName.trim(),
                                            "birthDate" to birthDate,
                                            "photoUrl" to null,
                                            "email" to email.trim(),
                                            "provider" to "password",
                                            "role" to "customer",
                                            "membershipLevel" to "BRONCE",
                                            "loyaltyPoints" to 0L,
                                            "createdAt" to FieldValue.serverTimestamp()
                                        )
                                        firestore.collection("users").document(uid)
                                            .set(data, SetOptions.merge())
                                            .addOnSuccessListener {
                                                // Primero se confirmó el perfil en Firestore.
                                                // Guardamos la foto solo en este dispositivo, usando el UID.
                                                scope.launch {
                                                    val foto = selectedPhoto
                                                    val saved = if (foto == null) true else withContext(Dispatchers.IO) {
                                                        LocalProfilePhotoStore.save(context, uid, foto)
                                                    }
                                                    isLoading = false
                                                    if (!saved) {
                                                        Toast.makeText(context,
                                                            "Cuenta creada, pero no se pudo guardar la foto local.",
                                                            Toast.LENGTH_LONG).show()
                                                    }
                                                    onRegisterSuccess()
                                                }
                                            }
                                            // Si falla el perfil, la cuenta de autenticación ya existe y puede recuperarse al iniciar sesión.
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "Cuenta creada, pero falló el perfil: ${e.localizedMessage}. Iniciá sesión para reintentar."
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage = e.localizedMessage ?: "No se pudo registrar la cuenta."
                                }
                        }
                    }
                },
                enabled = !isLoading && !loadingPhoto,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = morado),
                shape = RoundedCornerShape(9.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Continuar", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(25.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  o  ", color = moradoOscuro, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.weight(1f))
            }
            Spacer(Modifier.height(25.dp))

            OutlinedButton(
                // Cierra la sesión previa de Google antes de abrir el selector de cuenta.
                onClick = {
                    if (webClientId.isBlank()) {
                        errorMessage = "No se encontró default_web_client_id. Revisá Firebase y Gradle."
                    } else {
                        isLoading = true
                        errorMessage = null
                        googleClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(49.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD7CEDF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Registrarme con Google", color = Color(0xFF333333))
            }
            Spacer(Modifier.height(15.dp))

            // Permite regresar al inicio de sesión si el usuario ya tiene cuenta.
            TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = moradoOscuro, fontSize = 16.sp)
            }
        }
    }
}