package com.dreamteam.bookmaster.data

// Modelo de datos de una tarea: contiene su identificación, descripción, imagen y fecha de creación.
data class TaskItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)