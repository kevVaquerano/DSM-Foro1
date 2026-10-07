// Interfaz genérica para definir operaciones básicas
// que pueden realizarse sobre elementos gestionables.
interface IGestionable<T> {

    fun agregar(item: T)

    fun listar(): List<T>

    fun eliminar(id: String): Boolean
}

// Clase principal encargada de administrar libros,
// clientes y pedidos dentro del sistema.
class SistemaLibreria : IGestionable<Libro> {

    // Colecciones principales del sistema.
    // Almacenan temporalmente la información en memoria.
    val listaLibros = mutableListOf<Libro>()
    val listaPedidos = mutableListOf<Pedido>()
    val listaClientes = mutableListOf<Cliente>()

    // Inicialización de datos de prueba para poder
    // probar las funciones del sistema.
    init {

        // Libros iniciales del catálogo.
        listaLibros.add(
            Libro(
                "9788498381498",
                "El Principito",
                "A. Saint-Exupéry",
                "Ficción",
                12.0,
                8,
                3
            )
        )

        listaLibros.add(
            Libro(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                "Tecnología",
                35.0,
                2,
                5
            )
        ) // Stock crítico

        listaLibros.add(
            Libro(
                "9780140449136",
                "Kotlin Programming",
                "Big Nerd Ranch",
                "Tecnología",
                45.0,
                10,
                4
            )
        )

        // Clientes iniciales del sistema.
        listaClientes.add(
            Cliente(
                "C01",
                "Hayzel González",
                "hayzel@udb.edu.sv",
                "PLATA",
                50
            )
        )

        listaClientes.add(
            Cliente(
                "C02",
                "Kevin Vaquerano",
                "kevin@udb.edu.sv",
                "ORO",
                120
            )
        )
    }

    // Agrega un nuevo libro al catálogo.
    override fun agregar(item: Libro) {

        listaLibros.add(item)
    }

    // Devuelve el catálogo de libros.
    // Se retorna una copia para evitar modificaciones
    // directas desde fuera de la clase.
    override fun listar(): List<Libro> {

        return listaLibros.toList()
    }

    // Elimina un libro utilizando su identificador.
    // Retorna true si fue eliminado correctamente.
    override fun eliminar(id: String): Boolean {

        return listaLibros.removeIf { it.id == id }
    }

    // Busca un libro mediante su ID.
    // Retorna el libro encontrado o null si no existe.
    fun buscarLibroPorId(id: String): Libro? {

        return listaLibros.find { it.id == id }
    }

    // Normaliza títulos para compararlos sin importar:
    // - mayúsculas/minúsculas
    // - espacios adicionales
    // - algunas diferencias de tildes.
    private fun normalizarTitulo(titulo: String): String {

        return titulo
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ü', 'u')
    }

    // Busca libros por título aplicando normalización
    // para evitar duplicados como:
    // "El Principito" y "el princípito".
    fun buscarLibroPorTitulo(titulo: String): Libro? {

        val tituloNormalizado = normalizarTitulo(titulo)

        return listaLibros.find {

            normalizarTitulo(it.titulo) == tituloNormalizado
        }
    }

    // Obtiene los libros cuyo stock actual es menor
    // o igual al stock mínimo configurado.
    // Estos libros requieren atención.
    fun obtenerAlertasStock(): List<Libro> {

        return listaLibros.filter {
            it.stockActual <= it.stockMinimo
        }
    }

    // Procesa una compra completa:
    // 1. Verifica disponibilidad de stock.
    // 2. Calcula total de compra.
    // 3. Aplica puntos según membresía.
    // 4. Actualiza inventario.
    // 5. Genera y almacena el pedido.
    fun procesarPedido(
        cliente: Cliente,
        items: List<ItemPedido>
    ): Pedido {

        var total = 0.0

        // Validación y cálculo del total.
        for (item in items) {

            if (item.libro.stockActual < item.cantidad) {

                throw IllegalArgumentException(
                    "Stock insuficiente para: ${item.libro.titulo}"
                )
            }

            total += item.libro.precio * item.cantidad
        }

        // Determina el multiplicador de puntos
        // según el nivel de membresía del cliente.
        val factor = when (cliente.nivelMembresia.uppercase()) {

            "ORO" -> 2.0

            "PLATA" -> 1.5

            else -> 1.0 // BRONCE
        }

        // Conversión a entero porque los puntos
        // se manejan como valores completos.
        val puntosGanados = (total * factor).toInt()

        cliente.puntosFidelidad += puntosGanados

        // Actualización del inventario después
        // de confirmar la compra.
        for (item in items) {

            item.libro.stockActual -= item.cantidad
        }

        // Generación del código único del ticket.
        val codigoTicket = "UDB-${(1000..9999).random()}"

        // Creación y almacenamiento del pedido.
        val nuevoPedido = Pedido(
            codigoTicket,
            cliente,
            items,
            total,
            puntosGanados
        )

        listaPedidos.add(nuevoPedido)

        return nuevoPedido
    }
}