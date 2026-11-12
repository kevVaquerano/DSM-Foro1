package com.dreamteam.bookmaster.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

/**
 * CLASE DE DATOS: OrderHistory
 * Representa la estructura local de una compra recuperada desde Firebase.
 * Almacena el total pagado, la cantidad de libros, un texto resumen de la compra y la fecha en milisegundos.
 */
// Estructura para almacenar los datos que descargamos de Firebase
data class OrderHistory(
    val totalAmount: Double = 0.0,
    val itemsCount: Int = 0,
    val summary: String = "",
    val timestamp: Long = 0L
)

/**
 * PANTALLA PRINCIPAL: HistoryScreen
 * Es la vista que muestra al usuario todas las compras que ha realizado.
 * Se conecta en tiempo real a Firebase para descargar y mostrar la información.
 * @param onBackClick Función lambda que se ejecuta al presionar el botón de regresar.
 */
@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    // ESTADOS DE LA PANTALLA:
    // orders: Guarda la lista de compras descargadas. Comienza vacía.
    var orders by remember { mutableStateOf<List<OrderHistory>>(emptyList()) }
    // isLoading: Controla si se muestra la animación de carga. Comienza en true.
    var isLoading by remember { mutableStateOf(true) }

    // Paleta de colores local para la pantalla
    val darkBlueText = Color(0xFF0D1B2A)
    val backgroundGray = Color(0xFFF5F6F8)

    /**
     * EFECTO SECUNDARIO: LaunchedEffect
     * Este bloque se ejecuta automáticamente cuando la pantalla se abre.
     * Su función es conectar con Firebase y empezar a escuchar cambios en el historial.
     */
    // Descarga de datos en tiempo real al abrir la pantalla
    LaunchedEffect(Unit) {
        // Obtenemos el ID del usuario actual que tiene la sesión iniciada
        val userId = Firebase.auth.currentUser?.uid

        // Si no hay usuario logueado, detenemos la carga y mostramos la pantalla vacía
        if (userId == null) {
            Log.e("HistoryScreen", "No hay usuario autenticado. UID es null.")
            isLoading = false
            return@LaunchedEffect
        }

        // Consultamos la misma ruta donde guardamos la compra en NavGraph: users/{userId}/orders
        Firebase.firestore.collection("users")
            .document(userId)
            .collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena de la compra más nueva a la más vieja
            .addSnapshotListener { snapshot, error ->
                // Manejo de errores de conexión o permisos
                if (error != null) {
                    Log.e("HistoryScreen", "Error al cargar historial", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                // Si recibimos datos correctamente, los procesamos
                if (snapshot != null) {
                    // AQUÍ ESTÁ LA SOLUCIÓN:
                    // Extraemos los valores como String y luego a número para evitar el crasheo de tipos de Firebase.
                    val fetchedOrders = snapshot.documents.map { doc ->
                        // Conversiones seguras: Firebase a veces guarda Double como Long. Convertir a String primero lo evita.
                        val total = doc.get("totalAmount")?.toString()?.toDoubleOrNull() ?: 0.0
                        val count = doc.get("itemsCount")?.toString()?.toIntOrNull() ?: 0
                        val summary = doc.getString("summary") ?: "Sin detalles"
                        // Convierte el Timestamp nativo de Firebase a milisegundos (Long) para poder formatearlo después
                        val time = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()

                        OrderHistory(total, count, summary, time)
                    }
                    // Actualizamos la variable de estado con la lista descargada
                    orders = fetchedOrders
                }
                // Terminamos la carga para ocultar el spinner
                isLoading = false
            }
    }

    // CONTENEDOR PRINCIPAL DE LA UI
    Surface(modifier = Modifier.fillMaxSize(), color = backgroundGray) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            /**
             * SECCIÓN: Encabezado
             * Contiene el botón de retroceso y el título de la pantalla.
             */
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .shadow(2.dp, RoundedCornerShape(12.dp))
                ) {
                    Text(text = "⬅️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Historial de Compras 📜",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlueText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            /**
             * SECCIÓN: Lógica de visualización dinámica
             * Decide qué mostrar en el centro de la pantalla dependiendo de los estados (isLoading, orders).
             */
            // Lógica de visualización
            if (isLoading) {
                // ESTADO 1: Cargando. Muestra un círculo giratorio mientras Firebase responde.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF42A5F5))
                }
            } else if (orders.isEmpty()) {
                // ESTADO 2: Vacío. Si Firebase respondió pero la lista está en blanco, muestra este mensaje.
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aún no has realizado compras",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // ESTADO 3: Con datos. Muestra una lista desplazable (LazyColumn) con las tarjetas de compra.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(orders) { order ->
                        OrderCard(order)
                    }
                }
            }
        }
    }
}

/**
 * COMPONENTE UI: OrderCard
 * Dibuja la tarjeta visual para una única orden de compra dentro de la lista.
 * Muestra la fecha, cantidad de artículos, resumen y total pagado.
 * @param order Objeto de tipo OrderHistory con los datos de esta compra específica.
 */
@Composable
fun OrderCard(order: OrderHistory) {
    // Formateador para convertir los milisegundos (Long) a un texto legible por el usuario (Día/Mes/Año Hora:Minuto)
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(order.timestamp))

    // Tarjeta blanca con bordes redondeados y sombra
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Fila superior: Fecha de la compra (Izquierda) y Cantidad de artículos (Derecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = dateString, color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "${order.itemsCount} artículos",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF42A5F5),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Texto central: Resumen de lo comprado (Ej. "2x El Principito")
            Text(
                text = order.summary,
                fontSize = 14.sp,
                color = Color(0xFF0D1B2A),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Línea divisoria gris tenue
            Divider(color = Color(0xFFF5F6F8))

            Spacer(modifier = Modifier.height(8.dp))

            // Fila inferior: Etiqueta "Total:" y el precio final pagado formateado a 2 decimales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}