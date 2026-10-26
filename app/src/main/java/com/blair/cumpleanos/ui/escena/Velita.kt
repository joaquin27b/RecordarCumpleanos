package com.blair.cumpleanos.ui.escena

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.Cian
import com.blair.cumpleanos.ui.theme.CianClaro
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.CianProfundo
import com.blair.cumpleanos.ui.theme.Coral
import com.blair.cumpleanos.ui.theme.Cromo2
import com.blair.cumpleanos.ui.theme.Cromo3
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.NochePiso
import com.blair.cumpleanos.ui.theme.NocheBase
import com.blair.cumpleanos.ui.theme.Resplandor2
import com.blair.cumpleanos.ui.theme.RojoCta

/**
 * Cara de la mascota. FESTEJO: ojos cerrados de felicidad, boca grande y brazos arriba.
 * SENALA: contenta, con el brazo derecho estirado hacia abajo, al boton de agregar.
 */
enum class Expresion { CONTENTA, FESTEJO, SENALA }

// Todo se dibuja en unidades de un lienzo de 100 x 160. El cuerpo va de
// x 18..82 e y 66..149: 64 x 83, relacion 1:1.3, rechoncho y no un cilindro.
private const val ANCHO_U = 100f
private const val ALTO_U = 160f
/** Alto extra para el reflejo en el piso. */
private const val REFLEJO_U = 58f
/** Linea del piso: donde apoya la sombra y desde donde se espeja el reflejo. */
const val PIE_VELITA_U = 151f

private val CUERPO = RoundRect(
    Rect(18f, 66f, 82f, 149f),
    topLeft = CornerRadius(30f), topRight = CornerRadius(30f),
    bottomRight = CornerRadius(22f), bottomLeft = CornerRadius(22f),
)

/** Piezas en unidades: no dependen del tamaño, se arman una sola vez por proceso. */
private object Piezas {
    val cuerpo = Path().apply { addRoundRect(CUERPO) }

    /** Cera derretida arriba: es lo que hace leer "vela" y no "caramelo". */
    val cera = Path().apply {
        moveTo(18f, 96f)
        lineTo(18f, 94f)
        // El borde superior lo da el clip del cuerpo; aca solo importa el de abajo.
        lineTo(18f, 60f)
        lineTo(82f, 60f)
        lineTo(82f, 82f)
        cubicTo(82f, 86f, 77f, 86f, 77f, 82f)
        cubicTo(77f, 79f, 72f, 78f, 71f, 82f)
        lineTo(71f, 90f)
        cubicTo(71f, 95f, 64f, 95f, 64f, 90f)
        lineTo(64f, 81f)
        cubicTo(63f, 77f, 55f, 77f, 54f, 81f)
        cubicTo(53f, 84f, 47f, 84f, 46f, 81f)
        cubicTo(45f, 78f, 38f, 78f, 37f, 82f)
        lineTo(37f, 92f)
        cubicTo(37f, 97f, 30f, 97f, 30f, 92f)
        lineTo(30f, 84f)
        cubicTo(29f, 79f, 22f, 79f, 21f, 84f)
        cubicTo(20f, 88f, 18f, 88f, 18f, 86f)
        close()
    }

    /** Franjas diagonales de vela de cumpleaños, recortadas al cuerpo. */
    val franjas = Path().apply {
        for (k in 0..3) {
            val y = 92f + k * 17f
            moveTo(10f, y)
            lineTo(90f, y - 22f)
            lineTo(90f, y - 15f)
            lineTo(10f, y + 7f)
            close()
        }
    }

    /** Llama: gota con la base apoyada sobre la cabeza. Sin pabilo: oscuro contra el violeta no se ve. */
    val llama = gota(50f, 18f, 66f, 13f)
    val nucleo = gota(50f, 38f, 64f, 6.5f)

    val boca = Path().apply {
        moveTo(41f, 114f)
        cubicTo(45f, 115.5f, 55f, 115.5f, 59f, 114f)
        cubicTo(59f, 121f, 55f, 126f, 50f, 126f)
        cubicTo(45f, 126f, 41f, 121f, 41f, 114f)
        close()
    }
    val bocaFestejo = Path().apply {
        moveTo(38f, 112f)
        cubicTo(44f, 114f, 56f, 114f, 62f, 112f)
        cubicTo(62f, 122f, 57f, 129f, 50f, 129f)
        cubicTo(43f, 129f, 38f, 122f, 38f, 112f)
        close()
    }

