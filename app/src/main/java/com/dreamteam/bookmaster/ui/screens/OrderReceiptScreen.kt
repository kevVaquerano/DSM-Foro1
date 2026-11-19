package com.dreamteam.bookmaster.ui.screens

import android.app.Activity
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

/** Copia inmutable de artículos y totales registrada en Firestore. */
data class OrderReceiptItem(
    val bookId: String,
    val title: String,
    val quantity: Int,
    val unitPrice: Double
)

data class OrderReceiptPreview(
    val recipient: String,
    val date: String,
    val paymentBrand: String,
    val paymentLastFour: String,
    val deliveryMode: String,
    val deliveryAddress: String,
    val pickupEstimate: String,
    val membershipLevel: String,
    val items: List<OrderReceiptItem>,
    val subtotal: Double,
    val discount: Double,
    val shipping: Double,
    val total: Double,
    val code: String = "",
    val pointsPending: Int = 0,
    val status: String = "PENDIENTE"
)

@Composable
fun OrderReceiptScreen(
    receipt: OrderReceiptPreview,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val dark = BookMasterDarkPurple
    val purple = BookMasterPurple
    val line = Color(0xFFB99DD6)
    val muted = Color(0xFF61566C)
    val pale = Color(0xFFFCFAFF)
    fun money(n: Double) = String.format(Locale.US, "$%.2f", n)
    val rate = MembershipPricing.rate(receipt.membershipLevel).multiply(java.math.BigDecimal(100)).toInt()
    val pointsPreview = receipt.pointsPending

    // El recibo solo ofrece volver al inicio: no regresamos al Checkout.
    BackHandler(enabled = true) { }

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.rgb(71, 48, 113)
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        }
    }

    val savePdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                // Generamos primero un PDF local completo y comprobamos que sea legible.
                // Evita comunicar éxito si el destino recibe un PDF vacío o incompleto.
                val temporal = File.createTempFile("bookmaster_recibo_", ".pdf", context.cacheDir)
                try {
                    FileOutputStream(temporal).use { archivo ->
                        exportPreviewReceiptPdf(receipt, archivo)
                        archivo.fd.sync()
                    }
                    comprobarPdf(temporal)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { destino ->
                        temporal.inputStream().use { origen -> origen.copyTo(destino) }
                        destino.flush()
                    } ?: error("No se pudo abrir el archivo de destino")
                    Toast.makeText(context, "Recibo PDF guardado correctamente", Toast.LENGTH_LONG).show()
                } finally {
                    temporal.delete()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(pale)) {
        Box(Modifier.fillMaxWidth().background(dark).statusBarsPadding().height(62.dp)) {
            Text("Recibo de compra", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(), color = Color.White,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp), shadowElevation = 5.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReceiptInfoRow("Código del pedido:", receipt.code, dark)
                    HorizontalDivider(color = line)
                    ReceiptInfoRow("A nombre de:", receipt.recipient, dark)
                    HorizontalDivider(color = line)
                    ReceiptInfoRow("Fecha del pedido:", receipt.date, dark)
                    HorizontalDivider(color = line)
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Método de pago:", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(painterResource(R.drawable.ic_checkout_card), null, tint = purple, modifier = Modifier.size(22.dp))
                            Column {
                                Text("${receipt.paymentBrand} ···· ${receipt.paymentLastFour}", color = dark, fontSize = 14.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = line)
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Método de entrega:", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                painterResource(if (receipt.deliveryMode == "pickup") R.drawable.ic_checkout_store else R.drawable.ic_nav_home),
                                null, tint = purple, modifier = Modifier.size(21.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(if (receipt.deliveryMode == "pickup") "Retiro en sucursal · Gratis" else "Entrega a domicilio · $8.50",
                                    color = dark, fontSize = 14.sp)
                                if (receipt.deliveryMode == "pickup") {
                                    Text("Fecha y hora estimada del retiro:", color = dark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(receipt.pickupEstimate, color = dark, fontSize = 14.sp)
                                } else {
                                    Text("Dirección de entrega:", color = dark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(receipt.deliveryAddress, color = dark, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = line)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Descuento", color = dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        val bg = when (receipt.membershipLevel) {
                            "BRONCE" -> Color(0xFFE9C5A8)
                            "PLATA" -> Color(0xFFDDE1E6)
                            "ORO" -> Color(0xFFF5D982)
                            else -> Color(0xFFEDE5F8)
                        }
                        val fg = when (receipt.membershipLevel) {
                            "BRONCE" -> Color(0xFF704020)
                            "PLATA" -> Color(0xFF46515F)
                            "ORO" -> Color(0xFF715000)
                            else -> dark
                        }
                        Surface(color = bg, shape = RoundedCornerShape(50)) {
                            Text("$rate% · ${receipt.membershipLevel}", Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                color = fg, fontSize = 12.sp)
                        }
                    }
                    HorizontalDivider(color = line)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Artículos", Modifier.width(86.dp), color = dark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Descripción", Modifier.weight(1f), color = dark, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Text("Precio", Modifier.width(76.dp), color = dark, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = line)
                        receipt.items.forEachIndexed { index, item ->
                            key(item.bookId) {
                                val coverId = remember(item.bookId) { context.resources.getIdentifier(item.bookId, "drawable", context.packageName) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(76.dp).height(103.dp), contentAlignment = Alignment.Center) {
                                        if (coverId != 0) Image(painterResource(coverId), "Portada de ${item.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        else Text("Sin portada", fontSize = 10.sp, color = muted)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(item.title, color = dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        Text("Cantidad: ${item.quantity}", color = muted, fontSize = 12.sp)
                                    }
                                    Text(money(item.unitPrice * item.quantity), Modifier.width(76.dp),
                                        color = dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                                }
                                if (index < receipt.items.lastIndex) Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                                    val dash = 7.dp.toPx(); val gap = 6.dp.toPx(); var x = 0f
                                    while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(min(x + dash, size.width), 0f), strokeWidth = 1.dp.toPx()); x += dash + gap }
                                }
                            }
                        }
                    }
                    // Misma grada lila y sombra sutil que en Checkout, de borde a borde.
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReceiptInfoRow("Subtotal:", money(receipt.subtotal), dark)
                        ReceiptInfoRow("Descuento:", "−${money(receipt.discount)}", dark, Color(0xFF2E7D32))
                        ReceiptInfoRow("Subtotal con descuento:", money(receipt.subtotal - receipt.discount), dark)
                        ReceiptInfoRow("Costo de entrega:", money(receipt.shipping), dark)
                        if (pointsPreview > 0) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Puntos obtenidos:", Modifier.weight(1f), fontSize = 13.sp,
                                    color = dark, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Row(Modifier.weight(1.15f), horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(R.drawable.ic_crown_award), contentDescription = null,
                                        tint = when (receipt.membershipLevel) {
                                            "BRONCE" -> Color(0xFFCD7F32)
                                            "PLATA" -> Color(0xFFC0C0C0)
                                            "ORO" -> Color(0xFFFFD700)
                                            else -> Color(0xFF8794A3)
                                        }, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("+$pointsPreview puntos", fontSize = 14.sp, color = dark)
                                }
                            }
                        }
                        HorizontalDivider(color = line)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TOTAL:", color = dark, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(money(receipt.total), color = purple, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Ver mi pedido · Próximamente")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { savePdf.launch("BookMaster_${receipt.code}.pdf") },
                        modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Guardar PDF", fontSize = 12.sp)
                    }
                    Button(onClick = onHomeClick, modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                        Text("Volver al inicio", fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReceiptInfoRow(label: String, value: String, color: Color, valueColor: Color = color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, Modifier.weight(1.15f), fontSize = 14.sp, color = valueColor, textAlign = TextAlign.End)
    }
}

