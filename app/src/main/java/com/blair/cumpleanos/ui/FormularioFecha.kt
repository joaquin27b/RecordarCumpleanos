package com.blair.cumpleanos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.dominio.AnioNacimiento
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.Escenario1
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.NochePiso
import com.blair.cumpleanos.ui.theme.RojoCta
import com.blair.cumpleanos.ui.theme.TextoSecundario
import com.blair.cumpleanos.ui.theme.TintaSobreLuz
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle

/** Año bisiesto de referencia para la grilla: el 29/02 tiene que poder elegirse. */
private const val ANIO_GRILLA = 2024

/**
 * Calendario de DIA y MES, sin año.
 *
 * Propio y no el DatePicker de M3: ese trae siempre el selector de año y la
 * cabecera con el año. Tampoco hay milisegundos: no existe la conversion UTC
 * que corria todos los cumpleaños un dia en Argentina.
 *
 * Abre en el mes elegido (en el alta, hoy). Las flechas recorren los 12 meses en
 * ciclo. Sin dias de la semana: sin año no hay forma de saberlos.
 */
@Composable
fun SelectorDiaMes(dia: Int, mes: Int, onCambio: (dia: Int, mes: Int) -> Unit, modifier: Modifier = Modifier) {
    val loc = LocalConfiguration.current.locales[0]
    var visto by remember { mutableIntStateOf(mes) }
    val hoy = LocalDate.now()
    val dias = YearMonth.of(ANIO_GRILLA, visto).lengthOfMonth()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NochePiso.copy(alpha = 0.45f))
            .border(1.dp, EscenarioBorde.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { visto = if (visto == 1) 12 else visto - 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.desc_anterior), tint = CianNeon)
            }
            Text(
                Month.of(visto).getDisplayName(TextStyle.FULL_STANDALONE, loc)
                    .replaceFirstChar { it.titlecase(loc) },
                color = BlancoEscena,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { visto = if (visto == 12) 1 else visto + 1 }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.desc_siguiente), tint = CianNeon)
            }
        }
        (0 until (dias + 6) / 7).forEach { fila ->
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val d = fila * 7 + col + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (d <= dias) {
                            val elegido = d == dia && visto == mes
                            val esHoy = d == hoy.dayOfMonth && visto == hoy.monthValue
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (elegido) CianNeon else Escenario1.copy(alpha = 0.0f))
                                    .then(if (esHoy && !elegido) Modifier.border(1.5.dp, Dorado, CircleShape) else Modifier)
                                    .clickable(role = Role.RadioButton) { onCambio(d, visto) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    d.toString(),
                                    color = if (elegido) TintaSobreLuz else BlancoEscena,
                                    fontWeight = if (elegido) FontWeight.Black else FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Campo de texto sobre la escena: violeta translucido, borde cian al enfocar, label flotante. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true,
    teclado: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { t -> { Text(t) } },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = teclado,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BlancoEscena,
            unfocusedTextColor = BlancoEscena,
            focusedContainerColor = Escenario1.copy(alpha = 0.40f),
            unfocusedContainerColor = Escenario1.copy(alpha = 0.28f),
            disabledContainerColor = Escenario1.copy(alpha = 0.12f),
            focusedBorderColor = CianNeon,
            unfocusedBorderColor = EscenarioBorde.copy(alpha = 0.55f),
            disabledBorderColor = EscenarioBorde.copy(alpha = 0.25f),
            focusedLabelColor = CianNeon,
            unfocusedLabelColor = TextoSecundario,
            cursorColor = CianNeon,
            errorBorderColor = RojoCta,
            errorLabelColor = RojoCta,
            errorSupportingTextColor = RojoCta,
            errorContainerColor = Escenario1.copy(alpha = 0.40f),
            focusedSupportingTextColor = TextoSecundario,
            unfocusedSupportingTextColor = TextoSecundario,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Año de nacimiento, FUERA del calendario: opcional, y con un boton explicito
 * para decir que no se sabe.
 */
@Composable
fun CampoAnio(
    texto: String,
    onTexto: (String) -> Unit,
    sinAnio: Boolean,
    onSinAnio: (Boolean) -> Unit,
    validacion: AnioNacimiento,
    mostrarError: Boolean,
) {
    val error = if (!mostrarError || sinAnio) null else when (validacion) {
        AnioNacimiento.Invalido -> stringResource(R.string.error_anio_invalido)
        AnioNacimiento.Futuro -> stringResource(R.string.error_anio_futuro)
        AnioNacimiento.SinBisiesto -> stringResource(R.string.error_anio_bisiesto)
        AnioNacimiento.Desconocido, is AnioNacimiento.Valido -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CampoTexto(
            valor = if (sinAnio) "" else texto,
            onCambio = { nuevo -> onTexto(nuevo.filter { it.isDigit() }.take(4)) },
            etiqueta = stringResource(R.string.alta_anio),
            error = error,
            ayuda = if (sinAnio || texto.isBlank()) stringResource(R.string.alta_anio_desconocido) else null,
            habilitado = !sinAnio,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
        ChipEleccion(stringResource(R.string.alta_no_se_anio), sinAnio, { onSinAnio(!sinAnio) })
        Spacer(Modifier)
    }
}
