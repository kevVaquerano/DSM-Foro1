package com.dreamteam.bookmaster.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.dreamteam.bookmaster.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.Calendar
import java.util.UUID

/** Datos de tarjeta simulada para la interfaz. No contiene número completo ni CVV. */
data class DemoPaymentCard(
    val id: String,
    val brand: String,
    val lastFour: String,
    val holder: String = "CLIENTE DE PRUEBA",
    val expiry: String = ""
)

/**
 * Solo metadatos ficticios para representar tarjetas.
 * Ruta: users/{uid}/paymentMethods/{cardId}. Jamás guarda PAN completo ni CVV.
 */
object DemoPaymentStore {
    private var ownerUid: String? = null
    private var listener: ListenerRegistration? = null
    private val db get() = Firebase.firestore
    val cards = mutableStateListOf<DemoPaymentCard>()
    var defaultId by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private fun collection(uid: String) = db.collection("users").document(uid).collection("paymentMethods")
    private fun currentUid(): String? = Firebase.auth.currentUser?.uid?.takeIf { it == ownerUid }

    fun forUser(uid: String?) {
        if (ownerUid == uid && (listener != null || uid == null)) return
        listener?.remove()
        listener = null
        ownerUid = uid
        cards.clear()
        defaultId = null
        error = null
        loading = uid != null
        if (uid == null) return
        listener = collection(uid).addSnapshotListener { snapshots, exception ->
            if (ownerUid != uid || Firebase.auth.currentUser?.uid != uid) return@addSnapshotListener
            loading = false
            if (exception != null) {
                error = "No se pudieron cargar las tarjetas: ${exception.localizedMessage}"
                return@addSnapshotListener
            }
            error = null
            val documents = snapshots?.documents.orEmpty()
            val loaded = documents.mapNotNull { doc ->
                val brand = doc.getString("brand") ?: return@mapNotNull null
                val lastFour = doc.getString("lastFour") ?: return@mapNotNull null
                if (brand !in listOf("Visa", "Mastercard") || !lastFour.matches(Regex("\\d{4}"))) return@mapNotNull null
                DemoPaymentCard(doc.id, brand, lastFour, doc.getString("holder").orEmpty(), doc.getString("expiry").orEmpty())
            }
            cards.clear()
            cards.addAll(loaded.sortedBy { it.brand + it.lastFour })
            defaultId = documents.firstOrNull { it.getBoolean("isDefault") == true && loaded.any { c -> c.id == it.id } }?.id
                ?: loaded.firstOrNull()?.id
        }
    }

    private fun save(card: DemoPaymentCard, preferred: Boolean, callback: (Boolean, String?) -> Unit) {
        val uid = currentUid() ?: run { callback(false, "Sesión no iniciada"); return }
        val ref = collection(uid).document(card.id)
        val makeDefault = preferred || defaultId == null
        val data = mapOf(
            "brand" to card.brand,
            "lastFour" to card.lastFour,
            "holder" to card.holder,
            "expiry" to card.expiry,
            "isDefault" to (makeDefault || defaultId == card.id)
        )
        val batch = db.batch()
        if (makeDefault && defaultId != null && defaultId != card.id) {
            batch.update(collection(uid).document(defaultId!!), "isDefault", false)
        }
        batch.set(ref, data, SetOptions.merge())
        batch.commit().addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage) }
    }

    fun add(card: DemoPaymentCard, preferred: Boolean, callback: (Boolean, String?) -> Unit) = save(card, preferred, callback)
    fun update(card: DemoPaymentCard, preferred: Boolean, callback: (Boolean, String?) -> Unit) = save(card, preferred, callback)

    fun select(id: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = currentUid() ?: run { callback(false, "Sesión no iniciada"); return }
        if (cards.none { it.id == id }) { callback(false, "Tarjeta no encontrada"); return }
        if (defaultId == id) { callback(true, null); return }
        val batch = db.batch()
        defaultId?.let { batch.update(collection(uid).document(it), "isDefault", false) }
        batch.update(collection(uid).document(id), "isDefault", true)
        batch.commit().addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage) }
    }

    fun remove(id: String, callback: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = currentUid() ?: run { callback(false, "Sesión no iniciada"); return }
        val batch = db.batch()
        batch.delete(collection(uid).document(id))
        if (defaultId == id) {
            cards.firstOrNull { it.id != id }?.let {
                batch.update(collection(uid).document(it.id), "isDefault", true)
            }
        }
        batch.commit().addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { callback(false, it.localizedMessage) }
    }
}

