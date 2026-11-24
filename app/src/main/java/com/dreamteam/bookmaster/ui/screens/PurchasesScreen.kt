package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.core.view.WindowCompat
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.ui.theme.BookMasterDarkPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterPurple
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Datos del pedido registrados en users/{uid}/orders por BookMasterOrderRepository. */
private data class PurchaseOrder(
    val id: String,
    val code: String,
    val status: String,
    val createdAtMillis: Long?,
    val items: List<PurchaseLine>,
    val total: Double,
    val deliveryMode: String,
    val pickupEstimate: String,
    val deliveryEstimate: String,
    val estimatedArrivalMillis: Long?,
    val editUsed: Boolean,
    val editedAtMillis: Long?
)

// Datos mínimos de cada artículo para mostrar el historial.
private data class PurchaseLine(val title: String, val quantity: Int)

/**
 * Copia privada del detalle anterior a la cancelación, por usuario y pedido.
 * Las reglas actuales de Firestore obligan a vaciar items al cancelar.
 * No interviene en inventario, puntos ni validaciones de la transacción.
 */
private fun saveCancelledLines(context: Context, uid: String, orderId: String, lines: List<PurchaseLine>) {
    if (lines.isEmpty()) return
    val array = JSONArray()
    lines.forEach { line ->
        array.put(JSONObject().put("title", line.title).put("quantity", line.quantity))
    }
    context.getSharedPreferences("bookmaster_cancelled_orders_$uid", Context.MODE_PRIVATE)
        .edit().putString(orderId, array.toString()).apply()
}

