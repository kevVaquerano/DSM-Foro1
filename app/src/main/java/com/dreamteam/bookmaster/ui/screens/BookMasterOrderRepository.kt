package com.dreamteam.bookmaster.ui.screens

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.Timestamp
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

            // Compras sin tiempo de espera entre pedidos.
            // Firestore valida cada pedido y sus cambios de inventario.

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
            // Estimación calculada desde el momento de confirmar.
            // El plazo de cancelación se calcula desde createdAt del servidor.
            val estimateDate = Date(DemoDeliveryTimeline.arrivalMillis(
                System.currentTimeMillis(), lines.sumOf { it.quantity }, draft.deliveryMode
            ))
            val estimateLabel = DemoDeliveryTimeline.formatDate(estimateDate.time)

            val snapshot = draft.copy(
                code = code, date = "", paymentBrand = brand, paymentLastFour = lastFour,
                membershipLevel = level, pickupEstimate = estimateLabel, items = lines, subtotal = subtotalBD.toDouble(),
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
                "status" to "PENDIENTE", "editUsed" to false, "paymentStatus" to "SIMULADO",
                "recipient" to snapshot.recipient,
                "paymentBrand" to brand, "paymentLastFour" to lastFour,
                "deliveryMode" to snapshot.deliveryMode,
                "deliveryAddress" to snapshot.deliveryAddress,
                "pickupEstimate" to if (snapshot.deliveryMode == "pickup") snapshot.pickupEstimate else "",
                "deliveryEstimate" to if (snapshot.deliveryMode == "delivery") snapshot.pickupEstimate else "",
                "estimatedArrivalAt" to Timestamp(estimateDate),
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


    /**
     * Cancela un pedido pendiente durante sus primeros cinco minutos.
     * En una sola transacción devuelve las existencias y revierte los puntos
     * acreditados. El pedido sigue registrado como CANCELADO.
     */
    fun cancelarPedido(orderId: String, callback: (Boolean, String?) -> Unit) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) { callback(false, "Iniciá sesión nuevamente"); return }
        if (orderId.isBlank()) { callback(false, "Pedido inválido"); return }

        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        val orderRef = userRef.collection("orders").document(orderId)
        // Lectura inicial para conocer los libros; se vuelve a validar todo en la transacción.
        orderRef.get().addOnSuccessListener { initial ->
            if (!initial.exists() || initial.getString("userId") != uid || Firebase.auth.currentUser?.uid != uid) {
                callback(false, "Pedido no disponible")
                return@addOnSuccessListener
            }
            val originalQuantities = (initial.get("bookQuantities") as? Map<*, *>)
                ?.mapNotNull { (id, value) ->
                    val bookId = id as? String ?: return@mapNotNull null
                    val qty = (value as? Number)?.toLong() ?: return@mapNotNull null
                    bookId to qty
                }?.toMap().orEmpty()
            if (originalQuantities.isEmpty() || originalQuantities.size > 4 || originalQuantities.values.any { it !in 1L..100L }) {
                callback(false, "El detalle original del pedido no es válido")
                return@addOnSuccessListener
            }
            val bookRefs = originalQuantities.keys.sorted().map { db.collection("books").document(it) }
            db.runTransaction { tx ->
                // Todas las lecturas deben realizarse antes de escribir.
                val user = tx.get(userRef)
                val order = tx.get(orderRef)
                val books = bookRefs.map { tx.get(it) }
                if (!user.exists() || !order.exists()) error("Pedido no disponible")
                if (order.getString("userId") != uid || order.getString("orderId") != orderId)
                    error("Este pedido no pertenece a tu cuenta")
                if (order.getString("status") != "PENDIENTE" || order.getBoolean("editUsed") == true)
                    error("Este pedido ya no se puede cancelar")
                val createdAt = order.getTimestamp("createdAt")?.toDate()?.time
                    ?: error("Fecha de compra no disponible")
                val now = System.currentTimeMillis()
                if (now < createdAt || now >= createdAt + 300_000L)
                    error("Finalizaron los cinco minutos para cancelar")
                if (user.getString("lastDemoOrderId") != orderId)
                    error("Solo se puede cancelar la compra más reciente")
                val currentQuantities = (order.get("bookQuantities") as? Map<*, *>)
                    ?.mapNotNull { (id, value) ->
                        val bookId = id as? String ?: return@mapNotNull null
                        val qty = (value as? Number)?.toLong() ?: return@mapNotNull null
                        bookId to qty
                    }?.toMap().orEmpty()
                if (currentQuantities != originalQuantities)
                    error("El pedido cambió. Volvé a abrir Mis pedidos")
                val originalItems = order.get("items") as? List<*>
                    ?: error("Falta el detalle de libros")
                val originalIds = originalItems.map { (it as? Map<*, *>)?.get("bookId") as? String }
                if (originalItems.size != originalQuantities.size || originalIds.toSet() != originalQuantities.keys)
                    error("El pedido tiene un detalle inconsistente")

                val oldPoints = order.getLong("pointsEarned") ?: error("Faltan los puntos del pedido")
                val pointsNow = user.getLong("loyaltyPoints") ?: error("Puntos no disponibles")
                if (oldPoints < 0L || pointsNow < oldPoints) error("No se pueden revertir los puntos")
                val newPoints = pointsNow - oldPoints
                val newLevel = when {
                    newPoints >= 500L -> "ORO"
                    newPoints >= 200L -> "PLATA"
                    else -> "BRONCE"
                }
                val originalLevel = order.getString("membershipLevel") ?: error("Falta la membresía")
                if (originalLevel != newLevel)
                    error("Los puntos del pedido no coinciden con la membresía original")

                bookRefs.forEachIndexed { index, ref ->
                    val oldStock = books[index].getLong("stock") ?: error("Existencias no disponibles")
                    val restored = oldStock + (originalQuantities[ref.id] ?: 0L)
                    if (restored < oldStock) error("Error al devolver el inventario")
                    tx.update(ref, "stock", restored)
                }
                val currentLevel = user.getString("membershipLevel") ?: error("Membresía no disponible")
                if (newPoints != pointsNow || newLevel != currentLevel) {
                    tx.update(userRef, mapOf("loyaltyPoints" to newPoints, "membershipLevel" to newLevel))
                }
                // Se conserva lastDemoOrderId para identificar el ultimo pedido.
                tx.update(orderRef, mapOf(
                    "status" to "CANCELADO",
                    "editUsed" to true,
                    "bookQuantities" to emptyMap<String, Long>(),
                    "items" to emptyList<Map<String, Any>>(),
                    "subtotal" to 0.0, "discount" to 0.0, "shipping" to 0.0, "total" to 0.0,
                    "subtotalCents" to 0L, "discountCents" to 0L,
                    "shippingCents" to 0L, "totalCents" to 0L,
                    "pointsEarned" to 0L, "pointsCredited" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                true
            }.addOnSuccessListener { callback(true, null) }
                .addOnFailureListener { exception ->
                    Log.e("BM_ORDER_CANCEL", "Cancelación rechazada: orderId=$orderId", exception)
                    callback(false, exception.localizedMessage ?: "No se pudo cancelar el pedido")
                }
        }.addOnFailureListener { exception ->
            Log.e("BM_ORDER_CANCEL", "No se pudo leer el pedido: orderId=$orderId", exception)
            callback(false, exception.localizedMessage ?: "No se pudo consultar el pedido")
        }
    }
}