package com.blair.cumpleanos.ui

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.blair.cumpleanos.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner adaptativo en el bottomBar.
 *
 * Vive fuera del LazyColumn: adentro se destruiria y recrearia al scrollear,
 * quemando impresiones y disparando el filtro de invalid traffic de AdMob.
 */
@Composable
fun BannerAdaptativo(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity ?: return

    val ancho = (activity.resources.displayMetrics.widthPixels /
        activity.resources.displayMetrics.density).toInt()
    val tam = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, ancho)

    // Altura RESERVADA: sin ella AndroidView le pasa al AdView constraints de 0
    // y el creative carga pero nunca se pinta.
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            // navigationBarsPadding ANTES de height: al reves, el padding se
            // descuenta del alto del AdView en vez de sumarse por fuera.
            .navigationBarsPadding()
            .height(tam.height.dp),
        factory = {
            AdView(it).apply {
                setAdSize(tam)
                adUnitId = BuildConfig.AD_UNIT_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
