package com.blair.cumpleanos.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Hora de la alarma: apenas empieza el dia, para que el aviso ya este al desbloquear. */
private val HORA_MADRUGADA: LocalTime = LocalTime.of(0, 5)
internal const val ACCION_MADRUGADA = "com.blair.cumpleanos.MADRUGADA"

/**
 * El aviso del dia apenas empieza el dia. Ninguna app se entera del desbloqueo
 * estando cerrada (USER_PRESENT no llega a receptores del manifiesto desde
 * Android 8): la forma de avisar "ni bien desbloquea" es que la notificacion YA
 * este puesta. Sale muda (antes de [HORA_SONORA]) y espera en la pantalla de
 * bloqueo.
 *
 * setAndAllowWhileIdle: inexacta y sin permiso de alarma exacta (nada de
 * revision de policy en Play), pero corre aunque el telefono este en reposo,
 * cosa que el worker de WorkManager no garantiza. Se reprograma en cada disparo,
 * al arrancar la app, al reiniciar el telefono y al actualizar la app.
 */
class Madrugada : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        programar(ctx)
        if (intent.action != ACCION_MADRUGADA) return
        // goAsync: el receptor tiene ~10 s; la base y las notificaciones tardan menos.
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                correrTick(ctx.applicationContext)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        fun programar(ctx: Context) {
            val ahora = LocalDateTime.now()
            var proxima = ahora.toLocalDate().atTime(HORA_MADRUGADA)
            if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
            val disparo = PendingIntent.getBroadcast(
                ctx, 0,
                Intent(ctx, Madrugada::class.java).setAction(ACCION_MADRUGADA),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            ctx.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                proxima.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                disparo,
            )
        }
    }
}
