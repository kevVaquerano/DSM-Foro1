package com.dreamteam.bookmaster.data


// Modelo de datos de un libro y sus existencias.
data class Libro(
    val titulo: String = "",
    val autor: String = "",
    val categoria: String = "",
    val disponible: Boolean = true,
    val stock: Int= 0,
)

// Clase base que reúne los datos comunes de los usuarios.
abstract class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: String,
)

// Cliente con nivel de membresía y puntos de fidelidad.
class Cliente(
    id: String,
    nombre: String,
    email: String,
    val nivelMembresia: String = "BRONCE", // BRONCE, PLATA, ORO
    var puntosFidelidad: Int = 0
) : Usuario(id, nombre, email, "CLIENTE")

// Usuario especializado con el rol de administrador.
class Administrador(
    id: String,
    nombre: String,
    email: String
) : Usuario(id, nombre, email, "ADMINISTRADOR")

// Relaciona un libro con la cantidad solicitada en un pedido.
data class ItemPedido(
    val libro: Libro,
    val cantidad: Int
)

// Agrupa los artículos, el cliente, el total y el estado del pedido.
data class Pedido(
    val codigoTicket: String,
    val cliente: Cliente,
    val items: List<ItemPedido>,
    val total: Double,
    val puntosGanados: Int,
    var estado: String = "PENDIENTE" // PENDIENTE, ENTREGADO
)