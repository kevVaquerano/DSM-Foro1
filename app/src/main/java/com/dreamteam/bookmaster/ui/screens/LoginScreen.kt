
package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val firestore = Firebase.firestore

    // COLORES DEL NUEVO DISEÑO
    val morado = Color(0xFF7952B5)
    val moradoOscuro = Color(0xFF473071)
    val lilaFondo = Color(0xFFD8C9F0)
    val fondoCampo = Color(0xFFE7E4EB)

    // ESTADOS DEL FORMULARIO
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // =====================================
    // GOOGLE SIGN-IN - CONSERVADO
    // =====================================

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

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(gso) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts
            .StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(
                    ApiException::class.java
                )!!

                val credential = GoogleAuthProvider.getCredential(
                    account.idToken,
                    null
                )

                isLoading = true

                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->

                        val user = authResult.user

                        if (user != null) {
                            val userData = mapOf(
                                "uid" to user.uid,
                                "name" to (
                                        user.displayName
                                            ?: "Usuario de Google"
                                        ),
                                "email" to (user.email ?: ""),
                                "provider" to "Google",
                                "createdAt" to
                                        System.currentTimeMillis()
                            )

                            firestore.collection("users")
                                .document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    onNavigateToHome()
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    onNavigateToHome()
                                }
                        } else {
                            isLoading = false
                            onNavigateToHome()
                        }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = e.localizedMessage
                            ?: "Error al autenticar con Firebase"
                    }

            } catch (e: ApiException) {
                isLoading = false
                errorMessage =
                    "Error en Google Sign-In (${e.statusCode}): ${e.message}"
            }
        } else {
            isLoading = false

            errorMessage = if (result.resultCode == Activity.RESULT_CANCELED) {
                "Google canceló o no completó el inicio de sesión. " +
                        "Revisaremos la configuración OAuth y SHA-1."
            } else {
                "Google Sign-In no se completó. " +
                        "Código de resultado: ${result.resultCode}"
            }

            android.util.Log.e(
                "BookMasterGoogle",
                "Resultado Google Sign-In: ${result.resultCode}"
            )
        }
    }

    // =====================================
    // GATITO CON SONIDOS, SIN ANIMACIONES
    // =====================================

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

    var idMaullido by remember {
        mutableIntStateOf(0)
    }

    var idRonroneo by remember {
        mutableIntStateOf(0)
    }

    var maullidoListo by remember {
        mutableStateOf(false)
    }

    var ronroneoListo by remember {
        mutableStateOf(false)
    }

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
            reproductorGato.release()
        }
    }

    // =====================================
    // DISEÑO MORADO DEL LOGIN
    // =====================================

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

            // TÍTULO
            Text(
                text = "BookMaster",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = moradoOscuro
            )

            Spacer(modifier = Modifier.height(10.dp))

            // GATO ORIGINAL - SOLO SONIDOS
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
                        var canalRonroneo = 0

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
                                        reproductorGato.stop(
                                            canalRonroneo
                                        )
                                    }

                                    canalRonroneo =
                                        reproductorGato.play(
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
                                        reproductorGato.stop(
                                            canalRonroneo
                                        )
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

            // CORREO ELECTRÓNICO
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

            // CONTRASEÑA
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
                visualTransformation =
                    PasswordVisualTransformation(),
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

            // INICIAR SESIÓN CON CORREO
            Button(
                onClick = {
                    when {
                        email.isBlank() ||
                                password.isBlank() -> {
                            errorMessage =
                                "Por favor completa todos los campos"
                        }

                        !email.contains("@") -> {
                            errorMessage =
                                "Ingresa un correo válido"
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
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    errorMessage =
                                        e.localizedMessage
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
                        "Iniciar Sesión",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SEPARADOR
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

            // BOTÓN DE GOOGLE
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
                border = androidx.compose.foundation.BorderStroke(
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

            // REGISTRO
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
                    "Registrarme",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}