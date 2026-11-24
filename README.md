<div align="center">
# 📚 BookMaster
 
### Tu librería, en la palma de tu mano
 
Aplicación móvil de comercio electrónico para la venta de libros,<br>
desarrollada de forma nativa para Android.
 
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
 
</div>
---
 
## 📖 Tabla de contenidos
 
- [Sobre el proyecto](#-sobre-el-proyecto)
- [Mockups en Figma](#-Mockups-de-Figma) 
- [Características principales](#-características-principales)
- [Tecnologías y arquitectura](#️-tecnologías-y-arquitectura)
- [Capturas de pantalla](#-capturas-de-pantalla)
- [Instalación y configuración local](#-instalación-y-configuración-local)
- [Reglas de seguridad de Firestore](#-reglas-de-seguridad-de-firestore)
- [Estructura del repositorio](#-estructura-del-repositorio)
- [Equipo](#-equipo)
- 
---
## 🎯 Sobre el proyecto
 
**BookMaster** permite a los usuarios explorar un catálogo de libros, agregar productos a su carrito, realizar compras y consultar un historial de pedidos guardado de forma segura en la nube.

---
## 📖 Mockups de Figma:
 
- [Mockups en Figma]( https://www.figma.com/design/duYSlwo0M7w8PNx7GE5Jir/Etapa-1-DSM-DreamTeam?node-id=0-1&t=coqkGPRpG2d0rKW9-1)
  
---
 
 
> 🎓 Proyecto académico desarrollado por **Dream Team**.
 
---
 
## ✨ Características principales
 
| | Función | Descripción |
|---|---|---|
| 🔐 | **Autenticación de usuarios** | Inicio de sesión y registro de cuentas con Firebase Authentication. |
| 📚 | **Catálogo de libros** | Muestra título, autor, categoría, precio, calificación y stock disponible de cada libro. |
| 🛒 | **Carrito dinámico** | Agrega y elimina libros, y modifica cantidades en tiempo real. |
| ✅ | **Validación de inventario** | No permite agregar más libros de los que hay en stock. |
| 💳 | **Checkout** | Descuenta automáticamente el stock local tras una compra exitosa y almacena la orden. |
| ☁️ | **Historial en la nube** | Sincroniza en tiempo real con Firestore y muestra los pedidos anteriores del usuario autenticado. |
 
---
 
## 🛠️ Tecnologías y arquitectura
 
| Categoría | Tecnología |
|---|---|
| **Lenguaje** | [Kotlin](https://kotlinlang.org/) |
| **Interfaz de usuario** | [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material Design 3 |
| **Navegación** | Navigation Compose (`NavHost`, `NavController`) |
| **Base de datos** | [Firebase Cloud Firestore](https://firebase.google.com/products/firestore) |
| **Autenticación** | [Firebase Authentication](https://firebase.google.com/products/auth) |
| **Asincronía y estado** | Coroutines, `StateFlow` y `remember` / `mutableStateOf` |
 
---
 
## 📸 Capturas de pantalla
 
<!-- Agregagaaaaaaaaaaaar -->
 
<div align="center">
| Inicio de sesión | Catálogo | Carrito | Historial |
|:---:|:---:|:---:|:---:|
| *próximamente* | *próximamente* | *próximamente* | *próximamente* |
 
</div>
---
 
## 🚀 Instalación y configuración local
 
### Requisitos previos
 
- [Android Studio](https://developer.android.com/studio) instalado
- Un emulador o un dispositivo Android físico
- Una cuenta de Google para usar la consola de Firebase
### Pasos
 
**1. Clonar el repositorio**
 
```bash
git clone https://github.com/kevVaquerano/DSM-Foro1.git
```
 
**2. Abrir el proyecto en Android Studio**
 
Selecciona **File > Open** y busca la carpeta del proyecto clonado.
 
**3. Configurar Firebase**
 
1. Entra a la [Consola de Firebase](https://console.firebase.google.com/) y crea un nuevo proyecto.
2. Registra una aplicación Android con el nombre de paquete: `com.dreamteam.bookmaster`.
3. Descarga el archivo `google-services.json` y colócalo dentro de la carpeta `app/`.
4. Habilita **Authentication** (método Email/Password) y **Firestore Database** en la consola.
> ⚠️ **Importante:** no subas tu propio `google-services.json` a un repositorio público.
 
**4. Configurar las reglas de Firestore**
 
Copia las reglas de la siguiente sección en la pestaña **Rules** de tu base de datos.
 
**5. Compilar y ejecutar**
 
Elige tu emulador o dispositivo y presiona el botón **Run** ▶️ en Android Studio.
 
---
 
## 🔒 Reglas de seguridad de Firestore
 
Esta es la regla básicas permiten que cada usuario autenticado lea y escriba únicamente su propio carrito e historial:
 
```
rules_version = '2';
 
service cloud.firestore {
  match /databases/{database}/documents {
 
    // Cada usuario solo accede a sus propios datos
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
 
---
 
## 📂 Estructura del repositorio
 
```
DSM-Foro1/
├── app/                              # Código fuente de la aplicación Android
├── gradle/                           # Configuración del wrapper de Gradle
├── Documentación Jetpack Compose/    # Material de apoyo sobre Jetpack Compose
├── build.gradle.kts                  # Configuración de compilación del proyecto
├── settings.gradle.kts               # Módulos del proyecto
├── gradle.properties                 # Propiedades de Gradle
└── README.md                         # Este archivo
```
 
---
 
## 👥 Equipo
 
<div align="center">
| Estudiante | Carnet |
|---|:---:|
| **Kevin Arturo Vaquerano Morales** | `VM223215` |
| **Hayzel Evelyn González Gutiérrez** | `GG240801` |
 
</div>
---
 
<div align="center">
Hecho con ❤️ y ☕ por **Dream Team**
 
</div>
