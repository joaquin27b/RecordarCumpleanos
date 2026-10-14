package com.blair.cumpleanos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blair.cumpleanos.datos.BaseDatos
import com.blair.cumpleanos.datos.CumpleUi
import com.blair.cumpleanos.datos.Persona

import com.blair.cumpleanos.datos.PersonaConHistorial
import com.blair.cumpleanos.dominio.Estado
import com.blair.cumpleanos.dominio.anioDe
import com.blair.cumpleanos.dominio.edad
import com.blair.cumpleanos.dominio.estado
import com.blair.cumpleanos.repo.Movida
import com.blair.cumpleanos.repo.Repositorio
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ocurrencia pendiente con lo derivado que la UI necesita. */
data class FilaCumple(
    val c: CumpleUi,
    val estado: Estado,
    val diasRestantes: Int,
    /** null si no se conoce el anio de nacimiento. */
    val edad: Int?,
)

/** La lista es plana: el corte de grupo es un elemento, no un Map anidado. */
sealed interface FilaLista {
    data class Encabezado(val anio: Int, val esteAnio: Boolean) : FilaLista
    data class Dato(val fila: FilaCumple) : FilaLista
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListaViewModel(
    private val repo: Repositorio,
    private val db: BaseDatos,
) : ViewModel() {

    private fun hoy(): Int = LocalDate.now().toEpochDay().toInt()

    private val _mes = MutableStateFlow(YearMonth.now())
    val mes: StateFlow<YearMonth> = _mes

    private val _porAnio = MutableStateFlow(false)
    val porAnio: StateFlow<Boolean> = _porAnio

    /** Se muestran aparte: un cumpleanos sin saludar no se mezcla con los futuros. */
    val pasados: StateFlow<List<FilaCumple>> =
        filas().map { l -> l.filter { it.estado == Estado.PASADO } }.estado()

    /**
     * PROXIMOS con corte por anio.
     *
     * OLVIDADO (mas de 30 dias) no entra: la app se calla sola en vez de
     * acumular reproches.
     */
    val proximos: StateFlow<List<FilaLista>> =
        filas().map { l ->
            val anioHoy = anioDe(hoy())
            l.asSequence()
                .filter { it.estado == Estado.HOY || it.estado == Estado.PROXIMO }
                .sortedBy { it.c.fechaDia }
                .groupBy { anioDe(it.c.fechaDia) }
                .toSortedMap()
                .flatMap { (anio, filas) ->
                    buildList<FilaLista> {
                        add(FilaLista.Encabezado(anio, esteAnio = anio == anioHoy))
                        filas.forEach { add(FilaLista.Dato(it)) }
                    }
                }
        }.estado()

    val personas: StateFlow<List<Persona>> = db.personaDao().activas().estado()

    /** Cumpleaños del mes calendario, saludados o no: informativo, no pendientes. */
    val cumplesDelMes: StateFlow<Int> = YearMonth.now().let { ym ->
        db.cumpleDao().cantidadEntre(ym.atDay(1).toEpochDay().toInt(), ym.atEndOfMonth().toEpochDay().toInt())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }

    /** Para el selector de borrado: quien tiene historial y cuanto. */
    val conHistorial: StateFlow<List<PersonaConHistorial>> = db.cumpleDao().conHistorial().estado()

    val historial: StateFlow<List<CumpleUi>> =
        combine(_mes, _porAnio) { ym, anual -> ym to anual }
            .flatMapLatest { (ym, anual) ->
                val desde = if (anual) LocalDate.of(ym.year, 1, 1) else ym.atDay(1)
                val hasta = if (anual) LocalDate.of(ym.year, 12, 31) else ym.atEndOfMonth()
                db.cumpleDao().saludadosEntre(
                    desde = desde.toEpochDay().toInt(),
                    hasta = hasta.toEpochDay().toInt(),
                )
            }.estado()

    private fun filas() = db.cumpleDao().abiertos().map { lista ->
        val hoy = hoy()
        lista.map { c ->
            FilaCumple(
                c = c,
                estado = estado(c.fechaDia, hoy),
                diasRestantes = c.fechaDia - hoy,
                edad = edad(c.fechaDia, c.anioNacimiento),
            )
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.estado(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun mesAnterior() {
        _mes.value = if (_porAnio.value) _mes.value.minusYears(1) else _mes.value.minusMonths(1)
    }

    fun mesSiguiente() {
        _mes.value = if (_porAnio.value) _mes.value.plusYears(1) else _mes.value.plusMonths(1)
    }

    fun alternarPorAnio(anual: Boolean) { _porAnio.value = anual }

    fun crear(
        nombre: String, dia: Int, mes: Int, anioNacimiento: Int?, preavisos: String?, regaloPlan: String?, festejo: Int?,
    ) = lanzar { repo.crear(nombre, dia, mes, anioNacimiento, preavisos, hoy(), regaloPlan, festejo) }

    fun saludar(personaId: Long, cumpleId: Long, regaloReal: String?) =
        lanzar { repo.saludar(personaId, cumpleId, hoy(), regaloReal) }

    fun anotarRegaloReal(cumpleId: Long, texto: String?) = lanzar { repo.anotarRegaloReal(cumpleId, texto) }

    fun anularSaludo(cumpleId: Long) = lanzar { repo.anularSaludo(cumpleId) }

    fun resaludar(cumpleId: Long) = lanzar { repo.resaludar(cumpleId, hoy()) }

    fun editarFechaSaludo(cumpleId: Long, dia: Int) =
        lanzar { repo.editarFechaSaludo(cumpleId, dia) }

    fun borrarFila(cumpleId: Long) = lanzar { repo.borrarFila(cumpleId) }

    /** Colision contra el indice unico: la UI muestra error_colision, no crashea. */
    private val _colision = MutableStateFlow(false)
    val colision: StateFlow<Boolean> = _colision

    fun editarPersona(
        persona: Persona, nuevoDia: Int?, nuevoMes: Int?, regaloPlan: String?, festejo: Int?, alCerrar: () -> Unit,
    ) {
        viewModelScope.launch {
            when (repo.editarPersona(persona, nuevoDia, nuevoMes, hoy())) {
                Movida.Ok -> {
                    // Despues de mover: el plan va a la ocurrencia ya en su fecha nueva.
                    repo.anotarRegaloPlan(persona.id, regaloPlan)
                    repo.anotarFestejo(persona.id, festejo)
                    alCerrar()
                }
                Movida.Colision -> _colision.value = true
            }
        }
    }

    fun limpiarColision() { _colision.value = false }

    fun eliminarPersona(personaId: Long) = lanzar { repo.eliminarPersona(personaId) }

    fun borrarHistorial() = lanzar { repo.borrarHistorial() }

    fun borrarHistorialDe(personaId: Long) = lanzar { repo.borrarHistorialDe(personaId) }

    private val _personaAbierta = MutableStateFlow<Long?>(null)

    /** Ocurrencias pendientes de la persona cuya hoja esta abierta. */
    val pendientesDePersona: StateFlow<List<CumpleUi>> =
        _personaAbierta.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else db.cumpleDao().abiertos().map { l -> l.filter { it.personaId == id } }
        }.estado()

    /** La persona cuya hoja de edicion esta abierta. */
    val personaAbierta: StateFlow<Persona?> =
        _personaAbierta.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else db.personaDao().activas().map { l -> l.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun abrirPersona(id: Long?) { _personaAbierta.value = id; _colision.value = false }

    private fun lanzar(bloque: suspend () -> Unit) {
        viewModelScope.launch { bloque() }
    }

    companion object {
        fun factory(repo: Repositorio, db: BaseDatos) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ListaViewModel(repo, db) as T
        }
    }
}
