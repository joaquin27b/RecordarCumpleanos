package com.blair.cumpleanos.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.key
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.R
import com.google.android.gms.ads.nativead.NativeAd
import com.blair.cumpleanos.datos.CumpleUi
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.datos.PersonaConHistorial
import com.blair.cumpleanos.dominio.Estado
import com.blair.cumpleanos.dominio.Tiempo
import com.blair.cumpleanos.dominio.tiempo
import com.blair.cumpleanos.dominio.diaDelMes
import com.blair.cumpleanos.dominio.edad
import com.blair.cumpleanos.dominio.normalizar
import com.blair.cumpleanos.dominio.ConClave
import com.blair.cumpleanos.dominio.filtrarPorNombre
import com.blair.cumpleanos.ui.escena.TituloCromado
import com.blair.cumpleanos.ui.escena.Velita
import com.blair.cumpleanos.ui.escena.VelitaEnEscenario
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.HazFucsia
import com.blair.cumpleanos.ui.theme.RojoCta
import com.blair.cumpleanos.ui.theme.TextoSecundario
import com.blair.cumpleanos.ui.theme.TintaSobreLuz
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Reserva bajo la ultima fila para el FAB destacado y su glow. */
private val ALTO_PIE = 200.dp

/** Hasta cuantos dias antes la pildora va dorada. Mas lejos, violeta neutra. */
private const val DIAS_DESTACADO = 30

/** Que se borra: todo, o el historial de una sola persona. */
private sealed interface AccionHistorial {
    data object Todo : AccionHistorial
    data class DeUno(val id: Long, val nombre: String) : AccionHistorial
}

/**
 * Todo lo que va en la LazyColumn, plano. Un when sin else sobre esto obliga a
 * pintar cada variante: agregar una rompe la compilacion, no pasa en silencio.
 */
private sealed interface Item {
    val clave: String

    data object Cabecera : Item { override val clave = "cabecera" }
    data class Asistencia(val c: CumpleUi) : Item { override val clave = "asist" + c.id }
    data class Seccion(val texto: Int, val color: Color, override val clave: String) : Item
    data class Grupo(val h: FilaLista.Encabezado) : Item { override val clave = "g" + h.anio }
    data class Cumple(val f: FilaCumple) : Item { override val clave = "c" + f.c.id }
    data class Personas(val lista: List<Persona>) : Item { override val clave = "personas" }
    data object BuscadorPersonas : Item { override val clave = "buscadorPersonas" }
    data class PersonasSinResultados(val consulta: String) : Item { override val clave = "personasSinResultados" }
    data object SelectorHistorial : Item { override val clave = "ctrlHist" }
    data object Buscador : Item { override val clave = "buscador" }
    data object BuscandoTodo : Item { override val clave = "buscandoTodo" }
    data object SinResultados : Item { override val clave = "sinResultados" }
    /** Todo el historial en UNA fila de la LazyColumn: adentro scrollea por su cuenta. */
    data class Historial(val lista: List<CumpleUi>) : Item { override val clave = "histLista" }
    data class Vacio(val texto: Int, override val clave: String) : Item
    data object Pie : Item { override val clave = "pie" }
    /** Clave estable por lugar: Compose no lo trata como fila nueva. El anuncio vive afuera. */
    data class Nativo(val lugar: LugarNativo, val anuncio: NativeAd) : Item { override val clave = "nativo" + lugar.name }
}

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

/**
 * Pantalla principal sobre la escena ATENUADA: con tarjetas encima el escenario
 * completo no deja leer. Si no hay nadie cargado ni historial, la escena sube a
 * COMPLETA (lo decide MainActivity) y aca se para Velita a recibir.
 */