/** Reutilizado por ProfileScreen (manage=true) y CheckoutScreen (manage=false). */
@Composable
fun DemoPaymentMethods(
    manage: Boolean,
    modifier: Modifier = Modifier,
    checkoutSelectedId: String? = null,
    onCheckoutSelect: (String?) -> Unit = {}
) {
    val uid = Firebase.auth.currentUser?.uid
    LaunchedEffect(uid) { DemoPaymentStore.forUser(uid) }

    val dark = Color(0xFF473071)
    val purple = Color(0xFF7952B5)
    val soft = Color(0xFFF1EAF9)
    val muted = Color(0xFF766D80)
    val border = Color(0xFFD8C8E9)

    var addDialog by remember(uid) { mutableStateOf(false) }
    var editingId by remember(uid) { mutableStateOf<String?>(null) }
    var brand by remember(uid) { mutableStateOf("Visa") }
    var holder by remember(uid) { mutableStateOf("") }
    var digits by remember(uid) { mutableStateOf("") }
    var expiryDigits by remember(uid) { mutableStateOf("") }
    var cvv by remember(uid) { mutableStateOf("") }
    var preferred by remember(uid) { mutableStateOf(false) }
    var savingCard by remember(uid) { mutableStateOf(false) }
    var saveError by remember(uid) { mutableStateOf<String?>(null) }
    var actionError by remember(uid) { mutableStateOf<String?>(null) }

    fun resetForm() {
        brand = "Visa"
        holder = ""
        digits = ""
        expiryDigits = ""
        cvv = ""
        preferred = DemoPaymentStore.cards.isEmpty()
        saveError = null
    }

    fun closeForm() {
        resetForm()
        addDialog = false
        editingId = null
    }

    fun startEdit(card: DemoPaymentCard) {
        editingId = card.id
        brand = card.brand
        holder = card.holder
        digits = "" // Nunca almacenamos ni recuperamos el número completo.
        expiryDigits = card.expiry.filter(Char::isDigit).take(4)
        cvv = "" // El CVV nunca se guarda: no se puede precargar.
        preferred = DemoPaymentStore.defaultId == card.id
        addDialog = true
    }

    if (addDialog) {
        val validName = holder.trim().length in 3..60 &&
                Regex("""^[\p{L}\p{M}]+(?:[ '\-][\p{L}\p{M}]+)*$""").matches(holder.trim())
        // Formato solamente: valores INVENTADOS de prueba, sin verificar emisor ni banco.
        val editingCard = editingId?.let { id -> DemoPaymentStore.cards.find { it.id == id } }
        val replacingNumber = digits.isNotEmpty()
        val validNumber = if (editingCard != null && !replacingNumber) true else digits.length == 16
        val month = expiryDigits.take(2).toIntOrNull() ?: 0
        val yy = expiryDigits.drop(2).toIntOrNull() ?: -1
        val now = Calendar.getInstance()
        val currentYY = now.get(Calendar.YEAR) % 100
        val currentMonth = now.get(Calendar.MONTH) + 1
        val validExpiry = expiryDigits.length == 4 && month in 1..12 &&
                (yy > currentYY || (yy == currentYY && month >= currentMonth))
        // Si se edita sin cambiar el número, no se vuelve a solicitar el CVV.
        val validCvv = if (editingCard != null && !replacingNumber) true else cvv.length in 3..4
        val expiryFormatted = if (expiryDigits.length > 2)
            expiryDigits.take(2) + "/" + expiryDigits.drop(2) else expiryDigits

        AlertDialog(
            onDismissRequest = { if (!savingCard) closeForm() },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFFF0EAF5),
            title = {
                Text(if (editingCard == null) "Agregar tarjeta" else "Editar tarjeta", color = dark, fontSize = 20.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Nombre del titular", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = holder,
                        onValueChange = { if (it.length <= 60) holder = it },
                        placeholder = { Text("Nombre y apellido", fontSize = 13.sp) },
                        isError = holder.isNotBlank() && !validName,
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedBorderColor = purple)
                    )
                    Text("Tipo de tarjeta", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Visa", "Mastercard").forEach { option ->
                            Surface(
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, if (brand == option) purple else border)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = brand == option,
                                        onClick = { brand = option },
                                        colors = RadioButtonDefaults.colors(selectedColor = purple))
                                    Text(option, fontSize = 12.sp, color = dark)
                                }
                            }
                        }
                    }
                    Text("Número de tarjeta", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = digits.chunked(4).joinToString(" "),
                        onValueChange = { entered -> digits = entered.filter(Char::isDigit).take(16) },
                        placeholder = { Text(if (editingCard == null) "0000 0000 0000 0000"
                        else "Actual ···· ${editingCard.lastFour} (opcional)", fontSize = 13.sp) },
                        singleLine = true, isError = digits.isNotEmpty() && !validNumber,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedBorderColor = purple)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Vencimiento", color = dark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = expiryFormatted,
                                onValueChange = { expiryDigits = it.filter(Char::isDigit).take(4) },
                                placeholder = { Text("MM/AA", fontSize = 13.sp) },
                                singleLine = true, isError = expiryDigits.isNotEmpty() && !validExpiry,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                                    focusedBorderColor = purple)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Código de seguridad", color = dark, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it.filter(Char::isDigit).take(4) },
                                placeholder = { Text("CVV", fontSize = 13.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                                    focusedBorderColor = purple)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = preferred, onCheckedChange = { preferred = it },
                            colors = CheckboxDefaults.colors(checkedColor = purple))
                        Text("Establecer como predeterminada", color = dark, fontSize = 12.sp)
                    }
                    Text("Solo guardaremos marca, titular, últimos 4 dígitos y vencimiento de prueba en Firestore. El número completo y el CVV se descartan.",
                        fontSize = 11.sp, color = muted)
                    saveError?.let { Text(it, fontSize = 12.sp, color = Color(0xFFB13A4E)) }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { closeForm() }, enabled = !savingCard) {
                        Text("Cancelar", color = dark, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (!savingCard) {
                                val card = DemoPaymentCard(
                                    id = editingCard?.id ?: UUID.randomUUID().toString(),
                                    brand = brand,
                                    lastFour = if (replacingNumber) digits.takeLast(4)
                                    else editingCard?.lastFour.orEmpty(),
                                    holder = holder.trim(),
                                    expiry = expiryFormatted
                                )
                                savingCard = true
                                saveError = null
                                val finished: (Boolean, String?) -> Unit = { ok, message ->
                                    savingCard = false
                                    if (ok) closeForm()
                                    else saveError = message ?: "No se pudo guardar la tarjeta."
                                }
                                if (editingCard == null) DemoPaymentStore.add(card, preferred, finished)
                                else DemoPaymentStore.update(card, preferred, finished)
                                // PAN y CVV no se envían al repositorio.
                            }
                        },
                        enabled = uid != null && !savingCard &&
                                validName && validNumber &&
                                validExpiry && validCvv,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = purple
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (editingCard == null)
                                "Guardar tarjeta"
                            else
                                "Guardar cambios",
                            fontSize = 12.sp
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (DemoPaymentStore.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = purple)
        DemoPaymentStore.error?.let { Text(it, color = Color(0xFFB13A4E), fontSize = 12.sp) }
        actionError?.let { Text(it, color = Color(0xFFB13A4E), fontSize = 12.sp) }
        if (DemoPaymentStore.cards.isEmpty() && !DemoPaymentStore.loading) {
            Text("No hay tarjetas agregadas.", color = muted, fontSize = 13.sp)
        } else {
            DemoPaymentStore.cards.forEach { card ->
                val selected = if (manage) DemoPaymentStore.defaultId == card.id
                else checkoutSelectedId == card.id
                Surface(
                    shape = RoundedCornerShape(12.dp), color = soft,
                    border = BorderStroke(1.dp, if (selected) purple else border)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                if (manage) DemoPaymentStore.select(card.id) { ok, message -> if (!ok) actionError = message }
                                else onCheckoutSelect(card.id)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = purple),
                            modifier = Modifier.size(38.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${card.brand} ···· ${card.lastFour}", color = dark,
                                fontSize = if (manage) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(card.holder, color = muted, fontSize = if (manage) 11.sp else 13.sp, maxLines = 1)
                            if (card.expiry.isNotBlank())
                                Text("Vence ${card.expiry}", color = muted, fontSize = if (manage) 11.sp else 13.sp)
                            if (manage && DemoPaymentStore.defaultId == card.id)
                                Text("Predeterminada", fontSize = 11.sp, color = purple)
                        }
                        // Edición y eliminación disponibles también en Checkout.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { startEdit(card) }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_checkout_edit),
                                    contentDescription = "Editar tarjeta",
                                    tint = purple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = {
                                DemoPaymentStore.remove(card.id) { ok, message ->
                                    if (!ok) actionError = message
                                    else if (!manage && checkoutSelectedId == card.id) onCheckoutSelect(null)
                                }
                            }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete_profile),
                                    contentDescription = "Eliminar tarjeta",
                                    tint = Color(0xFF9B324A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                resetForm()
                editingId = null
                addDialog = true
            },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
            enabled = uid != null && !DemoPaymentStore.loading && DemoPaymentStore.cards.size < 10
        ) { Text("+ Agregar tarjeta", color = purple, fontSize = if (manage) 14.sp else 16.sp) }

    }
}