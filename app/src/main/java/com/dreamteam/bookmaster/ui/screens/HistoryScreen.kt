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

// Estructura para almacenar los datos que descargamos de Firebase
data class OrderHistory(
    val totalAmount: Double = 0.0,
    val itemsCount: Int = 0,
    val summary: String = "",
    val timestamp: Long = 0L
)

@Composable
fun HistoryScreen(onBackClick: () -> Unit) {
    var orders by remember { mutableStateOf<List<OrderHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val darkBlueText = Color(0xFF0D1B2A)
    val backgroundGray = Color(0xFFF5F6F8)

    // Descarga de datos en tiempo real al abrir la pantalla
    LaunchedEffect(Unit) {
        val userId = Firebase.auth.currentUser?.uid

        // Si no hay usuario logueado, detenemos la carga y mostramos vacío
        if (userId == null) {
            Log.e("HistoryScreen", "No hay usuario autenticado. UID es null.")
            isLoading = false
            return@LaunchedEffect
        }

        // Consultamos la misma ruta donde guardamos la compra
        Firebase.firestore.collection("users")
            .document(userId)
            .collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HistoryScreen", "Error al cargar historial", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // AQUÍ ESTÁ LA SOLUCIÓN:
                    // Extraemos los valores como String y luego a número para evitar el crasheo de tipos de Firebase.
                    val fetchedOrders = snapshot.documents.map { doc ->
                        val total = doc.get("totalAmount")?.toString()?.toDoubleOrNull() ?: 0.0
                        val count = doc.get("itemsCount")?.toString()?.toIntOrNull() ?: 0
                        val summary = doc.getString("summary") ?: "Sin detalles"
                        val time = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()

                        OrderHistory(total, count, summary, time)
                    }
                    orders = fetchedOrders
                }
                isLoading = false
            }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundGray) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

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

            // Lógica de visualización
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF42A5F5))
                }
            } else if (orders.isEmpty()) {
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

@Composable
fun OrderCard(order: OrderHistory) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(order.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            Text(
                text = order.summary,
                fontSize = 14.sp,
                color = Color(0xFF0D1B2A),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF5F6F8))
            Spacer(modifier = Modifier.height(8.dp))

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