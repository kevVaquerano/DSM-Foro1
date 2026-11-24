package com.dreamteam.bookmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.dreamteam.bookmaster.ui.navigation.NavGraph

/** Actividad principal que inicia la interfaz de BookMaster. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Define el contenido de la actividad mediante Jetpack Compose.
        setContent {
            // Aplica el tema de Material 3 al contenido.
            MaterialTheme {
                // Inicia el grafo de navegación de la aplicación.
                NavGraph()
            }
        }
    }
}