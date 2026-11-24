package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dreamteam.bookmaster.R
import com.dreamteam.bookmaster.ui.theme.BookMasterDarkPurple
import com.dreamteam.bookmaster.ui.theme.BookMasterPurple
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.random.Random
import kotlin.math.min

/**
 * Registra un PEDIDO DE PRUEBA en Firestore al confirmar.
 * No cobra tarjetas, no acredita puntos y no descuenta inventario.
 * Conserva la firma que usa NavGraph.
 */
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    onBackClick: () -> Unit,
    onPreviewReceipt: (OrderReceiptPreview) -> Unit = {}
) {
    val context = LocalContext.current
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    val pale = Color(0xFFFCFAFF)
    val line = Color(0xFFB99DD6) // Separadores lilas, finos pero visibles
    val muted = Color(0xFF61566C)
    val userId = Firebase.auth.currentUser?.uid
    val membership = rememberMembershipStatus()
    // Selección exclusiva de esta visita al Checkout: nunca altera la predeterminada en Firebase.
    var checkoutSelectedCardId by remember(userId) { mutableStateOf<String?>(null) }
    var registeringOrder by remember(userId) { mutableStateOf(false) }
    var orderError by remember(userId) { mutableStateOf<String?>(null) }
    // Los avisos aparecen después del primer intento de confirmar.
    var attemptedSubmit by remember(userId) { mutableStateOf(false) }

    // El encabezado coincide con BookDetailScreen, incluyendo la barra de estado.
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(71, 48, 113)
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    var deliveryMode by remember { mutableStateOf("") }
    // La dirección se lee del perfil; el diálogo permite actualizarla explícitamente.
    var address by remember(userId) { mutableStateOf("") }
    var recipientName by remember(userId) { mutableStateOf("") }
    var recipientEdited by remember(userId) { mutableStateOf(false) }
    var addressEdited by remember(userId) { mutableStateOf(false) }
    var loadingProfile by remember(userId) { mutableStateOf(true) }
    var profileError by remember(userId) { mutableStateOf(false) }
    var editRecipient by remember { mutableStateOf(false) }
    var editAddress by remember { mutableStateOf(false) }
    var tempRecipient by remember { mutableStateOf("") }
    var tempAddress by remember { mutableStateOf("") }
    var savingAddress by remember { mutableStateOf(false) }
    var addressSaveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            loadingProfile = false
            profileError = true
        } else {
            loadingProfile = true
            profileError = false
            Firebase.firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (Firebase.auth.currentUser?.uid == userId) {
                        if (!addressEdited) address = doc.getString("homeAddress").orEmpty().trim()
                        if (!recipientEdited) {
                            recipientName = listOf(
                                doc.getString("firstName").orEmpty().trim(),
                                doc.getString("lastName").orEmpty().trim()
                            ).filter { it.isNotBlank() }.joinToString(" ")
                        }
                        loadingProfile = false
                    }
                }
                .addOnFailureListener {
                    if (Firebase.auth.currentUser?.uid == userId) {
                        profileError = true
                        loadingProfile = false
                    }
                }
        }
    }

    // Fecha local únicamente informativa. La fecha definitiva vendrá del servidor.
    val previewTime = remember { System.currentTimeMillis() }
    val previewDate = remember(previewTime) {
        val calendar = Calendar.getInstance().apply { timeInMillis = previewTime }
        val day = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(previewTime))
        val hour12 = calendar.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE))
        val period = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m."
        "$day · $hour12:$minute $period"
    }

    val subtotal = cartItems.sumOf { it.book.price * it.quantity }
    val discount = MembershipPricing.discount(subtotal, membership.level)
    val discountedSubtotal = subtotal - discount
    val shipping = if (deliveryMode == "delivery") 8.50 else 0.0
    val total = discountedSubtotal + shipping
    val units = cartItems.sumOf { it.quantity }
    // El período de cancelación dura 5 minutos y después comienza la simulación.
    // Previsualización estable durante esta visita al checkout.
    val pickupEstimate = remember(previewTime, units) {
        DemoDeliveryTimeline.formatDate(
            DemoDeliveryTimeline.arrivalMillis(previewTime, units, "pickup")
        )
    }
    val deliveryEstimate = remember(previewTime, units) {
        DemoDeliveryTimeline.formatDate(
            DemoDeliveryTimeline.arrivalMillis(previewTime, units, "delivery")
        )
    }

    val nameValid = isValidCheckoutRecipient(recipientName)
    val selectedCardValid = checkoutSelectedCardId != null &&
            DemoPaymentStore.cards.any { it.id == checkoutSelectedCardId }
    val deliveryValid = deliveryMode.isNotEmpty()
    val addressValid = deliveryMode != "delivery" || address.trim().length in 10..300
    val membershipValid = !membership.loading && !membership.error && membership.level != null
    val canConfirm = cartItems.isNotEmpty() && nameValid && selectedCardValid &&
            deliveryValid && addressValid && membershipValid && !DemoPaymentStore.loading &&
            !loadingProfile && !profileError
    val requiredRed = Color(0xFFC62828)
    val membershipLabel = when {
        membership.loading -> "Consultando membresía…"
        membership.error -> "No se pudo consultar la membresía"
        membership.level == null -> "Membresía no configurada"
        else -> "${(MembershipPricing.rate(membership.level).toDouble() * 100).toInt()}% · ${membership.level}"
    }
    fun money(value: Double): String = String.format(Locale.US, "$%.2f", value)

    if (editRecipient) {
        AlertDialog(
            onDismissRequest = { editRecipient = false },
            title = { Text("Editar nombre del destinatario", color = dark, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 19.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempRecipient,
                        onValueChange = { if (it.length <= 90) tempRecipient = it },
                        label = { Text("Nombre y apellido") },
                        singleLine = true,
                        isError = tempRecipient.isNotBlank() && !isValidCheckoutRecipient(tempRecipient),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Entre 2 y 90 caracteres. Letras, espacios, apóstrofes y guiones.", fontSize = 12.sp, color = muted)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { recipientName = tempRecipient.trim(); recipientEdited = true; editRecipient = false },
                    enabled = isValidCheckoutRecipient(tempRecipient)
                ) { Text("Guardar", color = purple) }
            },
            dismissButton = { TextButton(onClick = { editRecipient = false }) { Text("Cancelar") } }
        )
    }
    if (editAddress) {
        AlertDialog(
            onDismissRequest = { if (!savingAddress) editAddress = false },
            title = { Text("Dirección de entrega", color = dark, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 19.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { if (it.length <= 300) { tempAddress = it; addressSaveError = null } },
                        label = { Text("Dirección completa") },
                        minLines = 3,
                        maxLines = 5,
                        isError = tempAddress.trim().length !in 10..300,
                        enabled = !savingAddress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Incluí municipio, colonia, calle, casa y referencia. Al guardar, se actualizará también en Mi perfil.", fontSize = 12.sp, color = muted)
                    addressSaveError?.let { Text(it, color = Color(0xFFB13A4E), fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uid = Firebase.auth.currentUser?.uid
                        val nueva = tempAddress.trim()
                        if (uid == null) {
                            addressSaveError = "Tenés que iniciar sesión para modificar la dirección."
                        } else if (nueva.length in 10..300 && !savingAddress) {
                            savingAddress = true
                            addressSaveError = null
                            Firebase.firestore.collection("users").document(uid)
                                .update("homeAddress", nueva)
                                .addOnSuccessListener {
                                    if (Firebase.auth.currentUser?.uid == uid) {
                                        address = nueva
                                        addressEdited = true
                                        editAddress = false
                                    }
                                    savingAddress = false
                                }
                                .addOnFailureListener {
                                    savingAddress = false
                                    addressSaveError = "No se pudo actualizar la dirección: ${it.localizedMessage}"
                                }
                        }
                    },
                    enabled = tempAddress.trim().length in 10..300 && !savingAddress
                ) { Text("Guardar", color = purple) }
            },
            dismissButton = { TextButton(onClick = { editAddress = false }, enabled = !savingAddress) { Text("Cancelar") } }
        )
    }

    Column(Modifier.fillMaxSize().background(pale)) {
        // Mismo encabezado que en Detalles del libro.
        Box(
            modifier = Modifier.fillMaxWidth().background(dark)
                .statusBarsPadding().height(62.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_detail_back),
                    contentDescription = "Volver al carrito",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            }
            Text(
                "Finalizar compra",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Formulario y resumen: un solo bloque blanco hasta el total, con sombra suave.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 5.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    CheckoutInfoLine("Código del pedido:", "Al confirmar", line, dark, muted)

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("A nombre de:", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (attemptedSubmit && !nameValid && !loadingProfile) {
                                    Text(" *", color = requiredRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                if (loadingProfile) "Cargando nombre…" else recipientName.ifBlank { "Sin nombre registrado" },
                                color = if (nameValid) dark else Color(0xFF9B324A), fontSize = 14.sp, fontWeight = FontWeight.Normal
                            )
                        }
                        IconButton(onClick = { tempRecipient = recipientName; editRecipient = true }) {
                            Icon(painterResource(R.drawable.ic_checkout_edit), contentDescription = "Editar destinatario", tint = purple)
                        }
                    }
                    if (attemptedSubmit && !nameValid && !loadingProfile) {
                        Text("Ingresá un nombre y apellido válidos.", color = requiredRed, fontSize = 11.sp)
                    }
                    HorizontalDivider(color = line, thickness = 1.dp)

                    CheckoutInfoLine("Fecha y hora del pedido:", previewDate, line, dark, muted)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Método de pago:", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (attemptedSubmit && !selectedCardValid && !DemoPaymentStore.loading) {
                                Text(" *", color = requiredRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_checkout_card), contentDescription = null, tint = purple, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tarjeta de crédito o débito", color = dark, fontSize = 13.sp)
                        }
                        DemoPaymentMethods(
                            manage = false,
                            modifier = Modifier.fillMaxWidth(),
                            checkoutSelectedId = checkoutSelectedCardId,
                            onCheckoutSelect = { checkoutSelectedCardId = it }
                        )
                        if (attemptedSubmit && !selectedCardValid && !DemoPaymentStore.loading) {
                            Text("Seleccioná una tarjeta para esta compra.", color = requiredRed, fontSize = 11.sp)
                        }
                    }
                    HorizontalDivider(color = line, thickness = 1.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Método de entrega:", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (attemptedSubmit && !deliveryValid) {
                                Text(" *", color = requiredRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = deliveryMode == "pickup", onClick = { deliveryMode = "pickup" }, colors = RadioButtonDefaults.colors(selectedColor = purple))
                            Icon(painterResource(R.drawable.ic_checkout_store), contentDescription = null, tint = purple, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retiro en sucursal · Gratis", color = dark, fontSize = 13.sp)
                        }
                        // Retiro: fecha/hora justo debajo de su opción, con calendario al extremo derecho.
                        if (deliveryMode == "pickup") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Fecha y hora estimada del retiro:", color = dark, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(pickupEstimate, color = dark, fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal)
                                    Text("Simulación: 5 min para cancelar + ${DemoDeliveryTimeline.extraMinutes(units, "pickup")} min adicionales para $units ejemplar(es).",
                                        color = muted, fontSize = 11.sp, lineHeight = 16.sp)
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_calendar_purple),
                                    contentDescription = "Fecha y hora estimada del retiro",
                                    tint = purple,
                                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = deliveryMode == "delivery", onClick = { deliveryMode = "delivery" }, colors = RadioButtonDefaults.colors(selectedColor = purple))
                            Icon(painterResource(R.drawable.ic_nav_home), contentDescription = null, tint = purple, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Entrega a domicilio · $8.50", color = dark, fontSize = 13.sp)
                        }
                        if (attemptedSubmit && !deliveryValid) {
                            Text("Elegí retiro en sucursal o entrega a domicilio.", color = requiredRed, fontSize = 11.sp)
                        }
                        if (deliveryMode == "delivery") {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Fecha y hora estimada de entrega:", color = dark,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(deliveryEstimate, color = dark, fontSize = 14.sp)
                                    Text("Simulación: 5 min para cancelar + ${DemoDeliveryTimeline.extraMinutes(units, "delivery")} min adicionales para $units ejemplar(es).",
                                        color = muted, fontSize = 11.sp, lineHeight = 16.sp)
                                }
                                Icon(painterResource(R.drawable.ic_calendar_purple),
                                    contentDescription = "Fecha de entrega estimada", tint = purple,
                                    modifier = Modifier.padding(end = 12.dp).size(24.dp))
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Dirección de entrega:", color = dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (attemptedSubmit && !addressValid && !loadingProfile) {
                                            Text(" *", color = requiredRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                    Text(
                                        when {
                                            loadingProfile -> "Cargando dirección…"
                                            address.isBlank() -> "Sin dirección. Tocá el lápiz para agregarla."
                                            else -> address
                                        },
                                        color = if (address.isBlank()) Color(0xFF9B324A) else dark,
                                        fontSize = 14.sp, fontWeight = FontWeight.Normal
                                    )
                                }
                                IconButton(onClick = { tempAddress = address; editAddress = true }) {
                                    Icon(painterResource(R.drawable.ic_checkout_edit), contentDescription = "Editar dirección de entrega", tint = purple)
                                }
                            }
                            if (attemptedSubmit && !addressValid && !loadingProfile) {
                                Text("La dirección debe tener entre 10 y 300 caracteres.", color = requiredRed, fontSize = 11.sp)
                            }
                        }
                    }

                    // Descuento como insignia del nivel real, en la misma fila.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descuento", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (!membership.loading && !membership.error && membership.level != null) {
                                val badgeBackground = when (membership.level) {
                                    "BRONCE" -> Color(0xFFE9C5A8)
                                    "PLATA" -> Color(0xFFDDE1E6)
                                    "ORO" -> Color(0xFFF5D982)
                                    else -> Color(0xFFEDE5F8)
                                }
                                val badgeForeground = when (membership.level) {
                                    "BRONCE" -> Color(0xFF704020)
                                    "PLATA" -> Color(0xFF46515F)
                                    "ORO" -> Color(0xFF715000)
                                    else -> dark
                                }
                                Surface(color = badgeBackground, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = membershipLabel,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        color = badgeForeground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(membershipLabel, color = dark, fontSize = 13.sp, textAlign = TextAlign.End)
                            }
                        }
                        HorizontalDivider(color = line, thickness = 1.dp)
                    }

                    // Encabezados iguales en color, peso y tamaño; valores organizados en 3 columnas.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Artículos", modifier = Modifier.width(86.dp), color = dark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Descripción", modifier = Modifier.weight(1f), color = dark, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("Precio", modifier = Modifier.width(76.dp), color = dark, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = line, thickness = 1.dp)
                        cartItems.forEachIndexed { itemIndex, item ->
                            key(item.book.id) {
                                val coverId = remember(item.book.id) {
                                    context.resources.getIdentifier(item.book.id, "drawable", context.packageName)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.width(76.dp).height(103.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color(0xFFEDE5F8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (coverId != 0) {
                                            Image(
                                                painter = painterResource(coverId),
                                                contentDescription = "Portada de ${item.book.title}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text("Sin portada", fontSize = 10.sp, color = muted, textAlign = TextAlign.Center)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(
                                            item.book.title, color = dark, fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                                        )
                                        Text("Cantidad: ${item.quantity}", color = muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                    Text(
                                        money(item.book.price * item.quantity),
                                        modifier = Modifier.width(76.dp),
                                        color = dark, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (itemIndex < cartItems.lastIndex) CheckoutDottedDivider(line)
                            }
                        }
                        if (cartItems.isEmpty()) Text("No hay artículos en el carrito.", color = muted, fontSize = 13.sp)
                    }

                    // Grada con sombra de extremo a extremo del bloque blanco.
                    // Dibujamos 18.dp por fuera de cada margen interior: width() no puede
                    // superar las restricciones de su padre y dejaba un hueco derecho.
                    Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                        val bleed = 18.dp.toPx()
                        val fullWidth = size.width + 2f * bleed
                        drawRect(
                            color = Color(0xFFD9D2DF),
                            topLeft = Offset(-bleed, 0f),
                            size = Size(fullWidth, 1.dp.toPx())
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x0D000000), Color.Transparent),
                                startY = 1.dp.toPx(),
                                endY = size.height
                            ),
                            topLeft = Offset(-bleed, 1.dp.toPx()),
                            size = Size(fullWidth, size.height - 1.dp.toPx())
                        )
                    }
                    // Resumen integrado en el formulario blanco.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CheckoutAmountRow("Subtotal:", money(subtotal), dark)
                        CheckoutAmountRow("Descuento:", "−${money(discount)}", dark, Color(0xFF2E7D32))
                        CheckoutAmountRow("Subtotal con descuento:", money(discountedSubtotal), dark)
                        CheckoutAmountRow("Costo de entrega:", money(shipping), dark)
                        HorizontalDivider(color = line, thickness = 1.dp)
                        CheckoutAmountRow("TOTAL:", money(total), dark, purple, emphasized = true)
                    }
                } // Column del formulario.
            } // Surface blanca, de borde a borde.

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(0.88f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, purple)
                    ) {
                        Text("CANCELAR", color = purple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = {
                            attemptedSubmit = true
                            orderError = null
                            if (!canConfirm) {
                                orderError = when {
                                    loadingProfile || membership.loading || DemoPaymentStore.loading -> "Esperá a que terminen de cargar tus datos."
                                    profileError -> "No se pudo cargar el perfil. Volvé a entrar e intentá nuevamente."
                                    !membershipValid -> "No se pudo verificar tu membresía."
                                    cartItems.isEmpty() -> "El carrito está vacío."
                                    else -> "Revisá los campos señalados con * en rojo."
                                }
                            } else {
                                val chosenCard = DemoPaymentStore.cards.firstOrNull { it.id == checkoutSelectedCardId }
                                if (chosenCard != null) {
                                    val receipt = OrderReceiptPreview(
                                        recipient = recipientName.trim(),
                                        date = previewDate,
                                        paymentBrand = chosenCard.brand,
                                        paymentLastFour = chosenCard.lastFour,
                                        deliveryMode = deliveryMode,
                                        deliveryAddress = if (deliveryMode == "delivery") address.trim() else "",
                                        // El campo existente transporta la estimación elegida al repositorio.
                                        // Allí se separa en pickupEstimate o deliveryEstimate.
                                        pickupEstimate = if (deliveryMode == "pickup") pickupEstimate else deliveryEstimate,
                                        membershipLevel = membership.level.orEmpty(),
                                        items = cartItems.map { item ->
                                            OrderReceiptItem(item.book.id, item.book.title, item.quantity, item.book.price)
                                        },
                                        subtotal = subtotal,
                                        discount = discount,
                                        shipping = shipping,
                                        total = total
                                    )
                                    if (!registeringOrder) {
                                        registeringOrder = true
                                        orderError = null
                                        BookMasterOrderRepository.registrarPedido(receipt, chosenCard.id) { saved, error ->
                                            registeringOrder = false
                                            if (saved != null) onPreviewReceipt(saved)
                                            else orderError = error ?: "No se pudo registrar el pedido"
                                        }
                                    }
                                }
                            }
                        },
                        // Se mantiene pulsable para que señale los campos incorrectos.
                        enabled = !registeringOrder,
                        modifier = Modifier.fillMaxWidth(0.88f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = purple)
                    ) {
                        if (registeringOrder) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ACEPTAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            orderError?.let { Text(it, color = Color(0xFFB13A4E), fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CheckoutInfoLine(label: String, value: String, line: Color, dark: Color, muted: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(value, color = dark, fontSize = 14.sp, fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End, modifier = Modifier.weight(1.15f))
        }
        HorizontalDivider(color = line, thickness = 1.dp)
    }
}

/** Línea segmentada entre portadas, sin bordes sólidos entre productos. */
@Composable
private fun CheckoutDottedDivider(color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        val dash = 7.dp.toPx()
        val gap = 6.dp.toPx()
        val stroke = 1.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, size.height / 2), Offset(min(x + dash, size.width), size.height / 2), strokeWidth = stroke)
            x += dash + gap
        }
    }
}

@Composable
private fun CheckoutAmountRow(
    label: String,
    value: String,
    dark: Color,
    valueColor: Color = dark,
    emphasized: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = dark,
            fontSize = if (emphasized) 16.sp else 13.sp,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Normal)
        Text(value, color = valueColor, fontSize = if (emphasized) 21.sp else 14.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

private fun isValidCheckoutRecipient(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.length in 2..90 &&
            Regex("""^[\p{L}\p{M}]+(?:[ '\-][\p{L}\p{M}]+)*$""").matches(trimmed)
}