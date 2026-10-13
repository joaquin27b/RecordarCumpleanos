package com.blair.cumpleanos.ui.escena

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Estrella de 4 puntas: rombo alargado con los lados curvados hacia adentro.
 *
 * Centrada en el origen, alto 2*r y ancho 2*r*0.72: alargada en vertical como
 * los destellos del poster. Se arma una vez y se reusa trasladandola.
 */
fun trazoDestello(r: Float): Path {
    val a = r * 0.72f
    val k = r * 0.13f
    return Path().apply {
        moveTo(0f, -r)
        quadraticTo(k, -k, a, 0f)
        quadraticTo(k, k, 0f, r)
        quadraticTo(-k, k, -a, 0f)
        quadraticTo(-k, -k, 0f, -r)
        close()
    }
}

/** Destello con halo opcional. El halo es un radial: brillo sin blur, valido en API 26. */
fun DrawScope.destello(trazo: Path, centro: Offset, r: Float, color: Color, alfa: Float, halo: Boolean) {
    if (alfa <= 0f) return
    if (halo) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.38f), Color.Transparent),
                center = centro,
                radius = r * 1.7f,
            ),
            radius = r * 1.7f,
            center = centro,
            alpha = alfa,
        )
    }
    translate(centro.x, centro.y) { drawPath(trazo, color, alpha = alfa) }
}
