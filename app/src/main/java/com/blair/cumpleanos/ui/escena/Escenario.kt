package com.blair.cumpleanos.ui.escena

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.Cian
import com.blair.cumpleanos.ui.theme.Cian2
import com.blair.cumpleanos.ui.theme.Cian3
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.HazFucsia
import com.blair.cumpleanos.ui.theme.HazLavanda
import com.blair.cumpleanos.ui.theme.HazMagenta
import com.blair.cumpleanos.ui.theme.HazMagenta2
import com.blair.cumpleanos.ui.theme.HazVioleta
import com.blair.cumpleanos.ui.theme.NocheBase
import com.blair.cumpleanos.ui.theme.NocheBase2
import com.blair.cumpleanos.ui.theme.NocheMedia2
import com.blair.cumpleanos.ui.theme.NochePiso
import com.blair.cumpleanos.ui.theme.Resplandor3
import com.blair.cumpleanos.ui.theme.RojoCta
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Las dos intensidades del fondo.
 *
 * El poster puede tener el escenario entero detras de tres lineas; una lista de
 * 8 tarjetas no: COMPLETA va solo en splash, estado vacio y festejo. Lista,
 * alta y hojas van sobre ATENUADA.
 */
enum class Intensidad { COMPLETA, ATENUADA }

/** Donde apoya la mascota, en fraccion del tamaño. Ancla piso y cenital. */
val PISO_ESCENARIO = Offset(0.5f, 0.60f)

/** Semilla FIJA: el confeti cae siempre en el mismo lugar, no "hierve" al recomponer. */
private const val SEMILLA = 27

private const val HACES = 8
private const val CONFETI = 40
private const val CONFETI_ATENUADA = 12
private const val ESTRELLAS = 10
private const val ESTRELLAS_ATENUADA = 4
private const val BOKEH = 12
private const val LUCES_PUBLICO = 42

/**
 * Banda de arriba sin estrellas ni confeti de los que quedan en ATENUADA:
 * barra de estado + margen + marquesina (~82 dp) + su halo. Sin ella una
 * estrella quedaba a caballo del borde de la marquesina, cortada por la barra.
 */
private const val RESERVA_ARRIBA_DP = 180f

/** Alto holgado de la barra de estado. */
private const val BARRA_ESTADO_DP = 52f

/** Margen lateral de la lista: el borde de las tarjetas cae siempre en esta x. */
private const val BORDE_TARJETA_DP = 16f

/** Alfa de los haces que quedan en ATENUADA: 0.22 * 0.45 = 0.10 de maxima. */
private const val FACTOR_HAZ_ATENUADO = 0.45f
private const val ALFA_CONFETI_ATENUADO = 0.40f
private const val ALFA_ESTRELLA_ATENUADA = 0.35f

private class Haz(val trazo: Path, val pincel: Brush, val enAtenuada: Boolean)

private class Circulo(val centro: Offset, val r: Float, val pincel: Brush)

private class Pieza(
    val tipo: Int,
    val centro: Offset,
    val ancho: Float,
    val alto: Float,
    val giro: Float,
    val color: Color,
    val alfa: Float,
    val tira: Path?,
)

private class Estrella(val centro: Offset, val r: Float, val color: Color, val halo: Boolean, val trazo: Path)

private class Luz(val centro: Offset, val color: Color, val halo: Brush)

/** Todo lo que depende del tamaño, armado una vez. El draw solo recorre listas. */
private class Geometria(size: Size, d: Density, piso: Offset) {
    val base: Brush
    val origenHaces: Offset
    val haces: List<Haz>
    val bokeh: List<Circulo>
    val confeti: List<Pieza>
    val estrellas: List<Estrella>
    val cenital: List<Pair<Path, Brush>>
    val pisoCentro: Offset
    val pisoPincel: Brush
    val pisoR: Float
    val bandaPiso: Brush
    val bandaPisoRect: Rect
    val publicoAtras: Path
    val contraluz: Brush
    val contraluzRect: Rect
    val bordePiso: Brush
    val publicoFrente: Path
    val luces: List<Luz>
    val luzCuerpo: Size

