// Excepción personalizada para cancelar una operación
// y regresar al Menú Principal.
class VolverAlMenuException : Exception()
// Lee una entrada del usuario.
// Si escribe "M", permite regresar al menú principal.
fun leerEntrada(mensaje: String): String {
    print(mensaje)
    val entrada = readln().trim()

    if (entrada.equals("M", ignoreCase = true)) {
        throw VolverAlMenuException()
    }

    return entrada
}

// Valida valores decimales positivos.
// Utilizado principalmente para el precio del libro.
fun leerDoublePositivo(mensaje: String): Double {
    while (true) {
        val entrada = leerEntrada(mensaje)

        if (entrada.isEmpty()) {
            println("                                                           ⫷ ❌  El precio no puede quedar vacío.  ❌ ⫸")

        } else if (!entrada.matches(Regex("\\d+(\\.\\d+)?"))) {
            println("                                             ⫷ ❌  Ingrese un precio válido usando solo números y punto decimal.  ❌ ⫸")

        } else {
            val valor = entrada.toDoubleOrNull()

            if (valor != null && valor > 0) {
                return valor
            }

            println("                                                           ⫷ ❌  El precio debe ser mayor que 0.  ❌ ⫸")
        }
    }
}

// Valida números enteros mayores o iguales a cero.
// Utilizado para stock inicial y stock mínimo.
fun leerEnteroPositivo(mensaje: String): Int {
    while (true) {
        val entrada = leerEntrada(mensaje)

        if (entrada.isEmpty()) {
            println("                                                         ⫷ ❌  Este campo no puede quedar vacío.  ❌ ⫸")

        } else if (!entrada.all { it.isDigit() }) {
            println("                                               ⫷ ❌  Ingrese únicamente números enteros iguales o mayores que 0.  ❌ ⫸")

        } else {
            val valor = entrada.toIntOrNull()

            if (valor == null) {

                println("                                                        ⫷ ❌  El número ingresado es demasiado grande.  ❌ ⫸")

            } else if (valor == 0) {

                println("                                                     ⫷ ❌  El valor no puede ser 0. Debe ser mayor que 0. ❌ ⫸")

            } else {

                return valor
            }
        }
    }
}

// Solicita un ID único de libro.
// Valida que sea numérico, tenga 13 dígitos
// y no exista previamente en el sistema.
fun leerIdUnico(sistema: SistemaLibreria): String {
    while (true) {

        val id = leerEntrada("ID del libro (13 dígitos): ")

        if (id.isEmpty()) {
            println("                                                            ⫷ ❌  El ID no puede quedar vacío.  ❌ ⫸")

        } else if (!id.all { it.isDigit() }) {
            println("                                                          ⫷ ❌  El ID solo puede contener números.  ❌ ⫸")

        } else if (id.length != 13) {
            println("                                                       ⫷ ❌  El ID debe contener exactamente 13 dígitos.  ❌ ⫸")

        } else if (sistema.buscarLibroPorId(id) != null) {
            println("                                                             ⫷ ❌  Ya existe un libro con ese ID.  ❌ ⫸")

        } else {
            return id
        }
    }
}

// Solicita un título y evita títulos duplicados.
// La comparación ignora diferencias de mayúsculas,
// minúsculas y algunas tildes.
fun leerTituloUnico(sistema: SistemaLibreria): String {
    while (true) {

        val titulo = leerEntrada("Título: ")

        if (titulo.isEmpty()) {
            println("                                                            ⫷ ❌  El título no puede quedar vacío.  ❌ ⫸")

        } else if (sistema.buscarLibroPorTitulo(titulo) != null) {
            println("                                                           ⫷ ❌  Ya existe un libro con ese título.  ❌ ⫸")

        } else {
            return titulo
        }
    }
}

// Valida el nombre del autor.
// Permite letras, espacios, puntos y guiones.
// Evita números y símbolos no permitidos.
fun leerAutorValido(): String {
    while (true) {

        val autor = leerEntrada("Autor: ")

        if (autor.isEmpty()) {
            println("                                                           ⫷ ❌  El autor no puede quedar vacío.  ❌ ⫸")

        } else if (!autor.any { it.isLetter() }) {
            println("                                                            ⫷ ❌  El autor debe contener letras.  ❌ ⫸")

        } else if (!autor.all {
                it.isLetter() ||
                it.isWhitespace() ||
                it == '.' ||
                it == '-'
            }) {

            println("                                            ⫷ ❌  El autor solo puede contener letras, espacios, puntos y guiones.  ❌ ⫸")

        } else if (!autor.first().isLetter()) {

            println("                                                    ⫷ ❌  El nombre del autor debe comenzar con una letra.  ❌ ⫸")

        } else if (autor.endsWith(".")) {

            println("                                                   ⫷ ❌  El nombre del autor no puede terminar con un punto.  ❌ ⫸")

        } else {
            return autor
        }
    }
}

