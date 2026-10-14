package com.blair.cumpleanos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.ui.escena.destello
import com.blair.cumpleanos.ui.escena.trazoDestello
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.DegradeCian
import com.blair.cumpleanos.ui.theme.DegradeRojo
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.Escenario1
import com.blair.cumpleanos.ui.theme.Escenario3
import com.blair.cumpleanos.ui.theme.Resplandor2
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.HazFucsia
import com.blair.cumpleanos.ui.theme.NochePiso
import com.blair.cumpleanos.ui.theme.RadioTarjeta
import com.blair.cumpleanos.ui.theme.Resplandor3
import com.blair.cumpleanos.ui.theme.RojoCta
import com.blair.cumpleanos.ui.theme.TextoSecundario
import com.blair.cumpleanos.ui.theme.TintaSobreLuz

// Alfas de superficie: la tarjeta deja ver el confeti de la escena atenuada.
private val FondoTarjeta = Escenario1.copy(alpha = 0.55f)
private val BordeTarjeta = EscenarioBorde.copy(alpha = 0.45f)

/** Anillos de resplandor por fuera de un rectangulo redondeado: separacion y alfa. */
private val ANILLOS_HALO = listOf(2f to 0.45f, 5f to 0.22f, 9f to 0.10f, 14f to 0.04f)

/**
 * Resplandor POR FUERA del composable, con anillos y no con Modifier.shadow ni
 * blur: la sombra se ve a traves de una superficie translucida y ensucia el
 * interior, y blur es API 31+. Va antes del clip.
 */
fun Modifier.halo(color: Color, radio: Dp = RadioTarjeta, trazo: Dp = 1.6.dp, fuerza: Float = 1f): Modifier = drawBehind {
    val r = radio.toPx()
    ANILLOS_HALO.forEach { (sep, a0) ->
        val a = a0 * fuerza
        val s = sep * density
        drawRoundRect(
            color,
            topLeft = Offset(-s, -s),
            size = Size(size.width + 2 * s, size.height + 2 * s),
            cornerRadius = CornerRadius(r + s),
            alpha = a,
            style = Stroke(trazo.toPx()),
        )
    }
}

/** Sombra difusa violeta abajo: capas desplazadas a alfa baja. */
private fun Modifier.sombraDifusa(radio: Dp): Modifier = drawBehind {
    val r = radio.toPx()
    for (k in 1..3) {
        val dy = (3f + k * 3f) * density
        val ex = k * 2f * density
        drawRoundRect(
            Resplandor3,
            topLeft = Offset(-ex, dy),
            size = Size(size.width + 2 * ex, size.height),
            cornerRadius = CornerRadius(r + ex),
            alpha = 0.16f,
        )
    }
}

/**
 * Tarjeta sobre la escena atenuada: violeta al 55% con reborde de neon.
 *
 * @param neon color del reborde y su resplandor: dice el estado de un vistazo
 *   (rojo sin saludar, cian hoy, dorado cerca, violeta lejos).
 * @param intenso reborde mas grueso y halo pleno: lo que pide accion ya.
 * @param confeti un puñado de confeti propio, solo para la de "cumple hoy".
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    neon: Color = EscenarioBorde,
    intenso: Boolean = false,
    confeti: Boolean = false,
    onClick: (() -> Unit)? = null,
    relleno: PaddingValues = PaddingValues(16.dp),
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(RadioTarjeta)
    Column(
        modifier
            .fillMaxWidth()
            .sombraDifusa(RadioTarjeta)
            .halo(neon, fuerza = if (intenso) 1f else 0.55f)
            .clip(forma)
            .background(FondoTarjeta)
            .border(if (intenso) 2.dp else 1.4.dp, neon.copy(alpha = if (intenso) 1f else 0.85f), forma)
            .then(if (confeti) Modifier.confetiPropio() else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(relleno),
        content = contenido,
    )
}

/** Radio de la marquesina: con ~82 dp de alto, 30 la dejaba casi pildora. */
private val RadioMarquesina = 22.dp

/** Lamparitas por lado largo. Los cortos llevan las que entren con el mismo paso. */
private const val LAMPARITAS_POR_LADO = 12

/**
 * Marquesina de teatro: recuadro magenta con reborde de neon y lamparitas
 * alrededor. Destaca la cabecera del resto de las tarjetas.
 */
