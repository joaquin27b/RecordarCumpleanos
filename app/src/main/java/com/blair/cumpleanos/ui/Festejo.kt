package com.blair.cumpleanos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.Expresion
import com.blair.cumpleanos.ui.escena.LluviaConfeti
import com.blair.cumpleanos.ui.escena.TituloCromado
import com.blair.cumpleanos.ui.escena.VelitaEnEscenario
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Confirmacion de saludo: rafaga de confeti, Velita un segundo con cara de
 * festejo y "¡Saludado!" cromado. Mientras dura, el Escenario sube a COMPLETA
 * (onActivo) y vuelve solo a ATENUADA.
 *
 * @param disparo cada valor nuevo distinto de 0 dispara un festejo.
 */
@Composable
fun Festejo(disparo: Int, onActivo: (Boolean) -> Unit) {
    val activo by rememberUpdatedState(onActivo)
    val velita = remember { Animatable(0f) }
    val confeti = remember { Animatable(0f) }
    val texto = remember { Animatable(0f) }

    LaunchedEffect(disparo) {
        if (disparo == 0) return@LaunchedEffect
        activo(true)
        velita.snapTo(0f)
        confeti.snapTo(0f)
        texto.snapTo(0f)
        coroutineScope {
            launch { confeti.animateTo(1f, tween(1_900, easing = LinearEasing)) }
            launch { texto.animateTo(1f, tween(300)) }
            launch {
                velita.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
                delay(900)
                texto.animateTo(0f, tween(300))
                velita.animateTo(0f, tween(260))
            }
        }
        activo(false)
    }

    if (disparo == 0) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        VelitaEnEscenario(
            expresion = Expresion.FESTEJO,
            llamaViva = false,
            escala = { velita.value },
        )
        TituloCromado(
            texto = stringResource(R.string.festejo_saludado),
            tamano = 44.sp,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * 0.16f)
                .graphicsLayer {
                    alpha = texto.value
                    scaleX = 0.8f + 0.2f * texto.value
                    scaleY = 0.8f + 0.2f * texto.value
                },
        )
        LluviaConfeti(progreso = { confeti.value }, cantidad = 44, semilla = disparo)
    }
}
