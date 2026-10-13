package com.blair.cumpleanos.ui.escena

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.ui.theme.DegradeCromo
import com.blair.cumpleanos.ui.theme.HazMagenta2
import com.blair.cumpleanos.ui.theme.Outfit
import com.blair.cumpleanos.ui.theme.Resplandor3

/**
 * Titulo de marca en 3D cromado: resplandor magenta, extrusion y frente cromado.
 *
 * Una linea por palabra: el degradado del cromo es por linea, como en el
 * poster, y no uno solo estirado sobre el bloque entero.
 *
 * @param brillo 0..1 recorre un barrido de luz sobre el cromo; fuera de rango no
 *   se dibuja. Se lee en la fase de dibujo: animarlo no recompone.
 * @param extrusion cuantas copias desplazadas dan el espesor.
 */
@Composable
fun TituloCromado(
    texto: String,
    tamano: TextUnit,
    modifier: Modifier = Modifier,
    brillo: () -> Float = { -1f },
    resplandor: Boolean = true,
    extrusion: Int = 5,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        texto.split(' ').filter { it.isNotBlank() }.forEach { linea ->
            LineaCromada(linea, tamano, brillo, resplandor, extrusion)
        }
    }
}

@Composable
private fun LineaCromada(
    linea: String,
    tamano: TextUnit,
    brillo: () -> Float,
    resplandor: Boolean,
    extrusion: Int,
) {
    val estilo = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Black,
        fontSize = tamano,
        lineHeight = tamano * 1.02f,
        // Letras bien juntas: abierto, el bloque se desarma.
        letterSpacing = (-0.03f * tamano.value).sp,
        textAlign = TextAlign.Center,
    )
    Box(contentAlignment = Alignment.Center) {
        if (resplandor) {
            // Shadow con blur sobre texto funciona en todas las API; el cuerpo de
            // esta capa queda tapado por la extrusion y el frente.
            Text(
                linea,
                style = estilo.copy(
                    color = HazMagenta2.copy(alpha = 0.5f),
                    shadow = Shadow(HazMagenta2, Offset.Zero, blurRadius = tamano.value * 0.9f),
                ),
            )
        }
        for (i in extrusion downTo 1) {
            Text(linea, style = estilo.copy(color = Resplandor3), modifier = Modifier.offset(y = i.dp))
        }
        Text(
            linea,
            style = estilo.copy(brush = DegradeCromo),
            modifier = Modifier
                // Offscreen: el barrido con SrcAtop pinta solo donde hay letra.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val p = brillo()
                    if (p in 0f..1f) {
                        val x = -0.3f * size.width + p * 1.6f * size.width
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                start = Offset(x - size.height * 0.6f, 0f),
                                end = Offset(x + size.height * 0.6f, size.height),
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                },
        )
    }
}
