package com.blair.cumpleanos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.datos.Persona
import com.blair.cumpleanos.dominio.AnioNacimiento
import com.blair.cumpleanos.dominio.Cadencia
import com.blair.cumpleanos.dominio.mesDe
import com.blair.cumpleanos.dominio.validarAnio
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.NocheMedia
import com.blair.cumpleanos.ui.theme.RojoCta
import java.time.LocalDate

/**
 * Edicion de una persona en hoja, sobre su propia escena atenuada.
 *
 * El dia es diaHabitual, el CRUDO: nacido el 29/02 guarda 29 aunque la
 * ocurrencia de un año no bisiesto este en 28. El mes sale del ancla.
 */
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
    val hoy = LocalDate.now()
    var nombre by rememberSaveable(persona.id) { mutableStateOf(persona.nombre) }
    var dia by rememberSaveable(persona.id) { mutableIntStateOf(persona.diaHabitual) }
    var mes by rememberSaveable(persona.id) { mutableIntStateOf(mesDe(persona.ancla)) }
    var anioTexto by rememberSaveable(persona.id) { mutableStateOf(persona.anioNacimiento?.toString() ?: "") }
    var sinAnio by rememberSaveable(persona.id) { mutableStateOf(false) }
    var preavisos by rememberSaveable(persona.id) {
        mutableStateOf(Cadencia.preavisosEfectivos(persona.preavisos).toList())
    }
    var intento by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    var confirmarHist by remember { mutableStateOf(false) }

    val validacion = validarAnio(if (sinAnio) "" else anioTexto, dia, mes, hoy)

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
                    // Sin esto el boton de borrar historial queda fuera de pantalla.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.per_titulo),
                        color = BlancoEscena,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(10.dp))
                    DestelloIcono(Dorado, 16.dp)
                }

                CampoTexto(
                    valor = nombre,
                    onCambio = { nombre = it },
                    etiqueta = stringResource(R.string.alta_nombre),
                    error = if (intento && nombre.isBlank()) stringResource(R.string.error_nombre_vacio) else null,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )

                Etiqueta(stringResource(R.string.per_fecha))
                SelectorDiaMes(dia, mes, { d, m -> dia = d; mes = m })
                if (colision) {
                    Text(stringResource(R.string.error_colision), color = RojoCta, style = MaterialTheme.typography.bodySmall)
                }

                CampoAnio(
                    texto = anioTexto,
                    onTexto = { anioTexto = it },
                    sinAnio = sinAnio,
                    onSinAnio = { sinAnio = it },
                    validacion = validacion,
                    mostrarError = true,
                )

                Etiqueta(stringResource(R.string.alta_preavisos))
                ChipsPreaviso(preavisos.toSet()) { preavisos = it.toList() }

                BotonPrincipal(
                    stringResource(R.string.accion_guardar),
                    onClick = {
                        intento = true
                        val anio = when (validacion) {
                            is AnioNacimiento.Valido -> validacion.anio
                            AnioNacimiento.Desconocido -> null
                            AnioNacimiento.Invalido, AnioNacimiento.Futuro, AnioNacimiento.SinBisiesto -> return@BotonPrincipal
                        }
                        if (nombre.isBlank()) return@BotonPrincipal
                        onGuardar(
                            persona.copy(
                                nombre = nombre.trim(),
                                anioNacimiento = anio,
                                preavisos = preavisos.sortedDescending().joinToString(","),
                            ),
                            dia,
                            mes,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { confirmarBorrar = true }) {
                        Text(stringResource(R.string.accion_eliminar), color = RojoCta, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onCerrar) { Text(stringResource(R.string.accion_cancelar), color = CianNeon) }
                }
                TextButton(onClick = { confirmarHist = true }) {
                    Text(stringResource(R.string.per_borrar_hist), color = RojoCta)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text(stringResource(R.string.per_borrar_titulo, persona.nombre)) },
            text = { Text(stringResource(R.string.per_borrar_cuerpo)) },
            confirmButton = {
                TextButton(onClick = { confirmarBorrar = false; onEliminar() }) {
                    Text(stringResource(R.string.accion_eliminar), color = RojoCta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrar = false }) { Text(stringResource(R.string.accion_cancelar)) }
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
                    Text(stringResource(R.string.accion_borrar), color = RojoCta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarHist = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}
