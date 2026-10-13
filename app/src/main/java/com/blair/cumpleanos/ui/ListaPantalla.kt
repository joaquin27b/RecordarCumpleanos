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
import com.blair.cumpleanos.dominio.diaDelMes
import com.blair.cumpleanos.dominio.edad
import com.blair.cumpleanos.ui.escena.TituloCromado
import com.blair.cumpleanos.ui.escena.VelitaEnEscenario
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.RojoCta
import com.blair.cumpleanos.ui.theme.TextoSecundario
import java.time.YearMonth
import java.util.Locale

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
    pasados: List<FilaCumple>,
    proximos: List<FilaLista>,
    personas: List<Persona>,
    historial: List<CumpleUi>,
    mes: YearMonth,
    porAnio: Boolean,
    onSaludar: (FilaCumple) -> Unit,
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
                Item.Cabecera -> Cabecera(anim)
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
                is Item.Saludo -> TarjetaSaludo(item.c, loc, anim)
                is Item.Vacio -> Text(
                    stringResource(item.texto),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = anim.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                // El FAB y el banner no tapan la ultima fila.
                Item.Pie -> Spacer(Modifier.height(120.dp))
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

@Composable
private fun Cabecera(modifier: Modifier) {
    TituloCromado(
        texto = stringResource(R.string.app_name).uppercase(locale()).replace(' ', ' '),
        tamano = 24.sp,
        resplandor = false,
        extrusion = 3,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
    )
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
                texto = stringResource(R.string.app_name).uppercase(locale()),
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
    Tarjeta(modifier, destacada = hoy, onClick = { onEditar(f.c.personaId) }) {
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
                val detalle = listOfNotNull(fechaLarga(f.c.fechaDia, loc), textoEdad(res, f.edad)).joinToString("  ·  ")
                Text(detalle, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
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
    Tarjeta(modifier) {
        personas.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider(color = BlancoEscena.copy(alpha = 0.10f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEditar(p.id) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    Text(
                        p.nombre,
                        color = BlancoEscena,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(fechaLarga(p.ancla, loc), color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { onBorrar(p) }) {
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
private fun TarjetaSaludo(c: CumpleUi, loc: Locale, modifier: Modifier) {
    val res = LocalContext.current.resources
    Tarjeta(modifier) {
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
                Text(
                    res.getString(R.string.hist_cumplio_el, fechaDiaMesAnio(c.fechaDia, loc)),
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
                textoEdad(res, edad(c.fechaDia, c.anioNacimiento))?.let {
                    Text(it, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
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