    init {
        val w = size.width
        val h = size.height
        val dp = d.density
        val azar = Random(SEMILLA)
        fun r(a: Float, b: Float) = a + azar.nextFloat() * (b - a)

        // Lo que queda en ATENUADA no puede pisar un contorno: ni el de la
        // marquesina en reposo ni el borde lateral de las tarjetas. Se corre en
        // vez de re-sortear, asi el resto de la escena no cambia de lugar.
        val reserva = RESERVA_ARRIBA_DP * dp
        val bordeIzq = BORDE_TARJETA_DP * dp
        val bordeDer = w - BORDE_TARJETA_DP * dp
        // Ni cruzando el borde ni en la canaleta: ahi cae el halo de la tarjeta
        // y cualquier cosa parece asomar de atras.
        // Y con aire: pegada al reborde, a traves de la tarjeta translucida, se lee igual.
        val aire = 12f * dp
        fun sinBordes(x: Float, ext: Float): Float = x.coerceIn(bordeIzq + ext + aire, bordeDer - ext - aire)
        fun bajoReserva(y: Float, ext: Float, desde: Float, hasta: Float): Float {
            val t = ((y - desde) / (hasta - desde)).coerceIn(0f, 1f)
            return lerp(reserva + ext, hasta, t)
        }

        base = Brush.radialGradient(
            0f to Resplandor3,
            0.30f to NocheMedia2,
            0.58f to NocheBase2,
            0.82f to NocheBase,
            1f to NochePiso,
            center = Offset(w * 0.5f, h * 0.35f),
            // Mayor que la diagonal: el borde del circulo no se ve nunca.
            radius = hypot(w, h) * 1.05f,
        )

        // Haces: cuñas irregulares que nacen detras del centro superior.
        origenHaces = Offset(w * 0.5f, h * 0.05f)
        val largo = hypot(w, h) * 1.1f
        val angulos = floatArrayOf(-64f, -46f, -29f, -11f, 7f, 25f, 43f, 62f)
        val colores = listOf(HazMagenta, HazFucsia, HazLavanda, HazMagenta2, HazVioleta, HazFucsia, HazMagenta, HazLavanda)
        val atenuados = setOf(2, 4, 6)
        haces = List(HACES) { i ->
            val centro = Math.toRadians((angulos[i] + r(-4f, 4f)).toDouble())
            val medio = Math.toRadians((r(5f, 13f) / 2f).toDouble())
            fun punta(a: Double) = Offset(
                origenHaces.x + (sin(a) * largo).toFloat(),
                origenHaces.y + (cos(a) * largo).toFloat(),
            )
            val p1 = punta(centro - medio)
            val p2 = punta(centro + medio)
            val trazo = Path().apply {
                moveTo(origenHaces.x, origenHaces.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            // Radial desde el origen y no vertical: las cuñas laterales tambien
            // tienen que apagarse hacia la punta, no hacia abajo.
            val c = colores[i]
            Haz(
                trazo,
                Brush.radialGradient(
                    0f to c.copy(alpha = 0.22f),
                    0.45f to c.copy(alpha = 0.10f),
                    1f to Color.Transparent,
                    center = origenHaces,
                    radius = largo,
                ),
                enAtenuada = i in atenuados,
            )
        }

        bokeh = List(BOKEH) {
            val c = Offset(r(0f, w), r(0f, h * 0.85f))
            val radio = r(40f, 110f) * dp
            val color = if (azar.nextBoolean()) HazLavanda else Cian2
            Circulo(
                c, radio,
                Brush.radialGradient(
                    listOf(color.copy(alpha = r(0.04f, 0.09f)), Color.Transparent),
                    center = c,
                    radius = radio,
                ),
            )
        }

        val paletaConfeti = listOf(CianNeon, Dorado, HazFucsia, BlancoEscena, RojoCta, Cian3)
        confeti = List(CONFETI) {
            val tipo = azar.nextInt(3)
            val color = paletaConfeti[azar.nextInt(paletaConfeti.size)]
            val ancho = when (tipo) {
                0 -> r(5f, 12f) * dp
                1 -> r(1.8f, 3.5f) * dp
                else -> r(10f, 20f) * dp
            }
            val alto = if (tipo == 0) r(2.5f, 5f) * dp else ancho
            val ext = maxOf(ancho, alto) / 2f + 2f * dp
            val enAtenuada = it < CONFETI_ATENUADA
            var cx = r(0f, w)
            var cy = r(0f, h * 0.78f)
            if (enAtenuada) {
                cx = sinBordes(cx, ext)
                cy = bajoReserva(cy, ext, 0f, h * 0.78f)
            }
            val tira = if (tipo == 2) Path().apply {
                moveTo(-ancho / 2f, 0f)
                cubicTo(-ancho / 6f, -ancho * 0.35f, ancho / 6f, ancho * 0.35f, ancho / 2f, 0f)
            } else null
            Pieza(
                tipo, Offset(cx, cy), ancho, alto,
                r(0f, 360f), color, r(0.55f, 0.95f), tira,
            )
        }

        estrellas = List(ESTRELLAS) { i ->
            // Tamaños muy distintos: dos grandes, el resto chicas.
            val radio = (if (i < 2) r(14f, 22f) else r(4f, 10f)) * dp
            var cx = r(0.04f * w, 0.96f * w)
            var cy = r(0.03f * h, 0.7f * h)
            // Halo incluido: es lo que se ve cortado, no solo las puntas.
            val ext = radio * 1.7f + 2f * dp
            if (i < ESTRELLAS_ATENUADA) {
                cx = sinBordes(cx, ext)
                cy = bajoReserva(cy, ext, 0.03f * h, 0.7f * h)
            } else {
                // En COMPLETA no hay marquesina, pero la barra de estado igual corta.
                cy = maxOf(cy, BARRA_ESTADO_DP * dp + ext)
            }
            Estrella(
                Offset(cx, cy),
                radio,
                if (azar.nextInt(3) == 0) Dorado else BlancoEscena,
                halo = radio > 9f * dp,
                trazo = trazoDestello(radio),
            )
        }

        // Cenital: trapecios apilados, cada uno al 3-5%: bordes difusos sin blur.
        pisoCentro = Offset(w * piso.x, h * piso.y)
        cenital = List(4) { k ->
            val arriba = w * (0.025f + 0.012f * k)
            val abajo = w * (0.17f + 0.05f * k)
            val trazo = Path().apply {
                moveTo(pisoCentro.x - arriba, 0f)
                lineTo(pisoCentro.x + arriba, 0f)
                lineTo(pisoCentro.x + abajo, pisoCentro.y)
                lineTo(pisoCentro.x - abajo, pisoCentro.y)
                close()
            }
            trazo to Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.02f)),
                startY = 0f,
                endY = pisoCentro.y,
            )
        }

