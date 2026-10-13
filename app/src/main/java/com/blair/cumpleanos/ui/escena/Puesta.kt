package com.blair.cumpleanos.ui.escena

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Alto total del lienzo de Velita con reflejo, en unidades. */
private const val ALTO_CON_REFLEJO_U = 218f

/**
 * Velita parada en el piso del Escenario.
 *
 * Calcula la posicion contra PISO_ESCENARIO para que los pies caigan justo en
 * la elipse iluminada: splash, vacio y festejo la ponen en el MISMO lugar, asi
 * pasar de uno al otro no la mueve.
 *
 * @param escala 0..1+, para la entrada elastica. Pivota en los pies.
 */
@Composable
fun VelitaEnEscenario(
    modifier: Modifier = Modifier,
    expresion: Expresion = Expresion.CONTENTA,
    llamaViva: Boolean = true,
    escala: () -> Float = { 1f },
    encendidoLlama: () -> Float = { 1f },
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val ancho = minOf(maxWidth * 0.44f, 210.dp)
        val pieY = maxHeight * PISO_ESCENARIO.y
        Box(
            Modifier
                .offset(x = (maxWidth - ancho) / 2, y = pieY - ancho * (PIE_VELITA_U / 100f))
                .width(ancho)
                .graphicsLayer {
                    val e = escala()
                    scaleX = e
                    scaleY = e
                    transformOrigin = TransformOrigin(0.5f, PIE_VELITA_U / ALTO_CON_REFLEJO_U)
                },
        ) {
            Velita(
                Modifier.width(ancho),
                expresion = expresion,
                reflejo = true,
                llamaViva = llamaViva,
                encendidoLlama = encendidoLlama,
            )
        }
    }
}
