package com.blair.cumpleanos.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.blair.cumpleanos.R

/**
 * Sufijo _v1: importance es INMUTABLE despues de crear el canal. Cambiarla
 * exige un id nuevo.
 */
const val CANAL_CUMPLE = "cumple_v1"
const val CANAL_RESUMEN = "resumen_v1"

object Canales {

    /**
     * Se llama en CADA arranque: nombre y descripcion si se actualizan, y sin
     * esto el usuario que cambia el idioma del sistema ve los canales en el
     * idioma viejo para siempre.
     */
    fun registrar(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CANAL_CUMPLE,
                ctx.getString(R.string.ch_cumple_nombre),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = ctx.getString(R.string.ch_cumple_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CANAL_RESUMEN,
                ctx.getString(R.string.ch_resumen_nombre),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = ctx.getString(R.string.ch_resumen_desc) }
        )
    }
}
