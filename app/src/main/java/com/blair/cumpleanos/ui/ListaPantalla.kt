package com.blair.cumpleanos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.datos.CumpleUi
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.datos.PersonaConHistorial
import com.blair.cumpleanos.dominio.Estado
import com.blair.cumpleanos.dominio.edad
import java.time.YearMonth
import java.util.Locale

/** Habilitado solo desde el dia del cumpleanos: antes no hay nada que saludar. */
/** Que se borra: todo, o el historial de una sola persona. */
private sealed interface AccionHistorial {
    data object Todo : AccionHistorial
    data class DeUno(val id: Long, val nombre: String) : AccionHistorial
}

private val Celeste = Color(0xFF4FC3F7)
private val SobreCeleste = Color(0xFF00344A)

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

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

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pasados.isNotEmpty()) {
            item { Seccion(stringResource(R.string.sec_pasados), MaterialTheme.colorScheme.error) }
            items(pasados, key = { "p" + it.c.id }) { TarjetaCumple(it, loc, onSaludar, onEditarPersona) }
        }

        item { Seccion(stringResource(R.string.sec_proximos)) }
        if (proximos.isEmpty()) {
            item { Vacio(stringResource(R.string.lista_vacia)) }
        }
        items(proximos, key = { f ->
            when (f) {
                is FilaLista.Encabezado -> "h" + f.anio
                is FilaLista.Dato -> "d" + f.fila.c.id
            }
        }) { f ->
            when (f) {
                is FilaLista.Encabezado -> Grupo(f)
                is FilaLista.Dato -> TarjetaCumple(f.fila, loc, onSaludar, onEditarPersona)
            }
        }

        item { Seccion(stringResource(R.string.sec_personas) + " (" + personas.size + ")") }
        items(personas, key = { "n" + it.id }) { p ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    p.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditarPersona(p.id) }
                        .padding(vertical = 12.dp),
                )
                IconButton(onClick = { aBorrar = p }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.accion_eliminar),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) { Seccion(stringResource(R.string.sec_historial)) }
                TextButton(
                    onClick = { borrarHist = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(
                        stringResource(R.string.accion_borrar_historial),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMesAnterior) { Text("◀") }
                Text(
                    if (porAnio) mes.year.toString() else nombreMes(mes, loc),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TextButton(onClick = onMesSiguiente) { Text("▶") }
                Spacer(Modifier.padding(horizontal = 4.dp))
                FilterChip(
                    selected = !porAnio,
                    onClick = { onAlternarPorAnio(false) },
                    label = { Text(stringResource(R.string.hist_mes)) },
                )
                Spacer(Modifier.padding(horizontal = 2.dp))
                FilterChip(
                    selected = porAnio,
                    onClick = { onAlternarPorAnio(true) },
                    label = { Text(stringResource(R.string.hist_anio)) },
                )
            }
        }
        if (historial.isEmpty()) {
            item { Vacio(stringResource(R.string.hist_vacio)) }
        }
        items(historial, key = { "s" + it.id }) { TarjetaHistorial(it, loc) }

        // 140dp: el FAB no tapa la ultima fila con el banner ocupando el bottomBar.
        item { Spacer(Modifier.height(140.dp)) }
    }

    aBorrar?.let { p ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text(stringResource(R.string.per_borrar_titulo, p.nombre)) },
            text = { Text(stringResource(R.string.per_borrar_cuerpo)) },
            confirmButton = {
                TextButton(onClick = { onEliminarPersona(p.id); aBorrar = null }) {
                    Text(stringResource(R.string.accion_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { aBorrar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
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
                        Text(
                            stringResource(R.string.hist_vacio),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            stringResource(R.string.hist_borrar_todo),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { confirmar = AccionHistorial.Todo; borrarHist = false }
                                .padding(vertical = 12.dp),
                        )
                        HorizontalDivider()
                        conHistorial.forEach { p ->
                            Text(
                                p.nombre + "  ·  " + LocalContext.current.resources
                                    .getQuantityString(R.plurals.hist_saludos, p.cantidad, p.cantidad),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        confirmar = AccionHistorial.DeUno(p.personaId, p.nombre)
                                        borrarHist = false
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { borrarHist = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
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
                        is AccionHistorial.DeUno ->
                            stringResource(R.string.per_borrar_hist_cuerpo, acc.nombre)
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
                }) { Text(stringResource(R.string.accion_borrar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }
}

@Composable
private fun Seccion(texto: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Column {
        Spacer(Modifier.height(16.dp))
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
        HorizontalDivider(Modifier.padding(top = 6.dp, bottom = 2.dp), thickness = 2.dp, color = color)
    }
}

@Composable
private fun Grupo(h: FilaLista.Encabezado) {
    Text(
        stringResource(if (h.esteAnio) R.string.grupo_este_anio else R.string.grupo_proximo_anio)
            .uppercase() + "  ·  " + h.anio,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun Vacio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun TarjetaCumple(
    f: FilaCumple,
    loc: Locale,
    onSaludar: (FilaCumple) -> Unit,
    onEditar: (Long) -> Unit,
) {
    val res = LocalContext.current.resources
    val fondo = when (f.estado) {
        Estado.HOY -> MaterialTheme.colorScheme.primaryContainer
        Estado.PASADO -> MaterialTheme.colorScheme.errorContainer
        // PROXIMO va con el fondo normal: lo unico grisado es el boton.
        else -> CardDefaults.cardColors().containerColor
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fondo),
        modifier = Modifier.fillMaxWidth().clickable { onEditar(f.c.personaId) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(f.c.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(fechaDiaMesAnio(f.c.fechaDia, loc), style = MaterialTheme.typography.bodyMedium)
            textoEdad(res, f.edad)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(cuerpoCumple(res, f.diasRestantes), style = MaterialTheme.typography.labelLarge)
                Button(
                    onClick = { onSaludar(f) },
                    enabled = f.diasRestantes <= 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Celeste,
                        contentColor = SobreCeleste,
                    ),
                ) {
                    Text(
                        stringResource(R.string.accion_marcar_saludado),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaHistorial(c: CumpleUi, loc: Locale) {
    val res = LocalContext.current.resources
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            c.nombre,
            style = MaterialTheme.typography.titleSmall,
            textDecoration = if (c.anulado) TextDecoration.LineThrough else null,
        )
        Text(
            res.getString(R.string.hist_cumplio_el, fechaDiaMesAnio(c.fechaDia, loc)),
            style = MaterialTheme.typography.bodySmall,
        )
        c.saludadoDia?.let {
            Text(
                res.getString(R.string.hist_saludado_el, fechaDiaMesAnio(it, loc)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        textoEdad(res, edad(c.fechaDia, c.anioNacimiento))?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        if (c.anulado) {
            Text(
                stringResource(R.string.hist_anulado),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