// Valida la categoría del libro.
// Solo permite letras y espacios.
fun leerCategoriaValida(): String {
    while (true) {

        val categoria = leerEntrada("Categoría: ")

        if (categoria.isEmpty()) {
            println("                                                          ⫷ ❌  La categoría no puede quedar vacía.   ❌ ⫸")

        } else if (!categoria.all { it.isLetter() || it.isWhitespace() }) {

            println("                                                    ⫷ ❌  La categoría solo puede contener letras y espacios.  ❌ ⫸")

        } else {
            return categoria
        }
    }
}

// Busca un libro existente mediante su ID.
// Se utiliza para eliminar libros y realizar compras.
// Repite la solicitud hasta encontrar un libro válido.
fun leerLibroExistente(
    sistema: SistemaLibreria,
    mensaje: String
): Libro {

    while (true) {

        val id = leerEntrada(mensaje)
        val libro = sistema.buscarLibroPorId(id)

        if (libro != null) {
            return libro
        }

        println("                                                      ✉   ⚠️  No se encontró ningún libro con ese ID. ⚠️    ✉")
    }
}


// Valida la cantidad de libros que desea comprar.
// Comprueba que sea un número positivo
// y que no supere el stock disponible.
fun leerCantidadCompra(stockDisponible: Int): Int {

    while (true) {

        val entrada = leerEntrada("Cantidad: ")

        if (entrada.isEmpty()) {

            println("                                                            ⫷ ❌  La cantidad no puede quedar vacía.  ❌ ⫸")

        } else if (!entrada.all { it.isDigit() }) {

            println("                                                       ⫷ ❌  Ingrese únicamente números enteros positivos.  ❌ ⫸")

        } else {

            val cantidad = entrada.toIntOrNull()

            if (cantidad == null) {

                println("                                                          ⫷ ❌  El número ingresado es demasiado grande.  ❌ ⫸")

            } else if (cantidad <= 0) {

                println("                                                             ⫷ ❌  La cantidad debe ser mayor que 0.  ❌ ⫸")

            } else if (cantidad > stockDisponible) {

                println("                                                           ⫷ ❌  Stock insuficiente. Disponibles: $stockDisponible.  ❌ ⫸")

            } else {

                return cantidad
            }
        }
    }
}


