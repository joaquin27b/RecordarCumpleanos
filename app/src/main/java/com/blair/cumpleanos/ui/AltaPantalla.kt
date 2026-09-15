package com.blair.cumpleanos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.Cadencia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * DatePickerState entrega medianoche UTC. Convertir con systemDefault() en
 * AR (UTC-3) da las 21:00 del dia anterior y corre todo un dia.
 */
private fun aLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun aMillis(f: LocalDate): Long = f.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltaPantalla(
    onGuardar: (nombre: String, dia: Int, mes: Int, anioNacimiento: Int?, preavisos: String?) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onCancelar() }

    var nombre by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf<LocalDate?>(null) }
    var sinAnio by remember { mutableStateOf(false) }
    var abrirPicker by remember { mutableStateOf(false) }
    var errorNombre by remember { mutableStateOf(false) }
    var errorFecha by remember { mutableStateOf(false) }
    var preavisos by remember { mutableStateOf(Cadencia.POR_DEFECTO.toSet()) }

    val loc = LocalConfiguration.current.locales[0]
    val hoy = LocalDate.now()

    Column(
        modifier
            .fillMaxWidth()
            // Sin imePadding: el Scaffold ya suma WindowInsets.ime al innerPadding.
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.alta_titulo), style = MaterialTheme.typography.headlineSmall)

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

        Text(stringResource(R.string.alta_fecha), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { abrirPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                fecha?.let {
                    val e = it.toEpochDay().toInt()
                    if (sinAnio) fechaLarga(e, loc) else fechaDiaMesAnio(e, loc)
                } ?: stringResource(R.string.alta_elegir_fecha)
            )
        }
        if (errorFecha) {
            Text(
                stringResource(R.string.error_fecha_vacia),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(stringResource(R.string.alta_preavisos), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.alta_preavisos_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cadencia.DISPONIBLES.forEach { d ->
                FilterChip(
                    selected = d in preavisos,
                    onClick = {
                        preavisos = if (d in preavisos) preavisos - d else preavisos + d
                    },
                    label = { Text(stringResource(R.string.alta_preaviso_dias, d)) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(onClick = {
                errorNombre = nombre.isBlank()
                errorFecha = fecha == null
                val f = fecha
                if (!errorNombre && f != null) {
                    onGuardar(
                        nombre.trim(),
                        f.dayOfMonth,
                        f.monthValue,
                        if (sinAnio) null else f.year,
                        preavisos.sortedDescending().joinToString(","),
                    )
                }
            }) { Text(stringResource(R.string.accion_guardar)) }
        }

        Spacer(Modifier.height(96.dp))
    }

    if (abrirPicker) {
        val inicial = fecha ?: if (sinAnio) hoy else LocalDate.of(hoy.year - 30, 1, 1)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = aMillis(inicial),
            initialDisplayedMonthMillis = aMillis(inicial),
            // Sin anio se elige la OCURRENCIA: este anio o el que viene.
            yearRange = if (sinAnio) hoy.year..(hoy.year + 1) else 1900..hoy.year,
            selectableDates = object : SelectableDates {
                /** Nadie nace en el futuro. Con sinAnio el rango ya es un unico anio bisiesto. */
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
                    estado.selectedDateMillis?.let { fecha = aLocalDate(it); errorFecha = false }
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
