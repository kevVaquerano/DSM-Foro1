package com.dreamteam.bookmaster.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simulación local; no son plazos reales de transporte. */
object DemoDeliveryTimeline {
    // Ventana inicial de cinco minutos para cancelar el pedido.
    const val CANCELLATION_WINDOW_MINUTES = 5
    // Factor de conversión de minutos a milisegundos.
    private const val MINUTE_MS = 60_000L

    // Tiempo adicional según la cantidad de ejemplares y la modalidad de entrega.
    fun extraMinutes(units: Int, deliveryMode: String): Int {
        val home = deliveryMode == "delivery"
        return when {
            units <= 2 -> if (home) 7 else 5
            units <= 5 -> if (home) 10 else 7
            units <= 10 -> if (home) 13 else 10
            else -> if (home) 16 else 13
        }
    }

    // Estima la llegada sumando la ventana de cancelación y el tiempo adicional.
    fun arrivalMillis(createdAtMillis: Long, units: Int, deliveryMode: String): Long =
        createdAtMillis + (CANCELLATION_WINDOW_MINUTES + extraMinutes(units, deliveryMode)) * MINUTE_MS

    // Presenta la fecha y hora estimadas en un formato legible para el usuario.
    fun formatDate(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy · h:mm a", Locale.US).format(Date(millis))
            .replace(" AM", " a. m.").replace(" PM", " p. m.")

    /** Los pedidos cancelados son terminales; no progresan en la simulación.
     * Los pedidos sin edición histórica comienzan preparación a los cinco minutos.
     */
    fun status(storedStatus: String, createdAtMillis: Long?, units: Int,
               deliveryMode: String, nowMillis: Long,
               editedAtMillis: Long? = null): String {
        // Conserva los estados finales y evita calcular etapas sin fecha de creación.
        if (storedStatus in setOf("CANCELADO", "CANCELLED", "ENTREGADO")) return storedStatus
        if (createdAtMillis == null) return storedStatus
        // Las reglas V5 conservan PENDIENTE internamente tras editar.
        // updatedAt existe solo cuando la unica edicion ya fue guardada.
        val start = if (editedAtMillis != null &&
            storedStatus in setOf("PENDIENTE", "EN_PREPARACION"))
            editedAtMillis else createdAtMillis + CANCELLATION_WINDOW_MINUTES * MINUTE_MS
        if (nowMillis < start) return if (editedAtMillis != null) "EN_PREPARACION" else "PENDIENTE"
        // Calcula el final de la simulación y el punto medio de la preparación.
        val end = start + extraMinutes(units, deliveryMode) * MINUTE_MS
        if (nowMillis >= end) return "ENTREGADO"
        val halfway = start + (end - start) / 2
        if (nowMillis < halfway) return "EN_PREPARACION"
        // Después de la preparación, distingue envío a domicilio de retiro en sucursal.
        return if (deliveryMode == "delivery") "ENVIADO" else "LISTO_PARA_RETIRAR"
    }
}