// Recupera el detalle local de un pedido cancelado para la cuenta correspondiente.
private fun readCancelledLines(context: Context, uid: String, orderId: String): List<PurchaseLine> {
    val raw = context.getSharedPreferences("bookmaster_cancelled_orders_$uid", Context.MODE_PRIVATE)
        .getString(orderId, null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            val title = entry.optString("title").trim()
            val quantity = entry.optInt("quantity", 0)
            if (title.isNotBlank() && quantity > 0) PurchaseLine(title, quantity) else null
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Historial de pedidos: solo admite cancelación durante cinco minutos. */
@Composable
fun PurchasesScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val uid = Firebase.auth.currentUser?.uid
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    val pale = Color(0xFFFCFAFF)

    // Estados del historial y de la solicitud de cancelación.
    var orders by remember(uid) { mutableStateOf<List<PurchaseOrder>>(emptyList()) }
    var loading by remember(uid) { mutableStateOf(uid != null) }
    var errorText by remember(uid) { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var cancelTarget by remember { mutableStateOf<PurchaseOrder?>(null) }
    var cancelling by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    // El reloj solo actualiza la interfaz; el inicio del plazo siempre es createdAt de Firestore.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(71, 48, 113)
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    // DisposableEffect elimina el listener al salir de la pantalla o cambiar de cuenta.
    DisposableEffect(uid) {
        if (uid == null) {
            loading = false
            errorText = "Iniciá sesión para consultar tus pedidos."
            onDispose { }
        } else {
            val registration = Firebase.firestore.collection("users").document(uid)
                .collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("PurchasesScreen", "No se pudieron consultar los pedidos", error)
                        errorText = "No se pudieron cargar tus pedidos. Revisá tu conexión."
                        loading = false
                        return@addSnapshotListener
                    }
                    errorText = null
                    // Convierte los documentos de Firestore en modelos para la interfaz.
                    orders = snapshot?.documents?.map { doc ->
                        val purchaseLines = (doc.get("items") as? List<*>)
                            .orEmpty().mapNotNull { raw ->
                                val item = raw as? Map<*, *> ?: return@mapNotNull null
                                PurchaseLine(
                                    title = item["title"] as? String ?: "Libro",
                                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                )
                            }
                        PurchaseOrder(
                            id = doc.id,
                            code = doc.getString("orderCode") ?: doc.id,
                            status = doc.getString("status")?.uppercase(Locale.ROOT) ?: "PENDIENTE",
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time,
                            items = if (
                                doc.getString("status")?.uppercase(Locale.ROOT) in setOf("CANCELADO", "CANCELLED")
                                && purchaseLines.isEmpty()
                            ) readCancelledLines(context, uid, doc.id) else purchaseLines,
                            total = (doc.get("total") as? Number)?.toDouble() ?: 0.0,
                            deliveryMode = doc.getString("deliveryMode").orEmpty(),
                            pickupEstimate = doc.getString("pickupEstimate").orEmpty(),
                            deliveryEstimate = doc.getString("deliveryEstimate").orEmpty(),
                            estimatedArrivalMillis = doc.getTimestamp("estimatedArrivalAt")?.toDate()?.time,
                            editUsed = doc.getBoolean("editUsed") == true,
                            editedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time
                        )
                    }.orEmpty()
                    loading = false
                }
            onDispose { registration.remove() }
        }
    }

    // Solicita confirmación antes de iniciar la transacción de cancelación.
    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!cancelling) cancelTarget = null },
            title = {
                Text("¿Cancelar pedido?", color = dark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            },
            text = { Text("¿Estás seguro de que querés cancelar ${target.code}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(enabled = !cancelling, onClick = { cancelTarget = null }) {
                        Text("Volver")
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp), enabled = !cancelling, onClick = {
                        cancelling = true
                        BookMasterOrderRepository.cancelarPedido(target.id) { ok, err ->
                            cancelling = false
                            cancelTarget = null
                            if (ok) {
                                // Firestore exige vaciar "items" al cancelar. Conservamos
                                // el detalle original solo para mostrar el historial local.
                                if (uid != null) {
                                    saveCancelledLines(context, uid, target.id, target.items)
                                }
                                orders = orders.map { order ->
                                    if (order.id == target.id) order.copy(status = "CANCELADO", total = 0.0, editUsed = true)
                                    else order
                                }
                            }
                            actionError = if (ok) null else err ?: "No se pudo cancelar"
                        }
                    }) {
                        Text(
                            text = if (cancelling) "Cancelando…" else "Cancelar pedido",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

    // El estado visible se calcula de la fecha oficial y las cantidades actuales.
    // No se necesitan escrituras automáticas de Firestore para mostrar la transición.
    val visibleOrders = orders.filter { order ->
        val units = order.items.sumOf { it.quantity }
        val timelineStage = DemoDeliveryTimeline.status(order.status, order.createdAtMillis,
            units, order.deliveryMode, nowMillis, order.editedAtMillis)
        // Una edición confirmada cierra la etapa pendiente aunque el estado
        // almacenado permanezca PENDIENTE por las reglas de Firestore.
        val stage = if (order.editUsed && timelineStage == "PENDIENTE") "EN_PREPARACION" else timelineStage
        when (selectedTab) {
            0 -> stage in setOf("PENDIENTE", "EN_PREPARACION", "CANCELADO", "CANCELLED")
            1 -> stage in setOf("ENVIADO", "LISTO_PARA_RETIRAR")
            else -> stage == "ENTREGADO"
        }
    }

    Column(Modifier.fillMaxSize().background(pale)) {
        // Encabezado como BookDetailScreen. El NavGraph ya aplica el margen superior
        // del sistema en esta pantalla: no repetir statusBarsPadding aquí.
        Box(
            modifier = Modifier.fillMaxWidth().background(dark)
                .height(62.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_detail_back),
                    contentDescription = "Volver al inicio",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Text(
                text = "Mis pedidos",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Pestañas en la misma pantalla; las tarjetas se filtran sin navegar.
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White,
            contentColor = purple) {
            listOf("Pendientes", "Enviados", "Entregados").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index)
                        FontWeight.Bold else FontWeight.Medium) })
            }
        }

        // Presenta errores, carga, ausencia de pedidos o el listado filtrado.
        actionError?.let { message ->
            Text(message, color = Color(0xFFB13A4E), modifier = Modifier.padding(12.dp))
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = purple)
            }
            errorText != null -> Box(Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center) {
                Text(errorText.orEmpty(), color = dark)
            }
            visibleOrders.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (selectedTab == 0) "📦" else if (selectedTab == 1) "🚚" else "✓", fontSize = 42.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(when (selectedTab) {
                        0 -> "No tenés pedidos pendientes"
                        1 -> "No tenés pedidos enviados o listos para retirar"
                        else -> "Todavía no tenés pedidos entregados"
                    }, color = dark,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleOrders, key = { it.id }) { order ->
                    PurchaseOrderCard(order, nowMillis, isLatestOrder = order.id == orders.firstOrNull()?.id, onCancel = { cancelTarget = order })
                }
            }
        }
    }
}

