plugins {
    // Habilita la compilación de la aplicación Android.
    alias(libs.plugins.android.application)

    // Permite utilizar Kotlin con Jetpack Compose.
    alias(libs.plugins.kotlin.compose)

    // Conecta la aplicación con los servicios de Firebase.
    id("com.google.gms.google-services")
}

android {

    // Identificador del paquete principal de BookMaster.
    namespace = "com.dreamteam.bookmaster"

    // Versión del SDK utilizada para compilar la aplicación.
    compileSdk {
        version = release(37)
    }

    defaultConfig {

        // Identificador único de la aplicación.
        applicationId = "com.dreamteam.bookmaster"

        // Versión mínima de Android compatible.
        minSdk = 24

        // Versión de Android para la que está preparada la aplicación.
        targetSdk = 37

        // Información de la versión actual del proyecto.
        versionCode = 1
        versionName = "1.0"

        // Configuración para ejecutar pruebas instrumentadas.
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    // Configuración utilizada al generar la versión de distribución.
    buildTypes {
        release {
            optimization {
                enable = true

                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    // Establece Java 11 como versión de compatibilidad.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Habilita las herramientas de interfaz de Jetpack Compose.
    buildFeatures {
        compose = true
    }
}

/**
 * Dependencias utilizadas por BookMaster.
 *
 * Se agrupan según su función para facilitar
 * la lectura y el mantenimiento del proyecto.
 */
dependencies {

    // Mantiene compatibles las bibliotecas de Jetpack Compose.
    implementation(platform(libs.androidx.compose.bom))

    // Componentes principales para crear la interfaz.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Funciones básicas de Android y ciclo de vida.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Permite navegar entre las pantallas de BookMaster.
    implementation(libs.androidx.navigation.compose)

    // Bibliotecas para realizar pruebas del proyecto.
    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Herramientas de diseño y depuración de Compose.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    /**
     * Firebase
     *
     * El BOM permite administrar las versiones compatibles
     * de los diferentes servicios de Firebase.
     */
    implementation(
        platform("com.google.firebase:firebase-bom:33.4.0")
    )

    // Autenticación de usuarios por correo y Google.
    implementation("com.google.firebase:firebase-auth-ktx")

    // Almacena la información de los usuarios en Firestore.
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Permite almacenar archivos mediante Firebase Storage.
    implementation("com.google.firebase:firebase-storage-ktx")

    /**
     * Autenticación con Google
     *
     * Se mantienen las bibliotecas de Google Sign-In
     * y Credential Manager para conservar compatibilidad.
     */
    implementation(libs.play.services.auth)

    implementation("androidx.credentials:credentials:1.3.0")

    implementation(
        "androidx.credentials:credentials-play-services-auth:1.3.0"
    )

    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )

    // Permite cargar imágenes en interfaces Jetpack Compose.
    implementation("io.coil-kt:coil-compose:2.6.0")
}