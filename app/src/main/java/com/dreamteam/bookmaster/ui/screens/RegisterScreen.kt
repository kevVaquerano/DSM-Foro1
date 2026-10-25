package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Pantalla de registro de nuevos usuarios.
 *
 * Recopila los datos personales, valida el formulario
 * y crea la cuenta utilizando Firebase Authentication.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Servicios utilizados para crear y guardar las cuentas.
    val auth = remember { Firebase.auth }
    val firestore = remember { Firebase.firestore }

    // Almacenan temporalmente los datos ingresados.
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Controlan el proceso de registro y los mensajes de error.
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Contenedor del formulario con desplazamiento vertical.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Encabezado del formulario.
        Text(
            text = "Registro de Usuario",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Solicita el nombre completo del nuevo usuario.
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = {
                Text("Nombre Completo")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Solicita el correo que se utilizará para iniciar sesión.
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = {
                Text("Correo Electrónico")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Oculta la contraseña mientras el usuario la escribe.
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = {
                Text("Contraseña")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Permite comprobar que ambas contraseñas coincidan.
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = {
                Text("Confirmar Contraseña")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Presenta los errores encontrados en el formulario.
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        /**
         * Valida la información antes de crear la cuenta.
         *
         * Comprueba los campos obligatorios, el correo,
         * la longitud de la contraseña y su confirmación.
         */
        Button(
            onClick = {
                when {
                    name.isBlank() ||
                            email.isBlank() ||
                            password.isBlank() ||
                            confirmPassword.isBlank() -> {
                        errorMessage = "Todos los campos son obligatorios."
                        return@Button
                    }

                    !email.contains("@") -> {
                        errorMessage = "Ingrese un correo válido."
                        return@Button
                    }

                    password.length < 6 -> {
                        errorMessage =
                            "La contraseña debe tener al menos 6 caracteres."
                        return@Button
                    }

                    password != confirmPassword -> {
                        errorMessage = "Las contraseñas no coinciden."
                        return@Button
                    }
                }

                // Activa la carga y limpia los mensajes anteriores.
                isLoading = true
                errorMessage = null

                // Crea la cuenta mediante correo y contraseña.
                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password.trim()
                )
                    .addOnSuccessListener { result ->

                        // Recupera el identificador del usuario creado.
                        val uid = result.user?.uid

                        if (uid == null) {
                            isLoading = false
                            errorMessage =
                                "No se pudo obtener el usuario registrado."
                        } else {

                            // Prepara la información que se guardará.
                            val userData = mapOf(
                                "uid" to uid,
                                "name" to name.trim(),
                                "email" to email.trim(),
                                "createdAt" to System.currentTimeMillis()
                            )

                            // Almacena el perfil en la colección de usuarios.
                            firestore.collection("users")
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    onRegisterSuccess()
                                }
                                .addOnFailureListener { exception ->
                                    isLoading = false

                                    errorMessage =
                                        "Cuenta creada, pero falló el guardado " +
                                                "en base de datos: " +
                                                "${exception.localizedMessage}"
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        isLoading = false

                        errorMessage = exception.localizedMessage
                            ?: "Error al registrar usuario."
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Muestra un indicador mientras Firebase procesa el registro.
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Registrarse")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Regresa al inicio de sesión si el usuario ya tiene cuenta.
        TextButton(
            onClick = onNavigateToLogin,
            enabled = !isLoading
        ) {
            Text("¿Ya tienes cuenta? Inicia Sesión")
        }
    }
}