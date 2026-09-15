package com.blair.cumpleanos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.dominio.Cadencia
import com.blair.cumpleanos.dominio.anioDe
import com.blair.cumpleanos.dominio.mesDe
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Privadas de archivo: duplicarlas evita acoplar esta hoja a AltaPantalla. */
private fun aLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun aMillis(f: LocalDate): Long = f.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/**
 * Reconstruye la fecha editable desde la entidad.
 *
 * diaHabitual es el dia CRUDO: nacido el 29/02 guarda 29 aunque la ocurrencia
 * vigente este clampeada a 28. Se vuelve a clampear solo para mostrarla.
 */
private fun fechaDe(p: Persona): LocalDate {
    val anio = p.anioNacimiento ?: anioDe(p.ancla)
    val ym = YearMonth.of(anio, mesDe(p.ancla))
    return ym.atDay(minOf(p.diaHabitual, ym.lengthOfMonth()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaPersona(
    persona: Persona,
    colision: Boolean,
    onGuardar: (Persona, Int, Int) -> Unit,
    onEliminar: () -> Unit,
    onBorrarHistorial: () -> Unit,
    onCerrar: () -> Unit,
) {
    var nombre by remember(persona.id) { mutableStateOf(persona.nombre) }
    var fecha by remember(persona.id) { mutableStateOf(fechaDe(persona)) }
    var sinAnio by remember(persona.id) { mutableStateOf(persona.anioNacimiento == null) }
    var preavisos by remember(persona.id) {
        mutableStateOf(Cadencia.preavisosEfectivos(persona.preavisos).toSet())
    }
    var abrirPicker by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    var confirmarHist by remember { mutableStateOf(false) }
    var errorNombre by remember { mutableStateOf(false) }

    val loc = LocalConfiguration.current.locales[0]
    val hoy = LocalDate.now()

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                // Sin esto el boton de borrar historial queda fuera de pantalla.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.per_titulo),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errorNombre = false },
                label = { Text(stringResource(R.string.alta_nombre)) },
                isError = errorNombre,
                supportingText = { if (errorNombre) Text(stringResource(R.string.error_nombre_vacio)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = sinAnio, onCheckedChange = { sinAnio = it })
                Spacer(Modifier.padding(horizontal = 6.dp))
                Column {
                    Text(stringResource(R.string.alta_sin_anio), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.alta_sin_anio_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(stringResource(R.string.per_fecha), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { abrirPicker = true }, modifier = Modifier.fillMaxWidth()) {
                val e = fecha.toEpochDay().toInt()
                Text(if (sinAnio) fechaLarga(e, loc) else fechaDiaMesAnio(e, loc))
            }

            if (colision) {
                Text(
                    stringResource(R.string.error_colision),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(stringResource(R.string.alta_preavisos), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cadencia.DISPONIBLES.forEach { d ->
                    FilterChip(
                        selected = d in preavisos,
                        onClick = { preavisos = if (d in preavisos) preavisos - d else preavisos + d },
                        label = { Text(stringResource(R.string.alta_preaviso_dias, d)) },
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { confirmarBorrar = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.accion_eliminar)) }

                Row {
                    TextButton(onClick = onCerrar) { Text(stringResource(R.string.accion_cancelar)) }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Button(onClick = {
                        errorNombre = nombre.isBlank()
                        if (!errorNombre) {
                            onGuardar(
                                persona.copy(
                                    nombre = nombre.trim(),
                                    anioNacimiento = if (sinAnio) null else fecha.year,
                                    preavisos = preavisos.sortedDescending().joinToString(","),
                                ),
                                fecha.dayOfMonth,
                                fecha.monthValue,
                            )
                        }
                    }) { Text(stringResource(R.string.accion_guardar)) }
                }
            }

            TextButton(
                onClick = { confirmarHist = true },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(R.string.per_borrar_hist)) }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text(stringResource(R.string.per_borrar_titulo, persona.nombre)) },
            text = { Text(stringResource(R.string.per_borrar_cuerpo)) },
            confirmButton = {
                TextButton(onClick = { confirmarBorrar = false; onEliminar() }) {
                    Text(stringResource(R.string.accion_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrar = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    if (confirmarHist) {
        AlertDialog(
            onDismissRequest = { confirmarHist = false },
            title = { Text(stringResource(R.string.per_borrar_hist)) },
            text = { Text(stringResource(R.string.per_borrar_hist_cuerpo, persona.nombre)) },
            confirmButton = {
                TextButton(onClick = { confirmarHist = false; onBorrarHistorial() }) {
                    Text(stringResource(R.string.accion_borrar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarHist = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    if (abrirPicker) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = aMillis(fecha),
            initialDisplayedMonthMillis = aMillis(fecha),
            yearRange = if (sinAnio) hoy.year..(hoy.year + 1) else 1900..hoy.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    sinAnio || !aLocalDate(utcTimeMillis).isAfter(hoy)

                override fun isSelectableYear(year: Int): Boolean =
                    if (sinAnio) year in hoy.year..(hoy.year + 1) else year <= hoy.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { abrirPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { fecha = aLocalDate(it) }
                    abrirPicker = false
                }) { Text(stringResource(R.string.accion_guardar)) }
            },
            dismissButton = {
                TextButton(onClick = { abrirPicker = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        ) {
            DatePicker(state = estado, title = null, showModeToggle = !sinAnio)
        }
    }
}
