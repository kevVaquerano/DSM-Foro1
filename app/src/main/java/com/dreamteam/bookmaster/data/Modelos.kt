package com.dreamteam.bookmaster.data


// Clases de Datos
data class Libro(
    val titulo: String = "",
    val autor: String = "",
    val categoria: String = "",
    val disponible: Boolean = true,
    val stock: Int= 0,
)

// Clases de Usuario con Herencia
abstract class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: String,
)

class Cliente(
    id: String,
    nombre: String,
    email: String,
    val nivelMembresia: String = "BRONCE", // BRONCE, PLATA, ORO
    var puntosFidelidad: Int = 0
) : Usuario(id, nombre, email, "CLIENTE")

class Administrador(
    id: String,
    nombre: String,
    email: String
) : Usuario(id, nombre, email, "ADMINISTRADOR")

// Estructura de Pedidos
data class ItemPedido(
    val libro: Libro,
    val cantidad: Int
)

data class Pedido(
    val codigoTicket: String,
    val cliente: Cliente,
    val items: List<ItemPedido>,
    val total: Double,
    val puntosGanados: Int,
    var estado: String = "PENDIENTE" // PENDIENTE, ENTREGADO
)