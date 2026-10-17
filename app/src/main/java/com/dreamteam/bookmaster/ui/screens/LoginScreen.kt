package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BookMaster UDB",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Inicio de Sesión",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = "" },
            label = { Text("Correo Institucional / Usuario") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = "" },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() -> {
                            errorMessage = "Por favor complete todos los campos."
                        }
                        !email.contains("@") -> {
                            errorMessage = "Ingrese un correo electrónico válido."
                        }
                        password.length < 6 -> {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                        }
                        else -> {
                            cargando = true
                            errorMessage = ""

                            // Autenticación real con Firebase Auth
                            val auth = FirebaseAuth.getInstance()
                            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                                .addOnSuccessListener {
                                    cargando = false
                                    onNavigateToHome() // Redirige solo si las credenciales son válidas
                                }
                                .addOnFailureListener { exception ->
                                    cargando = false
                                    errorMessage = "Error al iniciar sesión: Datos incorrectos o sin conexión."
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToRegister,
            enabled = !cargando
        ) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}