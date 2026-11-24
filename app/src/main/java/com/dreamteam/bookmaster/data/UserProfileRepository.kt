package com.dreamteam.bookmaster.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Mantiene los perfiles antiguos sin sustituir documentos ni subcolecciones. */
object UserProfileRepository {
    // Acceso a la instancia de Firestore.
    private val db get() = Firebase.firestore

    /**
     * Prepara el perfil en una transacción, completa campos ausentes y corrige niveles inválidos cuando corresponde.
     * Una cuenta con membresía previa nunca pierde puntos ni nivel al iniciar sesión.
     * Esta inicialización NO acredita puntos por compras.
     */
    fun prepareProfile(user: FirebaseUser, provider: String, callback: (Boolean, String?) -> Unit) {
        val ref = db.collection("users").document(user.uid)
        db.runTransaction { transaction ->
            val doc = transaction.get(ref)
            // Reúne los campos que deben inicializarse o repararse.
            val missing = mutableMapOf<String, Any>()
            // Las cuentas nuevas comienzan como clientes con membresía Bronce.
            if (!doc.exists()) {
                missing["uid"] = user.uid
                missing["email"] = user.email.orEmpty()
                missing["provider"] = provider
                missing["role"] = "customer"
                missing["createdAt"] = FieldValue.serverTimestamp()
                missing["membershipLevel"] = "BRONCE"
                missing["loyaltyPoints"] = 0L
            } else {
                if (!doc.contains("role")) missing["role"] = "customer"
                if (!doc.contains("email") && user.email != null) missing["email"] = user.email!!
                if (!doc.contains("provider")) missing["provider"] = provider

                // Nunca inicializar membresía en cuentas administrativas.
                if (doc.getString("role") == null || doc.getString("role") == "customer") {
                    // Admite puntos numéricos y registros antiguos almacenados como texto.
                    val pointsRaw = doc.get("loyaltyPoints")
                    val points = when (pointsRaw) {
                        is Number -> pointsRaw.toLong()
                        is String -> pointsRaw.trim().toLongOrNull()
                        else -> null
                    }?.takeIf { it >= 0L }
                    val storedTier = doc.getString("membershipLevel")
                        ?.trim()?.uppercase(java.util.Locale.ROOT)
                    val validTier = storedTier in setOf("BRONCE", "PLATA", "ORO")
                    // Completa la membresía según los datos disponibles, sin reiniciar saldos válidos.
                    when {
                        !doc.contains("loyaltyPoints") && !validTier -> {
                            missing["loyaltyPoints"] = 0L
                            missing["membershipLevel"] = "BRONCE"
                        }
                        !doc.contains("loyaltyPoints") && storedTier == "BRONCE" -> {
                            missing["loyaltyPoints"] = 0L
                        }
                        points != null && !validTier -> {
                            missing["membershipLevel"] = when {
                                points >= 500L -> "ORO"
                                points >= 200L -> "PLATA"
                                else -> "BRONCE"
                            }
                        }
                        // Si falta el saldo en una cuenta Plata/Oro, NO inventar 0 puntos.
                    }
                }
            }
            // Merge conserva los demás campos del documento.
            if (missing.isNotEmpty()) transaction.set(ref, missing, SetOptions.merge())
            // Devuelve si el perfil ya contiene los datos personales requeridos.
            doc.getString("firstName").orEmpty().isNotBlank() &&
                    doc.getString("lastName").orEmpty().isNotBlank() &&
                    isValidBirthDate(doc.getString("birthDate").orEmpty())
        }.addOnSuccessListener { complete -> callback(complete, null) }
            .addOnFailureListener { e ->
                callback(false, e.localizedMessage ?: "No se pudo preparar el perfil")
            }
    }

    /** Valida y guarda los datos necesarios para completar el perfil. */
    fun savePersonalInfo(
        user: FirebaseUser, firstName: String, lastName: String, birthDate: String,
        callback: (Boolean, String?) -> Unit
    ) {
        if (firstName.isBlank() || lastName.isBlank() || !isValidBirthDate(birthDate)) {
            callback(false, "Revisá nombres, apellidos y fecha de nacimiento")
            return
        }
        val update = mapOf(
            "firstName" to firstName.trim(), "lastName" to lastName.trim(),
            "birthDate" to birthDate
        )
        // No escribir role, email ni createdAt. No reemplazar otros datos existentes.
        db.collection("users").document(user.uid)
            .set(update, SetOptions.merge())
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage ?: "No se pudo guardar el perfil") }
    }

    /** Actualiza exclusivamente nombres y apellidos; no escribe fecha de nacimiento ni rol. */
    fun updateNames(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        callback: (Boolean, String?) -> Unit
    ) {
        if (firstName.isBlank() || lastName.isBlank()) {
            callback(false, "Ingresá nombres y apellidos válidos.")
            return
        }
        db.collection("users").document(user.uid)
            .update(mapOf("firstName" to firstName.trim(), "lastName" to lastName.trim()))
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage ?: "No se pudieron guardar los nombres.") }
    }

    /** Guarda nombres y dirección habitual sin alterar nacimiento, correo, rol o compras. */
    fun updateNamesAndAddress(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        homeAddress: String,
        callback: (Boolean, String?) -> Unit
    ) {
        // Normaliza los espacios antes de validar y guardar.
        val first = firstName.trim()
        val last = lastName.trim()
        val address = homeAddress.trim()
        if (first.isBlank() || last.isBlank()) {
            callback(false, "Ingresá nombres y apellidos válidos.")
            return
        }
        if (address.length > 300) {
            callback(false, "La dirección debe tener como máximo 300 caracteres.")
            return
        }
        db.collection("users").document(user.uid)
            .update(mapOf("firstName" to first, "lastName" to last, "homeAddress" to address))
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage ?: "No se pudo actualizar el perfil.") }
    }

    /** Comprueba el formato ISO, la validez de la fecha y que no sea futura. */
    fun isValidBirthDate(value: String): Boolean = try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        formatter.isLenient = false
        val date = formatter.parse(value)
        date != null && !date.after(java.util.Date()) && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    } catch (_: Exception) { false }
}