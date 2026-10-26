// Archivo principal de configuración Gradle de BookMaster.
// Declara los plugins que estarán disponibles para los módulos del proyecto.

plugins {

    // Plugin necesario para compilar aplicaciones Android.
    alias(libs.plugins.android.application) apply false

    // Habilita el uso de Kotlin y Jetpack Compose.
    alias(libs.plugins.kotlin.compose) apply false

    // Declara el plugin de Firebase y Google Services.
    // Se aplica individualmente en el módulo que lo necesita.
    id("com.google.gms.google-services") version "4.4.2" apply false
}