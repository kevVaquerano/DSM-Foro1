package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Obtención del ID de cliente web desde los recursos generados por google-services.json
    val webClientId = remember(context) {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resourceId != 0) context.getString(resourceId) else ""
    }

    // Configuración de Google Sign-In
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                isLoading = true
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            val userData = mapOf(
                                "uid" to user.uid,
                                "name" to (user.displayName ?: "Usuario de Google"),
                                "email" to (user.email ?: ""),
                                "provider" to "Google",
                                "createdAt" to System.currentTimeMillis()
                            )

                            // Guardar perfil en Firebase Firestore
                            firestore.collection("users").document(user.uid)
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
                        errorMessage = e.localizedMessage ?: "Error al autenticar con Firebase"
                    }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Error en Google Sign-In (${e.statusCode}): ${e.message}"
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Correo Electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Iniciar Sesión Tradicional
        Button(
            onClick = {
                when {
                    email.isBlank() || password.isBlank() -> {
                        errorMessage = "Por favor completa todos los campos"
                        return@Button
                    }
                    !email.contains("@") -> {
                        errorMessage = "Ingresa un correo válido"
                        return@Button
                    }
                }

                isLoading = true
                errorMessage = null

                auth.signInWithEmailAndPassword(email.trim(), password.trim())
                    .addOnSuccessListener {
                        isLoading = false
                        onNavigateToHome()
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Error al iniciar sesión"
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón Iniciar Sesión con Google
        OutlinedButton(
            onClick = {
                if (webClientId.isEmpty()) {
                    errorMessage = "No se encontró default_web_client_id. Sincroniza Gradle."
                    return@OutlinedButton
                }
                isLoading = true
                errorMessage = null
                val signInIntent = googleSignInClient.signInIntent
                googleLauncher.launch(signInIntent)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🌐 Continuar con Google")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}