/** Comprueba que el documento generado tenga al menos una página representable. */
private fun comprobarPdf(archivo: File) {
    require(archivo.length() > 500L) { "El PDF generado está vacío o incompleto" }
    val descriptor = ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)
    PdfRenderer(descriptor).use { renderer ->
        require(renderer.pageCount > 0) { "El PDF no contiene páginas" }
        renderer.openPage(0).use { pagina ->
            val bitmap = Bitmap.createBitmap(300, 425, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.WHITE)
                pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                var tieneContenido = false
                // Examinar la mitad superior donde siempre se escribe el título.
                loop@ for (y in 10 until 165 step 3) {
                    for (x in 12 until 275 step 3) {
                        if (bitmap.getPixel(x, y) != android.graphics.Color.WHITE) {
                            tieneContenido = true
                            break@loop
                        }
                    }
                }
                require(tieneContenido) { "El visor detectó una página sin contenido visible" }
            } finally {
                bitmap.recycle()
            }
        }
    }
}

/** Recibo de pedido registrado de prueba, sin representar un cobro bancario. */
private fun exportPreviewReceiptPdf(receipt: OrderReceiptPreview, output: java.io.OutputStream) {
    val pdf = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(71, 48, 113)
            textSize = 13f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        var y = 54f
        fun line(value: String, fontSize: Float = 13f) {
            ink.textSize = fontSize
            canvas.drawText(value.take(85), 42f, y, ink)
            y += 24f
        }
        line("BOOKMASTER - RECIBO DE PEDIDO", 15f)
        line("Código del pedido: ${receipt.code}")
        line("Destinatario: ${receipt.recipient}")
        line("Fecha y hora del pedido: ${receipt.date}")
        line("Tarjeta de prueba: ${receipt.paymentBrand} **** ${receipt.paymentLastFour}")
        line("Entrega: ${if (receipt.deliveryMode == "pickup") "Retiro en sucursal" else "Domicilio"}")
        if (receipt.deliveryMode == "pickup") line("Retiro estimado: ${receipt.pickupEstimate}")
        else {
            line("Dirección:")
            receipt.deliveryAddress.chunked(70).take(3).forEach { line(it) }
        }
        line("Membresía: ${receipt.membershipLevel}")
        y += 8f
        line("ARTICULOS", 15f)
        receipt.items.take(16).forEach { item ->
            line("${item.quantity} x ${item.title.take(40)}  =  ${String.format(Locale.US, "$%.2f", item.quantity * item.unitPrice)}")
        }
        y += 12f
        line("Subtotal: ${String.format(Locale.US, "$%.2f", receipt.subtotal)}")
        line("Descuento: ${String.format(Locale.US, "$%.2f", receipt.discount)}")
        line("Envío: ${String.format(Locale.US, "$%.2f", receipt.shipping)}")
        line("TOTAL: ${String.format(Locale.US, "$%.2f", receipt.total)}", 16f)
        if (receipt.pointsPending > 0) line("Puntos obtenidos: ${receipt.pointsPending}")
        pdf.finishPage(page)
        pdf.writeTo(output)
    } finally {
        pdf.close()
    }
}