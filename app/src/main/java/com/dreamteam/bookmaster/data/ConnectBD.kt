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

/**
 * CLASE DE DATOS: Compra
 * Representa la estructura de una orden de compra guardada en el historial.
 * Almacena el ID del documento, el costo total de la compra y la lista de los IDs de los libros comprados.
 */
// Modelo existente: historial de la coleccion heredada "historial_compras".
data class Compra(
    val id: String,
    val total: Double,
    val librosIds: List<String>
)

/**
 * CLASE DE DATOS: ProductoCarritoGuardado
 * Representa un producto individual dentro del carrito de compras del usuario en la base de datos.
 * Solo guarda el ID del libro y cuántos ejemplares (cantidad) se quieren comprar para optimizar espacio.
 */
// Solo persistimos el identificador y la cantidad: el resto proviene del catalogo.
data class ProductoCarritoGuardado(
    val libroId: String,
    val cantidad: Int
)

/**
 * VIEWMODEL: ConnectBD
 * Esta clase se encarga de gestionar toda la comunicación entre la aplicación y la base de datos Firestore.
 * Administra el historial de compras heredado y el carrito de compras en la nube por usuario.
 */
class ConnectBD : ViewModel() {

    // Instancia de Firestore para interactuar con la base de datos.
    private val db = Firebase.firestore

    // Variables reactivas (StateFlow) para mantener el historial de compras actualizado en la interfaz.
    // _historial es modificable de forma interna, y historial es de solo lectura para la vista.
    private val _historial = MutableStateFlow<List<Compra>>(emptyList())
    val historial: StateFlow<List<Compra>> = _historial.asStateFlow()

    /**
     * MÉTODO: guardarCompra
     * Crea un nuevo registro de compra en la colección "historial_compras" con el usuario, total y libros.
     * Si tiene éxito, manda a llamar 'obtenerHistorial' para actualizar la lista en pantalla.
     */
    // Se mantienen intactas las operaciones del historial heredado.
    // NOTA: HistoryScreen.kt actualmente usa users/{uid}/orders, NO esta coleccion.
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

    /**
     * MÉTODO: obtenerHistorial
     * Consulta la base de datos buscando todas las compras hechas por un usuario específico.
     * Las ordena por fecha (la más reciente primero) y actualiza el StateFlow '_historial'.
     */
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

    /**
     * MÉTODO PRIVADO: uidActual
     * Función auxiliar para obtener rápidamente el ID del usuario que tiene la sesión iniciada actualmente.
     */
    // El usuario se obtiene siempre de la sesion actual de Firebase.
    // Resultado: true si Firestore confirmo el cambio; false en caso contrario.
    private fun uidActual(): String? = Firebase.auth.currentUser?.uid

    /**
     * MÉTODO: guardarProductoCarrito
     * Añade o actualiza la cantidad de un libro específico en el carrito del usuario logueado.
     * Lo guarda en la ruta: users/{uid}/cart/{libroId}.
     */
    // users/{uid}/cart/{libroId} -> { quantity: Int }
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

    /**
     * MÉTODO: eliminarProductoCarrito
     * Borra un documento (un libro) específico de la subcolección del carrito del usuario.
     */
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

    /**
     * MÉTODO: obtenerCarrito
     * Descarga todos los productos que el usuario tiene en su carrito.
     * Obliga a consultar el servidor directamente (sin caché) para garantizar datos reales.
     */
    // Lee desde el servidor para evitar mostrar un carrito obsoleto de la cache.
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
                val productos = documentos.mapNotNull { doc ->
                    val cantidad = doc.getLong("quantity")?.toInt() ?: 0
                    if (cantidad > 0) ProductoCarritoGuardado(doc.id, cantidad) else null
                }
                onResultado(productos, null)
            }
            .addOnFailureListener { e -> onResultado(null, e.message) }
    }

    /**
     * MÉTODO: vaciarCarrito
     * Elimina absolutamente todos los productos del carrito del usuario.
     * Usa una operación "batch" (por lotes) para borrar todos los documentos de un solo golpe.
     */
    // Se usara DESPUES de confirmar la compra y guardar la orden.
    // No invocar al cerrar sesion: cerrar sesion no debe borrar datos remotos.
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
                // Carritos normales son pequenos. Firestore admite maximo 500
                // operaciones por lote; si se amplía el catalogo, paginar este borrado.
                if (documentos.size() > 500) {
                    onResultado(false, "Carrito demasiado grande para vaciarlo en un lote")
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                documentos.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit()
                    .addOnSuccessListener { onResultado(true, null) }
                    .addOnFailureListener { e -> onResultado(false, e.message) }
            }
            .addOnFailureListener { e -> onResultado(false, e.message) }
    }
}