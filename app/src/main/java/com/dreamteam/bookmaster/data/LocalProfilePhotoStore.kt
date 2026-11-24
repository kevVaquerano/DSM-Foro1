package com.dreamteam.bookmaster.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Guarda la foto del perfil en el almacenamiento privado
 * de la aplicación, utilizando el UID de Firebase.
 */
object LocalProfilePhotoStore {

    // Crea la carpeta privada y asigna un archivo independiente a cada UID.
    private fun fileFor(context: Context, uid: String): File {
        val directory = File(context.filesDir, "profile_photos")
        if (!directory.exists()) directory.mkdirs()
        return File(directory, "$uid.jpg")
    }

    // Lee la imagen seleccionada y reduce su tamaño antes de guardarla.
    fun decodeSelectedPhoto(context: Context, uri: Uri): Bitmap? {
        return try {
            // Consulta las dimensiones originales sin cargar todos los píxeles.
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            // Descarta imágenes que no se pudieron reconocer.
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }

            // Reduce la resolución durante la decodificación para ahorrar memoria.
            var sample = 1
            while (
                bounds.outWidth / sample > 1024 ||
                bounds.outHeight / sample > 1024
            ) {
                sample *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
            }

            val bitmap = context.contentResolver
                .openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return null

            // Limita el lado mayor a 512 píxeles, conservando las proporciones.
            val maxEdge = 512

            if (bitmap.width <= maxEdge && bitmap.height <= maxEdge) {
                bitmap
            } else {
                val ratio = minOf(
                    maxEdge.toFloat() / bitmap.width,
                    maxEdge.toFloat() / bitmap.height
                )

                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    // Guarda o reemplaza la fotografía local en formato JPEG con calidad 85.
    fun save(
        context: Context,
        uid: String,
        bitmap: Bitmap
    ): Boolean = try {
        val file = fileFor(context, uid)

        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
    } catch (_: Exception) {
        false
    }

    /** Quita la imagen local sin afectar el perfil almacenado en Firestore. */
    fun delete(context: Context, uid: String): Boolean = try {
        val file = fileFor(context, uid)
        !file.exists() || file.delete()
    } catch (_: Exception) { false }

    // Recupera la fotografía del usuario; devuelve null si no existe o falla la lectura.
    fun load(context: Context, uid: String): Bitmap? = try {
        val file = fileFor(context, uid)

        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}