@Composable
fun Marquesina(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    val forma = RoundedCornerShape(RadioMarquesina)
    Box(
        modifier
            .fillMaxWidth()
            .halo(HazFucsia, radio = RadioMarquesina, trazo = 2.dp)
            .clip(forma)
            .background(
                Brush.verticalGradient(
                    listOf(Resplandor2, Escenario3.copy(alpha = 0.92f), Escenario1.copy(alpha = 0.95f)),
                ),
            )
            .border(2.dp, HazFucsia, forma)
            .lamparitas()
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

/** Lamparitas doradas y blancas alternadas por dentro del borde. Fijas: no titilan. */
private fun Modifier.lamparitas(): Modifier = drawBehind {
    val m = 5.dp.toPx()
    val r = 1.6.dp.toPx()
    val ancho = size.width
    val alto = size.height
    // Fuera de las esquinas redondeadas: ahi el borde se curva y la lamparita queda afuera.
    val esquina = RadioMarquesina.toPx() * 0.6f
    val largo = ancho - 2 * (m + esquina)
    val paso = largo / (LAMPARITAS_POR_LADO - 1)
    val puntos = buildList {
        for (k in 0 until LAMPARITAS_POR_LADO) {
            val x = m + esquina + k * paso
            add(Offset(x, m)); add(Offset(x, alto - m))
        }
        var y = m + esquina + paso / 2f
        while (y < alto - m - esquina) { add(Offset(m, y)); add(Offset(ancho - m, y)); y += paso }
    }
    puntos.forEachIndexed { i, c ->
        val color = if (i % 2 == 0) Dorado else BlancoEscena
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = c, radius = r * 3f),
            r * 3f,
            c,
        )
        drawCircle(color, r, c)
    }
}

/** Un puñado de confeti en la esquina de la tarjeta destacada. Fijo, sin animar. */
private fun Modifier.confetiPropio(): Modifier = drawBehind {
    val piezas = listOf(
        Triple(Offset(size.width - 28.dp.toPx(), 14.dp.toPx()), Dorado, 30f),
        Triple(Offset(size.width - 52.dp.toPx(), 10.dp.toPx()), HazFucsia, -20f),
        Triple(Offset(size.width - 16.dp.toPx(), 34.dp.toPx()), CianNeon, 60f),
        Triple(Offset(size.width - 40.dp.toPx(), 28.dp.toPx()), BlancoEscena, 10f),
        Triple(Offset(size.width - 66.dp.toPx(), 22.dp.toPx()), RojoCta, -45f),
    )
    piezas.forEach { (c, color, giro) ->
        translate(c.x, c.y) {
            rotate(giro, Offset.Zero) {
                drawRect(color, Offset(-4.dp.toPx(), -1.5.dp.toPx()), Size(8.dp.toPx(), 3.dp.toPx()), alpha = 0.85f)
            }
        }
    }
    destello(trazoDestello(6.dp.toPx()), Offset(size.width - 80.dp.toPx(), 12.dp.toPx()), 6.dp.toPx(), BlancoEscena, 0.9f, true)
}

enum class TipoPildora { DORADA, ROJA, VIOLETA }

/**
 * Pildora luminosa. DORADA destaca (cumple hoy, faltan pocos dias), ROJA
 * urge (sin saludar, ya paso) y VIOLETA es la neutra para lo lejano.
 */