    fun gota(cx: Float, punta: Float, base: Float, r: Float): Path = Path().apply {
        val centroY = base - r
        moveTo(cx, punta)
        cubicTo(cx + r * 0.45f, punta + (centroY - punta) * 0.45f, cx + r, centroY - r * 0.9f, cx + r, centroY)
        cubicTo(cx + r, centroY + r * 0.6f, cx + r * 0.55f, base, cx, base)
        cubicTo(cx - r * 0.55f, base, cx - r, centroY + r * 0.6f, cx - r, centroY)
        cubicTo(cx - r, centroY - r * 0.9f, cx - r * 0.45f, punta + (centroY - punta) * 0.45f, cx, punta)
        close()
    }

    val destello = trazoDestello(1f)
}

private object Pinceles {
    val cuerpo = Brush.verticalGradient(listOf(CianClaro, Cian, CianProfundo), startY = 66f, endY = 149f)
    /** Luz principal: radial claro arriba a la izquierda. */
    val luz = Brush.radialGradient(
        listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(34f, 82f),
        radius = 46f,
    )
    /** Rim light: solo el borde inferior derecho, el reflejo de la luz del escenario. */
    val rim = Brush.linearGradient(
        0f to Color.Transparent,
        0.55f to Color.Transparent,
        1f to CianNeon,
        start = Offset(18f, 66f),
        end = Offset(82f, 149f),
    )
    val cera = Brush.verticalGradient(listOf(BlancoEscena, Cromo2, Cromo3), startY = 64f, endY = 96f)
    val glow = Brush.radialGradient(
        listOf(Cian.copy(alpha = 0.25f), Cian.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(50f, 108f),
        radius = 62f,
    )
    val sombra = Brush.radialGradient(
        listOf(NochePiso.copy(alpha = 0.35f), Color.Transparent),
        center = Offset(50f, PIE_VELITA_U),
        radius = 24f,
    )
    val llama = Brush.verticalGradient(listOf(RojoCta, Dorado, Dorado), startY = 18f, endY = 66f)
    val nucleo = Brush.verticalGradient(listOf(Color(0xFFFFFBE6), Color(0xFFFFF3B0)), startY = 38f, endY = 64f)
    val halo = Brush.radialGradient(
        listOf(Dorado.copy(alpha = 0.50f), RojoCta.copy(alpha = 0.14f), Color.Transparent),
        center = Offset(50f, 48f),
        radius = 38f,
    )
    /** Resplandor propio de la llama, por fuera del halo: la hace "prender". */
    val brilloLlama = Brush.radialGradient(
        listOf(Color(0xFFFFF3B0).copy(alpha = 0.55f), Dorado.copy(alpha = 0.18f), Color.Transparent),
        center = Offset(50f, 50f),
        radius = 20f,
    )
    val cachete = Brush.radialGradient(listOf(Coral.copy(alpha = 0.45f), Color.Transparent), radius = 8f)
    val bocaInterior = Brush.verticalGradient(listOf(NocheBase, Resplandor2), startY = 112f, endY = 129f)
    /** Reflejo que se desvanece hacia abajo: mascara para BlendMode.DstIn. */
    val desvanecer = Brush.verticalGradient(
        listOf(Color.Black, Color.Transparent),
        startY = PIE_VELITA_U,
        endY = PIE_VELITA_U + REFLEJO_U,
    )
}

/**
 * Velita: la mascota, una vela de cumpleaños con cara, en 3D glossy.
 *
 * El volumen sale de capas, no de blur (Modifier.blur es API 31+ y minSdk 26):
 * degradado vertical + radial de luz principal + rim light cian abajo a la
 * derecha + dos especulares + resplandor exterior con radiales y copias del
 * contorno a alfa baja. Sin rim light la figura queda pegada al fondo.
 *
 * El ancho manda: el alto sale de la relacion del lienzo (con o sin reflejo).
 *
 * @param llamaViva parpadeo sutil de la llama. Es animacion infinita: no
 *   prenderla en mas de un lugar a la vez.
 * @param brillo 0..1, para entrar en escena (escala de la llama, el "pop").
 */
@Composable
fun Velita(
    modifier: Modifier = Modifier,
    expresion: Expresion = Expresion.CONTENTA,
    reflejo: Boolean = false,
    llamaViva: Boolean = true,
    destellos: Boolean = true,
    encendidoLlama: () -> Float = { 1f },
    /** 0..1, rebote de la mano que señala. Solo con [Expresion.SENALA]. */
    senal: () -> Float = { 0f },
) {
    val parpadeo: State<Float> = if (llamaViva) {
        rememberInfiniteTransition(label = "llama").animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "escalaLlama",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val alto = ALTO_U + if (reflejo) REFLEJO_U else 0f
    Spacer(
        modifier
            .aspectRatio(ANCHO_U / alto)
            .drawWithCache {
                val s = size.width / ANCHO_U
                onDrawBehind {
                    scale(s, s, Offset.Zero) {
                        dibujarVelita(expresion, parpadeo.value * encendidoLlama(), destellos, reflejo, senal())
                    }
                }
            },
    )
}

/** Velita entera en unidades de 100 x 160. Tambien la usa el festejo. */
fun DrawScope.dibujarVelita(expresion: Expresion, llama: Float, destellos: Boolean, reflejo: Boolean, senal: Float = 0f) {
    if (reflejo) {
        // Reflejo en el piso: la misma silueta espejada, alfa 0.18, desvanecida.
        drawIntoCanvas { c ->
            c.saveLayer(Rect(0f, PIE_VELITA_U, ANCHO_U, PIE_VELITA_U + REFLEJO_U), Paint())
            withTransform({ scale(1f, -1f, Offset(50f, PIE_VELITA_U)) }) {
                brazos(expresion, 0.18f, senal)
                drawPath(Piezas.cuerpo, Pinceles.cuerpo, alpha = 0.18f)
            }
            drawRect(
                Pinceles.desvanecer,
                Offset(0f, PIE_VELITA_U),
                Size(ANCHO_U, REFLEJO_U),
                blendMode = BlendMode.DstIn,
            )
            c.restore()
        }
    }

    // Resplandor exterior: radial + dos copias del contorno, mas anchas y tenues.
    drawRect(Pinceles.glow, Offset(-20f, 40f), Size(140f, 136f))
    for ((k, a) in listOf(1.10f to 0.08f, 1.05f to 0.12f)) {
        scale(k, k, Offset(50f, 108f)) { drawPath(Piezas.cuerpo, Cian, alpha = a) }
    }
    // Sombra de apoyo, mas angosta que el cuerpo.
    scale(1f, 0.22f, Offset(50f, PIE_VELITA_U)) {
        drawCircle(Pinceles.sombra, 24f, Offset(50f, PIE_VELITA_U))
    }

    brazos(expresion, 1f, senal)

    drawPath(Piezas.cuerpo, Pinceles.cuerpo)
    clipPath(Piezas.cuerpo) {
        drawPath(Piezas.franjas, Color.White, alpha = 0.16f)
        drawRect(Pinceles.luz, Offset(18f, 66f), Size(64f, 83f))
        drawPath(Piezas.cera, Pinceles.cera)
        // Sombrita bajo el borde de la cera: le da espesor.
        translate(0f, 1.6f) { drawPath(Piezas.cera, CianProfundo, alpha = 0.25f, style = Stroke(1.2f)) }
    }
    drawPath(Piezas.cuerpo, Pinceles.rim, style = Stroke(width = 2.6f))

    // Especulares: dos elipses blancas arriba a la izquierda. Plastico pulido.
    rotate(-24f, Offset(30f, 80f)) {
        drawOval(Color.White, Offset(26f, 72f), Size(7f, 15f), alpha = 0.9f)
    }
    drawOval(Color.White, Offset(24.5f, 92f), Size(4f, 5f), alpha = 0.6f)

    cara(expresion)

    // Halo calido: ilumina la parte de arriba del cuerpo.
    drawRect(Pinceles.halo, Offset(14f, 14f), Size(72f, 72f))
    if (llama > 0.01f) {
        scale(llama, llama, Offset(50f, 66f)) {
            drawCircle(Pinceles.brilloLlama, 20f, Offset(50f, 50f))
            drawPath(Piezas.llama, Pinceles.llama)
            drawPath(Piezas.nucleo, Pinceles.nucleo, alpha = 0.95f)
        }
    }

    if (destellos) {
        destelloU(Offset(12f, 40f), 7f, BlancoEscena)
        destelloU(Offset(88f, 28f), 5f, Dorado)
        destelloU(Offset(91f, 92f), 3.6f, BlancoEscena)
        destelloU(Offset(8f, 118f), 2.8f, Dorado)
    }
}

private fun DrawScope.destelloU(c: Offset, r: Float, color: Color) {
    translate(c.x, c.y) { scale(r, r, Offset.Zero) { drawPath(Piezas.destello, color) } }
}

/** Hacia donde apunta el brazo que señala: abajo y apenas afuera, al boton que tiene debajo. */
private val DIRECCION_SENAL = Offset(0.34f, 0.94f)

private fun DrawScope.brazos(expresion: Expresion, alfa: Float, senal: Float) {
    val trazo = Stroke(width = 9f, cap = StrokeCap.Round)
    val izq: Pair<Offset, Offset>
    val der: Pair<Offset, Offset>
    when (expresion) {
        Expresion.FESTEJO -> {
            izq = Offset(22f, 104f) to Offset(9f, 86f)
            der = Offset(78f, 104f) to Offset(91f, 86f)
        }
        Expresion.SENALA -> {
            izq = Offset(22f, 112f) to Offset(12f, 126f)
            // Estirado y largo; el rebote lo alarga y lo acorta unas unidades.
            val hombro = Offset(78f, 108f)
            der = hombro to hombro + DIRECCION_SENAL * (30f + 7f * senal)
        }
        Expresion.CONTENTA -> {
            izq = Offset(22f, 112f) to Offset(12f, 126f)
            der = Offset(78f, 112f) to Offset(88f, 126f)
        }
    }
    for ((a, b) in listOf(izq, der)) {
        drawLine(CianProfundo, a, b, strokeWidth = trazo.width, cap = StrokeCap.Round, alpha = alfa)
        drawLine(CianClaro, a, b, strokeWidth = trazo.width - 3.5f, cap = StrokeCap.Round, alpha = alfa * 0.8f)
    }
    if (expresion == Expresion.SENALA) {
        // La mano: una bolita al final del brazo y un dedo estirado, para que se
        // lea "señala" y no "brazo caido".
        val punta = der.second + DIRECCION_SENAL * 9f
        drawLine(CianProfundo, der.second, punta, strokeWidth = 5f, cap = StrokeCap.Round, alpha = alfa)
        drawLine(CianClaro, der.second, punta, strokeWidth = 2.6f, cap = StrokeCap.Round, alpha = alfa)
        drawCircle(CianProfundo, 6.2f, der.second, alpha = alfa)
        drawCircle(CianClaro, 4.6f, der.second, alpha = alfa)
    }
}

private fun DrawScope.cara(expresion: Expresion) {
    // Cachetes
    translate(30f, 112f) { drawCircle(Pinceles.cachete, 8f, Offset.Zero) }
    translate(70f, 112f) { drawCircle(Pinceles.cachete, 8f, Offset.Zero) }

    if (expresion == Expresion.FESTEJO) {
        // Ojos cerrados de felicidad: arcos hacia arriba, gruesos.
        val ojo = Stroke(width = 3.2f, cap = StrokeCap.Round)
        for (x in listOf(37f, 63f)) {
            drawArc(NocheBase, 200f, 140f, false, Offset(x - 6f, 97f), Size(12f, 10f), style = ojo)
        }
        drawPath(Piezas.bocaFestejo, Pinceles.bocaInterior)
        clipPath(Piezas.bocaFestejo) { drawOval(Coral, Offset(42f, 120f), Size(16f, 12f)) }
        drawPath(Piezas.bocaFestejo, NocheBase, style = Stroke(1.2f))
    } else {
        for (x in listOf(37f, 63f)) {
            drawOval(NocheBase, Offset(x - 5f, 94f), Size(10f, 13f))
            // Tres brillos de tamaños distintos: con uno solo el ojo queda muerto.
            drawCircle(Color.White, 2.7f, Offset(x - 1.6f, 97.6f))
            drawCircle(Color.White, 1.4f, Offset(x + 2.1f, 103f))
            drawCircle(Color.White, 0.7f, Offset(x + 3.2f, 96.6f), alpha = 0.9f)
        }
        drawPath(Piezas.boca, Pinceles.bocaInterior)
        clipPath(Piezas.boca) { drawOval(Coral, Offset(44.5f, 120.5f), Size(11f, 8f)) }
        drawPath(Piezas.boca, NocheBase, style = Stroke(1.1f))
    }
}
