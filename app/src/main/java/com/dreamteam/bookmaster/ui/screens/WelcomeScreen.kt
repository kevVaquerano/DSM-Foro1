package com.dreamteam.bookmaster.ui.screens

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dreamteam.bookmaster.R

/**
 * Pantalla de bienvenida de BookMaster.
 *
 * Contiene el fondo principal, el logo animado,
 * la frase decorativa, el gatito interactivo
 * y el botón para continuar al inicio de sesión.
 */
@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    // Obtiene las dimensiones para adaptar el diseño a la pantalla.
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val context = LocalContext.current

    // Crea el reproductor para los dos efectos de sonido.
    val reproductorGato = remember(context) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )
            .build()
    }

    // Identificadores de los archivos de audio cargados.
    var idMaullido by remember { mutableIntStateOf(0) }
    var idRonroneo by remember { mutableIntStateOf(0) }

    // Indican si los sonidos están listos para reproducirse.
    var maullidoListo by remember { mutableStateOf(false) }
    var ronroneoListo by remember { mutableStateOf(false) }

    // Guarda el canal activo del ronroneo.
    var canalRonroneo by remember { mutableIntStateOf(0) }

    /**
     * Prepara los efectos de sonido al abrir la pantalla.
     *
     * SoundPool carga los audios y permite reproducirlos
     * sin iniciar un reproductor nuevo en cada pulsación.
     */
    DisposableEffect(reproductorGato) {
        reproductorGato.setOnLoadCompleteListener {
                _, sampleId, status ->

            if (sampleId == idMaullido) {
                maullidoListo = status == 0
            }

            if (sampleId == idRonroneo) {
                ronroneoListo = status == 0
            }
        }

        idMaullido = reproductorGato.load(
            context,
            R.raw.maullido,
            1
        )

        idRonroneo = reproductorGato.load(
            context,
            R.raw.ronroneo,
            1
        )

        // Detiene cualquier sonido y libera los recursos.
        onDispose {
            if (canalRonroneo != 0) {
                reproductorGato.stop(canalRonroneo)
            }

            reproductorGato.release()
        }
    }

    /**
     * Transición compartida por las animaciones repetitivas.
     *
     * Controla la flotación del gato, el pulso del botón
     * y el desplazamiento del brillo del logo.
     */
    val infiniteTransition = rememberInfiniteTransition(
        label = "AnimacionesWelcome"
    )

    // Mueve suavemente el gatito de arriba hacia abajo.
    val movimientoGato by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MovimientoGatito"
    )

    // Aumenta y disminuye ligeramente el tamaño del botón.
    val escalaBoton by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsoBoton"
    )

    /**
     * Desplaza el reflejo de luz sobre el logo.
     *
     * La animación incluye una pausa inicial,
     * el recorrido del brillo y una pausa final.
     */
    val posicionBrillo by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4500

                -0.6f at 0
                -0.6f at 1300
                1.6f at 2600 using LinearEasing
                1.6f at 4500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BrilloLogo"
    )

    // Controla la entrada lateral del logo BookMaster.
    val desplazamientoLogo = remember {
        Animatable(-1f)
    }

    LaunchedEffect(Unit) {
        desplazamientoLogo.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * Animación de caída de la frase y sus comillas.
     *
     * Las imágenes comienzan fuera de la pantalla
     * y llegan a su posición con un pequeño rebote.
     */
    val alturaCaida = screenHeight.value

    val caidaUltimaComilla = remember(alturaCaida) {
        Animatable(-alturaCaida)
    }

    val caidaFrase = remember(alturaCaida) {
        Animatable(-alturaCaida)
    }

    val caidaPrimeraComilla = remember(alturaCaida) {
        Animatable(-alturaCaida)
    }

    // Configuración compartida para el efecto de rebote.
    val efectoRebote = remember {
        spring<Float>(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    /**
     * Ejecuta la caída de manera consecutiva:
     *
     * 1. Última comilla.
     * 2. Frase de bienvenida.
     * 3. Primera comilla.
     */
    LaunchedEffect(alturaCaida) {
        caidaUltimaComilla.animateTo(
            targetValue = 0f,
            animationSpec = efectoRebote
        )

        caidaFrase.animateTo(
            targetValue = 0f,
            animationSpec = efectoRebote
        )

        caidaPrimeraComilla.animateTo(
            targetValue = 0f,
            animationSpec = efectoRebote
        )
    }

    // Contenedor que permite superponer los elementos decorativos.
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Imagen de fondo que ocupa toda la pantalla.
        Image(
            painter = painterResource(
                R.drawable.fondo_welcome
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Primera comilla: aparece en tercer lugar.
        Image(
            painter = painterResource(
                R.drawable.primera_comilla
            ),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(screenWidth * 0.36f),
                    y = -(screenHeight * 0.38f) +
                            caidaPrimeraComilla.value.dp
                )
                .width(screenWidth * 0.18f),
            contentScale = ContentScale.Fit
        )

        // Frase principal: cae después de la última comilla.
        Image(
            painter = painterResource(
                R.drawable.frase_fondo
            ),
            contentDescription =
                "Donde cada libro deja una enseñanza",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(screenWidth * 0.02f),
                    y = -(screenHeight * 0.23f) +
                            caidaFrase.value.dp
                )
                .width(screenWidth * 0.85f),
            contentScale = ContentScale.Fit
        )

        // Última comilla: es el primer elemento que cae.
        Image(
            painter = painterResource(
                R.drawable.ultima_comilla
            ),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = screenWidth * 0.35f,
                    y = -(screenHeight * 0.10f) +
                            caidaUltimaComilla.value.dp
                )
                .width(screenWidth * 0.18f),
            contentScale = ContentScale.Fit
        )

        /**
         * Logo principal con entrada lateral y reflejo.
         *
         * graphicsLayer controla su desplazamiento,
         * mientras drawWithContent dibuja el brillo
         * sobre la imagen original.
         */
        Image(
            painter = painterResource(
                R.drawable.logo_bookmaster
            ),
            contentDescription = "BookMaster",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(screenWidth * 0.09f),
                    y = screenHeight * 0.12f
                )
                .width(screenWidth * 0.73f)
                .graphicsLayer {
                    translationX =
                        desplazamientoLogo.value * size.width

                    compositingStrategy =
                        CompositingStrategy.Offscreen
                }
                .zIndex(2f)
                .drawWithContent {

                    // Dibuja primero el contenido original del logo.
                    drawContent()

                    val centro = size.width * posicionBrillo
                    val ancho = size.width * 0.25f

                    // Crea un degradado que simula un reflejo de luz.
                    val reflejo = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        start = Offset(
                            x = centro - ancho,
                            y = 0f
                        ),
                        end = Offset(
                            x = centro + ancho,
                            y = size.height
                        )
                    )

                    // Aplica el reflejo sobre la imagen del logo.
                    drawRect(
                        brush = reflejo,
                        blendMode = BlendMode.SrcAtop
                    )
                },
            contentScale = ContentScale.Fit
        )

        /**
         * Gatito interactivo con movimiento de flotación.
         *
         * Una pulsación corta reproduce el maullido.
         * Una pulsación larga reproduce el ronroneo
         * hasta que el usuario suelta la pantalla.
         */
        Image(
            painter = painterResource(
                R.drawable.gato_bookmaster
            ),
            contentDescription = "Gatito BookMaster",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = screenWidth * 0.20f,
                    y = (screenHeight * 0.26f) +
                            movimientoGato.dp
                )
                .width(screenWidth * 0.55f)
                .zIndex(1f)
                .pointerInput(
                    reproductorGato,
                    maullidoListo,
                    ronroneoListo
                ) {
                    detectTapGestures(

                        // Reproduce el maullido con un toque corto.
                        onTap = {
                            if (maullidoListo) {
                                reproductorGato.play(
                                    idMaullido,
                                    1f, 1f,
                                    1, 0, 1f
                                )
                            }
                        },

                        // Inicia el ronroneo al mantener presionado.
                        onLongPress = {
                            if (ronroneoListo) {

                                if (canalRonroneo != 0) {
                                    reproductorGato.stop(canalRonroneo)
                                }

                                canalRonroneo = reproductorGato.play(
                                    idRonroneo,
                                    1f, 1f,
                                    1, -1, 1f
                                )
                            }
                        },

                        // Detiene el sonido al finalizar la pulsación.
                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {
                                if (canalRonroneo != 0) {
                                    reproductorGato.stop(canalRonroneo)
                                    canalRonroneo = 0
                                }
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )

        /**
         * Botón principal con efecto de pulso.
         *
         * Su imagen aumenta ligeramente de tamaño
         * y al tocarla abre la pantalla de inicio de sesión.
         */
        Image(
            painter = painterResource(
                R.drawable.boton_comenzar
            ),
            contentDescription = "Comienza ahora",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = -(screenWidth * 0.10f),
                    y = screenHeight * 0.40f
                )
                .width(screenWidth * 0.59f)
                .graphicsLayer {
                    scaleX = escalaBoton
                    scaleY = escalaBoton
                }
                .zIndex(3f)
                .clickable {
                    onLogin()
                },
            contentScale = ContentScale.Fit
        )
    }
}