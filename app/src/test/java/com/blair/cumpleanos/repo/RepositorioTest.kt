package com.blair.cumpleanos.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.blair.cumpleanos.datos.BaseDatos
import com.blair.cumpleanos.datos.Cumple
import com.blair.cumpleanos.dominio.anioDe
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun d(a: Int, m: Int, dd: Int) = LocalDate.of(a, m, dd).toEpochDay().toInt()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorioTest {

    private lateinit var db: BaseDatos
    private lateinit var repo: Repositorio

    /** 14/09/2026, el mismo "hoy" de todos los casos. */
    private val hoy = d(2026, 9, 14)

    @Before fun abrir() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaseDatos::class.java,
        ).build()
        repo = Repositorio(db)
    }

    @After fun cerrar() = db.close()

    @Test fun `crear siembra UNA sola ocurrencia, no una por anio vivido`() = runTest {
        val id = repo.crear("Ana", 15, 3, 1990, null, hoy)
        val filas = db.cumpleDao().abiertosDe(id)
        assertEquals(1, filas.size)
        assertEquals(d(2027, 3, 15), filas[0].fechaDia)
    }

    @Test fun `sin anio de nacimiento la persona se crea igual`() = runTest {
        val id = repo.crear("Beto", 25, 12, null, null, hoy)
        assertNull(db.personaDao().porId(id)!!.anioNacimiento)
        assertEquals(d(2026, 12, 25), db.cumpleDao().abiertosDe(id)[0].fechaDia)
    }

    @Test fun `diaHabitual guarda el dia crudo, no el clampeado`() = runTest {
        val id = repo.crear("Cora", 29, 2, 1992, null, hoy)
        assertEquals(29, db.personaDao().porId(id)!!.diaHabitual)
        assertEquals(d(2027, 2, 28), db.cumpleDao().abiertosDe(id)[0].fechaDia)
    }

    @Test fun `saludar marca y genera la ocurrencia del anio siguiente`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val c = db.cumpleDao().abiertosDe(id)[0]
        repo.saludar(id, c.id, hoy)

        val abiertos = db.cumpleDao().abiertosDe(id)
        assertEquals(1, abiertos.size)
        assertEquals(d(2027, 12, 25), abiertos[0].fechaDia)
        assertEquals(hoy, db.personaDao().porId(id)!!.ultimoSaludoDia)
        assertEquals(true, db.cumpleDao().porId(c.id)!!.saludado)
    }

    @Test fun `saludar el mismo dia del cumpleanos no adelanta un anio de mas`() = runTest {
        val id = repo.crear("Hoy", 14, 9, 2000, null, hoy)
        val c = db.cumpleDao().abiertosDe(id)[0]
        assertEquals(hoy, c.fechaDia)
        repo.saludar(id, c.id, hoy)
        assertEquals(d(2027, 9, 14), db.cumpleDao().abiertosDe(id)[0].fechaDia)
    }

    @Test fun `asegurarCobertura es idempotente`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        repeat(3) { repo.asegurarCobertura(id, hoy) }
        assertEquals(1, db.cumpleDao().abiertosDe(id).size)
    }

    @Test fun `cobertura tras dos anios apagado genera UNA, no dos`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val futuro = d(2029, 1, 10)
        db.cumpleDao().marcarSaludado(db.cumpleDao().abiertosDe(id)[0].id, futuro)
        repo.asegurarCobertura(id, futuro)
        val abiertos = db.cumpleDao().abiertosDe(id)
        assertEquals(1, abiertos.size)
        assertTrue(abiertos[0].fechaDia >= futuro)
    }

    @Test fun `editar fecha mueve la ocurrencia pendiente`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val p = db.personaDao().porId(id)!!
        assertEquals(Movida.Ok, repo.editarPersona(p, 3, 11, hoy))
        assertEquals(d(2026, 11, 3), db.cumpleDao().abiertosDe(id)[0].fechaDia)
        assertEquals(3, db.personaDao().porId(id)!!.diaHabitual)
    }

    @Test fun `mover sobre una ocurrencia existente devuelve Colision, no crashea`() = runTest {
        // Dos pendientes: 03/11 (la que se movera) y 25/12 (el destino ocupado).
        val id = repo.crear("Ana", 3, 11, 1990, null, hoy)
        db.cumpleDao().insertar(Cumple(personaId = id, fechaDia = d(2026, 12, 25)))
        val p = db.personaDao().porId(id)!!

        assertEquals(Movida.Colision, repo.editarPersona(p, 25, 12, hoy))

        assertEquals(
            listOf(d(2026, 11, 3), d(2026, 12, 25)),
            db.cumpleDao().abiertosDe(id).map { it.fechaDia }.sorted(),
        )
        assertEquals(3, db.personaDao().porId(id)!!.diaHabitual)
    }

    @Test fun `renombrar sin tocar la fecha no mueve nada`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val p = db.personaDao().porId(id)!!
        assertEquals(Movida.Ok, repo.editarPersona(p.copy(nombre = "Ana Maria"), 25, 12, hoy))
        assertEquals(d(2026, 12, 25), db.cumpleDao().abiertosDe(id)[0].fechaDia)
        assertEquals("Ana Maria", db.personaDao().porId(id)!!.nombre)
    }

    @Test fun `eliminar borra pendientes y conserva saludos`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val c = db.cumpleDao().abiertosDe(id)[0]
        repo.saludar(id, c.id, hoy)
        repo.eliminarPersona(id)

        assertEquals(0, db.cumpleDao().abiertosDe(id).size)
        assertNotNull(db.cumpleDao().porId(c.id))
        assertEquals(false, db.personaDao().porId(id)!!.activo)
    }

    @Test fun `anular devuelve a pendiente y conserva saludadoDia`() = runTest {
        val id = repo.crear("Ana", 25, 12, 1990, null, hoy)
        val c = db.cumpleDao().abiertosDe(id)[0]
        repo.saludar(id, c.id, hoy)
        repo.anularSaludo(c.id)

        val f = db.cumpleDao().porId(c.id)!!
        assertEquals(false, f.saludado)
        assertEquals(true, f.anulado)
        assertEquals(hoy, f.saludadoDia)
    }

    @Test fun `la edad sale del anio de la ocurrencia`() = runTest {
        val id = repo.crear("Marzo", 10, 3, 1990, null, hoy)
        val f = db.cumpleDao().abiertosDe(id)[0].fechaDia
        assertEquals(2027, anioDe(f))
    }
}
