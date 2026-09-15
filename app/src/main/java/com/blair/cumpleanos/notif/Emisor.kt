package com.blair.cumpleanos.notif

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.blair.cumpleanos.MainActivity
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.fechaLarga
import java.util.Locale

/** Id de resumen fuera del rango de personaId para no pisar una notificacion de cumple. */
private const val ID_RESUMEN = Int.MAX_VALUE

object Emisor {

    /**
     * Una notificacion POR PERSONA, no por ocurrencia: el id es personaId, asi
     * el preaviso de D-7 se reemplaza por el de D-1 en vez de apilarse.
     *
     * Sin botones de accion: tocarla abre la app.
     */
    fun cumple(ctx: Context, personaId: Long, nombre: String, fechaDia: Int, diasRestantes: Int) {
        val loc: Locale = ctx.resources.configuration.locales[0]
        val texto = when (diasRestantes) {
            0 -> ctx.getString(R.string.notif_cumple_hoy, nombre)
            1 -> ctx.getString(R.string.notif_cumple_manana, nombre, fechaLarga(fechaDia, loc))
            else -> ctx.getString(R.string.notif_cumple_titulo, nombre, fechaLarga(fechaDia, loc))
        }
        emitir(ctx, personaId.toInt(), CANAL_CUMPLE, nombre, texto, NotificationCompat.PRIORITY_HIGH)
    }

    fun resumen(ctx: Context, titulo: String, cuerpo: String) {
        emitir(ctx, ID_RESUMEN, CANAL_RESUMEN, titulo, cuerpo, NotificationCompat.PRIORITY_LOW)
    }

    private fun emitir(ctx: Context, id: Int, canal: String, titulo: String, texto: String, prioridad: Int) {
        val abrir = PendingIntent.getActivity(
            ctx,
            id,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(prioridad)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revocado entre el chequeo y el notify.
        }
    }
}
