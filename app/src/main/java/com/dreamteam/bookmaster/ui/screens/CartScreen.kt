package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Rediseño del carrito. El guardado y las cantidades permanecen a cargo de NavGraph. */
@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onIncreaseQuantity: (CartItem) -> Unit,
    onDecreaseQuantity: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onBackClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    onClearCart: () -> Unit = {}
) {
    val context = LocalContext.current
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    val line = Color(0xFFCAC4D0) // Borde gris suave de los campos del login
    val pale = Color(0xFFFCFAFF)
    var editing by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var itemPendingRemoval by remember { mutableStateOf<CartItem?>(null) }
    val membership = rememberMembershipStatus()
    val subtotal = cartItems.sumOf { it.book.price * it.quantity }
    val discount = MembershipPricing.discount(subtotal, membership.level)
    val total = subtotal - discount
    val units = cartItems.sumOf { it.quantity }
    val hasItems = cartItems.isNotEmpty()
    val editEnabled = hasItems || editing
    val clearEnabled = hasItems && !editing
    val discountPercentage = if (subtotal > 0.0) (discount / subtotal * 100.0).roundToInt() else 0
    val discountLevelName = membership.level?.lowercase()?.replaceFirstChar { it.titlecase() }

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(71, 48, 113)
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
    }
    // Durante la edición solo «Listo» y las X permiten acciones.
    BackHandler(enabled = editing) { }
    LaunchedEffect(cartItems.isEmpty()) {
        if (cartItems.isEmpty()) editing = false
    }

    // Confirmación individual antes de eliminar un libro desde la X.
    itemPendingRemoval?.let { pending ->
        AlertDialog(
            onDismissRequest = { itemPendingRemoval = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFFFCFAFF),
            title = {
                Text("¿Eliminar artículo?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = dark, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Estás seguro de que querés eliminar \"${pending.book.title}\" del carrito?",
                    color = dark
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { itemPendingRemoval = null }) {
                        Text("CANCELAR", color = purple, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            itemPendingRemoval = null
                            onRemoveItem(pending)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = purple),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {}
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFFFCFAFF),
            title = { Text("¿Vaciar carrito?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = dark, fontWeight = FontWeight.Bold) },
            text = {
                Text("¿Estás seguro de que querés vaciar el carrito? Se eliminarán todos los artículos.", color = dark)
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showClearConfirmation = false }) {
                        Text("CANCELAR", color = purple, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showClearConfirmation = false
                            onClearCart()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = purple),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("VACIAR", fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = {}
        )
    }

    Column(Modifier.fillMaxSize().background(pale)) {
        // Una sola línea: regresar, título y acciones. Conserva la altura de Checkout.
        Row(
            modifier = Modifier.fillMaxWidth().background(dark)
                .statusBarsPadding().height(62.dp)
                .padding(start = 5.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                enabled = !editing,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_detail_back),
                    contentDescription = "Regresar",
                    tint = if (editing) Color.White.copy(alpha = 0.4f) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Mi carrito",
                modifier = Modifier.weight(1f).padding(start = 3.dp),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OutlinedButton(
                onClick = { editing = !editing },
                enabled = editEnabled,
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (editEnabled) 0.7f else 0.28f)),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    painterResource(R.drawable.ic_checkout_edit),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (editEnabled) 1f else 0.38f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (editing) "LISTO" else "EDITAR", color = Color.White.copy(alpha = if (editEnabled) 1f else 0.38f),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(7.dp))
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                enabled = clearEnabled,
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (clearEnabled) 0.7f else 0.28f)),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = 0.38f)
                )
            ) {
                Icon(
                    painterResource(R.drawable.ic_delete_profile),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (clearEnabled) 1f else 0.38f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("VACIAR", color = Color.White.copy(alpha = if (clearEnabled) 1f else 0.38f),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (cartItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Tu carrito está vacío", color = dark,
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems, key = { it.book.id }) { item ->
                    RedesignedCartItem(
                        item = item,
                        editing = editing,
                        onIncrease = { if (!editing) onIncreaseQuantity(item) },
                        onDecrease = { if (!editing) onDecreaseQuantity(item) },
                        onRemove = { itemPendingRemoval = item }
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(0.dp),
                shadowElevation = 5.dp
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    CartSummaryLine("Cantidad total:", "$units ejemplar(es)", dark)
                    CartSummaryLine("Subtotal de libros:", MembershipPricing.money(subtotal), dark)
                    CartSummaryLine(
                        when {
                            membership.loading -> "Consultando membresía…"
                            membership.error -> "No se pudo verificar membresía"
                            else -> "Descuento (${discountLevelName ?: "Sin nivel"} · $discountPercentage%)"
                        },
                        "−${MembershipPricing.money(discount)}", dark, Color(0xFF2E7D32)
                    )
                    HorizontalDivider(color = line, thickness = 1.dp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total sin envío:", color = dark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(MembershipPricing.money(total), color = purple,
                            fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onCheckoutClick,
                        enabled = !editing && !membership.loading && !membership.error,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = purple)
                    ) {
                        Text("Continuar con la compra", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CartSummaryLine(label: String, value: String, dark: Color, valueColor: Color = dark) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = dark, fontSize = 15.sp, lineHeight = 19.sp,
            modifier = Modifier.weight(1f), maxLines = 2)
        Spacer(Modifier.width(8.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, maxLines = 1)
    }
}

@Composable
private fun RedesignedCartItem(
    item: CartItem,
    editing: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    // La X solo aparece cuando terminó el deslizamiento de la tarjeta.
    var showRemoveIcon by remember { mutableStateOf(false) }
    LaunchedEffect(editing) {
        if (editing) {
            delay(290L) // Desplazamiento: 260 ms; pequeño margen para que termine antes.
            showRemoveIcon = true
        } else {
            showRemoveIcon = false // Al salir de Editar desaparece inmediatamente.
        }
    }
    val shifted by animateDpAsState(
        targetValue = if (editing) 53.dp else 0.dp,
        animationSpec = tween(durationMillis = 260),
        label = "DeslizarCarrito"
    )
    val coverId = remember(item.book.id) {
        context.resources.getIdentifier(item.book.id, "drawable", context.packageName)
    }
    val borderShape = RoundedCornerShape(18.dp)
    val loginBorder = Color(0xFF473071)
    val coverWidth by animateDpAsState(if (editing) 64.dp else 79.dp, label = "PortadaCarrito")

    // Una sola base lila continua: al entrar en modo Editar aparece la zona de X,
    // mientras el bloque blanco se retrae hacia la izquierda sin mostrar espacios.
    Box(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize()
            .clip(borderShape)
            .background(Color(0xFFDAB8F2))
            .border(1.3.dp, loginBorder, borderShape),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(end = shifted),
            color = Color.White,
            shape = borderShape,
            border = BorderStroke(1.3.dp, loginBorder),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Portada real y completa del drawable del catálogo.
                Box(
                    modifier = Modifier.width(coverWidth).height(110.dp)
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
                        Text("📖", fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subtotal grande, alineado arriba a la derecha de cada artículo.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.book.title,
                            modifier = Modifier.weight(1f),
                            color = dark,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = MembershipPricing.money(item.book.price * item.quantity),
                            color = dark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (editing) 16.sp else 19.sp,
                            maxLines = 1,
                            textAlign = TextAlign.End
                        )
                    }
                    Text(
                        "Precio unitario: ${MembershipPricing.money(item.book.price)}",
                        color = purple,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 2
                    )
                    Text("Cantidad:", color = dark, fontSize = 13.sp)

                    // Mismo contador segmentado de BookDetail, adaptado a la tarjeta.
                    Row(
                        modifier = Modifier.height(32.dp)
                            .border(1.dp, Color(0xFFB99CD6), RoundedCornerShape(9.dp))
                            .clip(RoundedCornerShape(9.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(30.dp).fillMaxHeight()
                                .clickable(enabled = !editing && item.quantity > 0) { onDecrease() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "−", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = if (!editing && item.quantity > 0) dark else Color(0xFFBFB5C7)
                            )
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFB99CD6)))
                        Box(Modifier.width(32.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text(item.quantity.toString(), fontWeight = FontWeight.Bold,
                                color = dark, fontSize = 13.sp)
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFB99CD6)))
                        Box(
                            modifier = Modifier.width(30.dp).fillMaxHeight()
                                .clickable(enabled = !editing && item.quantity < item.book.stock) { onIncrease() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = if (!editing && item.quantity < item.book.stock) dark else Color(0xFFBFB5C7)
                            )
                        }
                    }
                }
            }
        }
        if (showRemoveIcon && editing) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).width(53.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRemove,
                    enabled = showRemoveIcon && editing,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cart_remove_x),
                        contentDescription = "Eliminar ${item.book.title}",
                        tint = dark,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }
        }
    }
}