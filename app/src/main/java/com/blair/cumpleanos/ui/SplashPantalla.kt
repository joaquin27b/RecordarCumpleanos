package com.blair.cumpleanos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.LluviaConfeti
import com.blair.cumpleanos.ui.escena.TituloCromado
import com.blair.cumpleanos.ui.escena.VelitaEnEscenario
import com.blair.cumpleanos.ui.theme.NocheBase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lo que dura el show de apertura antes de dar paso a la lista. */
private const val DURACION_MS = 2_600L

/**
 * Apertura del show, sobre el Escenario de la app en intensidad COMPLETA.
 *
 * No dibuja fondo propio: el Escenario vive detras de todo y es el mismo que
 * despues baja a ATENUADA, por eso no hay corte de color. Lo unico opaco es un
 * velo de la noche del splash del sistema que se levanta en los primeros 400 ms.
 *
 * @param encendido los haces, que el Escenario lee: este composable los anima.
 */
@Composable
fun SplashPantalla(encendido: Animatable<Float, *>, onFin: () -> Unit) {
    val velo = remember { Animatable(1f) }
    val velita = remember { Animatable(0f) }
    val llama = remember { Animatable(0f) }
    val confeti = remember { Animatable(0f) }
    val titulo = remember { Animatable(0f) }
    val brillo = remember { Animatable(-1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { velo.animateTo(0f, tween(400)) }
            launch { encendido.animateTo(1f, tween(1_100, easing = LinearEasing)) }
            launch {
                delay(250)
                velita.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
            }
            launch {
                delay(850)
                llama.animateTo(
                    1f,
                    keyframes {
                        durationMillis = 420
                        1.35f at 170
                        0.9f at 290
                    },
                )
            }
            launch {
                delay(850)
                confeti.animateTo(1f, tween(1_750, easing = LinearEasing))
            }
            launch {
                delay(1_050)
                titulo.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            }
            launch {
                delay(1_400)
                brillo.snapTo(0f)
                brillo.animateTo(1f, tween(750, easing = LinearEasing))
                brillo.snapTo(-1f)
            }
            launch { delay(DURACION_MS); onFin() }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        VelitaEnEscenario(
            escala = { velita.value },
            encendidoLlama = { llama.value },
        )
        TituloCromado(
            texto = stringResource(R.string.marca),
            tamano = if (maxWidth < 360.dp) 38.sp else 46.sp,
            brillo = { brillo.value },
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * 0.12f)
                .graphicsLayer {
                    val t = titulo.value
                    alpha = t
                    scaleX = 0.88f + 0.12f * t
                    scaleY = 0.88f + 0.12f * t
                },
        )
        LluviaConfeti(progreso = { confeti.value })
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = velo.value }
                .background(NocheBase),
        )
    }
}
