package com.dreamteam.bookmaster.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dreamteam.bookmaster.data.UserProfileRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Locale

@Composable
fun CompleteProfileScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = remember(context) {
        DatePickerDialog(context, { _, year, month, day ->
            birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
        }, 2000, 0, 1).apply { datePicker.maxDate = System.currentTimeMillis() }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Completar perfil", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF473071))
        Spacer(Modifier.height(12.dp))
        Text("Para continuar, completá los datos que necesitamos para tu cuenta de BookMaster.")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(firstName, { firstName = it }, label = { Text("Nombres") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(lastName, { lastName = it }, label = { Text("Apellidos") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { picker.show() }, enabled = !working, modifier = Modifier.fillMaxWidth()) {
            Text(if (birthDate.isBlank()) "Seleccionar fecha de nacimiento" else "Nacimiento: $birthDate")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            val user = Firebase.auth.currentUser
            if (user == null) { error = "Sesión no iniciada"; return@Button }
            working = true
            UserProfileRepository.savePersonalInfo(user, firstName, lastName, birthDate) { ok, message ->
                working = false
                if (ok) onComplete() else error = message
            }
        }, enabled = !working, modifier = Modifier.fillMaxWidth()) {
            if (working) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Guardar y continuar")
        }
    }
}
