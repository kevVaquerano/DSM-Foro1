// Modelo que representa un libro dentro del catálogo.
data class Libro(

    val id: String,

    val titulo: String,

    val autor: String,

    val categoria: String,

    val precio: Double,

    // Puede modificarse porque cambia al realizar ventas.
    var stockActual: Int,

    // Define el límite mínimo para generar alertas.
    val stockMinimo: Int
)

// Modelo que representa un cliente registrado.
data class Cliente(

    val id: String,

    val nombre: String,

    val email: String,

    val nivelMembresia: String,

    // Aumenta según las compras realizadas.
    var puntosFidelidad: Int
)

// Representa un libro dentro de un pedido
// junto con la cantidad solicitada.
data class ItemPedido(

    val libro: Libro,

    val cantidad: Int
)

// Modelo que representa una compra realizada.
// Guarda información del cliente,
// productos adquiridos, total y puntos obtenidos.
data class Pedido(

    val codigoTicket: String,

    val cliente: Cliente,

    val items: List<ItemPedido>,

    val total: Double,

    val puntosGanados: Int
)