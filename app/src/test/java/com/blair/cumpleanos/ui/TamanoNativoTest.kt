package com.blair.cumpleanos.ui

import android.view.View.MeasureSpec
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Lo que el anuncio de prueba nunca muestra: creatividades reales de cualquier aspecto. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w360dp-h800dp-xhdpi")
class TamanoNativoTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val densidad = ctx.resources.displayMetrics.density

    private fun altoMediaDp(aspecto: Float): Int {
        val caja = CajaAspecto(ctx, altoMaxDp = 180).also { it.aspecto = aspecto }
        caja.measure(
            MeasureSpec.makeMeasureSpec((328 * densidad).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        return (caja.measuredHeight / densidad).toInt()
    }

    @Test fun `media vertical queda topeado`() = assertEquals(180, altoMediaDp(9f / 16f))

    @Test fun `media cuadrado queda topeado`() = assertEquals(180, altoMediaDp(1f))

    @Test fun `media apaisado usa su aspecto`() = assertEquals(164, altoMediaDp(2f))

    @Test fun `media muy ancho no baja del minimo de AdMob`() = assertEquals(120, altoMediaDp(4f))

    @Test fun `compacto tiene alto fijo`() {
        val v = VistaNativoCompacto(ctx)
        v.measure(
            MeasureSpec.makeMeasureSpec((328 * densidad).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        assertEquals(ALTO_COMPACTO_DP, (v.measuredHeight / densidad).toInt())
    }
}