        pisoR = w * 0.52f
        pisoPincel = Brush.radialGradient(
            0f to CianNeon.copy(alpha = 0.34f),
            0.35f to Cian.copy(alpha = 0.20f),
            0.7f to HazVioleta.copy(alpha = 0.10f),
            1f to Color.Transparent,
            center = pisoCentro,
            radius = pisoR,
        )
        bandaPisoRect = Rect(0f, pisoCentro.y, w, h)
        bandaPiso = Brush.verticalGradient(
            listOf(Color.Transparent, NochePiso.copy(alpha = 0.55f)),
            startY = pisoCentro.y,
            endY = h,
        )

        // Borde del escenario: un aro tenue que dibuja la plataforma.
        bordePiso = Brush.radialGradient(
            0.80f to Color.Transparent,
            0.93f to CianNeon.copy(alpha = 0.16f),
            1f to Color.Transparent,
            center = pisoCentro,
            radius = pisoR * 0.92f,
        )

        // Publico: dos filas de cabezas y hombros en el 18% inferior.
        val tope = h * 0.82f
        // Contraluz detras de las cabezas: sin ella la silueta se funde con la noche.
        contraluzRect = Rect(0f, tope - 90f * dp, w, tope + 60f * dp)
        contraluz = Brush.verticalGradient(
            0f to Color.Transparent,
            0.62f to HazVioleta.copy(alpha = 0.42f),
            1f to Color.Transparent,
            startY = contraluzRect.top,
            endY = contraluzRect.bottom,
        )
        fun fila(desdeY: Float, escala: Float): Path = Path().apply {
            addRect(Rect(0f, desdeY + 34f * dp * escala, w, h))
            var x = -12f * dp
            while (x < w + 12f * dp) {
                val cab = r(9f, 15f) * dp * escala
                val cy = desdeY + r(0f, 14f) * dp * escala + cab
                addOval(Rect(Offset(x, cy), cab))
                val hombroAncho = cab * 2.2f
                addOval(Rect(x - hombroAncho, cy + cab * 0.9f, x + hombroAncho, cy + cab * 4f))
                x += r(17f, 27f) * dp * escala
            }
        }
        publicoAtras = fila(tope - 10f * dp, 0.82f)
        publicoFrente = fila(tope + 14f * dp, 1f)

        // Telefonos en alto: luz calida o fria con halo.
        luzCuerpo = Size(3f * dp, 5f * dp)
        luces = List(LUCES_PUBLICO) {
            val c = Offset(r(0f, w), r(tope - 22f * dp, tope + 26f * dp))
            val color = if (azar.nextInt(3) == 0) CianNeon else Dorado
            Luz(
                c, color,
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.5f), Color.Transparent),
                    center = c,
                    radius = 10f * dp,
                ),
            )
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Fondo de TODA la app: escenario de recital en una sola pasada de dibujo.
 *
 * Un solo drawWithCache y no decenas de composables: la geometria se arma una
 * vez por tamaño y el draw solo recorre listas. nivel, vaiven y encendido se leen
 * en la fase de dibujo, asi que animarlos repinta sin recomponer. graphicsLayer
 * lo aisla en su propia capa: scrollear la lista de encima no lo repinta.
 *
 * @param piso donde apoya la mascota; ancla el piso iluminado y el cenital.
 * @param encendido 0..1, haces que se prenden uno por uno en el splash.
 */
