package com.blair.cumpleanos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.blair.cumpleanos.ui.escena.Expresion
import com.blair.cumpleanos.ui.escena.Velita
import com.blair.cumpleanos.ui.theme.Dorado

/** Ancho de Velita arriba del boton: grande, que se vea apenas se abre la app. */
private val ANCHO_MASCOTA_INVITA = 104.dp

/** Grados de balanceo: leve, no un vaiven. */
private const val BALANCEO_GRADOS = 3f

/** Cuanto crece la llama en el pico del rebote: 1 + esto. */
private const val CRECE_LLAMA = 0.35f

/** Ida o vuelta del rebote de la mano: lento, nada estroboscopico. */
private const val MEDIO_CICLO_MS = 1100

/**
 * Fase 0..1..0 del rebote de la mano que señala. Corre SOLO con la pantalla
 * visible: repeatOnLifecycle(STARTED) la corta al salir de la app o apagar la
 * pantalla, y la retoma al volver. Fuera de composicion no existe.
 */
@Composable
private fun rememberSenal(): State<Float> {
    val fase = remember { Animatable(0f) }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                fase.animateTo(1f, tween(MEDIO_CICLO_MS, easing = FastOutSlowInEasing))
                fase.animateTo(0f, tween(MEDIO_CICLO_MS, easing = FastOutSlowInEasing))
            }
        }
    }
    return fase.asState()
}

/**
 * Velita parada arriba del boton de agregar, señalandolo hacia abajo, como la
 * mascota de Ya La Tomé y Por Vencer. Se balancea, la mano rebota, la llama
 * crece y un halo dorado late detras: tiene que despegarse del escenario.
 */
@Composable
fun MascotaInvita(modifier: Modifier = Modifier) {
    val senal = rememberSenal()
    Box(modifier, contentAlignment = Alignment.Center) {
        // Halo dorado que late con la mano: separa a Velita de la noche violeta.
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension * (0.7f + 0.1f * senal.value)
            drawCircle(
                Brush.radialGradient(
                    listOf(Dorado.copy(alpha = 0.6f), Dorado.copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
            )
        }
        Velita(
            Modifier
                .width(ANCHO_MASCOTA_INVITA)
                .graphicsLayer {
                    rotationZ = (senal.value - 0.5f) * 2f * BALANCEO_GRADOS
                    transformOrigin = TransformOrigin(0.5f, 0.95f)
                },
            expresion = Expresion.SENALA,
            senal = { senal.value },
            encendidoLlama = { 1f + CRECE_LLAMA * senal.value },
        )
    }
}

/** Alto de la cola del globo, que apunta hacia abajo a la mascota. */
private val COLA_GLOBO = 12.dp
private val RADIO_GLOBO = 22.dp

/** Globo claro: sobre la noche del escenario tiene que saltar a la vista. */
private val FONDO_GLOBO = Color(0xFFF8FAFC)
private val TEXTO_GLOBO = Color(0xFF100038)

/**
 * Globo de dialogo de [MascotaInvita], encima de ella. La cola sale del centro,
 * sobre la cabeza de Velita. Entra con un rebote desde la cola, late apenas y
 * tocarlo abre el alta igual que el boton.
 */
@Composable
fun GloboMascota(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrada.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val pulso = rememberSenal()
    val forma = remember { FormaGlobo() }
    Box(
        modifier
            .graphicsLayer {
                val e = entrada.value
                val p = 1f + 0.03f * pulso.value
                scaleX = (0.6f + 0.4f * e) * p
                scaleY = (0.6f + 0.4f * e) * p
                alpha = e.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .halo(Dorado, radio = RADIO_GLOBO, fuerza = 1.4f)
            .background(FONDO_GLOBO, forma)
            .border(3.dp, Dorado, forma)
            .clip(forma)
            .clickable(onClick = onClick)
            .padding(bottom = COLA_GLOBO)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TEXTO_GLOBO,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Rectangulo redondeado con una cola triangular abajo, al centro. Union y no dos
 * figuras: el borde rodea las dos sin una linea en la base de la cola.
 */
private class FormaGlobo : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cola = with(density) { COLA_GLOBO.toPx() }
        val radio = with(density) { RADIO_GLOBO.toPx() }
        val medio = with(density) { 10.dp.toPx() }
        val base = size.height - cola
        val cuerpo = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, base, CornerRadius(radio))) }
        val x = size.width / 2
        val punta = Path().apply {
            moveTo(x - medio, base - 1f)
            lineTo(x, size.height)
            lineTo(x + medio, base - 1f)
            close()
        }
        return Outline.Generic(Path().apply { op(cuerpo, punta, PathOperation.Union) })
    }
}