@Composable
fun ListaPantalla(
    /** Cumpleaños del mes calendario, saludados o no. 0 = sin pildora. */
    cumplesDelMes: Int,
    pasados: List<FilaCumple>,
    proximos: List<FilaLista>,
    personas: List<Persona>,
    historial: List<CumpleUi>,
    mes: YearMonth,
    porAnio: Boolean,
    onSaludar: (FilaCumple) -> Unit,
    onEditarRegalo: (CumpleUi) -> Unit,
    onEditarPersona: (Long) -> Unit,
    onEliminarPersona: (Long) -> Unit,
    onBorrarHistorial: () -> Unit,
    onBorrarHistorialDe: (Long) -> Unit,
    conHistorial: List<PersonaConHistorial>,
    aniosHistorial: IntRange,
    onElegirPeriodo: (YearMonth) -> Unit,
    onAlternarPorAnio: (Boolean) -> Unit,
    busqueda: String,
    onBuscar: (String) -> Unit,
    /** Pregunta de asistencia pendiente, UNA sola; null = ninguna. */
    asistencia: CumpleUi?,
    onResponderAsistencia: (cumpleId: Long, asistio: Boolean) -> Unit,
    onAsistenciaMostrada: (CumpleUi) -> Unit,
    /** Cambia al tocar la notificacion de asistencia: la lista vuelve arriba, a la tarjeta. */
    irArriba: Any?,
    /**
     * Los anota quien llama, FUERA de la LazyColumn y fuera del cambio a AltaPantalla:
     * abrir el alta los pausa y al volver reaparecen los mismos, sin otra request.
     * Solo trae anuncio el que manda; el resto es null y no se dibuja.
     */
    nativos: Map<LugarNativo, NativeAd?>,
    modifier: Modifier = Modifier,
) {
    val loc = locale()
    // Mismo criterio que el ViewModel: "  " o una tilde suelta no es buscar.
    val buscando = normalizar(busqueda).isNotEmpty()
    var aBorrar by remember { mutableStateOf<Persona?>(null) }
    var borrarHist by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<AccionHistorial?>(null) }
    // Filtra mientras se escribe: lo que queda en la tarjeta son las sugerencias,
    // y tocar una abre la persona. Mismo criterio que el historial: sin tildes,
    // mayusculas ni emojis, por inicio de palabra.
    var buscaPersona by rememberSaveable { mutableStateOf("") }
    val clavesPersonas = remember(personas) { personas.map { ConClave(it, normalizar(it.nombre)) } }
    val consultaPersona = normalizar(buscaPersona)
    val personasVistas = if (consultaPersona.isEmpty()) personas else filtrarPorNombre(clavesPersonas, consultaPersona)

    if (esVacio(pasados, proximos, personas) && conHistorial.isEmpty()) {
        EstadoVacio(modifier)
        return
    }

    val items = buildList {
        fun nativo(lugar: LugarNativo) = nativos[lugar]?.let { add(Item.Nativo(lugar, it)) }
        add(Item.Cabecera)
        asistencia?.let {
            add(Item.Asistencia(it))
            nativo(LugarNativo.ASISTENCIA)
        }
        if (pasados.isNotEmpty()) {
            add(Item.Seccion(R.string.sec_pasados, RojoCta, "secPasados"))
            pasados.forEach { add(Item.Cumple(it)) }
        }
        add(Item.Seccion(R.string.sec_proximos, CianNeon, "secProximos"))
        if (proximos.isEmpty()) add(Item.Vacio(R.string.lista_vacia, "vacioProximos"))
        proximos.forEach { f ->
            when (f) {
                is FilaLista.Encabezado -> {
                    if (!f.esteAnio) nativo(LugarNativo.ESTE_ANIO)
                    add(Item.Grupo(f))
                }
                is FilaLista.Dato -> add(Item.Cumple(f.fila))
            }
        }
        if (personas.isNotEmpty()) {
            nativo(LugarNativo.PERSONAS)
            add(Item.Seccion(R.string.sec_personas, Dorado, "secPersonas"))
            add(Item.BuscadorPersonas)
            add(if (personasVistas.isEmpty()) Item.PersonasSinResultados(buscaPersona.trim()) else Item.Personas(personasVistas))
        }
        nativo(LugarNativo.HISTORIAL)
        add(Item.Seccion(R.string.sec_historial, CianNeon, "secHistorial"))
        add(Item.Buscador)
        // Buscando, el rango no aplica: sus controles mentirian.
        if (buscando) add(Item.BuscandoTodo) else add(Item.SelectorHistorial)
        // "No hay saludos" y "nadie se llama asi" son cosas distintas.
        if (historial.isEmpty()) add(if (buscando) Item.SinResultados else Item.Vacio(R.string.hist_vacio, "vacioHist"))
        else add(Item.Historial(historial))
        nativo(LugarNativo.FINAL)
        add(Item.Pie)
    }
    // El ancla de cada lugar es la fila junto a la que va su anuncio: existe
    // aunque el anuncio todavia no, y por ella se sabe si el lugar esta a la vista.
    val anclas = buildMap<Any, LugarNativo> {
        asistencia?.let { put("asist" + it.id, LugarNativo.ASISTENCIA) }
        proximos.firstOrNull { it is FilaLista.Encabezado && !it.esteAnio }
            ?.let { put("g" + (it as FilaLista.Encabezado).anio, LugarNativo.ESTE_ANIO) }
        if (personas.isNotEmpty()) put("secPersonas", LugarNativo.PERSONAS)
        put("secHistorial", LugarNativo.HISTORIAL)
        put("pie", LugarNativo.FINAL)
    }

    val vistos = remember { mutableSetOf<Any>() }
    val inicio = remember { System.currentTimeMillis() }

    val estadoLista = rememberLazyListState()
    val tapada = listaTapada()
    LugaresEnPantalla(estadoLista, anclas)
    LaunchedEffect(irArriba) { if (irArriba != null) estadoLista.animateScrollToItem(0) }
    // Mostrarla es preguntar: se anota el dia, y la notificacion ya no sale.
    asistencia?.let { c -> LaunchedEffect(c.id) { onAsistenciaMostrada(c) } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = estadoLista,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.clave }) { i, item ->
            val anim = Modifier.entrada(item.clave, i, vistos, inicio)
            when (item) {
                Item.Cabecera -> Cabecera(cumplesDelMes, loc, anim)
                is Item.Asistencia -> TarjetaAsistencia(item.c, loc, onResponderAsistencia, anim)
                // Sin animacion de entrada: un anuncio no arranca transparente ni corrido.
                // Tapada por una hoja, invisible pero en su lugar: nada se corre.
                is Item.Nativo -> TarjetaNativo(
                    item.anuncio,
                    Modifier
                        .padding(vertical = 4.dp)
                        .alpha(if (tapada) 0f else 1f),
                    item.lugar.tamano,
                )
                is Item.Seccion -> EncabezadoSeccion(
                    stringResource(item.texto) + if (item.clave == "secPersonas") " (${personas.size})" else "",
                    item.color,
                    anim,
                ) {
                    if (item.clave == "secHistorial") {
                        TextButton(onClick = { borrarHist = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = RojoCta, modifier = Modifier.width(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.accion_borrar_historial), color = RojoCta, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is Item.Grupo -> Text(
                    stringResource(if (item.h.esteAnio) R.string.grupo_este_anio else R.string.grupo_proximo_anio)
                        .uppercase(loc) + "  ·  " + item.h.anio,
                    color = Dorado,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = anim.padding(top = 6.dp, start = 4.dp),
                )
                is Item.Cumple -> TarjetaCumple(item.f, loc, onSaludar, onEditarPersona, anim)
                is Item.Personas -> TarjetaPersonas(item.lista, loc, onEditarPersona, { aBorrar = it }, anim)
                Item.SelectorHistorial -> SelectorPeriodo(mes, porAnio, aniosHistorial, onElegirPeriodo, onAlternarPorAnio, anim)
                is Item.Historial -> ListaHistorial(item.lista, loc, onEditarRegalo, anim)
                is Item.Vacio -> Text(
                    stringResource(item.texto),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = anim.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Item.Buscador -> Buscador(busqueda, onBuscar, stringResource(R.string.hist_buscar), anim)
                Item.BuscadorPersonas -> Buscador(buscaPersona, { buscaPersona = it }, stringResource(R.string.per_buscar), anim)
                is Item.PersonasSinResultados -> Text(
                    stringResource(R.string.hist_sin_resultados, item.consulta),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = anim.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Item.BuscandoTodo -> Text(
                    stringResource(R.string.hist_buscando_todo),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = anim.padding(horizontal = 4.dp),
                )
                Item.SinResultados -> Text(
                    stringResource(R.string.hist_sin_resultados, busqueda.trim()),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = anim.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                // Al final del scroll el FAB no tapa la ultima fila: 60 de boton,
                // 30 de halo, 32 de separacion del banner y aire: el nativo del
                // final sube entero por encima del boton.
                Item.Pie -> Spacer(Modifier.height(ALTO_PIE))
            }
        }
    }

    aBorrar?.let { p ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text(stringResource(R.string.per_borrar_titulo, p.nombre)) },
            text = { Text(stringResource(R.string.per_borrar_cuerpo)) },
            confirmButton = {
                TextButton(onClick = { onEliminarPersona(p.id); aBorrar = null }) {
                    Text(stringResource(R.string.accion_eliminar), color = RojoCta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { aBorrar = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }

    if (borrarHist) {
        AlertDialog(
            onDismissRequest = { borrarHist = false },
            title = { Text(stringResource(R.string.accion_borrar_historial)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.hist_borrar_elegir))
                    Spacer(Modifier.height(8.dp))
                    if (conHistorial.isEmpty()) {
                        Text(stringResource(R.string.hist_vacio), color = TextoSecundario)
                    } else {
                        TextButton(
                            onClick = { confirmar = AccionHistorial.Todo; borrarHist = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.hist_borrar_todo),
                                color = RojoCta,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        HorizontalDivider(color = BlancoEscena.copy(alpha = 0.12f))
                        conHistorial.forEach { p ->
                            TextButton(
                                onClick = {
                                    confirmar = AccionHistorial.DeUno(p.personaId, p.nombre)
                                    borrarHist = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    p.nombre + "  ·  " + pluralStringResource(R.plurals.hist_saludos, p.cantidad, p.cantidad),
                                    color = BlancoEscena,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { borrarHist = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }

    confirmar?.let { acc ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = {
                Text(
                    when (acc) {
                        AccionHistorial.Todo -> stringResource(R.string.hist_borrar_titulo)
                        is AccionHistorial.DeUno -> stringResource(R.string.per_borrar_hist)
                    }
                )
            },
            text = {
                Text(
                    when (acc) {
                        AccionHistorial.Todo -> stringResource(R.string.hist_borrar_cuerpo)
                        is AccionHistorial.DeUno -> stringResource(R.string.per_borrar_hist_cuerpo, acc.nombre)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (acc) {
                        AccionHistorial.Todo -> onBorrarHistorial()
                        is AccionHistorial.DeUno -> onBorrarHistorialDe(acc.id)
                    }
                    confirmar = null
                }) { Text(stringResource(R.string.accion_borrar), color = RojoCta, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

/** Sin nadie a quien saludar: la escena sube a completa y Velita recibe. */
fun esVacio(pasados: List<FilaCumple>, proximos: List<FilaLista>, personas: List<Persona>): Boolean =
    personas.isEmpty() && pasados.isEmpty() && proximos.isEmpty()

/** Velita de la cabecera: mas alta que la marquesina, la rompe por arriba. */
private val ALTO_VELITA_CABECERA = 106.dp

/** Ancho que sale del aspecto de Velita (100 x 160 unidades). */
private val ANCHO_VELITA_CABECERA = ALTO_VELITA_CABECERA * 0.625f

/** Cuanto sobresale Velita del borde neon: por arriba y por la izquierda. */
private val DESBORDE_ARRIBA = 30.dp
private val DESBORDE_IZQUIERDA = 8.dp

/**
 * Cabecera en marquesina: Velita rompiendo el marco, titulo, lema y cuantos
 * cumplen este mes.
 *
 * Velita va AFUERA de la Marquesina, hermana y no hija: la marquesina clipea a
 * sus hijos para el fondo redondeado. El padding de arriba reserva el desborde:
 * sin el, la LazyColumn la corta contra su borde, bajo la barra de estado.
 */
@Composable
private fun Cabecera(cumplesDelMes: Int, loc: Locale, modifier: Modifier) {
    Box(modifier.padding(top = 12.dp + DESBORDE_ARRIBA, bottom = 4.dp)) {
        Marquesina {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Lugar de Velita dentro del marco: su ancho menos lo que sale
                // por la izquierda y el padding de la marquesina, mas aire.
                Spacer(Modifier.width(ANCHO_VELITA_CABECERA - DESBORDE_IZQUIERDA - 10.dp + 8.dp))
                Column(Modifier.weight(1f)) {
                    // El titulo y el contador van a su ancho natural: si no entran
                    // juntos, el contador baja de linea. Nada se achica ni se corta,
                    // sea cual sea el ancho del telefono o el tamaño de fuente.
                    FlowRow(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TituloCromado(
                            texto = stringResource(R.string.marca),
                            tamano = 19.sp,
                            resplandor = false,
                            extrusion = 2,
                            alineacion = Alignment.Start,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        // Informativo: en 0 no se muestra, una pildora vacia es ruido.
                        // El margen final la separa de las lamparitas del marco.
                        if (cumplesDelMes > 0) {
                            ContadorMes(cumplesDelMes, loc, Modifier.padding(end = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.marca_lema),
                        color = Dorado,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Velita(
            Modifier
                .height(ALTO_VELITA_CABECERA)
                .offset(x = -DESBORDE_IZQUIERDA, y = -DESBORDE_ARRIBA),
        )
    }
}

/**
 * "ESTE MES / 3" en dos lineas: sin el nombre del mes, que era lo que la
 * alargaba y la cortaba en pantallas angostas.
 */
@Composable
private fun ContadorMes(cantidad: Int, loc: Locale, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Dorado)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.contador_mes).uppercase(loc),
            color = TintaSobreLuz,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            cantidad.toString(),
            color = TintaSobreLuz,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            lineHeight = 20.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val angosta = maxWidth < 360.dp
        VelitaEnEscenario()
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * 0.06f)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TituloCromado(
                texto = stringResource(R.string.marca),
                tamano = if (angosta) 36.sp else 42.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.lista_vacia),
                color = BlancoEscena,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.vacio_cuerpo),
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TarjetaCumple(
    f: FilaCumple,
    loc: Locale,
    onSaludar: (FilaCumple) -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier,
) {
    val res = LocalContext.current.resources
    val hoy = f.estado == Estado.HOY
    val acento = when (f.estado) {
        Estado.HOY -> CianNeon
        Estado.PASADO, Estado.OLVIDADO -> RojoCta
        Estado.PROXIMO -> if (f.diasProxima <= DIAS_DESTACADO) Dorado else BlancoEscena.copy(alpha = 0.6f)
    }
    val urgente = f.estado == Estado.PASADO || f.estado == Estado.OLVIDADO
    val neon = when (f.estado) {
        Estado.HOY -> CianNeon
        Estado.PASADO, Estado.OLVIDADO -> RojoCta
        Estado.PROXIMO -> if (f.diasProxima <= DIAS_DESTACADO) Dorado else EscenarioBorde
    }
    Tarjeta(
        modifier,
        neon = neon,
        intenso = hoy || urgente,
        confeti = hoy,
        onClick = { onEditar(f.c.personaId) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // La insignia es la fecha que manda: bajo "ESTE AÑO" un "5 OCT" ya pasado
            // se leia fuera de orden. El cumple sigue en la linea de detalle.
            if (f.mandaFestejo) InsigniaFecha(diaDelMes(f.proxima), mesCorto(f.proxima, loc), HazFucsia, festejo = true)
            else InsigniaFecha(diaDelMes(f.c.fechaDia), mesCorto(f.c.fechaDia, loc), acento)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    f.c.nombre,
                    color = BlancoEscena,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val cuando = tiempo(f.c.fechaDia, f.c.fechaDia - f.diasRestantes)
                val detalle = listOfNotNull(fechaLarga(f.c.fechaDia, loc), textoEdad(res, f.edad, cuando)).joinToString("  ·  ")
                Text(detalle, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                LineaRegalo(
                    f.c.regaloPlan?.let { stringResource(R.string.regalo_plan, it) }
                        ?: stringResource(R.string.regalo_tocar),
                    apagada = f.c.regaloPlan == null,
                )
                // El mismo dia del cumple tambien se dice: oculto, parecia un dato perdido.
                f.c.fechaFestejoDia?.let {
                    LineaFestejo(
                        if (it == f.c.fechaDia) stringResource(R.string.festejo_mismo_dia)
                        else stringResource(R.string.festejo_linea, fechaConDia(it, loc))
                    )
                }
                Spacer(Modifier.height(2.dp))
                PildoraDeEstado(f)
            }
        }
        if (f.diasRestantes <= 0) {
            Spacer(Modifier.height(14.dp))
            BotonPrincipal(
                stringResource(R.string.accion_ya_salude),
                onClick = { onSaludar(f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PildoraDeEstado(f: FilaCumple) {
    when (f.estado) {
        Estado.HOY -> Pildora(stringResource(R.string.cumple_hoy), TipoPildora.DORADA)
        Estado.PASADO, Estado.OLVIDADO -> Pildora(
            pluralStringResource(R.plurals.pildora_sin_saludar, -f.diasRestantes, -f.diasRestantes),
            TipoPildora.ROJA,
            icono = Icons.Filled.Warning,
        )
        Estado.PROXIMO -> Pildora(
            when {
                f.mandaFestejo && f.diasProxima == 0 -> stringResource(R.string.pildora_festeja_hoy)
                f.mandaFestejo && f.diasProxima == 1 -> stringResource(R.string.pildora_festejo_manana)
                f.mandaFestejo -> pluralStringResource(R.plurals.pildora_festejo_en, f.diasProxima, f.diasProxima)
                f.diasRestantes == 1 -> stringResource(R.string.cumple_manana)
                else -> pluralStringResource(R.plurals.pildora_faltan, f.diasRestantes, f.diasRestantes)
            },
            if (f.diasProxima <= DIAS_DESTACADO) TipoPildora.DORADA else TipoPildora.VIOLETA,
        )
    }
}

@Composable
private fun TarjetaPersonas(
    personas: List<Persona>,
    loc: Locale,
    onEditar: (Long) -> Unit,
    onBorrar: (Persona) -> Unit,
    modifier: Modifier,
) {
    // Compacta: una fila por persona, nombre y fecha; el alto lo pone el texto.
    Tarjeta(modifier, neon = HazFucsia, relleno = PaddingValues(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
        personas.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = BlancoEscena.copy(alpha = 0.10f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEditar(p.id) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    Text(
                        p.nombre,
                        color = BlancoEscena,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        fechaLarga(p.ancla, loc) + "  ·  " + (p.anioNacimiento?.toString() ?: stringResource(R.string.persona_sin_anio)),
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { onBorrar(p) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.accion_eliminar), tint = RojoCta)
                }
            }
        }
    }
}

/** Tope del recuadro del historial: mas alto que esto scrollea por dentro. */
private val ALTO_MAX_HISTORIAL = 460.dp
private val ANCHO_BARRA = 6.dp

/**
 * Saludos del periodo con scroll propio, dentro de un recuadro que marca la zona
 * desplazable: el patron de Por Vencer, ya probado en produccion.
 *
 * Column con verticalScroll y tope de alto, NO una LazyColumn: una lazy adentro de
 * otra con alto infinito tira IllegalStateException. Al llegar a un extremo el
 * gesto sigue en la pantalla (nested scroll). La barra solo existe si el
 * contenido supera el tope.
 */
@Composable
private fun ListaHistorial(historial: List<CumpleUi>, loc: Locale, onTocar: (CumpleUi) -> Unit, modifier: Modifier) {
    val scroll = rememberScrollState()
    val forma = RoundedCornerShape(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(CianNeon.copy(alpha = 0.05f))
            .border(1.dp, CianNeon.copy(alpha = 0.45f), forma)
            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = ALTO_MAX_HISTORIAL)
                .barraDesplazamiento(scroll, CianNeon.copy(alpha = 0.18f), CianNeon.copy(alpha = 0.85f))
                .verticalScroll(scroll)
                // Hueco para la barra y el halo de las tarjetas: no quedan debajo.
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = ANCHO_BARRA + 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            historial.forEach { c ->
                key(c.id) { TarjetaSaludo(c, loc, { onTocar(c) }, Modifier) }
            }
        }
    }
}

/** Riel y barra a la derecha del viewport. Antes de verticalScroll: mide el viewport. */
private fun Modifier.barraDesplazamiento(scroll: ScrollState, riel: Color, color: Color): Modifier =
    drawWithContent {
        drawContent()
        val max = scroll.maxValue
        if (max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
        val alto = size.height
        val ancho = ANCHO_BARRA.toPx()
        val x = size.width - ancho
        drawRoundRect(riel, topLeft = Offset(x, 0f), size = Size(ancho, alto), cornerRadius = CornerRadius(ancho / 2))
        val altoBarra = (alto * alto / (alto + max)).coerceAtLeast(32.dp.toPx())
        val y = (alto - altoBarra) * scroll.value / max
        drawRoundRect(color, topLeft = Offset(x, y), size = Size(ancho, altoBarra), cornerRadius = CornerRadius(ancho / 2))
    }

/**
 * Sin imePadding: el Scaffold ya suma WindowInsets.ime y con los dos el campo
 * sube el doble del teclado.
 */
@Composable
private fun Buscador(texto: String, onCambio: (String) -> Unit, etiqueta: String, modifier: Modifier) {
    CampoTexto(
        valor = texto,
        onCambio = onCambio,
        etiqueta = etiqueta,
        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
        iconoInicio = { Icon(Icons.Filled.Search, contentDescription = null, tint = CianNeon) },
        iconoFin = if (texto.isEmpty()) null else {
            {
                IconButton(onClick = { onCambio("") }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.desc_limpiar_busqueda), tint = TextoSecundario)
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun TarjetaSaludo(c: CumpleUi, loc: Locale, onEditarRegalo: () -> Unit, modifier: Modifier) {
    val res = LocalContext.current.resources
    Tarjeta(modifier, neon = if (c.anulado) RojoCta else CianNeon, onClick = onEditarRegalo) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaFecha(diaDelMes(c.fechaDia), mesCorto(c.fechaDia, loc), if (c.anulado) RojoCta else CianNeon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    c.nombre,
                    color = BlancoEscena,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (c.anulado) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val hoy = LocalDate.now().toEpochDay().toInt()
                val cuando = tiempo(c.fechaDia, hoy)
                Text(
                    res.getString(
                        if (cuando == Tiempo.HOY) R.string.hist_cumple_hoy else R.string.hist_cumplio_el,
                        fechaDiaMesAnio(c.fechaDia, loc),
                    ),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                )
                c.saludadoDia?.let {
                    Text(
                        res.getString(R.string.hist_saludado_el, fechaDiaMesAnio(it, loc)),
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                textoEdad(res, edad(c.fechaDia, c.anioNacimiento), cuando)?.let {
                    Text(it, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                }
                // NULL de las filas de la v1: se dice, no se deja un hueco.
                LineaRegalo(
                    c.regaloReal?.let { stringResource(R.string.regalo_real, it) }
                        ?: stringResource(R.string.regalo_sin_anotar),
                    apagada = c.regaloReal == null,
                )
                // Junto al regalo y solo si hubo festejo aparte (o ya se respondio).
                if (c.fechaFestejoDia != null || c.asistio != null) {
                    LineaFestejo(
                        when (c.asistio) {
                            true -> stringResource(R.string.asistencia_fuiste)
                            false -> stringResource(R.string.asistencia_no_fuiste)
                            null -> stringResource(R.string.asistencia_sin_responder)
                        },
                        apagada = c.asistio == null,
                    )
                }
            }
            if (c.anulado) {
                Box(Modifier.padding(start = 8.dp)) {
                    Pildora(stringResource(R.string.hist_anulado), TipoPildora.ROJA)
                }
            }
        }
    }
}

/**
 * "¿Fuiste al festejo de Patry?" con Si y No. Arriba de todo, una sola: la del
 * festejo mas reciente o la que pidio la notificacion.
 */
@Composable
private fun TarjetaAsistencia(
    c: CumpleUi,
    loc: Locale,
    onResponder: (cumpleId: Long, asistio: Boolean) -> Unit,
    modifier: Modifier,
) {
    Tarjeta(modifier, neon = HazFucsia, intenso = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_festejo), contentDescription = null, tint = HazFucsia, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.asistencia_pregunta, c.nombre),
                color = BlancoEscena,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
        }
        c.fechaFestejoDia?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.festejo_linea, fechaConDia(it, loc)),
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 36.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonPrincipal(
                stringResource(R.string.asistencia_si),
                onClick = { onResponder(c.id, true) },
                modifier = Modifier.weight(1f),
            )
            BotonPrincipal(
                stringResource(R.string.asistencia_no),
                onClick = { onResponder(c.id, false) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LineaFestejo(texto: String, apagada: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_festejo),
            contentDescription = null,
            tint = if (apagada) TextoSecundario else HazFucsia,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            texto,
            color = if (apagada) TextoSecundario else BlancoEscena,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (apagada) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LineaRegalo(texto: String, apagada: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_regalo),
            contentDescription = null,
            tint = if (apagada) TextoSecundario else Dorado,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            texto,
            color = if (apagada) TextoSecundario else BlancoEscena,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (apagada) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
