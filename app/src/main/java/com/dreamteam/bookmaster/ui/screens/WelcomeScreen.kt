
package com.dreamteam.bookmaster.ui.screens

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
import android.media.AudioAttributes
import android.media.SoundPool
import com.dreamteam.bookmaster.R

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // =====================================
    // SONIDOS INTERACTIVOS DEL GATITO
    // =====================================

    val context = LocalContext.current

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

    var idMaullido by remember {
        mutableIntStateOf(0)
    }

    var idRonroneo by remember {
        mutableIntStateOf(0)
    }

    var maullidoListo by remember {
        mutableStateOf(false)
    }

    var ronroneoListo by remember {
        mutableStateOf(false)
    }

    var canalRonroneo by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(reproductorGato) {

        reproductorGato.setOnLoadCompleteListener {
                _, sampleId, status ->

            if (sampleId == idMaullido) {
                maullidoListo = (status == 0)
            }

            if (sampleId == idRonroneo) {
                ronroneoListo = (status == 0)
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

        onDispose {
            if (canalRonroneo != 0) {
                reproductorGato.stop(canalRonroneo)
            }

            reproductorGato.release()
        }
    }

    // =====================================
    // ANIMACIÓN DE FLOTACIÓN DEL GATITO
    // =====================================

    val infiniteTransition = rememberInfiniteTransition(
        label = "AnimacionGatito"
    )

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
        label = "MovimientoVertical"
    )

    // =====================================
    // SLIDE IN DEL LOGO BOOKMASTER
    // =====================================

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

    // =====================================
    // PULSE DEL BOTÓN COMIENZA AHORA
    // =====================================

    val animacionBoton = rememberInfiniteTransition(
        label = "AnimacionBoton"
    )

    val escalaBoton by animacionBoton.animateFloat(
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

    // =====================================
    // ANIMACIÓN DE BRILLO DEL LOGO
    // =====================================

    val animacionBrillo = rememberInfiniteTransition(
        label = "BrilloLogo"
    )

    val posicionBrillo by animacionBrillo.animateFloat(
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
        label = "DesplazamientoBrillo"
    )

    // =====================================
    // NUEVO: CAÍDA ESTILO TETRIS
    // =====================================

    // Cada imagen empieza por encima
    // de su posición final.

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

    // Rebote suave al llegar
    val efectoRebote = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Orden:
    // 1. Última comilla
    // 2. Frase
    // 3. Primera comilla

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

    // =====================================
    // CONTENEDOR PRINCIPAL
    // =====================================

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Fondo de Canva
        Image(
            painter = painterResource(
                R.drawable.fondo_welcome
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // =====================================
        // PRIMERA COMILLA DECORATIVA
        // CAE EN TERCER LUGAR
        // =====================================

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

        // =====================================
        // FRASE DE BIENVENIDA
        // CAE EN SEGUNDO LUGAR
        // =====================================

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

        // =====================================
        // ÚLTIMA COMILLA DECORATIVA
        // CAE EN PRIMER LUGAR
        // =====================================

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

        // =====================================
        // LOGO BOOKMASTER CON SLIDE + BRILLO
        // =====================================

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

                    drawContent()

                    val centro = size.width * posicionBrillo
                    val ancho = size.width * 0.25f

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

                    drawRect(
                        brush = reflejo,
                        blendMode = BlendMode.SrcAtop
                    )
                },
            contentScale = ContentScale.Fit
        )

        // =====================================
        // GATITO INTERACTIVO CON SONIDOS
        // =====================================

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
                .pointerInput(reproductorGato) {
                    detectTapGestures(

                        // Un toque: maullido
                        onTap = {
                            if (maullidoListo) {
                                reproductorGato.play(
                                    idMaullido,
                                    1f,
                                    1f,
                                    1,
                                    0,
                                    1f
                                )
                            }
                        },

                        // Mantener presionado: ronroneo
                        onLongPress = {
                            if (ronroneoListo) {

                                if (canalRonroneo != 0) {
                                    reproductorGato.stop(
                                        canalRonroneo
                                    )
                                }

                                canalRonroneo =
                                    reproductorGato.play(
                                        idRonroneo,
                                        1f,
                                        1f,
                                        1,
                                        -1,
                                        1f
                                    )
                            }
                        },

                        // Soltar: detener ronroneo
                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {

                                if (canalRonroneo != 0) {
                                    reproductorGato.stop(
                                        canalRonroneo
                                    )

                                    canalRonroneo = 0
                                }
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )

        // =====================================
        // BOTÓN COMIENZA AHORA CON PULSE
        // =====================================

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
                .clickable { onLogin() },
            contentScale = ContentScale.Fit
        )
    }
}