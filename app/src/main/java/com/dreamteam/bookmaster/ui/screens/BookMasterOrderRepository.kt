package com.dreamteam.bookmaster.ui.screens

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pedidos de prueba: registra pedido, acredita puntos y descuenta inventario en una transacción.
 * NO cobra tarjetas. Requiere reglas de seguridad que validen estas escrituras. */
object BookMasterOrderRepository {
    fun registrarPedido(
        draft: OrderReceiptPreview,
        paymentCardId: String,
        callback: (OrderReceiptPreview?, String?) -> Unit
    ) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) { callback(null, "Iniciá sesión para registrar el pedido"); return }
        if (draft.items.size > 4) {
            callback(null, "Por ahora podés comprar hasta 4 títulos diferentes por pedido de prueba.")
            return
        }
        if (draft.items.isEmpty() || draft.items.map { it.bookId }.distinct().size != draft.items.size || draft.items.any { it.quantity < 1 || it.quantity > 100 } ||
            draft.deliveryMode !in listOf("pickup", "delivery") ||
            draft.recipient.isBlank() || (draft.deliveryMode == "delivery" && draft.deliveryAddress.length !in 10..300)) {
            callback(null, "Revisá los datos del pedido"); return
        }
        val db = Firebase.firestore
        val orderRef = db.collection("users").document(uid).collection("orders").document()
        // Firestore genera un ID aleatorio único para el pedido.
        val code = "BM-" + SimpleDateFormat("yyyy", Locale.US).format(Date()) + "-" +
                orderRef.id.take(10).uppercase(Locale.ROOT)
        val userRef = db.collection("users").document(uid)
        val cardRef = userRef.collection("paymentMethods").document(paymentCardId)
        val itemRefs = draft.items.map { db.collection("books").document(it.bookId) }
        val cartRefs = draft.items.map { userRef.collection("cart").document(it.bookId) }

        db.runTransaction { tx ->
            // Primero todas las lecturas, después la escritura.
            val userDoc = tx.get(userRef)
            if (!userDoc.exists()) error("No se encontró el perfil")
            val cardDoc = tx.get(cardRef)
            if (!cardDoc.exists()) error("La tarjeta seleccionada ya no está disponible")
            val bookDocs = itemRefs.map { tx.get(it) }
            val cartDocs = cartRefs.map { tx.get(it) }
            draft.items.forEachIndexed { index, item ->
                if (!cartDocs[index].exists() || cartDocs[index].getLong("quantity") != item.quantity.toLong()) {
                    error("El carrito cambió. Volvé al carrito y revisá las cantidades")
                }
            }
            val points = when (val raw = userDoc.get("loyaltyPoints")) {
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull()
                else -> null
            } ?: error("No fue posible verificar la membresía")
            if (points < 0L) error("Puntos de membresía inválidos")
            val level = when {
                points >= 500L -> "ORO"
                points >= 200L -> "PLATA"
                else -> "BRONCE"
            }
            val storedLevel = userDoc.getString("membershipLevel")?.uppercase(Locale.ROOT)
            if (storedLevel !in setOf("BRONCE", "PLATA", "ORO")) error("Membresía no configurada")
            val stockAfter = mutableListOf<Long>()
            val lines = draft.items.mapIndexed { index, item ->
                val book = bookDocs[index]
                if (!book.exists()) error("Uno de los libros ya no está disponible")
                val price = (book.get("price") as? Number)?.toDouble()
                    ?: error("No se pudo verificar el precio")
                val stock = (book.get("stock") as? Number)?.toLong()
                    ?: error("No se pudo verificar la existencia")
                if (!price.isFinite() || price < 0 || stock < item.quantity) {
                    error("Sin existencias suficientes para ${item.title}")
                }
                stockAfter.add(stock - item.quantity.toLong())
                val name = book.getString("title").orEmpty()
                if (name.isBlank()) error("Libro sin nombre")
                OrderReceiptItem(item.bookId, name, item.quantity, price)
            }
            fun cents(n: Double): BigDecimal = BigDecimal.valueOf(n).setScale(2, RoundingMode.HALF_UP)
            val subtotalBD = lines.fold(BigDecimal.ZERO) { acc, item ->
                acc + cents(item.unitPrice).multiply(BigDecimal(item.quantity))
            }.setScale(2, RoundingMode.HALF_UP)
            val discountBD = subtotalBD.multiply(MembershipPricing.rate(level)).setScale(2, RoundingMode.HALF_UP)
            val shippingBD = if (draft.deliveryMode == "delivery") BigDecimal("8.50") else BigDecimal("0.00")
            val finalBD = subtotalBD - discountBD + shippingBD
            // Oro comienza en 500 puntos: el saldo deja de crecer al llegar a ese nivel.
            // Si faltan menos puntos que los generados, acreditar solo los necesarios.
            val rawEarned = (subtotalBD - discountBD).setScale(0, RoundingMode.DOWN).toLong() * 10L
            val pointsEarned = rawEarned.coerceAtMost((500L - points).coerceAtLeast(0L))
            if (pointsEarned > Int.MAX_VALUE || points > Long.MAX_VALUE - pointsEarned) {
                error("Cantidad de puntos fuera de rango")
            }
            val newPoints = points + pointsEarned
            val newLevel = when {
                newPoints >= 500L -> "ORO"
                newPoints >= 200L -> "PLATA"
                else -> "BRONCE"
            }
            val brand = cardDoc.getString("brand") ?: error("Tarjeta sin marca")
            val lastFour = cardDoc.getString("lastFour") ?: error("Tarjeta sin terminación")
            if (brand !in listOf("Visa", "Mastercard") || !lastFour.matches(Regex("[0-9]{4}"))) {
                error("Método de pago inválido")
            }
            val snapshot = draft.copy(
                code = code, date = "", paymentBrand = brand, paymentLastFour = lastFour,
                membershipLevel = level, items = lines, subtotal = subtotalBD.toDouble(),
                discount = discountBD.toDouble(), shipping = shippingBD.toDouble(),
                total = finalBD.toDouble(), pointsPending = pointsEarned.toInt(), status = "PENDIENTE"
            )
            val subtotalCents = subtotalBD.movePointRight(2).longValueExact()
            val discountCents = discountBD.movePointRight(2).longValueExact()
            val shippingCents = shippingBD.movePointRight(2).longValueExact()
            val totalCents = finalBD.movePointRight(2).longValueExact()
            tx.set(orderRef, mapOf(
                "orderId" to orderRef.id,
                "bookQuantities" to lines.associate { it.bookId to it.quantity.toLong() },
                "subtotalCents" to subtotalCents,
                "discountCents" to discountCents,
                "shippingCents" to shippingCents,
                "totalCents" to totalCents,
                "userId" to uid, "orderCode" to code,
                "createdAt" to FieldValue.serverTimestamp(),
                "status" to "PENDIENTE", "paymentStatus" to "SIMULADO",
                "recipient" to snapshot.recipient,
                "paymentBrand" to brand, "paymentLastFour" to lastFour,
                "deliveryMode" to snapshot.deliveryMode,
                "deliveryAddress" to snapshot.deliveryAddress,
                "pickupEstimate" to snapshot.pickupEstimate,
                "membershipLevel" to level,
                "items" to lines.map { mapOf("bookId" to it.bookId, "title" to it.title, "quantity" to it.quantity, "unitPrice" to it.unitPrice) },
                "subtotal" to snapshot.subtotal, "discount" to snapshot.discount,
                "shipping" to snapshot.shipping, "total" to snapshot.total,
                // Se acreditan de inmediato al registrar el pedido de prueba.
                "pointsPending" to 0L, "pointsEarned" to pointsEarned,
                "pointsCredited" to true
            ))
            // Todas estas escrituras se confirman juntas o ninguna se confirma.
            tx.update(userRef, mapOf(
                "loyaltyPoints" to newPoints,
                "membershipLevel" to newLevel,
                "lastDemoOrderId" to orderRef.id
            ))
            itemRefs.forEachIndexed { index, ref ->
                tx.update(ref, mapOf(
                    "stock" to stockAfter[index],
                    "lastDemoOrderId" to orderRef.id,
                    "lastDemoBuyerUid" to uid
                ))
            }
            // Pedido, puntos, stock y vaciado del carrito en una sola transacción.
            cartRefs.forEach { tx.delete(it) }
            snapshot
        }.addOnSuccessListener { saved ->
            // Fecha oficial: la obtenemos del documento después de registrarlo.
            orderRef.get().addOnSuccessListener { doc ->
                val timestamp = doc.getTimestamp("createdAt")?.toDate()
                val date = if (timestamp != null) {
                    val day = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(timestamp)
                    val hour = SimpleDateFormat("h:mm", Locale.US).format(timestamp)
                    val period = SimpleDateFormat("a", Locale.US).format(timestamp)
                        .let { if (it.equals("AM", true)) "a. m." else "p. m." }
                    "$day · $hour $period"
                } else "Fecha pendiente de sincronización"
                callback(saved.copy(date = date), null)
            }.addOnFailureListener {
                // El pedido YA existe: no reintentarlo para evitar duplicados.
                callback(saved.copy(date = "Registrado en Firestore"), null)
            }
        }.addOnFailureListener { error ->
            callback(null, error.localizedMessage ?: "No se pudo registrar el pedido")
        }
    }
}