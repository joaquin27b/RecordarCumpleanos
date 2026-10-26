package com.blair.cumpleanos.repo

import androidx.room.withTransaction
import com.blair.cumpleanos.datos.BaseDatos
import com.blair.cumpleanos.datos.Cumple
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.dominio.diaDelMes
import com.blair.cumpleanos.dominio.mesDe
import com.blair.cumpleanos.dominio.proxima
import com.blair.cumpleanos.dominio.proximaOcurrencia

/** Resultado de mover una fecha: FIJO no puede pisar el indice unico. */
/** Resultado de corregir el dia en que se saludo. */
enum class FechaSaludo {
    OK,
    /** El saludo ya ocurrio: no puede ser despues de hoy. */
    FUTURA,
    /** La persona ya tiene otro saludo ese dia. */
    CHOQUE,
}

sealed interface Movida {
    data object Ok : Movida
    /** Ya existe otra ocurrencia de esa persona en esa fecha. */
    data object Colision : Movida
}

class Repositorio(private val db: BaseDatos) {

    private val personas = db.personaDao()
    private val cumples = db.cumpleDao()

    /**
     * Una persona nunca nace sin ocurrencia: se crea junto con la proxima.
     *
     * El anio de nacimiento NO siembra la fecha, solo la edad: sembrar desde
     * 1990 generaria 36 ocurrencias pendientes.
     */
    suspend fun crear(
        nombre: String,
        dia: Int,
        mes: Int,
        anioNacimiento: Int?,
        preavisos: String?,
        hoy: Int,
        regaloPlan: String? = null,
        fechaFestejoDia: Int? = null,
    ): Long =
        db.withTransaction {
            val primera = proximaOcurrencia(dia, mes, hoy)
            val id = personas.insertar(
                Persona(
                    nombre = nombre,
                    diaHabitual = dia,
                    anioNacimiento = anioNacimiento,
                    ancla = primera,
                    preavisos = preavisos,
                )
            )
            cumples.insertar(
                Cumple(personaId = id, fechaDia = primera, regaloPlan = regalo(regaloPlan), fechaFestejoDia = fechaFestejoDia)
            )
            id
        }

    /**
     * Garantiza una ocurrencia futura. Anual: una sola, sin bucle.
     *
     * Se llama al saludar y en cada tick. Idempotente via tieneCoberturaFutura.
     */
    suspend fun asegurarCobertura(personaId: Long, hoy: Int) {
        db.withTransaction {
            val p = personas.porId(personaId) ?: return@withTransaction
            if (!p.activo) return@withTransaction
            if (cumples.tieneCoberturaFutura(personaId, hoy)) return@withTransaction
            var siguiente = proxima(p.ancla, p.diaHabitual)
            while (siguiente < hoy) siguiente = proxima(siguiente, p.diaHabitual)
            if (cumples.otroEnFecha(personaId, siguiente, 0L) != null) return@withTransaction
            cumples.insertar(Cumple(personaId = personaId, fechaDia = siguiente))
            personas.actualizar(p.copy(ancla = siguiente))
        }
    }

    /**
     * Saludar MARCA, nunca borra: el historial es perpetuo.
     *
     * El regalo real queda en ESTA ocurrencia; la del año siguiente nace sin
     * plan, porque cada año se regala otra cosa.
     */
    suspend fun saludar(personaId: Long, cumpleId: Long, hoy: Int, regaloReal: String? = null) {
        db.withTransaction {
            cumples.marcarSaludado(cumpleId, hoy)
            cumples.anotarRegaloReal(cumpleId, regalo(regaloReal))
            val p = personas.porId(personaId) ?: return@withTransaction
            personas.actualizar(p.copy(ultimoSaludoDia = hoy))
            asegurarCobertura(personaId, hoy)
        }
    }

    /**
     * "No saludé" (skip): la persona no quiso, no se pudo... Se cierra como un
     * saludo, sin tocar el ultimo saludo de la persona, y nace la ocurrencia del
     * año que viene. Sale de los contadores y de los avisos.
     */
    suspend fun omitir(personaId: Long, cumpleId: Long, hoy: Int, motivo: String?) {
        db.withTransaction {
            cumples.omitir(cumpleId, hoy, regalo(motivo))
            asegurarCobertura(personaId, hoy)
        }
    }

    suspend fun anotarMotivoOmitido(cumpleId: Long, texto: String?) = cumples.anotarMotivoOmitido(cumpleId, regalo(texto))

    suspend fun anotarMotivoNoAsistio(cumpleId: Long, texto: String?) = cumples.anotarMotivoNoAsistio(cumpleId, regalo(texto))

    /** Editable en cualquier momento, desde el historial. */
    suspend fun anotarRegaloReal(cumpleId: Long, texto: String?) = cumples.anotarRegaloReal(cumpleId, regalo(texto))

    /** El regalo pensado va en la ocurrencia pendiente mas proxima de la persona. */
    suspend fun anotarRegaloPlan(personaId: Long, texto: String?) {
        db.withTransaction {
            val abierta = cumples.abiertosDe(personaId).minByOrNull { it.fechaDia } ?: return@withTransaction
            cumples.anotarRegaloPlan(abierta.id, regalo(texto))
        }
    }

    /** El festejo, como el regalo pensado, va en la ocurrencia pendiente mas proxima. null = sin festejo aparte. */
    suspend fun anotarFestejo(personaId: Long, dia: Int?) {
        db.withTransaction {
            val abierta = cumples.abiertosDe(personaId).minByOrNull { it.fechaDia } ?: return@withTransaction
            cumples.anotarFestejo(abierta.id, dia)
        }
    }

