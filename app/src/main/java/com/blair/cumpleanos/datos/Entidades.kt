package com.blair.cumpleanos.datos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "persona", indices = [Index("activo")])
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** Dia del mes de nacimiento, CRUDO. Ancla anti-drift: nacido el 29/02
     *  guarda 29 aunque la ocurrencia de un anio no bisiesto se clampee a 28. */
    val diaHabitual: Int,
    /** null = el usuario no sabe el anio. Sin anio no se calcula la edad. */
    val anioNacimiento: Int? = null,
    /** epochDay de la ultima ocurrencia generada. El MES del cumpleanos vive aca. */
    val ancla: Int,
    val ultimoSaludoDia: Int? = null,
    /** false = eliminado por el usuario. La fila vive para siempre: el historial la nombra. */
    val activo: Boolean = true,
    /** Preavisos en dias corridos, CSV. null = default. "" = ninguno.
     *  D-0 nunca es configurable. */
    val preavisos: String? = null,
)

@Entity(
    tableName = "cumple",
    foreignKeys = [
        ForeignKey(
            entity = Persona::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [
        Index(value = ["personaId", "fechaDia"], unique = true),
        Index(value = ["saludado", "saludadoDia"]),
    ],
)
data class Cumple(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personaId: Long,
    /** epochDay de ESTA ocurrencia. Unica fuente de verdad temporal. */
    val fechaDia: Int,
    val avisoUltimoDia: Int? = null,
    /** Registro perpetuo: saludar marca, nunca borra. */
    val saludado: Boolean = false,
    val saludadoDia: Int? = null,
    /** Marcado por error y dado de baja. Vuelve a pendiente pero sigue en el
     *  HISTORIAL, tachado, en el mes del saludadoDia original. */
    val anulado: Boolean = false,
    /** Lo que se piensa regalar en ESTA ocurrencia: cada anio se regala otra cosa.
     *  null = sin regalo anotado (todas las filas que vienen de la v1). */
    val regaloPlan: String? = null,
    /** Lo que se regalo de verdad, confirmado al saludar. null = sin confirmar. */
    val regaloReal: String? = null,
    /** epochDay del festejo de ESTA ocurrencia, si no es el mismo dia (cumple el
     *  martes, festeja el sabado). null = sin festejo aparte: no se avisa nada. */
    val fechaFestejoDia: Int? = null,
    /** Si fue al festejo de ESTA ocurrencia: un año va y al otro no. null = sin responder. */
    val asistio: Boolean? = null,
    /** epochDay en que se mostro la pregunta de asistencia. Distingue "no pregunte
     *  todavia" de "pregunte y no contesto": sin esto se repregunta a diario. */
    val asistenciaPreguntadaDia: Int? = null,
)
