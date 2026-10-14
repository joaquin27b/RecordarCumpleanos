package com.blair.cumpleanos.sembrador

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.blair.cumpleanos.CumpleanosApp
import com.blair.cumpleanos.notif.TickWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Carga 15 personas de prueba para medir fluidez y sacar capturas.
 *
 * Solo en debug y medicion (src/sembrador): release no la trae. Se dispara con
 *   adb shell am start -n <paquete>/com.blair.cumpleanos.sembrador.SembrarActivity [--ez tick true]
 * Nombres y regalos largos a proposito: son los que desbordan en español.
 */
class SembrarActivity : Activity() {

    private class Fija(
        val nombre: String,
        val dia: Int,
        val mes: Int,
        val anio: Int?,
        val regalo: String? = null,
        val preavisos: String? = null,
        /** Dias antes de hoy en que "se cargo": > 0 deja la ocurrencia en el pasado, sin saludar. */
        val cargadaHace: Int = 0,
        /** Festejo en dia/mes de este año. */
        val festejo: Pair<Int, Int>? = null,
    )

    private val fijas = listOf(
        // Festejo el mismo dia del cumple: no va como linea aparte ni duplica aviso.
        Fija("Valentina", 14, 10, 1990, "Un libro de Cortázar", festejo = 14 to 10),
        // Festejo MAÑANA y HOY, para probar los dos avisos.
        Fija("María de los Ángeles Etcheverría", 11, 10, 1987, "Una cafetera italiana de las grandes y café de especialidad", cargadaHace = 10, festejo = 15 to 10),
        Fija("Joaquín", 12, 10, null, cargadaHace = 10, festejo = 14 to 10),
        Fija("Lucía", 15, 10, 1995, "Auriculares"),
        Fija("Tomás", 17, 10, 2001, preavisos = "30,15,7,3,1", festejo = 18 to 10),
        Fija("Sofía Belén Gutiérrez Zabaleta", 25, 10, null, "Entradas para el recital de diciembre"),
        Fija("Martín", 2, 11, 1979),
        Fija("Camila", 20, 11, 1999, "Una planta"),
        Fija("Abuela Teresa", 8, 12, 1941, "Flores y una torta de ricota", "30,7"),
        Fija("Nicolás", 24, 12, 1992),
        Fija("Agustina", 6, 1, 2003),
        Fija("Ezequiel", 14, 2, 1985),
        Fija("Florencia", 29, 2, 1996),
        Fija("Ramiro", 3, 5, null),
        Fija("Paula", 30, 7, 1993, "Un vinilo"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --ez tick true: corre el tick ya, para ver los avisos sin esperar a las 9.
        if (intent.getBooleanExtra("tick", false)) {
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<TickWorker>().build())
            finish()
            return
        }
        val repo = (application as CumpleanosApp).repo
        val hoy = LocalDate.now().toEpochDay().toInt()
        CoroutineScope(Dispatchers.IO).launch {
            fijas.forEach { f ->
                val festejo = f.festejo?.let { (d, m) -> LocalDate.now().withMonth(m).withDayOfMonth(d).toEpochDay().toInt() }
                repo.crear(f.nombre, f.dia, f.mes, f.anio, f.preavisos, hoy - f.cargadaHace, f.regalo, festejo)
            }
            Log.i("Sembrador", "sembradas ${fijas.size}")
            runOnUiThread { finish() }
        }
    }
}
