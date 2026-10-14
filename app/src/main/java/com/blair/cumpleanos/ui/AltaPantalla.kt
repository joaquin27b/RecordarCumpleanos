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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.AnioNacimiento
import com.blair.cumpleanos.dominio.Cadencia
import com.blair.cumpleanos.dominio.proximaOcurrencia
import com.blair.cumpleanos.dominio.validarAnio
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.TextoSecundario
import java.time.LocalDate

/**
 * Alta a pantalla completa, sobre la escena atenuada de la app.
 *
 * El calendario abre SIEMPRE en hoy y elige solo dia y mes; el año va aparte,
 * opcional, con un boton explicito para "no sé".
 * Sin imePadding: el Scaffold ya suma WindowInsets.ime y con los dos el
 * contenido sube el doble.
 */
@Composable
fun AltaPantalla(
    onGuardar: (
        nombre: String, dia: Int, mes: Int, anioNacimiento: Int?, preavisos: String?, regaloPlan: String?, festejo: Int?,
    ) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onCancelar() }
    val hoy = LocalDate.now()

    var nombre by rememberSaveable { mutableStateOf("") }
    var dia by rememberSaveable { mutableIntStateOf(hoy.dayOfMonth) }
    var mes by rememberSaveable { mutableIntStateOf(hoy.monthValue) }
    var anioTexto by rememberSaveable { mutableStateOf("") }
    var sinAnio by rememberSaveable { mutableStateOf(false) }
    var preavisos by rememberSaveable { mutableStateOf(Cadencia.POR_DEFECTO.toList()) }
    var regaloPlan by rememberSaveable { mutableStateOf("") }
    var festejo by rememberSaveable { mutableStateOf<Int?>(null) }
    var intento by rememberSaveable { mutableStateOf(false) }

    val validacion = validarAnio(if (sinAnio) "" else anioTexto, dia, mes, hoy)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.alta_titulo),
                color = BlancoEscena,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(10.dp))
            DestelloIcono(Dorado, 18.dp)
        }

        CampoTexto(
            valor = nombre,
            onCambio = { nombre = it },
            etiqueta = stringResource(R.string.alta_nombre),
            error = if (intento && nombre.isBlank()) stringResource(R.string.error_nombre_vacio) else null,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )

        Etiqueta(stringResource(R.string.alta_fecha))
        SelectorDiaMes(dia, mes, { d, m -> dia = d; mes = m })

        CampoAnio(
            texto = anioTexto,
            onTexto = { anioTexto = it },
            sinAnio = sinAnio,
            onSinAnio = { sinAnio = it },
            validacion = validacion,
            mostrarError = intento || anioTexto.length == 4,
        )

        CampoRegalo(regaloPlan, { regaloPlan = it }, stringResource(R.string.regalo_plan_etiqueta))
        CampoFestejo(festejo, { festejo = it }, sugerida = proximaOcurrencia(dia, mes, hoy.toEpochDay().toInt()))

        Etiqueta(stringResource(R.string.alta_preavisos))
        Text(stringResource(R.string.alta_preavisos_desc), color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
        ChipsPreaviso(preavisos.toSet()) { preavisos = it.toList() }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar), color = CianNeon) }
            Spacer(Modifier.width(8.dp))
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
                    onGuardar(nombre.trim(), dia, mes, anio, preavisos.sortedDescending().joinToString(","), regaloPlan, festejo)
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(96.dp))
    }
}

/** Rotulo de un bloque del formulario. */
@Composable
fun Etiqueta(texto: String) {
    Text(
        texto.uppercase(),
        color = CianNeon,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** Preavisos disponibles. D-0 no esta: avisa siempre y no se configura. */
@Composable
fun ChipsPreaviso(elegidos: Set<Int>, onCambio: (Set<Int>) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Cadencia.DISPONIBLES.forEach { d ->
            ChipEleccion(
                pluralStringResource(R.plurals.alta_preaviso_dias, d, d),
                d in elegidos,
                { onCambio(if (d in elegidos) elegidos - d else elegidos + d) },
            )
        }
    }
}