@Composable
fun Escenario(
    intensidad: Intensidad,
    modifier: Modifier = Modifier,
    piso: Offset = PISO_ESCENARIO,
    encendido: () -> Float = { 1f },
) {
    val completa = intensidad == Intensidad.COMPLETA
    // La transicion no es un corte: la lista baja a atenuada y el vacio la levanta.
    val nivel by animateFloatAsState(
        targetValue = if (completa) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "nivelEscena",
    )
    // Vaiven solo en COMPLETA: en la lista los haces quedan quietos.
    val vaiven: State<Float> = if (completa) {
        rememberInfiniteTransition(label = "vaiven").animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(10_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "anguloHaces",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val g = Geometria(size, this, piso)
                onDrawBehind { pintar(g, nivel, vaiven.value, encendido().coerceIn(0f, 1f)) }
            },
    )
}

private fun DrawScope.pintar(g: Geometria, nivel: Float, vaiven: Float, encendido: Float) {
    drawRect(g.base)

    val bokehAlfa = lerp(0.6f, 1f, nivel)
    g.bokeh.forEach { drawCircle(it.pincel, it.r, it.centro, alpha = bokehAlfa) }

    rotate(vaiven, g.origenHaces) {
        g.haces.forEachIndexed { i, haz ->
            val prendido = (encendido * HACES - i).coerceIn(0f, 1f)
            val f = if (haz.enAtenuada) lerp(FACTOR_HAZ_ATENUADO, 1f, nivel) else nivel
            val a = f * prendido
            if (a > 0.004f) drawPath(haz.trazo, haz.pincel, alpha = a)
        }
    }

    if (nivel > 0.004f) {
        g.cenital.forEach { (trazo, pincel) -> drawPath(trazo, pincel, alpha = nivel * encendido) }
    }

    g.estrellas.forEachIndexed { i, e ->
        val a = if (i < ESTRELLAS_ATENUADA) lerp(ALFA_ESTRELLA_ATENUADA, 1f, nivel) else nivel
        destello(e.trazo, e.centro, e.r, e.color, a, e.halo)
    }

    g.confeti.forEachIndexed { i, p ->
        val a = if (i < CONFETI_ATENUADA) lerp(ALFA_CONFETI_ATENUADO, p.alfa, nivel) else p.alfa * nivel
        if (a > 0.004f) pieza(p, a)
    }

    if (nivel > 0.004f) {
        drawRect(g.bandaPiso, g.bandaPisoRect.topLeft, g.bandaPisoRect.size, alpha = nivel)
        // Elipse achatada: un radial circular aplastado en vertical.
        scale(1f, 0.17f, g.pisoCentro) {
            drawCircle(g.pisoPincel, g.pisoR, g.pisoCentro, alpha = nivel)
            drawCircle(g.bordePiso, g.pisoR * 0.92f, g.pisoCentro, alpha = nivel)
        }
        drawRect(g.contraluz, g.contraluzRect.topLeft, g.contraluzRect.size, alpha = nivel)
        drawPath(g.publicoAtras, NocheBase2, alpha = nivel)
        g.luces.forEach { l ->
            drawCircle(l.halo, 10f * density, l.centro, alpha = nivel)
            drawRect(
                l.color,
                Offset(l.centro.x - g.luzCuerpo.width / 2f, l.centro.y - g.luzCuerpo.height / 2f),
                g.luzCuerpo,
                alpha = nivel,
            )
        }
        drawPath(g.publicoFrente, NochePiso, alpha = nivel)
    }
}

private fun DrawScope.pieza(p: Pieza, alfa: Float) {
    translate(p.centro.x, p.centro.y) {
        rotate(p.giro, Offset.Zero) {
            when (p.tipo) {
                0 -> drawRect(p.color, Offset(-p.ancho / 2f, -p.alto / 2f), Size(p.ancho, p.alto), alpha = alfa)
                1 -> drawCircle(p.color, p.ancho, Offset.Zero, alpha = alfa)
                else -> drawPath(
                    p.tira!!, p.color, alpha = alfa,
                    style = Stroke(width = 2.4f * density, cap = StrokeCap.Round),
                )
            }
        }
    }
}
