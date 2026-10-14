package com.blair.cumpleanos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import com.blair.cumpleanos.ui.theme.Escenario1
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.HazFucsia
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.NocheMedia
import com.blair.cumpleanos.ui.theme.TextoSecundario

private const val DIA_MS = 86_400_000L

/**
 * Fecha del festejo, opcional, con su icono. Vacio = sin festejo aparte.
 *
 * El calendario abre en la fecha cargada o, si no hay, en la del cumple: lo
 * comun es mover el festejo unos dias. No deja elegir dias pasados.
 *
 * @param sugerida epochDay donde abre el calendario sin fecha cargada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFestejo(fecha: Int?, onCambio: (Int?) -> Unit, sugerida: Int) {
    val loc = LocalConfiguration.current.locales[0]
    var abierto by rememberSaveable { mutableStateOf(false) }
    val forma = RoundedCornerShape(18.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_festejo), contentDescription = null, tint = HazFucsia, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clip(forma)
                .background(Escenario1.copy(alpha = 0.28f))
                .border(1.dp, EscenarioBorde.copy(alpha = 0.55f), forma)
                .clickable(role = Role.Button) { abierto = true }
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                fecha?.let { stringResource(R.string.festejo_linea, fechaConDia(it, loc)) }
                    ?: stringResource(R.string.festejo_etiqueta),
                color = if (fecha != null) BlancoEscena else TextoSecundario,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            )
            if (fecha != null) {
                IconButton(onClick = { onCambio(null) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.festejo_quitar), tint = TextoSecundario)
                }
            }
        }
    }
    if (abierto) {
        val hoyMs = LocalDate.now().toEpochDay() * DIA_MS
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = maxOf((fecha ?: sugerida).toLong() * DIA_MS, hoyMs),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyMs
            },
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    // El picker trabaja en UTC a medianoche: dividir da el epochDay exacto.
                    estado.selectedDateMillis?.let { onCambio((it / DIA_MS).toInt()) }
                    abierto = false
                }) { Text(stringResource(R.string.accion_guardar), color = CianNeon, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.accion_cancelar), color = CianNeon) }
            },
        ) {
            DatePicker(
                state = estado,
                title = { Text(stringResource(R.string.festejo_titulo), Modifier.padding(start = 24.dp, top = 16.dp)) },
            )
        }
    }
}

/** Campo del regalo con su icono. Vacio se guarda como NULL: "sin regalo anotado". */
@Composable
fun CampoRegalo(valor: String, onCambio: (String) -> Unit, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_regalo), contentDescription = null, tint = Dorado, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        CampoTexto(
            valor = valor,
            onCambio = { onCambio(it.take(120)) },
            etiqueta = etiqueta,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Hoja del regalo: al saludar (confirma o cambia el plan antes de guardar el
 * saludo) y desde el historial (edita el regalo real cuando sea).
 *
 * @param inicial lo que se precarga: el plan al saludar, el real en el historial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaRegalo(
    titulo: String,
    cuerpo: String?,
    etiqueta: String,
    inicial: String?,
    textoBoton: String,
    onGuardar: (String?) -> Unit,
    onCerrar: () -> Unit,
) {
    var texto by rememberSaveable { mutableStateOf(inicial ?: "") }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NocheMedia,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CianNeon.copy(alpha = 0.6f)) },
    ) {
        Box {
            Escenario(Intensidad.ATENUADA, Modifier.matchParentSize())
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        titulo,
                        color = BlancoEscena,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(10.dp))
                    DestelloIcono(Dorado, 16.dp)
                }
                if (cuerpo != null) {
                    Text(cuerpo, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                }
                CampoRegalo(texto, { texto = it }, etiqueta)
                BotonPrincipal(textoBoton, onClick = { onGuardar(texto) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onCerrar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.accion_cancelar), color = CianNeon)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
