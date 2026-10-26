package com.blair.cumpleanos.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


/** Una persona y cuantos saludos tiene, para el selector de borrado. */
data class PersonaConHistorial(val personaId: Long, val nombre: String, val cantidad: Int)

/** Fila lista para la UI: cumple + datos de la persona, sin N+1. */
data class CumpleUi(
    val id: Long,
    val personaId: Long,
    val nombre: String,
    val anioNacimiento: Int?,
    val preavisos: String?,
    val fechaDia: Int,
    val saludado: Boolean,
    val saludadoDia: Int?,
    val anulado: Boolean,
    val avisoUltimoDia: Int?,
    /** null = sin regalo anotado (todas las filas de la v1). */
    val regaloPlan: String?,
    val regaloReal: String?,
    /** null = sin festejo aparte (todas las filas de la v1). */
    val fechaFestejoDia: Int?,
    /** null = sin responder (todas las filas de la v2). */
    val asistio: Boolean?,
    val asistenciaPreguntadaDia: Int?,
    /** null = no se salteo (todas las filas de la v3). */
    val omitidoDia: Int?,
    val motivoOmitido: String?,
    val motivoNoAsistio: String?,
)

/** Un festejo a avisar: quien y cuando. */
data class FestejoUi(val cumpleId: Long, val personaId: Long, val nombre: String, val fechaDia: Int, val fechaFestejoDia: Int)

@Dao
interface PersonaDao {

    @Insert
    suspend fun insertar(persona: Persona): Long

    @Update
    suspend fun actualizar(persona: Persona)

    @Query("SELECT * FROM persona WHERE id = :id")
    suspend fun porId(id: Long): Persona?

    /** Solo las que el usuario no elimino. */
    @Query("SELECT * FROM persona WHERE activo = 1 ORDER BY nombre")
    fun activas(): Flow<List<Persona>>

    @Query("UPDATE persona SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Long)

    @Query("SELECT * FROM persona WHERE activo = 1")
    suspend fun activasAhora(): List<Persona>
}

@Dao
interface CumpleDao {

    @Insert
    suspend fun insertar(cumple: Cumple): Long

    @Update
    suspend fun actualizar(cumple: Cumple)

    @Query("SELECT * FROM cumple WHERE id = :id")
    suspend fun porId(id: Long): Cumple?

    /** Una ocurrencia lista para la UI: la abre un boton de notificacion. */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId WHERE c.id = :id"""
    )
    suspend fun uiPorId(id: Long): CumpleUi?

    /** PROXIMOS: todo lo no saludado de personas activas. */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE c.saludado = 0 AND p.activo = 1
           ORDER BY c.fechaDia"""
    )
    fun abiertos(): Flow<List<CumpleUi>>

