package com.blair.cumpleanos.ui.escena

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin

/** Puntas de cada estrella de destello. */
const val PUNTAS_DESTELLO = 9

/** Radio interno relativo: bajo, rayos finos y filosos como un destello de luz. */
private const val INTERNO = 0.40f

/**
 * Estrella de 9 puntas centrada en el origen, una punta hacia arriba.
 *
 * Se arma una vez y se reusa trasladandola.
 */
fun trazoDestello(r: Float): Path = Path().apply {
    val pasos = PUNTAS_DESTELLO * 2
    for (k in 0 until pasos) {
        val radio = if (k % 2 == 0) r else r * INTERNO
        val a = -Math.PI / 2 + k * Math.PI / PUNTAS_DESTELLO
        val x = (cos(a) * radio).toFloat()
        val y = (sin(a) * radio).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
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