@Composable
fun Pildora(texto: String, tipo: TipoPildora, modifier: Modifier = Modifier, icono: ImageVector? = null) {
    val (fondo, tinta) = when (tipo) {
        TipoPildora.DORADA -> Brush.horizontalGradient(listOf(Dorado, Dorado)) to TintaSobreLuz
        TipoPildora.ROJA -> DegradeRojo to BlancoEscena
        TipoPildora.VIOLETA -> Brush.horizontalGradient(listOf(Escenario1, Escenario1)) to BlancoEscena
    }
    Row(
        modifier
            .then(
                if (tipo == TipoPildora.ROJA) {
                    // Resplandor rojo suave debajo
                    Modifier.drawBehind {
                        drawOval(
                            Brush.radialGradient(
                                listOf(RojoCta.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height * 0.9f),
                                radius = size.width * 0.6f,
                            ),
                            topLeft = Offset(-size.width * 0.1f, size.height * 0.2f),
                            size = Size(size.width * 1.2f, size.height * 1.3f),
                        )
                    }
                } else Modifier,
            )
            .clip(CircleShape)
            .background(fondo)
            .then(if (tipo == TipoPildora.VIOLETA) Modifier.border(1.dp, BordeTarjeta, CircleShape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(14.dp))
        Text(
            texto,
            color = tinta,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/**
 * Chip de eleccion: violeta translucido sin elegir; cian con texto oscuro y un
 * brillito cuando esta elegido.
 */
@Composable
fun ChipEleccion(texto: String, elegido: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(if (elegido) CianNeon else Escenario1.copy(alpha = 0.45f))
            .border(1.dp, if (elegido) CianNeon else EscenarioBorde.copy(alpha = 0.5f), CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (elegido) DestelloIcono(TintaSobreLuz, 12.dp)
        Text(
            texto,
            color = if (elegido) TintaSobreLuz else BlancoEscena.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (elegido) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}

/**
 * Boton principal: degradado cian, texto oscuro, resplandor abajo y escala 0.96
 * al presionar. Deshabilitado queda violeta translucido, sin degradado ni glow.
 */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
    destacado: Boolean = false,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (presionado) 0.96f else 1f, tween(120), label = "escalaBoton")
    Row(
        modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .then(
                if (habilitado) {
                    Modifier.drawBehind {
                        drawOval(
                            Brush.radialGradient(
                                listOf(CianNeon.copy(alpha = 0.42f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height * 0.85f),
                                radius = size.width * 0.55f,
                            ),
                            topLeft = Offset(-size.width * 0.05f, size.height * 0.25f),
                            size = Size(size.width * 1.1f, size.height * 1.25f),
                        )
                    }
                } else Modifier,
            )
            .then(if (destacado && habilitado) Modifier.glowFuerte() else Modifier)
            .heightIn(min = if (destacado) 60.dp else 52.dp)
            .clip(CircleShape)
            .background(if (habilitado) DegradeCian else Brush.horizontalGradient(listOf(Escenario1.copy(alpha = 0.5f), Escenario1.copy(alpha = 0.5f))))
            .then(if (destacado && habilitado) Modifier.border(1.5.dp, CianBorde, CircleShape) else Modifier)
            .clickable(
                interactionSource = interaccion,
                indication = androidx.compose.material3.ripple(color = TintaSobreLuz),
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (destacado) 34.dp else 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tinta = if (habilitado) TintaSobreLuz else BlancoEscena.copy(alpha = 0.5f)
        if (icono != null) {
            // Destacado: el + pesa lo mismo que el texto, no es un adorno.
            Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(if (destacado) 28.dp else 20.dp))
            Spacer(Modifier.width(if (destacado) 10.dp else 8.dp))
        }
        Text(
            texto,
            color = tinta,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (destacado) 17.sp else 15.sp,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Borde del boton destacado: cian casi blanco, separa el degradado del glow. */
private val CianBorde = Color(0xFFD8FCFF)

/** Glow de la accion principal: anillos por fuera, mas separados y opacos que el halo de tarjeta. */
private val ANILLOS_GLOW_FUERTE = listOf(2f to 0.60f, 5f to 0.38f, 9f to 0.20f, 14f to 0.09f, 20f to 0.04f)

/** Pildora: el radio de los anillos sigue la mitad del alto. Va antes del clip. */
private fun Modifier.glowFuerte(): Modifier = drawBehind {
    val r = size.height / 2f
    ANILLOS_GLOW_FUERTE.forEach { (sep, a) ->
        val s = sep * density
        drawRoundRect(
            CianNeon,
            topLeft = Offset(-s, -s),
            size = Size(size.width + 2 * s, size.height + 2 * s),
            cornerRadius = CornerRadius(r + s),
            alpha = a,
            style = Stroke(2.5f * density),
        )
    }
}

/** Estrella de 4 puntas como adorno de texto. */
@Composable
fun DestelloIcono(color: Color, tam: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tam)) {
        val r = size.minDimension / 2f
        translate(r, r) { drawPath(trazoDestello(r), color) }
    }
}

/** Encabezado de seccion: bold, en color, con un destello al final de la linea. */
@Composable
fun EncabezadoSeccion(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    accion: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            texto.uppercase(),
            color = color,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.2.sp,
        )
        Spacer(Modifier.width(8.dp))
        DestelloIcono(color, 14.dp)
        Spacer(Modifier.weight(1f))
        accion()
    }
}

/** Dia grande y mes corto: la "entrada" del cumpleaños, como un ticket de show. */
@Composable
fun InsigniaFecha(dia: Int, mes: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NochePiso.copy(alpha = 0.55f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            dia.toString(),
            color = BlancoEscena,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 24.sp,
        )
        Text(
            mes,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Franja solida detras del banner: sobre la escena, un AdView suelto se ve roto. */
@Composable
fun FranjaBanner(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(NochePiso)) {
        CompositionLocalProvider(LocalContentColor provides TextoSecundario) { contenido() }
    }
}

/**
 * Entrada escalonada: fade + subida, solo la primera vez que aparece la clave.
 *
 * El escalonado (40 ms por fila) vale para la primera tanda; lo que entra
 * despues scrolleando aparece sin demora, o las filas de abajo tardarian.
 */
@Composable
fun Modifier.entrada(clave: Any, orden: Int, vistos: MutableSet<Any>, inicio: Long): Modifier {
    val yaVisto = remember(clave) { clave in vistos }
    val p = remember(clave) { Animatable(if (yaVisto) 1f else 0f) }
    LaunchedEffect(clave) {
        if (!yaVisto) {
            vistos += clave
            val enTanda = System.currentTimeMillis() - inicio < 900
            if (enTanda) kotlinx.coroutines.delay(40L * orden.coerceAtMost(12))
            p.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 22.dp.toPx()
    }
}

