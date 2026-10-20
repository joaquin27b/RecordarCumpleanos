package com.blair.cumpleanos.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.blair.cumpleanos.CumpleanosApp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.Cadencia
import com.blair.cumpleanos.dominio.anioDe
import com.blair.cumpleanos.dominio.avisaFestejo
import com.blair.cumpleanos.dominio.avisaResumen
import com.blair.cumpleanos.dominio.edad
import com.blair.cumpleanos.dominio.mesDe
import com.blair.cumpleanos.dominio.notificaAsistencia
import com.blair.cumpleanos.dominio.ventanaAsistencia
import com.blair.cumpleanos.ui.nombreMesSolo
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit

private const val CLAVE_RESUMEN = "ultimo_resumen_ym"

/** Ultimo dia avisado por ocurrencia. En prefs y no en la tabla: el schema v2 lleva una sola columna de festejo. */
private fun claveFestejo(cumpleId: Long) = "festejo_aviso_$cumpleId"
private const val TRABAJO = "tick"
private const val HORA_AVISO = 9

class TickWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CumpleanosApp
        val hoyFecha = LocalDate.now()
        val hoy = hoyFecha.toEpochDay().toInt()
        val dao = app.db.cumpleDao()

        // Nadie puede quedarse sin ocurrencia futura por haber tenido el telefono apagado.
        app.db.personaDao().activasAhora().forEach { app.repo.asegurarCobertura(it.id, hoy) }

        dao.abiertosAhora().forEach { c ->
            if (Cadencia.avisa(c.fechaDia, hoy, c.avisoUltimoDia, c.preavisos)) {
                Emisor.cumple(
                    applicationContext, c.personaId, c.nombre, c.fechaDia, c.fechaDia - hoy,
                    edad(c.fechaDia, c.anioNacimiento),
                )
                dao.marcarAviso(c.id, hoy)
            }
        }

        // Festejos de hoy y de mañana, saludados o no: se saluda el martes y se festeja el sabado.
        dao.festejosEntre(hoy, hoy + 1).forEach { f ->
            val ultimo = app.prefs.getInt(claveFestejo(f.cumpleId), 0).takeIf { it != 0 }
            if (avisaFestejo(f.fechaFestejoDia, f.fechaDia, hoy, ultimo)) {
                Emisor.festejo(applicationContext, f.personaId, f.nombre, f.fechaFestejoDia, f.fechaFestejoDia - hoy)
                app.prefs.edit().putInt(claveFestejo(f.cumpleId), hoy).apply()
            }
        }

        // Asistencia: UNA por tick, la del festejo mas reciente; las demas esperan
        // su turno. Avisada o vista en la app, no se vuelve a notificar.
        ventanaAsistencia(hoy).let { v -> dao.asistenciasPendientesAhora(v.first, v.last) }
            .firstOrNull { notificaAsistencia(it.fechaFestejoDia, it.asistio, it.asistenciaPreguntadaDia, hoy) }
            ?.let { c ->
                Emisor.asistencia(applicationContext, c.personaId, c.id, c.nombre)
                dao.marcarAsistenciaPreguntada(c.id, hoy)
            }

        val ym = anioDe(hoy) * 100 + mesDe(hoy)
        val ultimo = app.prefs.getInt(CLAVE_RESUMEN, 0).takeIf { it != 0 }
        if (avisaResumen(ultimo, hoyFecha.year, hoyFecha.monthValue, hoyFecha.dayOfMonth)) {
            val delMes = dao.abiertosAhora().filter { mesDe(it.fechaDia) == hoyFecha.monthValue && anioDe(it.fechaDia) == hoyFecha.year }
            // Silencio si no hay nada: una notificacion vacia es ruido.
            if (delMes.isNotEmpty()) {
                Emisor.resumen(
                    applicationContext,
                    // El mes y no la cantidad: "Cumpleaños de 3" no se entendia.
                    applicationContext.getString(
                        R.string.notif_resumen_titulo,
                        nombreMesSolo(YearMonth.from(hoyFecha), applicationContext.resources.configuration.locales[0]),
                    ),
                    applicationContext.resources.getQuantityString(
                        R.plurals.notif_resumen_cuerpo, delMes.size, delMes.size,
                    ),
                )
            }
            app.prefs.edit().putInt(CLAVE_RESUMEN, ym).apply()
        }
        return Result.success()
    }

    companion object {
        /**
         * WorkManager y no AlarmManager: sobrevive al reboot sin BroadcastReceiver
         * y no necesita USE_EXACT_ALARM, que dispara revision de policy en Play.
         * Precision +-1h, irrelevante para un cumpleanos.
         */
        fun programar(ctx: Context) {
            val ahora = LocalDateTime.now()
            var proxima = ahora.toLocalDate().atTime(LocalTime.of(HORA_AVISO, 0))
            if (!proxima.isAfter(ahora)) proxima = proxima.plusDays(1)
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                TRABAJO,
                ExistingPeriodicWorkPolicy.KEEP,
                // Sin flex: con flex el initialDelay se suma al final del periodo
                // y la primera corrida cae al dia siguiente.
                PeriodicWorkRequestBuilder<TickWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(Duration.between(ahora, proxima))
                    .build(),
            )
        }
    }
}
