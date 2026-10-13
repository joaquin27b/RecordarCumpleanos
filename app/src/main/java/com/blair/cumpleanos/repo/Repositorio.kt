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
            cumples.insertar(Cumple(personaId = id, fechaDia = primera, regaloPlan = regalo(regaloPlan)))
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

    /** Editable en cualquier momento, desde el historial. */
    suspend fun anotarRegaloReal(cumpleId: Long, texto: String?) = cumples.anotarRegaloReal(cumpleId, regalo(texto))

    /** El regalo pensado va en la ocurrencia pendiente mas proxima de la persona. */
    suspend fun anotarRegaloPlan(personaId: Long, texto: String?) {
        db.withTransaction {
            val abierta = cumples.abiertosDe(personaId).minByOrNull { it.fechaDia } ?: return@withTransaction
            cumples.anotarRegaloPlan(abierta.id, regalo(texto))
        }
    }

    /** Marcado por error: vuelve a pendiente y queda tachado en el historial. */
    suspend fun anularSaludo(cumpleId: Long) = cumples.anularSaludo(cumpleId)

    suspend fun resaludar(cumpleId: Long, dia: Int) = cumples.resaludar(cumpleId, dia)

    /** "Lo saludé ayer, no hoy". */
    suspend fun editarFechaSaludo(cumpleId: Long, dia: Int) = cumples.editarFechaSaludo(cumpleId, dia)

    /** Unico borrado de una fila: error explicito del usuario. */
    suspend fun borrarFila(cumpleId: Long) = cumples.borrarFila(cumpleId)

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
