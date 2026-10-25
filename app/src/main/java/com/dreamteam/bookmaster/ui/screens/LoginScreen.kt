package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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

/**
 * Pantalla de inicio de sesión de BookMaster.
 *
 * Permite ingresar mediante correo y contraseña o con Google.
 * También incluye el gatito interactivo con efectos de sonido.
 */
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                                // Guarda los datos básicos del usuario en Firestore.
                                val userData = mapOf(
                                    "uid" to user.uid,
                                    "name" to (
                                            user.displayName ?: "Usuario de Google"
                                            ),
                                    "email" to (user.email ?: ""),
                                    "provider" to "Google",
                                    "createdAt" to System.currentTimeMillis()
                                )

                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userData)
                                    .addOnCompleteListener { saveTask ->

                                        // El acceso continúa aunque falle
                                        // el guardado del perfil, como en el original.
                                        if (!saveTask.isSuccessful) {
                                            Log.e(
                                                "BookMasterGoogle",
                                                "No se pudo guardar el perfil.",
                                                saveTask.exception
                                            )
                                        }

                                        isLoading = false
                                        onNavigateToHome()
                                    }
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

            // Informa si el usuario canceló el proceso de Google.
            errorMessage = if (
                result.resultCode == Activity.RESULT_CANCELED
            ) {
                "No se completó el inicio de sesión con Google."
            } else {
                "Google Sign-In no se completó. Código: ${result.resultCode}"
            }

            Log.e(
                "BookMasterGoogle",
                "Resultado Google Sign-In: ${result.resultCode}"
            )
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

    // Diseño principal de la pantalla de inicio de sesión.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lilaFondo)
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

            // Nombre principal de la aplicación.
            Text(
                text = "BookMaster",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
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
                                try {
                                    tryAwaitRelease()
                                } finally {
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
                text = "Inicia Sesión:",
                color = moradoOscuro,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
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
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Comprueba los datos y solicita el acceso con Firebase.
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() -> {
                            errorMessage =
                                "Por favor completa todos los campos"
                        }

                        !email.contains("@") -> {
                            errorMessage = "Ingresa un correo válido"
                        }

                        else -> {
                            isLoading = true
                            errorMessage = null

                            auth.signInWithEmailAndPassword(
                                email.trim(),
                                password.trim()
                            )
                                .addOnSuccessListener {
                                    isLoading = false
                                    onNavigateToHome()
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
                        text = "Iniciar Sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // Inicia el proceso de autenticación con Google.
            OutlinedButton(
                onClick = {
                    if (webClientId.isEmpty()) {
                        errorMessage =
                            "No se encontró default_web_client_id. Sincroniza Gradle."
                    } else {
                        isLoading = true
                        errorMessage = null

                        googleLauncher.launch(
                            googleSignInClient.signInIntent
                        )
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
                    text = "Sign in with Google",
                    color = Color(0xFF333333)
                )
            }

            Spacer(modifier = Modifier.height(17.dp))

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