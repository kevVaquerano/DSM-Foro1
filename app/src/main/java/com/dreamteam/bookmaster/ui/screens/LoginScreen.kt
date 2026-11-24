package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.dreamteam.bookmaster.data.UserProfileRepository
import kotlinx.coroutines.delay

/**
 * Pantalla de inicio de sesión de BookMaster.
 *
 * Permite ingresar mediante correo y contraseña o con Google.
 * También incluye el gatito interactivo con efectos de sonido.
 */
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onCompleteProfileNeeded: () -> Unit
) {
    val context = LocalContext.current

    // Servicios de Firebase utilizados para autenticar usuarios.
    val auth = remember { Firebase.auth }
    val firestore = remember { Firebase.firestore }

    // Colores del diseño morado.
    val morado = Color(0xFF7952B5)
    val moradoOscuro = Color(0xFF473071)
    val lilaFondo = Color(0xFFD8C9F0)
    val fondoCampo = Color(0xFFE7E4EB)

    // Estados de los campos y del proceso de autenticación.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Antes de preparar un perfil de cliente, comprobar si la cuenta pertenece al administrador.
    // Nunca crear ni cambiar roles desde la aplicación.
    fun continuarSegunRol(user: com.google.firebase.auth.FirebaseUser, provider: String) {
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { profile ->
                if (auth.currentUser?.uid != user.uid) return@addOnSuccessListener
                if (profile.exists() && profile.getString("role") == "admin" &&
                    profile.getString("uid") == user.uid) {
                    isLoading = false
                    onNavigateToAdmin()
                } else if (profile.exists() && profile.getString("role") == "admin") {
                    // Perfil administrativo mal configurado: impedir el acceso al módulo cliente.
                    isLoading = false
                    auth.signOut()
                    errorMessage = "El perfil de administrador no coincide con la cuenta. Revisá el UID en Firestore."
                } else if (profile.exists() && profile.getString("role") !in listOf(null, "customer")) {
                    isLoading = false
                    auth.signOut()
                    errorMessage = "Esta cuenta no tiene un rol autorizado."
                } else {
                    // Conserva el perfil existente y decide si debe completarse antes de entrar.
                    UserProfileRepository.prepareProfile(user, provider) { complete, error ->
                        isLoading = false
                        if (error != null) errorMessage = "No se pudo preparar tu perfil: $error"
                        else if (complete) onNavigateToHome()
                        else onCompleteProfileNeeded()
                    }
                }
            }
            .addOnFailureListener { exception ->
                isLoading = false
                errorMessage = "No se pudo verificar el tipo de cuenta: ${exception.localizedMessage}"
            }
    }

    // Obtiene el identificador OAuth generado por Firebase.
    val webClientId = remember(context) {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )

        if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            ""
        }
    }

    // Prepara la solicitud de acceso a la cuenta de Google.
    val googleSignInOptions = remember(webClientId) {
        GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(googleSignInOptions) {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    /**
     * Recibe el resultado de Google Sign-In.
     * Si Google devuelve una cuenta válida, utiliza su token
     * para iniciar sesión mediante Firebase Authentication.
     */
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            // Extrae la cuenta seleccionada del resultado de Google.
            val task = GoogleSignIn.getSignedInAccountFromIntent(
                result.data
            )

            try {
                val account = task.getResult(
                    ApiException::class.java
                )

                val idToken = account?.idToken

                if (idToken.isNullOrEmpty()) {
                    isLoading = false
                    errorMessage = "No se pudo obtener el token de Google."
                } else {
                    val credential = GoogleAuthProvider.getCredential(
                        idToken,
                        null
                    )

                    isLoading = true
                    errorMessage = null

                    // Autentica la cuenta de Google con Firebase.
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user

                            if (user == null) {
                                isLoading = false
                                errorMessage = "No se pudo obtener el usuario."
                            } else {

                                // Nunca reemplazar el perfil existente con .set(userData).
                                continuarSegunRol(user, "Google")
                            }
                        }
                        .addOnFailureListener { exception ->
                            isLoading = false
                            errorMessage = exception.localizedMessage
                                ?: "Error al autenticar con Firebase."
                        }
                }

            } catch (exception: ApiException) {
                isLoading = false
                errorMessage =
                    "Error en Google Sign-In (${exception.statusCode}): " +
                            "${exception.message}"
            }


        } else {
            isLoading = false

            // Recupera el código de error de Google para facilitar el diagnóstico.
            val googleTask = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)

            try {
                googleTask.getResult(ApiException::class.java)

                errorMessage =
                    "Google no completó el inicio de sesión. " +
                            "Resultado: ${result.resultCode}"

            } catch (e: ApiException) {

                errorMessage =
                    "Error de Google Sign-In. Código: ${e.statusCode}"

                Log.e(
                    "BookMasterGoogle",
                    "Fallo de Google Sign-In. " +
                            "Resultado: ${result.resultCode}, " +
                            "Estado: ${e.statusCode}",
                    e
                )
            }
        }
    }

    // Crea un reproductor ligero para los sonidos del gatito.
    val reproductorGato = remember(context) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )
            .build()
    }

    // Identificadores y estados de carga de cada sonido.
    var idMaullido by remember { mutableIntStateOf(0) }
    var idRonroneo by remember { mutableIntStateOf(0) }
    var maullidoListo by remember { mutableStateOf(false) }
    var ronroneoListo by remember { mutableStateOf(false) }

    // Guarda el canal activo para detener el ronroneo al soltar.
    var canalRonroneo by remember { mutableIntStateOf(0) }

    /**
     * Carga los audios al entrar en la pantalla.
     * Libera el reproductor cuando la pantalla deja de utilizarse.
     */
    DisposableEffect(reproductorGato) {
        reproductorGato.setOnLoadCompleteListener {
                _, sampleId, status ->

            if (sampleId == idMaullido) {
                maullidoListo = status == 0
            }

            if (sampleId == idRonroneo) {
                ronroneoListo = status == 0
            }
        }

        idMaullido = reproductorGato.load(
            context,
            R.raw.maullido,
            1
        )

        idRonroneo = reproductorGato.load(
            context,
            R.raw.ronroneo,
            1
        )

        onDispose {
            if (canalRonroneo != 0) {
                reproductorGato.stop(canalRonroneo)
            }

            reproductorGato.release()
        }
    }

    // El gatito se sacude brevemente cada 4 segundos mientras no lo tocan.
    // En el emulador, también se detiene al pasar el cursor por encima.
    val interaccionGatito = remember { MutableInteractionSource() }
    val cursorSobreGatito by interaccionGatito.collectIsHoveredAsState()
    var presionandoGatito by remember { mutableStateOf(false) }
    val rotacionGatito = remember { Animatable(0f) }

    LaunchedEffect(cursorSobreGatito, presionandoGatito) {
        if (cursorSobreGatito || presionandoGatito) {
            rotacionGatito.snapTo(0f)
        } else {
            while (true) {
                delay(3300)
                rotacionGatito.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 700
                        0f at 0
                        -9f at 160
                        8f at 300
                        -6f at 440
                        4f at 560
                        0f at 700
                    }
                )
            }
        }
    }

    // Diseño principal de la pantalla de inicio de sesión.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lilaFondo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFFCFAFF),
                    RoundedCornerShape(24.dp)
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Encabezado del formulario.
            Spacer(modifier = Modifier.height(15.dp))

            // Nombre principal de la aplicación.
            Text(
                text = "BookMaster",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.5.sp,
                color = moradoOscuro
            )

            Spacer(modifier = Modifier.height(10.dp))

            /**
             * Gatito interactivo:
             * un toque reproduce el maullido y una pulsación
             * prolongada reproduce el ronroneo.
             */
            Image(
                painter = painterResource(
                    R.drawable.gato_bookmaster
                ),
                contentDescription =
                    "Gatito: toca para maullar o mantén presionado para ronronear",
                modifier = Modifier
                    .size(145.dp)
                    .graphicsLayer { rotationZ = rotacionGatito.value }
                    .hoverable(interaccionGatito)
                    .pointerInput(
                        reproductorGato,
                        maullidoListo,
                        ronroneoListo
                    ) {
                        detectTapGestures(
                            onTap = {
                                if (maullidoListo) {
                                    reproductorGato.play(
                                        idMaullido,
                                        1f, 1f,
                                        1, 0, 1f
                                    )
                                }
                            },
                            onLongPress = {
                                if (ronroneoListo) {
                                    if (canalRonroneo != 0) {
                                        reproductorGato.stop(canalRonroneo)
                                    }

                                    canalRonroneo = reproductorGato.play(
                                        idRonroneo,
                                        1f, 1f,
                                        1, -1, 1f
                                    )
                                }
                            },
                            onPress = {
                                presionandoGatito = true
                                try {
                                    // Espera a que termine la pulsación para detener el sonido.
                                    tryAwaitRelease()
                                } finally {
                                    presionandoGatito = false
                                    if (canalRonroneo != 0) {
                                        reproductorGato.stop(canalRonroneo)
                                        canalRonroneo = 0
                                    }
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Iniciar sesión:",
                color = moradoOscuro,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(25.dp))

            // Campo para ingresar el correo electrónico.
            Text(
                text = "Correo electrónico:",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium,
                color = moradoOscuro
            )

            Spacer(modifier = Modifier.height(5.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = { Text("Ingresa tu correo electrónico", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                shape = RoundedCornerShape(9.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fondoCampo,
                    unfocusedContainerColor = fondoCampo,
                    focusedBorderColor = morado,
                    unfocusedBorderColor = Color(0xFFCAC4D0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de contraseña con caracteres ocultos.
            Text(
                text = "Contraseña:",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Medium,
                color = moradoOscuro
            )

            Spacer(modifier = Modifier.height(5.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text("Ingresa tu contraseña", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            painter = painterResource(
                                if (showPassword) R.drawable.ic_visibility
                                else R.drawable.ic_visibility_off
                            ),
                            contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = moradoOscuro,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(9.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = fondoCampo,
                    unfocusedContainerColor = fondoCampo,
                    focusedBorderColor = morado,
                    unfocusedBorderColor = Color(0xFFCAC4D0)
                )
            )

            // Presenta los mensajes de validación o autenticación.
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Comprueba los datos y solicita el acceso con Firebase.
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() -> {
                            errorMessage =
                                "Por favor completa todos los campos"
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            errorMessage = "Ingresa un correo válido"
                        }

                        else -> {
                            isLoading = true
                            errorMessage = null

                            // Firebase autentica las credenciales; el rol se verifica después.
                            auth.signInWithEmailAndPassword(
                                email.trim(),
                                password
                            )
                                .addOnSuccessListener { result ->
                                    val user = result.user
                                    if (user == null) {
                                        isLoading = false
                                        errorMessage = "No se encontró el usuario autenticado"
                                    } else {
                                        continuarSegunRol(user, "password")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false
                                    errorMessage =
                                        exception.localizedMessage
                                            ?: "Error al iniciar sesión"
                                }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = morado
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Iniciar sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Separador visual entre las dos opciones de acceso.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "  o  ",
                    color = moradoOscuro,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(21.dp))

            // Inicia el proceso de autenticación con Google.
            OutlinedButton(
                onClick = {
                    if (webClientId.isEmpty()) {
                        errorMessage =
                            "No se encontró default_web_client_id. Sincroniza Gradle."
                    } else {
                        isLoading = true
                        errorMessage = null

                        // Cierra la sesión previa de Google para permitir elegir otra cuenta.
                        googleSignInClient.signOut()
                            .addOnCompleteListener {
                                googleLauncher.launch(
                                    googleSignInClient.signInIntent
                                )
                            }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(49.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = moradoOscuro
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFD7CEDF)
                )
            ) {
                Image(
                    painter = painterResource(
                        R.drawable.ic_google
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Continuar con Google",
                    color = Color(0xFF333333)
                )
            }

            Spacer(modifier = Modifier.height(21.dp))

            // Opción para crear una cuenta nueva.
            Text(
                text = "¿No tienes usuario?",
                fontWeight = FontWeight.Medium,
                color = moradoOscuro
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onNavigateToRegister,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = morado
                )
            ) {
                Text(
                    text = "Registrarme",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}