package com.dreamteam.bookmaster.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Mantiene los perfiles antiguos sin sustituir documentos ni subcolecciones. */
object UserProfileRepository {
    private val db get() = Firebase.firestore

    /** Una cuenta sin datos completos debe pasar a CompleteProfileScreen. */
    fun prepareProfile(user: FirebaseUser, provider: String, callback: (Boolean, String?) -> Unit) {
        val ref = db.collection("users").document(user.uid)
        ref.get(Source.SERVER)
            .addOnSuccessListener { doc ->
                val missing = mutableMapOf<String, Any>()
                if (!doc.exists()) {
                    missing["uid"] = user.uid
                    missing["email"] = user.email.orEmpty()
                    missing["provider"] = provider
                    missing["role"] = "customer"
                    missing["createdAt"] = FieldValue.serverTimestamp()
                } else {
                    // Nunca reescribir role, createdAt, name, firstName ni lastName existentes.
                    if (!doc.contains("role")) missing["role"] = "customer"
                    if (!doc.contains("email") && user.email != null) missing["email"] = user.email!!
                    if (!doc.contains("provider")) missing["provider"] = provider
                }
                fun ready() {
                    val first = doc.getString("firstName").orEmpty().trim()
                    val last = doc.getString("lastName").orEmpty().trim()
                    val birth = doc.getString("birthDate").orEmpty().trim()
                    callback(first.isNotBlank() && last.isNotBlank() && isValidBirthDate(birth), null)
                }
                if (missing.isEmpty()) ready()
                else ref.set(missing, SetOptions.merge())
                    .addOnSuccessListener { ready() }
                    .addOnFailureListener { callback(false, it.localizedMessage ?: "No se pudo preparar el perfil") }
            }
            .addOnFailureListener { callback(false, it.localizedMessage ?: "No se pudo consultar el perfil") }
    }

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

    fun isValidBirthDate(value: String): Boolean = try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        formatter.isLenient = false
        val date = formatter.parse(value)
        date != null && !date.after(java.util.Date()) && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    } catch (_: Exception) { false }
}