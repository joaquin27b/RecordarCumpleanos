package com.blair.cumpleanos.ui.escena

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.Cian3
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.HazFucsia
import com.blair.cumpleanos.ui.theme.RojoCta
import kotlin.math.sin
import kotlin.random.Random

private class Cayendo(
    val x: Float,
    val y0: Float,
    val caida: Float,
    val vaiven: Float,
    val fase: Float,
    val giro0: Float,
    val vueltas: Float,
    val ancho: Float,
    val alto: Float,
    val color: Color,
    val redondo: Boolean,
)

/**
 * Lluvia de confeti finita: una sola pasada de 0 a 1.
 *
 * Todo en un Canvas, posiciones calculadas de progreso: no hay estado por pieza
 * ni recomposicion por frame. Fuera de 0..1 no dibuja nada.
 */
@Composable
fun LluviaConfeti(
    progreso: () -> Float,
    modifier: Modifier = Modifier,
    cantidad: Int = 36,
    semilla: Int = 7,
) {
    val paleta = listOf(CianNeon, Dorado, HazFucsia, BlancoEscena, RojoCta, Cian3)
    Spacer(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val dp = density
                val azar = Random(semilla)
                fun r(a: Float, b: Float) = a + azar.nextFloat() * (b - a)
                val piezas = List(cantidad) {
                    val redondo = azar.nextInt(4) == 0
                    val ancho = if (redondo) r(2.5f, 4f) * dp else r(6f, 12f) * dp
                    Cayendo(
                        x = r(0f, w),
                        y0 = -r(0.02f, 0.55f) * h,
                        caida = r(0.9f, 1.4f) * h,
                        vaiven = r(8f, 22f) * dp,
                        fase = r(0f, 6.28f),
                        giro0 = r(0f, 360f),
                        vueltas = r(-540f, 540f),
                        ancho = ancho,
                        alto = if (redondo) ancho else r(3f, 5.5f) * dp,
                        color = paleta[azar.nextInt(paleta.size)],
                        redondo = redondo,
                    )
                }
                onDrawBehind {
                    val p = progreso()
                    if (p <= 0f || p >= 1f) return@onDrawBehind
                    // Se apaga en el ultimo 20%: no desaparece de golpe.
                    val alfa = if (p > 0.8f) (1f - p) / 0.2f else 1f
                    piezas.forEach { c ->
                        val y = c.y0 + c.caida * p
                        val x = c.x + sin(c.fase + p * 9f) * c.vaiven
                        translate(x, y) {
                            rotate(c.giro0 + c.vueltas * p, Offset.Zero) {
                                if (c.redondo) {
                                    drawCircle(c.color, c.ancho, Offset.Zero, alpha = alfa)
                                } else {
                                    drawRect(c.color, Offset(-c.ancho / 2f, -c.alto / 2f), Size(c.ancho, c.alto), alpha = alfa)
                                }
                            }
                        }
                    }
                }
            },
    )
}
