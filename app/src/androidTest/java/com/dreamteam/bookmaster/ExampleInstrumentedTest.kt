package com.dreamteam.bookmaster

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada que se ejecuta en un dispositivo Android o emulador.
 *
 * Consulta la [documentación de pruebas](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Obtiene el contexto de la aplicación sometida a prueba.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Comprueba que el nombre del paquete corresponde a BookMaster.
        assertEquals("com.dreamteam.bookmaster", appContext.packageName)
    }
}