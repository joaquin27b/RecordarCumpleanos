package com.blair.cumpleanos

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blair.cumpleanos.datos.BaseDatos
import com.blair.cumpleanos.datos.MIGRACION_1_2
import com.blair.cumpleanos.datos.MIGRACION_2_3
import com.blair.cumpleanos.datos.MIGRACION_3_4
import com.blair.cumpleanos.notif.Canales
import com.blair.cumpleanos.notif.Madrugada
import com.blair.cumpleanos.notif.TickWorker
import com.blair.cumpleanos.repo.Repositorio
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CumpleanosApp : Application() {

    /**
     * SharedPreferences y no DataStore: este ultimo trae un .so nativo
     * (libdatastore_shared_counter) sin alineacion de 16 KB que Play marca
     * como posible crash, y aca se guarda un solo Int.
     */
    val prefs: SharedPreferences by lazy {
        getSharedPreferences("ajustes", MODE_PRIVATE)
    }

    val db: BaseDatos by lazy {
        // El nombre del archivo NO cambia: es la base de los testers.
        Room.databaseBuilder(this, BaseDatos::class.java, "cumpleanos.db")
            // TRUNCATE, no WAL: las reglas de backup copian cumpleanos.db y no el
            // -wal, y un restore perderia lo que no se habia volcado. El primer
            // arranque con TRUNCATE hace checkpoint del -wal de la v1.
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // Sin fallback destructivo: una actualizacion NUNCA borra datos.
            .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
            .build()
    }

    val repo: Repositorio by lazy { Repositorio(db) }

    override fun onCreate() {
        super.onCreate()
        Canales.registrar(this)
        TickWorker.programar(this)
        Madrugada.programar(this)
        // initialize() hace I/O de disco y red: en el Main Thread es ANR de arranque.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { MobileAds.initialize(this@CumpleanosApp) }
    }
}