/** Tarjeta con el resumen, estado, importe y plazo de cancelación del pedido. */
@Composable
private fun PurchaseOrderCard(
    order: PurchaseOrder, nowMillis: Long,
    isLatestOrder: Boolean, onCancel: () -> Unit
) {
    // Si el pedido fue creado antes de esta función, sigue siendo una referencia válida.
    // No se escribe un nuevo estado a Firestore desde esta pantalla.
    val cancellationDeadlineMillis = order.createdAtMillis?.let { created ->
        if (created <= Long.MAX_VALUE - 300_000L) created + 300_000L else Long.MAX_VALUE
    }
    val remainingMillis = cancellationDeadlineMillis?.let { (it - nowMillis).coerceAtLeast(0L) }
    // Solo el pedido más reciente, pendiente y sin edición previa puede cancelarse dentro del plazo.
    val cancellationWindowOpen = isLatestOrder && order.status == "PENDIENTE" && !order.editUsed &&
            remainingMillis != null && remainingMillis > 0L
    val units = order.items.sumOf { it.quantity }
    val timelineStatus = DemoDeliveryTimeline.status(
        order.status, order.createdAtMillis, units, order.deliveryMode, nowMillis, order.editedAtMillis
    )
    val computedStatus = if (order.editUsed && timelineStatus == "PENDIENTE") "EN_PREPARACION" else timelineStatus
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    // Traduce los estados internos a etiquetas visibles para el cliente.
    val statusLabel = when (computedStatus) {
        "PENDIENTE" -> "Pendiente"
        "PREPARACION", "EN_PREPARACION" -> "En preparación"
        "ENVIADO" -> "Enviado"
        "LISTO_PARA_RETIRAR" -> "Listo para retirar"
        "ENTREGADO" -> "Entregado"
        "CANCELADO", "CANCELLED" -> "Cancelado"
        else -> order.status.replace('_', ' ').lowercase(Locale.getDefault())
    }
    val count = units
    val summary = order.items.joinToString(", ") { "${it.quantity}x ${it.title}" }
    val date = order.createdAtMillis?.let {
        SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(it))
    } ?: "Fecha pendiente de sincronización"

    // Un borde morado fino identifica los pedidos todavía activos.
    val activeOrder = computedStatus != "ENTREGADO" && computedStatus !in setOf("CANCELADO", "CANCELLED")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        border = if (activeOrder) BorderStroke(1.dp, dark) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(order.code, color = dark, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                // Colores de identificación según el estado del pedido.
                val (badgeBackground, badgeText) = when (computedStatus) {
                    "PENDIENTE" -> Color(0xFFFCE8E8) to Color(0xFFB42332)
                    "EN_PREPARACION", "PREPARACION" -> Color(0xFFFFF0C2) to Color(0xFF926000)
                    "ENTREGADO" -> Color(0xFFDDF3E3) to Color(0xFF216E39)
                    "CANCELADO", "CANCELLED" -> Color(0xFFFCE8E8) to Color(0xFFB42332)
                    else -> Color(0xFFF1E8FA) to purple
                }
                Surface(color = badgeBackground, shape = RoundedCornerShape(50)) {
                    Text(statusLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = badgeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, color = Color.Gray, fontSize = 13.sp)
                Text("$count artículo(s)", color = purple, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = Color(0xFFE8DFEF))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(summary.ifBlank { "Sin detalle de artículos" }, color = dark, fontSize = 13.sp,
                    lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                if (cancellationWindowOpen) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(35.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC44747))) {
                        Icon(painter = painterResource(R.drawable.ic_delete_profile),
                            contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFC44747))
                        Spacer(Modifier.width(4.dp))
                        Text("CANCELAR", fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (order.deliveryMode == "pickup") "Método de entrega: Retiro en sucursal"
                    else "Método de entrega: Entrega a domicilio",
                    color = Color.Gray, fontSize = 13.sp, lineHeight = 17.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            // Se deriva desde createdAt + cantidad ACTUAL: nunca queda una fecha antigua
            // si el usuario cambió el pedido durante los primeros 5 minutos.
            if (computedStatus !in setOf("CANCELADO", "CANCELLED")) order.createdAtMillis?.let { created ->
                val estimated = if (order.editUsed && order.editedAtMillis != null)
                    order.editedAtMillis + DemoDeliveryTimeline.extraMinutes(units, order.deliveryMode) * 60_000L
                else DemoDeliveryTimeline.arrivalMillis(created, units, order.deliveryMode)
                val modeLabel = if (order.deliveryMode == "pickup") "Retiro" else "Entrega"
                Text("$modeLabel estimado: ${DemoDeliveryTimeline.formatDate(estimated)}",
                    color = dark, fontSize = 13.sp)
            }
            // Plazo de cancelación calculado desde la fecha original del pedido.
            // El contador solo aparece cuando la cancelación está disponible.
            if (order.status == "PENDIENTE" && computedStatus !in setOf("CANCELADO", "CANCELLED")) {
                if (remainingMillis == null) {
                    Text("Esperando fecha oficial para calcular el plazo de cancelación…",
                        color = Color.Gray, fontSize = 12.sp)
                } else if (cancellationWindowOpen) {
                    // Redondea los segundos restantes hacia arriba para el contador visible.
                    val seconds = (remainingMillis + 999L) / 1000L
                    val minutesText = (seconds / 60L).toString().padStart(2, '0')
                    val secondsText = (seconds % 60L).toString().padStart(2, '0')
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Tiempo restante para cancelar:", color = dark, fontSize = 13.sp)
                        Text("$minutesText:$secondsText", color = purple,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    LinearProgressIndicator(
                        progress = { (remainingMillis / 300_000f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = purple,
                        trackColor = Color(0xFFE8DFEF)
                    )
                } else {
                    Text(if (order.editUsed) "Pedido en preparación" else if (!isLatestOrder && remainingMillis > 0L) "Solo podés cancelar la compra más reciente" else "Plazo de cancelación finalizado · En preparación",
                        color = purple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (computedStatus in setOf("CANCELADO", "CANCELLED")) {
                Text("Pedido cancelado · No se procesará ni se entregará.",
                    color = Color(0xFFB42332), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Total:", color = dark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(String.format(Locale.US, "$%.2f", order.total), color = purple,
                    fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            }
        }
    }
}