package com.dreamteam.bookmaster.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Modelo de datos para representar lo que descargamos de Firebase
data class Compra(
    val id: String,
    val total: Double,
    val librosIds: List<String>
)

class ConnectBD : ViewModel() {

    private val db = Firebase.firestore

    // Variable que guardará la lista de compras para que la pantalla la observe
    private val _historial = MutableStateFlow<List<Compra>>(emptyList())
    val historial: StateFlow<List<Compra>> = _historial.asStateFlow()

    // Guarda la compra (lo que ya teníamos)
    fun guardarCompra(userId: String, total: Double, librosIds: List<String>) {
        val ordenDeCompra = hashMapOf(
            "userId" to userId,
            "total" to total,
            "libros_ids" to librosIds,
            "fecha" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("historial_compras")
            .add(ordenDeCompra)
            .addOnSuccessListener { documentReference ->
                println("Compra guardada con éxito. ID: ${documentReference.id}")
                // Opcional: recargar el historial automáticamente tras comprar
                obtenerHistorial(userId)
            }
            .addOnFailureListener { e ->
                println("Error al conectar con BD: ${e.message}")
            }
    }

    //  Función para leer las compras desde Firebase
    fun obtenerHistorial(userId: String) {
        db.collection("historial_compras")
            .whereEqualTo("userId", userId) // Filtra solo las compras de este usuario
            .orderBy("fecha", Query.Direction.DESCENDING) // Las más recientes primero
            .get()
            .addOnSuccessListener { documentos ->
                val listaCompras = documentos.map { doc ->
                    Compra(
                        id = doc.id,
                        total = doc.getDouble("total") ?: 0.0,
                        librosIds = doc.get("libros_ids") as? List<String> ?: emptyList()
                    )
                }
                // Actualizamos la variable observable
                _historial.value = listaCompras
            }
            .addOnFailureListener { e ->
                println("Error al obtener historial: ${e.message}")
            }
    }
}