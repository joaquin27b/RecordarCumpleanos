package com.blair.cumpleanos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Expresion
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.escena.PISO_ESCENARIO
import com.blair.cumpleanos.ui.escena.PIE_VELITA_U
import com.blair.cumpleanos.ui.escena.Velita

/** TEMPORAL, solo debug: mirar la escena en el telefono. Se borra en el paso 9. */
@Composable
fun VistaPrevia(vista: String) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val completa = vista != "atenuada"
        Escenario(if (completa) Intensidad.COMPLETA else Intensidad.ATENUADA)
        if (completa) {
            val ancho = minOf(maxWidth * 0.46f, 220.dp)
            val pieY = maxHeight * PISO_ESCENARIO.y
            Box(
                Modifier
                    .offset(x = (maxWidth - ancho) / 2, y = pieY - ancho * (PIE_VELITA_U / 100f))
                    .width(ancho),
            ) {
                Velita(
                    Modifier.width(ancho),
                    expresion = if (vista == "festejo") Expresion.FESTEJO else Expresion.CONTENTA,
                    reflejo = true,
                )
            }
        }
    }
}