    /** Marcado por error: vuelve a pendiente y queda tachado en el historial. */
    /** Si/No de la tarjeta o de la hoja del historial. null = vuelve a sin responder. */
    suspend fun anotarAsistencia(cumpleId: Long, asistio: Boolean?) = cumples.anotarAsistencia(cumpleId, asistio)

    suspend fun marcarAsistenciaPreguntada(cumpleId: Long, hoy: Int) = cumples.marcarAsistenciaPreguntada(cumpleId, hoy)

    suspend fun anularSaludo(cumpleId: Long) = cumples.anularSaludo(cumpleId)

    suspend fun resaludar(cumpleId: Long, dia: Int) = cumples.resaludar(cumpleId, dia)

    /**
     * "Lo saludé ayer, no hoy". Solo cambia saludadoDia: el indice unico es
     * (personaId, fechaDia) y fechaDia no se toca, asi que no hay ABORT posible y
     * el proximo cumpleaños (que sale de fechaDia y del ancla) no se mueve. Se
     * valida ANTES de escribir y se devuelve el motivo: nada de excepciones.
     */
    suspend fun editarFechaSaludo(cumpleId: Long, dia: Int, hoy: Int): FechaSaludo =
        db.withTransaction {
            if (dia > hoy) return@withTransaction FechaSaludo.FUTURA
            val c = cumples.porId(cumpleId) ?: return@withTransaction FechaSaludo.OK
            if (c.saludadoDia == dia) return@withTransaction FechaSaludo.OK
            if (cumples.otroSaludoEnDia(c.personaId, dia, cumpleId) != null) return@withTransaction FechaSaludo.CHOQUE
            cumples.editarFechaSaludo(cumpleId, dia)
            personas.porId(c.personaId)?.let { p ->
                personas.actualizar(p.copy(ultimoSaludoDia = cumples.ultimoSaludoDe(p.id)))
            }
            FechaSaludo.OK
        }

    /** Unico borrado de una fila: error explicito del usuario. */
    suspend fun borrarFila(cumpleId: Long) = cumples.borrarFila(cumpleId)

    /**
     * Elimina UN saludo del historial, elegido por el usuario. Se borra la fila de
     * esa ocurrencia y no se "dessaluda": un cumpleaños de hace meses no vuelve a
     * aparecer como no saludado. Despues se recalcula el ultimo saludo y se
     * asegura que la persona tenga su proximo cumpleaños.
     */
    suspend fun eliminarSaludo(cumpleId: Long, hoy: Int) {
        db.withTransaction {
            val c = cumples.porId(cumpleId) ?: return@withTransaction
            cumples.borrarFila(cumpleId)
            personas.porId(c.personaId)?.let { p ->
                personas.actualizar(p.copy(ultimoSaludoDia = cumples.ultimoSaludoDe(p.id)))
            }
            asegurarCobertura(c.personaId, hoy)
        }
    }

    /**
     * Editar la persona. Si cambia la fecha, mueve tambien la ocurrencia
     * pendiente mas proxima: el ancla y esa fila son el mismo hecho.
     *
     * Chequea el indice unico ANTES de mover: @Update usa ABORT por default
     * y la excepcion llegaria sin catch hasta el colector.
     */
    suspend fun editarPersona(persona: Persona, nuevoDia: Int?, nuevoMes: Int?, hoy: Int): Movida =
        db.withTransaction {
            val previo = personas.porId(persona.id) ?: return@withTransaction Movida.Ok
            if (nuevoDia == null || nuevoMes == null ||
                (nuevoDia == previo.diaHabitual && nuevoMes == mesDe(previo.ancla))
            ) {
                personas.actualizar(persona)
                return@withTransaction Movida.Ok
            }
            val nueva = proximaOcurrencia(nuevoDia, nuevoMes, hoy)
            val abierta = cumples.abiertosDe(persona.id).minByOrNull { it.fechaDia }
            if (abierta != null && cumples.otroEnFecha(persona.id, nueva, abierta.id) != null) {
                return@withTransaction Movida.Colision
            }
            personas.actualizar(persona.copy(ancla = nueva, diaHabitual = nuevoDia))
            abierta?.let { cumples.editarFechaCumple(it.id, nueva) }
            Movida.Ok
        }

    /**
     * "Eliminar" = dejar de avisar. La persona y sus saludos quedan latentes,
     * consultables para siempre.
     */
    suspend fun eliminarPersona(personaId: Long) {
        db.withTransaction {
            cumples.borrarPendientesDe(personaId)
            personas.desactivar(personaId)
        }
    }

    /** Borra TODO el historial. Los pendientes y las personas quedan intactos. */
    suspend fun borrarHistorial() {
        db.withTransaction {
            cumples.borrarSaludos()
            cumples.limpiarAnulados()
        }
    }

    suspend fun borrarHistorialDe(personaId: Long) {
        db.withTransaction {
            cumples.borrarSaludosDe(personaId)
            cumples.limpiarAnuladosDe(personaId)
        }
    }

    /** Dia del mes del ancla, para precargar el DatePicker. */
    fun diaDe(epochDay: Int): Int = diaDelMes(epochDay)

    /** Vacio y solo espacios se guardan como NULL: "sin regalo anotado", no "" roto. */
    private fun regalo(texto: String?): String? = texto?.trim()?.takeIf { it.isNotEmpty() }
}
