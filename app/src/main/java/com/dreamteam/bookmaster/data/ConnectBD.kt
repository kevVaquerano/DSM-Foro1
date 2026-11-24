package com.dreamteam.bookmaster.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Representa una compra de la colección heredada "historial_compras".
data class Compra(
    val id: String,
    val total: Double,
    val librosIds: List<String>
)

// Guarda únicamente el ID del libro y su cantidad; los demás datos provienen del catálogo.
data class ProductoCarritoGuardado(
    val libroId: String,
    val cantidad: Int
)

/** Gestiona el carrito persistente y las consultas del historial heredado en Firestore. */
class ConnectBD : ViewModel() {

    private val db = Firebase.firestore

    // Expone el historial como un flujo de solo lectura para los consumidores del ViewModel.
    private val _historial = MutableStateFlow<List<Compra>>(emptyList())
    val historial: StateFlow<List<Compra>> = _historial.asStateFlow()

    // Registra compras en la colección heredada.
    // PurchasesScreen.kt utiliza users/{uid}/orders en lugar de esta colección.
    fun guardarCompra(userId: String, total: Double, librosIds: List<String>) {
        val ordenDeCompra = hashMapOf(
            "userId" to userId,
            "total" to total,
            "libros_ids" to librosIds,
            "fecha" to FieldValue.serverTimestamp()
        )

        db.collection("historial_compras")
            .add(ordenDeCompra)
            .addOnSuccessListener { documentReference ->
                println("Compra guardada con exito. ID: ${documentReference.id}")
                obtenerHistorial(userId)
            }
            .addOnFailureListener { e ->
                println("Error al conectar con BD: ${e.message}")
            }
    }

    // Consulta las compras del usuario, ordenadas desde la más reciente.
    fun obtenerHistorial(userId: String) {
        db.collection("historial_compras")
            .whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documentos ->
                val listaCompras = documentos.map { doc ->
                    Compra(
                        id = doc.id,
                        total = doc.getDouble("total") ?: 0.0,
                        librosIds = (doc.get("libros_ids") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                _historial.value = listaCompras
            }
            .addOnFailureListener { e ->
                println("Error al obtener historial: ${e.message}")
            }
    }

    // Obtiene el identificador de la cuenta autenticada en Firebase.
    private fun uidActual(): String? = Firebase.auth.currentUser?.uid

    // Guarda o reemplaza la cantidad en users/{uid}/cart/{libroId}.
    // El callback informa si Firestore confirmó la escritura o devolvió un error.
    fun guardarProductoCarrito(
        libroId: String,
        cantidad: Int,
        onResultado: (Boolean, String?) -> Unit
    ) {
        val uid = uidActual()
        if (uid == null || libroId.isBlank() || cantidad <= 0) {
            onResultado(false, "Sesion no iniciada o producto/cantidad no validos")
            return
        }

        db.collection("users").document(uid)
            .collection("cart").document(libroId)
            .set(mapOf("quantity" to cantidad))
            .addOnSuccessListener { onResultado(true, null) }
            .addOnFailureListener { e -> onResultado(false, e.message) }
    }

    // Elimina únicamente el documento del libro indicado en el carrito de la cuenta actual.
    fun eliminarProductoCarrito(
        libroId: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        val uid = uidActual()
        if (uid == null || libroId.isBlank()) {
            onResultado(false, "Sesion no iniciada o ID de libro no valido")
            return
        }

        db.collection("users").document(uid)
            .collection("cart").document(libroId)
            .delete()
            .addOnSuccessListener { onResultado(true, null) }
            .addOnFailureListener { e -> onResultado(false, e.message) }
    }

    // Consulta el servidor para evitar recuperar datos obsoletos de la caché.
    fun obtenerCarrito(
        onResultado: (List<ProductoCarritoGuardado>?, String?) -> Unit
    ) {
        val uid = uidActual()
        if (uid == null) {
            onResultado(null, "No hay usuario autenticado")
            return
        }

        db.collection("users").document(uid)
            .collection("cart")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { documentos ->
                // Descarta registros con cantidades nulas, cero o negativas.
                val productos = documentos.mapNotNull { doc ->
                    val cantidad = doc.getLong("quantity")?.toInt() ?: 0
                    if (cantidad > 0) ProductoCarritoGuardado(doc.id, cantidad) else null
                }
                onResultado(productos, null)
            }
            .addOnFailureListener { e -> onResultado(null, e.message) }
    }

    // Elimina todos los artículos del carrito remoto cuando se solicita su vaciado.
    // No debe invocarse al cerrar sesión, para conservar el carrito de la cuenta.
    fun vaciarCarrito(
        onResultado: (Boolean, String?) -> Unit
    ) {
        val uid = uidActual()
        if (uid == null) {
            onResultado(false, "No hay usuario autenticado")
            return
        }

        db.collection("users").document(uid)
            .collection("cart")
            .get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    onResultado(true, null)
                    return@addOnSuccessListener
                }
                // La implementación limita el borrado a 500 documentos por lote.
                // Si se supera el límite, devuelve un error sin realizar el borrado.
                if (documentos.size() > 500) {
                    onResultado(false, "Carrito demasiado grande para vaciarlo en un lote")
                    return@addOnSuccessListener
                }
                // Confirma todas las eliminaciones juntas mediante un lote de escrituras.
                val batch = db.batch()
                documentos.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit()
                    .addOnSuccessListener { onResultado(true, null) }
                    .addOnFailureListener { e -> onResultado(false, e.message) }
            }
            .addOnFailureListener { e -> onResultado(false, e.message) }
    }
}