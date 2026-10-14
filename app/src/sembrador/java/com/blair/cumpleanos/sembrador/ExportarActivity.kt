package com.blair.cumpleanos.sembrador

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.escena.PIE_VELITA_U
import com.blair.cumpleanos.ui.escena.TituloCromado
import com.blair.cumpleanos.ui.escena.Velita
import com.blair.cumpleanos.ui.theme.Dorado
import java.io.File
import kotlinx.coroutines.delay

private const val ANCHO_FUNCION = 1024
private const val ALTO_FUNCION = 500
private const val LADO_ICONO = 512

/** Donde apoya Velita en el grafico de funcion, en fraccion del tamaño. */
private val PISO_FUNCION = Offset(0.25f, 0.66f)

/** Zona del texto, en fraccion: sin estrellas ni confeti encima del titulo. */
private val ZONA_TEXTO = Rect(0.42f, 0.10f, 0.95f, 0.66f)

/**
 * Exporta los assets de la ficha de Play desde el codigo REAL de la app.
 *
 * Solo en debug y medicion (src/sembrador). Deja los PNG en
 * /sdcard/Android/data/<paquete>/files/:
 *  - play_icon_512.png: las dos capas del icono adaptativo (los mismos
 *    VectorDrawable que usa el launcher) a 512, sangrado completo de 108 dp,
 *    como el "Play Store icon" de Android Studio.
 *  - play_feature_1024x500.png: Escenario COMPLETA + Velita con reflejo +
 *    titulo cromado y lema, renderizado por Compose a 1024x500 exactos.
 */
class ExportarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dir = getExternalFilesDir(null)!!
        exportarIcono(File(dir, "play_icon_512.png"))
        setContent {
            val capa = rememberGraphicsLayer()
            // Tamaño en px exactos, no en dp: 1024 / 2.81 no da entero.
            Box(
                Modifier
                    .layout { m, _ ->
                        val p = m.measure(Constraints.fixed(ANCHO_FUNCION, ALTO_FUNCION))
                        layout(ANCHO_FUNCION, ALTO_FUNCION) { p.place(0, 0) }
                    }
                    .drawWithContent {
                        capa.record { this@drawWithContent.drawContent() }
                        drawLayer(capa)
                    },
            ) {
                // Densidad propia: con la del telefono (2.81) el lienzo mide 364 dp y
                // el publico y el confeti quedan enormes. A 1.6 es un escenario de 640 dp.
                CompositionLocalProvider(LocalDensity provides Density(1.6f, 1f)) { Funcion() }
            }
            LaunchedEffect(Unit) {
                // Los haces y la llama terminan de entrar.
                delay(1_500)
                val bmp = capa.toImageBitmap().asAndroidBitmap()
                guardar(bmp, File(dir, "play_feature_1024x500.png"))
                Log.i("Exportar", "listo")
                finish()
            }
        }
    }

    private fun exportarIcono(destino: File) {
        val bmp = Bitmap.createBitmap(LADO_ICONO, LADO_ICONO, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { id ->
            ContextCompat.getDrawable(this, id)!!.apply {
                setBounds(0, 0, LADO_ICONO, LADO_ICONO)
                draw(c)
            }
        }
        guardar(bmp, destino)
    }

    private fun guardar(bmp: Bitmap, destino: File) {
        destino.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i("Exportar", "${destino.name} ${bmp.width}x${bmp.height}")
    }
}

@androidx.compose.runtime.Composable
private fun Funcion() {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        Escenario(Intensidad.COMPLETA, piso = PISO_FUNCION, despejar = ZONA_TEXTO)
        // Velita parada en el piso iluminado, con su reflejo.
        val ancho = 96.dp
        Velita(
            Modifier
                .width(ancho)
                .offset(
                    x = maxWidth * PISO_FUNCION.x - ancho / 2,
                    y = maxHeight * PISO_FUNCION.y - ancho * (PIE_VELITA_U / 100f),
                ),
            reflejo = true,
        )
        // Texto en el tercio central-derecho, lejos de los cuatro bordes.
        Column(
            Modifier
                .width(maxWidth * 0.50f)
                .offset(x = maxWidth * 0.43f, y = maxHeight * 0.20f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TituloCromado(texto = stringResource(R.string.marca), tamano = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.marca_lema),
                color = Dorado,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
