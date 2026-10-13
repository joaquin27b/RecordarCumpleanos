package com.blair.cumpleanos.datos

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blair.cumpleanos.repo.Repositorio
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NOMBRE = "migracion-test.db"

private fun d(a: Int, m: Int, dd: Int) = LocalDate.of(a, m, dd).toEpochDay().toInt()

/** Una fila de cumple como la dejo la v1, mas las dos columnas nuevas. */
private data class FilaCumple(
    val id: Long, val personaId: Long, val fechaDia: Int, val avisoUltimoDia: Int?,
    val saludado: Int, val saludadoDia: Int?, val anulado: Int,
)

private fun Cursor.intONull(col: String): Int? =
    getColumnIndexOrThrow(col).let { if (isNull(it)) null else getInt(it) }

/**
 * v1 -> v2 sobre el DDL REAL de 1.json, el que tienen los testers: verificado
 * contra el identity hash del AAB publicado (a302d200...).
 */
@RunWith(AndroidJUnit4::class)
class MigracionTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BaseDatos::class.java,
    )

    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** El "hoy" del primer arranque post-update. */
    private val hoy = d(2026, 10, 13)

    @Before @After fun limpiar() {
        ctx.deleteDatabase(NOMBRE)
    }

    /** Mismo builder que CumpleanosApp, salvo la migracion, que el test elige. */
    private fun abrirRoom(migracion: Migration): BaseDatos =
        Room.databaseBuilder(ctx, BaseDatos::class.java, NOMBRE)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(migracion)
            .build()

    /** 2 personas, 3 ocurrencias, 1 saludo con fecha. */
    private fun sembrarV1(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO persona (id, nombre, diaHabitual, anioNacimiento, ancla, ultimoSaludoDia, activo, preavisos) " +
                "VALUES (1, 'Ana', 15, 1990, ${d(2027, 3, 15)}, ${d(2026, 3, 16)}, 1, '7,1')"
        )
        db.execSQL(
            "INSERT INTO persona (id, nombre, diaHabitual, anioNacimiento, ancla, ultimoSaludoDia, activo, preavisos) " +
                "VALUES (2, 'Beto', 29, NULL, ${d(2027, 2, 28)}, NULL, 1, NULL)"
        )
        db.execSQL(
            "INSERT INTO cumple (id, personaId, fechaDia, avisoUltimoDia, saludado, saludadoDia, anulado) " +
                "VALUES (10, 1, ${d(2026, 3, 15)}, ${d(2026, 3, 15)}, 1, ${d(2026, 3, 16)}, 0)"
        )
        db.execSQL(
            "INSERT INTO cumple (id, personaId, fechaDia, avisoUltimoDia, saludado, saludadoDia, anulado) " +
                "VALUES (11, 1, ${d(2027, 3, 15)}, NULL, 0, NULL, 0)"
        )
        db.execSQL(
            "INSERT INTO cumple (id, personaId, fechaDia, avisoUltimoDia, saludado, saludadoDia, anulado) " +
                "VALUES (12, 2, ${d(2027, 2, 28)}, NULL, 0, NULL, 0)"
        )
    }

    private val esperadas = listOf(
        FilaCumple(10, 1, d(2026, 3, 15), d(2026, 3, 15), 1, d(2026, 3, 16), 0),
        FilaCumple(11, 1, d(2027, 3, 15), null, 0, null, 0),
        FilaCumple(12, 2, d(2027, 2, 28), null, 0, null, 0),
    )

    @Test fun conservaTodasLasFilasYLasColumnasNuevasVienenEnNull() {
        helper.createDatabase(NOMBRE, 1).use { sembrarV1(it) }

        // validateDroppedTables = true: compara contra 2.json, identity hash incluido.
        val db = helper.runMigrationsAndValidate(NOMBRE, 2, true, MIGRACION_1_2)

        db.query("SELECT * FROM persona ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToNext()
            assertEquals(1L, c.getLong(c.getColumnIndexOrThrow("id")))
            assertEquals("Ana", c.getString(c.getColumnIndexOrThrow("nombre")))
            assertEquals(15, c.getInt(c.getColumnIndexOrThrow("diaHabitual")))
            assertEquals(1990, c.intONull("anioNacimiento"))
            assertEquals(d(2027, 3, 15), c.getInt(c.getColumnIndexOrThrow("ancla")))
            assertEquals(d(2026, 3, 16), c.intONull("ultimoSaludoDia"))
            assertEquals(1, c.getInt(c.getColumnIndexOrThrow("activo")))
            assertEquals("7,1", c.getString(c.getColumnIndexOrThrow("preavisos")))
            c.moveToNext()
            assertEquals(2L, c.getLong(c.getColumnIndexOrThrow("id")))
            assertEquals("Beto", c.getString(c.getColumnIndexOrThrow("nombre")))
            assertEquals(29, c.getInt(c.getColumnIndexOrThrow("diaHabitual")))
            assertNull(c.intONull("anioNacimiento"))
            assertEquals(d(2027, 2, 28), c.getInt(c.getColumnIndexOrThrow("ancla")))
            assertNull(c.intONull("ultimoSaludoDia"))
            assertTrue(c.isNull(c.getColumnIndexOrThrow("preavisos")))
        }

        db.query("SELECT * FROM cumple ORDER BY id").use { c ->
            val leidas = buildList {
                while (c.moveToNext()) {
                    add(
                        FilaCumple(
                            c.getLong(c.getColumnIndexOrThrow("id")),
                            c.getLong(c.getColumnIndexOrThrow("personaId")),
                            c.getInt(c.getColumnIndexOrThrow("fechaDia")),
                            c.intONull("avisoUltimoDia"),
                            c.getInt(c.getColumnIndexOrThrow("saludado")),
                            c.intONull("saludadoDia"),
                            c.getInt(c.getColumnIndexOrThrow("anulado")),
                        )
                    )
                    assertTrue(c.isNull(c.getColumnIndexOrThrow("regaloPlan")))
                    assertTrue(c.isNull(c.getColumnIndexOrThrow("regaloReal")))
                }
            }
            assertEquals(esperadas, leidas)
        }
        db.close()
    }

    /** Una base que ya tiene una de las columnas (migracion a medio camino) no revienta. */
    @Test fun esIdempotente() {
        helper.createDatabase(NOMBRE, 1).use {
            sembrarV1(it)
            it.execSQL("ALTER TABLE `cumple` ADD COLUMN `regaloPlan` TEXT")
        }
        helper.runMigrationsAndValidate(NOMBRE, 2, true, MIGRACION_1_2).close()
    }

    /**
     * Telefono apagado meses: el ancla quedo un anio atras y la unica ocurrencia
     * pendiente ya paso. Al primer tick post-update asegurarCobertura tiene que
     * generar UNA ocurrencia futura, sin chocar con el indice unico.
     */
    @Test fun asegurarCoberturaDespuesDeMigrarNoDuplica() {
        helper.createDatabase(NOMBRE, 1).use {
            it.execSQL(
                "INSERT INTO persona (id, nombre, diaHabitual, anioNacimiento, ancla, ultimoSaludoDia, activo, preavisos) " +
                    "VALUES (1, 'Vieja', 5, 1980, ${d(2025, 3, 5)}, NULL, 1, '7,1')"
            )
            it.execSQL(
                "INSERT INTO cumple (id, personaId, fechaDia, avisoUltimoDia, saludado, saludadoDia, anulado) " +
                    "VALUES (1, 1, ${d(2025, 3, 5)}, NULL, 0, NULL, 0)"
            )
        }
        val room = abrirRoom(MIGRACION_1_2)
        try {
            val repo = Repositorio(room)
            runBlocking {
                repo.asegurarCobertura(1, hoy)
                // El tick corre N veces: la segunda no puede agregar nada.
                repo.asegurarCobertura(1, hoy)
            }
            val filas = room.query("SELECT fechaDia FROM cumple WHERE personaId = 1 ORDER BY fechaDia", null).use { c ->
                buildList { while (c.moveToNext()) add(c.getInt(0)) }
            }
            assertEquals(listOf(d(2025, 3, 5), d(2027, 3, 5)), filas)
            assertEquals(d(2027, 3, 5), runBlocking { room.personaDao().porId(1) }!!.ancla)
        } finally {
            room.close()
        }
    }

    /**
     * El job periodico de la v1 sobrevive al update (KEEP) y puede disparar con
     * la migracion corriendo. Se VERIFICA que la apertura serializa: el tick
     * espera a que la migracion termine. La pausa vive en una copia solo de test,
     * nunca en MIGRACION_1_2.
     */
    @Test fun elTickEsperaAQueTermineLaMigracion() {
        helper.createDatabase(NOMBRE, 1).use {
            it.execSQL(
                "INSERT INTO persona (id, nombre, diaHabitual, anioNacimiento, ancla, ultimoSaludoDia, activo, preavisos) " +
                    "VALUES (1, 'Vieja', 5, 1980, ${d(2025, 3, 5)}, NULL, 1, NULL)"
            )
            it.execSQL(
                "INSERT INTO cumple (id, personaId, fechaDia, avisoUltimoDia, saludado, saludadoDia, anulado) " +
                    "VALUES (1, 1, ${d(2025, 3, 5)}, NULL, 0, NULL, 0)"
            )
        }
        val empezo = CountDownLatch(1)
        val finMigracion = AtomicLong(0)
        val lenta = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                empezo.countDown()
                Thread.sleep(1_500)
                MIGRACION_1_2.migrate(db)
                finMigracion.set(System.nanoTime())
            }
        }
        val room = abrirRoom(lenta)
        try {
            val finTick = runBlocking {
                // Cualquier acceso abre la base y dispara la migracion: la UI.
                val apertura = async(Dispatchers.IO) { room.personaDao().porId(1) }
                assertTrue("la migracion no arranco", empezo.await(10, TimeUnit.SECONDS))
                // El tick llega con la migracion a mitad de camino.
                val tick = async(Dispatchers.IO) {
                    Repositorio(room).asegurarCobertura(1, hoy)
                    System.nanoTime()
                }
                apertura.await()
                tick.await()
            }
            assertTrue("el tick termino antes que la migracion", finTick > finMigracion.get())
            val n = room.query("SELECT COUNT(*) FROM cumple WHERE personaId = 1 AND fechaDia >= $hoy", null)
                .use { c -> c.moveToFirst(); c.getInt(0) }
            assertEquals(1, n)
        } finally {
            room.close()
        }
    }
}
