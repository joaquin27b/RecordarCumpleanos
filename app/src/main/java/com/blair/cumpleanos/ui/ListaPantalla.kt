package com.blair.cumpleanos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.datos.CumpleUi
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.datos.PersonaConHistorial
import com.blair.cumpleanos.dominio.Estado
import com.blair.cumpleanos.dominio.Tiempo
import com.blair.cumpleanos.dominio.tiempo
import com.blair.cumpleanos.dominio.diaDelMes
import com.blair.cumpleanos.dominio.edad
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
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Reserva bajo la ultima fila para el FAB destacado y su glow. */
private val ALTO_PIE = 150.dp

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
    data class Seccion(val texto: Int, val color: Color, override val clave: String) : Item
    data class Grupo(val h: FilaLista.Encabezado) : Item { override val clave = "g" + h.anio }
    data class Cumple(val f: FilaCumple) : Item { override val clave = "c" + f.c.id }
    data class Personas(val lista: List<Persona>) : Item { override val clave = "personas" }
    data object ControlesHistorial : Item { override val clave = "ctrlHist" }
    data class Saludo(val c: CumpleUi) : Item { override val clave = "s" + c.id }
    data class Vacio(val texto: Int, override val clave: String) : Item
    data object Pie : Item { override val clave = "pie" }
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
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onAlternarPorAnio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loc = locale()
    var aBorrar by remember { mutableStateOf<Persona?>(null) }
    var borrarHist by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf<AccionHistorial?>(null) }

    if (esVacio(pasados, proximos, personas) && conHistorial.isEmpty()) {
        EstadoVacio(modifier)
        return
    }

    val items = buildList {
        add(Item.Cabecera)
        if (pasados.isNotEmpty()) {
            add(Item.Seccion(R.string.sec_pasados, RojoCta, "secPasados"))
            pasados.forEach { add(Item.Cumple(it)) }
        }
        add(Item.Seccion(R.string.sec_proximos, CianNeon, "secProximos"))
        if (proximos.isEmpty()) add(Item.Vacio(R.string.lista_vacia, "vacioProximos"))
        proximos.forEach { f ->
            when (f) {
                is FilaLista.Encabezado -> add(Item.Grupo(f))
                is FilaLista.Dato -> add(Item.Cumple(f.fila))
            }
        }
        if (personas.isNotEmpty()) {
            add(Item.Seccion(R.string.sec_personas, Dorado, "secPersonas"))
            add(Item.Personas(personas))
        }
        add(Item.Seccion(R.string.sec_historial, CianNeon, "secHistorial"))
        add(Item.ControlesHistorial)
        if (historial.isEmpty()) add(Item.Vacio(R.string.hist_vacio, "vacioHist"))
        historial.forEach { add(Item.Saludo(it)) }
        add(Item.Pie)
    }

    val vistos = remember { mutableSetOf<Any>() }
    val inicio = remember { System.currentTimeMillis() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.clave }) { i, item ->
            val anim = Modifier.entrada(item.clave, i, vistos, inicio)
            when (item) {
                Item.Cabecera -> Cabecera(cumplesDelMes, loc, anim)
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
                Item.ControlesHistorial -> ControlesHistorial(mes, porAnio, loc, onMesAnterior, onMesSiguiente, onAlternarPorAnio, anim)
                is Item.Saludo -> TarjetaSaludo(item.c, loc, { onEditarRegalo(item.c) }, anim)
                is Item.Vacio -> Text(
                    stringResource(item.texto),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = anim.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                // Al final del scroll el FAB no tapa la ultima fila: 60 de boton,
                // 20 de glow, 16 de separacion del banner y aire.
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TituloCromado(
                            texto = stringResource(R.string.marca),
                            tamano = 19.sp,
                            resplandor = false,
                            extrusion = 2,
                            alineacion = Alignment.Start,
                        )
                        Spacer(Modifier.weight(1f))
                        // Informativo: en 0 no se muestra, una pildora vacia es ruido.
                        if (cumplesDelMes > 0) {
                            Pildora(
                                pluralStringResource(
                                    R.plurals.cumples_del_mes, cumplesDelMes,
                                    cumplesDelMes, nombreMesSolo(YearMonth.now(), loc),
                                ),
                                TipoPildora.DORADA,
                                modifier = Modifier.padding(start = 8.dp),
                            )
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
        Estado.PROXIMO -> if (f.diasRestantes <= DIAS_DESTACADO) Dorado else BlancoEscena.copy(alpha = 0.6f)
    }
    val urgente = f.estado == Estado.PASADO || f.estado == Estado.OLVIDADO
    val neon = when (f.estado) {
        Estado.HOY -> CianNeon
        Estado.PASADO, Estado.OLVIDADO -> RojoCta
        Estado.PROXIMO -> if (f.diasRestantes <= DIAS_DESTACADO) Dorado else EscenarioBorde
    }
    Tarjeta(
        modifier,
        neon = neon,
        intenso = hoy || urgente,
        confeti = hoy,
        onClick = { onEditar(f.c.personaId) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaFecha(diaDelMes(f.c.fechaDia), mesCorto(f.c.fechaDia, loc), acento)
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
                // El mismo dia del cumple es redundante: no va como linea aparte.
                f.c.fechaFestejoDia?.takeIf { it != f.c.fechaDia }?.let {
                    LineaFestejo(stringResource(R.string.festejo_linea, fechaConDia(it, loc)))
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
            if (f.diasRestantes == 1) stringResource(R.string.cumple_manana)
            else pluralStringResource(R.plurals.pildora_faltan, f.diasRestantes, f.diasRestantes),
            if (f.diasRestantes <= DIAS_DESTACADO) TipoPildora.DORADA else TipoPildora.VIOLETA,
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

@Composable
private fun ControlesHistorial(
    mes: YearMonth,
    porAnio: Boolean,
    loc: Locale,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onAlternarPorAnio: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMesAnterior) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.desc_anterior), tint = CianNeon)
        }
        Text(
            if (porAnio) mes.year.toString() else nombreMes(mes, loc),
            color = BlancoEscena,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMesSiguiente) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.desc_siguiente), tint = CianNeon)
        }
        Spacer(Modifier.width(6.dp))
        ChipEleccion(stringResource(R.string.hist_mes), !porAnio, { onAlternarPorAnio(false) })
        Spacer(Modifier.width(6.dp))
        ChipEleccion(stringResource(R.string.hist_anio), porAnio, { onAlternarPorAnio(true) })
    }
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
            }
            if (c.anulado) {
                Box(Modifier.padding(start = 8.dp)) {
                    Pildora(stringResource(R.string.hist_anulado), TipoPildora.ROJA)
                }
            }
        }
    }
}

@Composable
private fun LineaFestejo(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_festejo),
            contentDescription = null,
            tint = HazFucsia,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            texto,
            color = BlancoEscena,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
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
