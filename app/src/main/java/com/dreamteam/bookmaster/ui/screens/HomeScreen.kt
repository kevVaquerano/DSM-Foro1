package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamteam.bookmaster.data.Libro
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var librosState by remember { mutableStateOf<List<Libro>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Consultar Firestore al cargar la pantalla
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.mapNotNull { doc -> doc.toObject(Libro::class.java) }
                librosState = lista
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BookMaster UDB - Catálogo") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Salir", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (cargando) {
                CircularProgressIndicator()
            } else if (librosState.isEmpty()) {
                Text("No hay libros registrados en Firestore.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(librosState) { libro ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = libro.titulo,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Autor: ${libro.autor}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Categoría: ${libro.categoria}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Stock: ${libro.stock} unidades",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (libro.disponible && libro.stock > 0) "Estado: Disponible" else "Estado: Sin Stock",
                                    color = if (libro.disponible && libro.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}