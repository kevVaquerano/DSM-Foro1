package com.dreamteam.bookmaster.ui.screens

import androidx.compose.runtime.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Datos de membresía leídos del documento del usuario, nunca valores de demostración. */
data class MembershipStatus(
    val level: String? = null,
    val points: Long? = null,
    val loading: Boolean = true,
    val error: Boolean = false
)

/** Escucha cambios para actualizar perfil, carrito y checkout sin volver a iniciar sesión. */
@Composable
fun rememberMembershipStatus(): MembershipStatus {
    val uid = Firebase.auth.currentUser?.uid
    var status by remember(uid) { mutableStateOf(MembershipStatus()) }
    // Reinicia la consulta al cambiar de cuenta y controla la vida útil del listener.
    DisposableEffect(uid) {
        var active = true
        status = if (uid == null) MembershipStatus(loading = false, error = true)
        else MembershipStatus()
        // Consulta únicamente el documento de la cuenta autenticada.
        val registration = if (uid == null) null else
            Firebase.firestore.collection("users").document(uid)
                .addSnapshotListener { document, error ->
                    // Ignora respuestas tardías de una sesión que ya no está activa.
                    if (!active || Firebase.auth.currentUser?.uid != uid) return@addSnapshotListener
                    if (error != null || document == null || !document.exists()) {
                        status = MembershipStatus(loading = false, error = true)
                    } else {
                        // Aceptamos temporalmente el número histórico guardado como texto.
                        // Los registros nuevos usan Long. Un valor inválido no da beneficios.
                        val rawPoints = document.get("loyaltyPoints")
                        val points = when (rawPoints) {
                            is Long -> rawPoints
                            is Int -> rawPoints.toLong()
                            is String -> rawPoints.trim().toLongOrNull()
                            else -> null
                        }?.takeIf { it >= 0L }
                        // Normaliza y comprueba que el nivel registrado sea reconocido.
                        val storedLevel = document.getString("membershipLevel")
                            ?.trim()?.uppercase(Locale.ROOT)
                        val validLevel = storedLevel in setOf("BRONCE", "PLATA", "ORO")
                        // Los puntos son la fuente de verdad para el nivel efectivo.
                        // Si todavía no hay saldo válido, no otorgamos ningún descuento.
                        val effectiveLevel = if (points == null || !validLevel) null else when {
                            points >= 500L -> "ORO"
                            points >= 200L -> "PLATA"
                            else -> "BRONCE"
                        }
                        status = MembershipStatus(
                            level = effectiveLevel,
                            points = points,
                            loading = false,
                            error = points == null || !validLevel
                        )
                    }
                }
        // Libera el listener al salir de la composición o cambiar de usuario.
        onDispose {
            active = false
            registration?.remove()
        }
    }
    return status
}

/** Centraliza los porcentajes de descuento y su presentación en la interfaz. */
object MembershipPricing {
    // Aplica la tasa correspondiente; un nivel desconocido no recibe descuento.
    fun rate(level: String?): BigDecimal = when (level) {
        "BRONCE" -> BigDecimal("0.03")
        "PLATA" -> BigDecimal("0.05")
        "ORO" -> BigDecimal("0.10")
        else -> BigDecimal.ZERO
    }
    // Calcula el descuento monetario y lo redondea a dos decimales.
    fun discount(subtotal: Double, level: String?): Double =
        BigDecimal.valueOf(subtotal).multiply(rate(level))
            .setScale(2, RoundingMode.HALF_UP).toDouble()
    // Formatea los importes en dólares con dos decimales.
    fun money(value: Double): String = String.format(Locale.US, "$%.2f", value)
    // Etiqueta visible con el nombre del nivel y su porcentaje de descuento.
    fun label(level: String?): String = when (level) {
        "BRONCE" -> "Bronce · 3%"
        "PLATA" -> "Plata · 5%"
        "ORO" -> "Oro · 10%"
        else -> "Membresía no configurada"
    }
}