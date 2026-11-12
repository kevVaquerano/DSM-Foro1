#  Estudiantes: 
Kevin Arturo Vaquerano Morales  VM223215
Hayzel Evelyn González Gutiérrez GG240801



# 📚 BookMaster 

BookMaster es una aplicación móvil de comercio electrónico (e-commerce) para la venta de libros, desarrollada nativamente para Android utilizando **Kotlin**, **Jetpack Compose** y **Firebase**. 

La aplicación permite a los usuarios explorar un catálogo de libros, agregar productos a su carrito, realizar compras y mantener un historial de sus pedidos guardado de forma segura en la nube.

## ✨ Características Principales

* **Autenticación de Usuarios:** Sistema de inicio de sesión y registro de cuentas (Integrado con Firebase Auth).
* **Catálogo de Libros:** Visualización de libros disponibles con detalles como título, autor, categoría, precio, calificación y stock disponible.
* **Carrito de Compras Dinámico:** 
  * Agregar y eliminar libros.
  * Modificar cantidades en tiempo real.
  * Validación de inventario (no permite agregar más libros del stock disponible).
* **Procesamiento de Compras (Checkout):** Descuento automático del stock local tras una compra exitosa y almacenamiento de la orden.
* **Historial de Compras en la Nube:** Sincronización en tiempo real con Firestore para mostrar los pedidos anteriores del usuario autenticado.

## 🛠️ Tecnologías y Arquitectura

* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Navegación:** Navigation Compose (`NavHost`, `NavController`)
* **Base de Datos:** [Firebase Cloud Firestore](https://firebase.google.com/products/firestore)
* **Autenticación:** [Firebase Authentication](https://firebase.google.com/products/auth)
* **Asincronía & Estado:** Coroutines, StateFlow y `remember/mutableStateOf`.



## 🚀 Instalación y Configuración local

Para ejecutar este proyecto en tu entorno local, sigue estos pasos:

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/TU_USUARIO/BookMaster.git](https://github.com/TU_USUARIO/BookMaster.git)




Abrir en Android Studio:
Abre Android Studio, selecciona File > Open y busca la carpeta del proyecto clonado.

Configurar Firebase:

Ve a la Consola de Firebase y crea un nuevo proyecto.

Registra una aplicación Android con el nombre del paquete: com.dreamteam.bookmaster.

Descarga el archivo google-services.json y colócalo en la carpeta app/ de tu proyecto.

Habilita Authentication (Email/Password) y Firestore Database en la consola de Firebase.




Reglas de Seguridad (Firestore):
Asegúrate de configurar las reglas básicas en tu base de datos para permitir la lectura y escritura del carrito y el historial:

 rules_version = '2';
 service cloud.firestore {
   match /databases/{database}/documents {
     match /users/{userId}/{document=**} {
       allow read, write: if request.auth != null && request.auth.uid == userId;
     }
   }
 }


Compilar y Ejecutar:
Selecciona tu emulador o dispositivo físico y presiona el botón de Run en ANDROID STUDIO.

Contribución
Este es un proyecto académico/personal de Dream Team.