    /** Version suspend de abiertos(), para el worker. */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE c.saludado = 0 AND p.activo = 1
           ORDER BY c.fechaDia"""
    )
    suspend fun abiertosAhora(): List<CumpleUi>

    /** HISTORIAL: saludados en un rango, por fecha de saludo. Incluye personas eliminadas. */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE c.saludadoDia IS NOT NULL AND c.saludadoDia BETWEEN :desde AND :hasta
           ORDER BY c.saludadoDia DESC"""
    )
    fun saludadosEntre(desde: Int, hasta: Int): Flow<List<CumpleUi>>

    /** HISTORIAL completo, sin rango: el buscador filtra en Kotlin sobre esto. */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE c.saludadoDia IS NOT NULL
           ORDER BY c.saludadoDia DESC"""
    )
    fun saludadosTodos(): Flow<List<CumpleUi>>

    /** Primer dia con un saludo registrado: desde donde arranca el selector de años. */
    @Query("SELECT MIN(saludadoDia) FROM cumple WHERE saludadoDia IS NOT NULL")
    fun primerSaludo(): Flow<Int?>

    /** Ficha de una persona: todo, saludado o no. */
    @Query("SELECT * FROM cumple WHERE personaId = :personaId ORDER BY fechaDia DESC")
    fun historialDe(personaId: Long): Flow<List<Cumple>>

    @Query("SELECT * FROM cumple WHERE personaId = :personaId AND saludado = 0 ORDER BY fechaDia")
    suspend fun abiertosDe(personaId: Long): List<Cumple>

    @Query("SELECT EXISTS(SELECT 1 FROM cumple WHERE personaId = :personaId AND saludado = 0 AND fechaDia >= :hoy)")
    suspend fun tieneCoberturaFutura(personaId: Long, hoy: Int): Boolean

    /** Colision contra el indice unico (personaId, fechaDia) antes de mover una fecha. */
    @Query("SELECT id FROM cumple WHERE personaId = :personaId AND fechaDia = :dia AND id <> :excepto")
    suspend fun otroEnFecha(personaId: Long, dia: Int, excepto: Long): Long?

    @Query("UPDATE cumple SET saludado = 1, anulado = 0, saludadoDia = :hoy, omitidoDia = NULL WHERE id = :id")
    suspend fun marcarSaludado(id: Long, hoy: Int)

    /** "No saludé": se cierra como un saludo, con el dia y el motivo del skip. */
    @Query(
        """UPDATE cumple SET saludado = 1, anulado = 0, saludadoDia = :hoy, omitidoDia = :hoy,
           motivoOmitido = :motivo WHERE id = :id"""
    )
    suspend fun omitir(id: Long, hoy: Int, motivo: String?)

    @Query("UPDATE cumple SET motivoOmitido = :texto WHERE id = :id")
    suspend fun anotarMotivoOmitido(id: Long, texto: String?)

    @Query("UPDATE cumple SET motivoNoAsistio = :texto WHERE id = :id")
    suspend fun anotarMotivoNoAsistio(id: Long, texto: String?)

    /** Anular: vuelve a pendiente pero conserva saludadoDia para seguir en el historial. */
    @Query("UPDATE cumple SET saludado = 0, anulado = 1 WHERE id = :id")
    suspend fun anularSaludo(id: Long)

    @Query("UPDATE cumple SET saludado = 1, anulado = 0, saludadoDia = :dia WHERE id = :id")
    suspend fun resaludar(id: Long, dia: Int)

    /** Corrige el dia del saludo; si era un "No saludé", tambien el dia del skip. */
    @Query(
        """UPDATE cumple SET saludadoDia = :dia,
           omitidoDia = CASE WHEN omitidoDia IS NULL THEN NULL ELSE :dia END WHERE id = :id"""
    )
    suspend fun editarFechaSaludo(id: Long, dia: Int)

    /** Otro saludo de la misma persona en ese dia, si lo hay. */
    @Query("SELECT id FROM cumple WHERE personaId = :personaId AND saludadoDia = :dia AND id <> :excepto LIMIT 1")
    suspend fun otroSaludoEnDia(personaId: Long, dia: Int, excepto: Long): Long?

    /** El saludo vigente mas reciente de la persona: lo que va en Persona.ultimoSaludoDia. */
    @Query("SELECT MAX(saludadoDia) FROM cumple WHERE personaId = :personaId AND saludado = 1 AND omitidoDia IS NULL")
    suspend fun ultimoSaludoDe(personaId: Long): Int?

    @Query("UPDATE cumple SET fechaDia = :dia, avisoUltimoDia = NULL WHERE id = :id")
    suspend fun editarFechaCumple(id: Long, dia: Int)

    @Query("UPDATE cumple SET regaloPlan = :texto WHERE id = :id")
    suspend fun anotarRegaloPlan(id: Long, texto: String?)

    @Query("UPDATE cumple SET regaloReal = :texto WHERE id = :id")
    suspend fun anotarRegaloReal(id: Long, texto: String?)

    /** Mover el festejo borra la respuesta: hablaba de otra fecha. */
    @Query(
        """UPDATE cumple SET
             asistio = CASE WHEN fechaFestejoDia IS :dia THEN asistio ELSE NULL END,
             asistenciaPreguntadaDia = CASE WHEN fechaFestejoDia IS :dia THEN asistenciaPreguntadaDia ELSE NULL END,
             fechaFestejoDia = :dia
           WHERE id = :id"""
    )
    suspend fun anotarFestejo(id: Long, dia: Int?)

    @Query("UPDATE cumple SET asistio = :asistio WHERE id = :id")
    suspend fun anotarAsistencia(id: Long, asistio: Boolean?)

    @Query("UPDATE cumple SET asistenciaPreguntadaDia = :dia WHERE id = :id")
    suspend fun marcarAsistenciaPreguntada(id: Long, dia: Int)

    /**
     * Festejos ya pasados sin respuesta, de personas activas, del mas reciente al
     * mas viejo. desde/hasta acotan la ventana: ayer y los 30 dias anteriores.
     * Saludado o no: se festeja aparte del saludo.
     */
    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE p.activo = 1 AND c.asistio IS NULL AND c.fechaFestejoDia BETWEEN :desde AND :hasta
           ORDER BY c.fechaFestejoDia DESC, c.id DESC"""
    )
    fun asistenciasPendientes(desde: Int, hasta: Int): Flow<List<CumpleUi>>

    @Query(
        """SELECT c.id, c.personaId, p.nombre, p.anioNacimiento, p.preavisos, c.fechaDia,
                  c.saludado, c.saludadoDia, c.anulado, c.avisoUltimoDia, c.regaloPlan, c.regaloReal, c.fechaFestejoDia,
                  c.asistio, c.asistenciaPreguntadaDia, c.omitidoDia, c.motivoOmitido, c.motivoNoAsistio
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE p.activo = 1 AND c.asistio IS NULL AND c.fechaFestejoDia BETWEEN :desde AND :hasta
           ORDER BY c.fechaFestejoDia DESC, c.id DESC"""
    )
    suspend fun asistenciasPendientesAhora(desde: Int, hasta: Int): List<CumpleUi>

    /**
     * Festejos entre dos dias, saludado o no: se saluda el martes y se festeja
     * el sabado. El que cae el mismo dia del cumple no entra: gana el aviso del cumple.
     */
    @Query(
        """SELECT c.id AS cumpleId, c.personaId, p.nombre, c.fechaDia, c.fechaFestejoDia
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE p.activo = 1 AND c.fechaFestejoDia BETWEEN :desde AND :hasta
             AND c.fechaFestejoDia <> c.fechaDia"""
    )
    suspend fun festejosEntre(desde: Int, hasta: Int): List<FestejoUi>

    /**
     * Ocurrencias de personas activas en un rango con algo por hacer: los
     * contadores ESTE MES y ESTE AÑO. Cuenta mientras no se saludo, o mientras el
     * festejo aparte todavia no paso (se haya ido o no). Saludada y sin festejo por
     * delante, sale: con todo el mes saludado el cartel quedaba diciendo 1 (1.2.2).
     * Un "No saludé" sale siempre, aunque tenga festejo por delante.
     * Es un Flow de la tabla: saludar, editar fechas o mover el festejo lo recalcula.
     */
    @Query(
        """SELECT COUNT(*) FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE p.activo = 1 AND c.fechaDia BETWEEN :desde AND :hasta
             AND (c.saludado = 0 OR (c.omitidoDia IS NULL AND c.fechaFestejoDia IS NOT NULL AND c.fechaFestejoDia >= :hoy))"""
    )
    fun pendientesEntre(desde: Int, hasta: Int, hoy: Int): Flow<Int>

    @Query("UPDATE cumple SET avisoUltimoDia = :dia WHERE id = :id")
    suspend fun marcarAviso(id: Long, dia: Int)

    /** Al eliminar persona: se van los pendientes, los saludados NUNCA. */
    @Query("DELETE FROM cumple WHERE personaId = :personaId AND saludado = 0")
    suspend fun borrarPendientesDe(personaId: Long)

    /** Personas con al menos un saludo registrado. Incluye las eliminadas. */
    @Query(
        """SELECT c.personaId AS personaId, p.nombre AS nombre, COUNT(*) AS cantidad
           FROM cumple c JOIN persona p ON p.id = c.personaId
           WHERE c.saludadoDia IS NOT NULL
           GROUP BY c.personaId, p.nombre
           ORDER BY p.nombre"""
    )
    fun conHistorial(): Flow<List<PersonaConHistorial>>

    /** Borra saludos. Los anulados NO se borran: son ocurrencias pendientes. */
    @Query("DELETE FROM cumple WHERE saludado = 1")
    suspend fun borrarSaludos()

    @Query("DELETE FROM cumple WHERE saludado = 1 AND personaId = :personaId")
    suspend fun borrarSaludosDe(personaId: Long)

    /** Un anulado sigue pendiente: sacarlo del historial es limpiar saludadoDia, no borrar la fila. */
    @Query("UPDATE cumple SET saludadoDia = NULL, anulado = 0 WHERE anulado = 1")
    suspend fun limpiarAnulados()

    @Query("UPDATE cumple SET saludadoDia = NULL, anulado = 0 WHERE anulado = 1 AND personaId = :personaId")
    suspend fun limpiarAnuladosDe(personaId: Long)

    /** Unico borrado de un saludo: correccion manual de un error del usuario. */
    @Query("DELETE FROM cumple WHERE id = :id")
    suspend fun borrarFila(id: Long)
}