// Punto principal de ejecución del sistema.
// Controla el menú principal y las opciones del usuario.
fun main() {

    val sistema = SistemaLibreria()
    var opcion: Int


    println("=====================================================================================================================================================")
    println("                                                                     BOOKMASTER UDB - CONSOLA                                                        ")
    println("=====================================================================================================================================================")


    do {

        println("\n------------------------------------------------------------------------- MENÚ PRINCIPAL ------------------------------------------------------------")
        println("1. Listar catálogo de libros.")
        println("2. Agregar nuevo libro (CRUD).")
        println("3. Eliminar libro.")
        println("4. Procesar venta / Reserva (Lógica de negocio).")
        println("5. Ver alertas de stock crítico.")
        println("6. Reporte general del sistema.")
        println("7. Salir.")
        println("-----------------------------------------------------------------------------------------------------------------------------------------------------")
        print("Seleccione una opción: ")

        try {
            val entrada = readlnOrNull()?.trim() ?: break

            println("-----------------------------------------------------------------------------------------------------------------------------------------------------")

            // Validación de la opción del menú.
            // Solo permite números enteros del 1 al 7.
            opcion = if (entrada.isEmpty() || !entrada.all { it.isDigit() }) {

                println("\n                                                           ⚠️  Ingrese únicamente números del menú.  ⚠️")
                -1

            } else {

                val numero = entrada.toIntOrNull()

                if (numero == null || numero !in 1..7) {

                    println("\n                                                      ⚠️  Opción inválida. Ingrese un número del 1 al 7.  ⚠️")
                    -1

                } else {

                    numero
                }
            }
            
            when (opcion) {

                // Mostrar todos los libros registrados
                1 -> {

                    println("\n---------------------------------------------------------------------- CATÁLOGO DE LIBROS -----------------------------------------------------------")

                    sistema.listar().forEach { libro ->

                        println(
                            "[${libro.id}] ${libro.titulo} | " +
                            "Autor: ${libro.autor} | " +
                            "Precio: $${libro.precio} | " +
                            "Stock: ${libro.stockActual} " +
                            "(Min: ${libro.stockMinimo})"
                        )
                    }

                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")
                }

                // Registrar un nuevo libro en el sistema
                2 -> {
                    println("\n--------------------------------------------------------------------- REGISTRAR NUEVO LIBRO ---------------------------------------------------------")
                    println("➥  Escriba M en cualquier momento para volver al Menú Principal.")
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")

                    val id = leerIdUnico(sistema)
                    val titulo = leerTituloUnico(sistema)
                    val autor = leerAutorValido()
                    val categoria = leerCategoriaValida()
                    val precio = leerDoublePositivo("Precio: ")
                    val stock = leerEnteroPositivo("Stock Inicial: ")
                    val stockMinimo = leerEnteroPositivo("Stock Mínimo: ")
                    
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")

                    sistema.agregar(
                        Libro(
                            id,
                            titulo,
                            autor,
                            categoria,
                            precio,
                            stock,
                            stockMinimo
                        )
                    )

                    println("\n✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                    println("                                                                  ✅ ¡Libro agregado exitosamente! ✅")
                    println("✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                }

                // Eliminar un libro existente
                3 -> {
                    println("\n------------------------------------------------------------------------ ELIMINAR LIBRO -------------------------------------------------------------")
                    println("➥  Escriba M en cualquier momento para volver al Menú Principal.")
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")

                    val libro = leerLibroExistente(
                        sistema,
                        "Ingrese el ID del libro a eliminar: "
                    )
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")

                    sistema.eliminar(libro.id)

                    println("\n✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                    println("                                                              ✅ Libro eliminado correctamente. ✅")
                    println("✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                }

                // Procesar compra y generar pedido
                4 -> {
                    println("\n--------------------------------------------------------------------- PROCESAR VENTA DE TICKET ------------------------------------------------------")
                    println("➥  Escriba M en cualquier momento para volver al Menú Principal.")
                    println("---------------------------------------------------------------------------------------------------------------------------------------------------")

                    val cliente = sistema.listaClientes.first()

                    println("Cliente seleccionado: ${cliente.nombre} (Nivel: ${cliente.nivelMembresia})")

                    val libro = leerLibroExistente(
                        sistema,
                        "Ingrese el ID del libro a comprar: "
                    )

                    val cantidad = leerCantidadCompra(libro.stockActual)
                    println("---------------------------------------------------------------------------------------------------------------------------------------------------")

                    val pedido = sistema.procesarPedido(
                        cliente,
                        listOf(ItemPedido(libro, cantidad))
                    )

                    println("\n✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                    println("                                                                    🎉 ¡VENTA COMPLETADA! 🎉")
                    println("✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                    println("Ticket generado: ${pedido.codigoTicket}")
                    println("Total pagado: $${pedido.total}")
                    println("Puntos ganados: +${pedido.puntosGanados} (Total acum: ${cliente.puntosFidelidad})")
                    println("✦ ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── ✦")
                }

                // Mostrar libros cuyo stock alcanzó el mínimo establecido
                5 -> {
                    println("\n-------------------------------------------------------------------- ALERTAS DE STOCK CRÍTICO -------------------------------------------------------")

                    val alertas = sistema.obtenerAlertasStock()

                    if (alertas.isEmpty()) {
                        println(" No hay libros con stock crítico por ahora.")
                    } else {
                        alertas.forEach { libro ->
                            println(
                                " ALERTA: '${libro.titulo}' - " +
                                "Quedan ${libro.stockActual} unidades " +
                                "(Mínimo requerido: ${libro.stockMinimo})"
                            )
                            println("-----------------------------------------------------------------------------------------------------------------------------------------------------")
                        }
                    }
                }

                // Mostrar resumen general del sistema
                6 -> {

                    println("\n------------------------------------------------------------------------- REPORTE GENERAL -----------------------------------------------------------")


                    println("Total de títulos en sistema: ${sistema.listaLibros.size}")
                    println("Total de pedidos procesados: ${sistema.listaPedidos.size}")
                    println("Total Recaudado: $${sistema.listaPedidos.sumOf { it.total }}")
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")
                }

                // Finalizar aplicación
                7 -> {
                    println("\n-----------------------------------------------------------------------------------------------------------------------------------------------------")
                    println("                                                          ➡️   Saliendo de BookMaster UDB...  ➡️")
                    println("-----------------------------------------------------------------------------------------------------------------------------------------------------")
                }
            }
            
        } catch (e: VolverAlMenuException) {
            opcion = -1
        } 
        
        catch (e: Exception) {
            println("\n                                           ⫷ ❌  Error: Ingreso de datos no válido. Se registró en el Log de errores.  ❌ ⫸")
            LoggerService.logError(
                "                                              ⫷ ❌  Error de interacción en menú principal.   ❌ ⫸",
                e
            )

            opcion = -1
        }

    } while (opcion != 7)
}