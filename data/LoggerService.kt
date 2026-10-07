import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


// Servicio encargado de registrar errores del sistema.
// Guarda la fecha, mensaje y detalles de la excepción.
object LoggerService {

    // Archivo donde se almacenan los registros de error.
    private const val LOG_FILE = "errores.log"

    // Registra un error ocurrido durante la ejecución.
    // Puede recibir una excepción opcional para guardar detalles adicionales.
    fun logError(
        mensaje: String,
        excepcion: Throwable? = null
    ) {

        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val fechaHora = formatoFecha.format(Date())

        val detalleError =
            excepcion?.message
                ?: excepcion?.toString()
                ?: "Sin detalles adicionales"

        val registro =
            "[$fechaHora] ERROR: $mensaje | Detalle: $detalleError\n"

        try {

            File(LOG_FILE).appendText(registro)

        } catch (e: Exception) {

            System.err.println(
                "No se pudo guardar el registro de error: ${e.message}"
            )
        }
    }
}