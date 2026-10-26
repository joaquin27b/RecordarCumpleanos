package com.blair.cumpleanos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blair.cumpleanos.R
import com.blair.cumpleanos.ui.escena.Escenario
import com.blair.cumpleanos.ui.escena.Intensidad
import com.blair.cumpleanos.ui.theme.BlancoEscena
import com.blair.cumpleanos.ui.theme.CianNeon
import com.blair.cumpleanos.ui.theme.Dorado
import com.blair.cumpleanos.ui.theme.Escenario1
import com.blair.cumpleanos.ui.theme.EscenarioBorde
import com.blair.cumpleanos.ui.theme.NocheMedia
import java.time.Month
import java.time.YearMonth

private val FormaCampo = RoundedCornerShape(18.dp)

/** Fondo y borde de los campos del formulario (CampoFestejo): el mismo lenguaje. */
private fun Modifier.campo(): Modifier = this
    .heightIn(min = 52.dp)
    .clip(FormaCampo)
    .background(Escenario1.copy(alpha = 0.28f))
    .border(1.dp, EscenarioBorde.copy(alpha = 0.55f), FormaCampo)

/**
 * Selector del historial, el patron de Por Vencer: un campo con el periodo que
 * abre [HojaPeriodo]. Al lado, Mes o Año. Por año la hoja ofrece solo el año.
 */
@Composable
fun SelectorPeriodo(
    mes: YearMonth,
    porAnio: Boolean,
    anios: IntRange,
    onElegir: (YearMonth) -> Unit,
    onAlternarPorAnio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loc = LocalConfiguration.current.locales[0]
    var abierto by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .campo()
                .clickable(role = Role.Button) { abierto = true }
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (porAnio) mes.year.toString() else nombreMes(mes, loc),
                color = BlancoEscena,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.hist_elegir_periodo), tint = CianNeon)
        }
        Spacer(Modifier.width(8.dp))
        ChipEleccion(stringResource(R.string.hist_mes), !porAnio, { onAlternarPorAnio(false) })
        Spacer(Modifier.width(6.dp))
        ChipEleccion(stringResource(R.string.hist_anio), porAnio, { onAlternarPorAnio(true) })
    }
    if (abierto) {
        HojaPeriodo(
            mes = mes,
            soloAnio = porAnio,
            anios = minOf(anios.first, mes.year)..maxOf(anios.last, mes.year),
            onElegir = { abierto = false; onElegir(it) },
            onCerrar = { abierto = false },
        )
    }
}

/**
 * Desplegables de año y mes, y Ver aplica. Hoja inferior y no dialogo, como en
 * Por Vencer: lo que vaya al pie queda lejos de los botones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaPeriodo(
    mes: YearMonth,
    soloAnio: Boolean,
    anios: IntRange,
    onElegir: (YearMonth) -> Unit,
    onCerrar: () -> Unit,
) {
    val loc = LocalConfiguration.current.locales[0]
    var anio by remember { mutableStateOf(mes.year) }
    var mesElegido by remember { mutableStateOf(mes.month) }
    val nativo = nativoEn(LugarNativo.HOJA)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (soloAnio) R.string.hist_elegir_anio else R.string.hist_elegir_mes_anio),
                        color = BlancoEscena,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(10.dp))
                    DestelloIcono(Dorado, 16.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Del mas nuevo al mas viejo: el actual queda arriba sin scrollear.
                    Desplegable(
                        texto = anio.toString(),
                        opciones = anios.reversed().map { it to it.toString() },
                        onElegir = { anio = it },
                        modifier = Modifier.weight(1f),
                    )
                    if (!soloAnio) {
                        Desplegable(
                            texto = nombreMesSolo(YearMonth.of(anio, mesElegido), loc)
                                .replaceFirstChar { it.titlecase(loc) },
                            opciones = Month.entries.map {
                                it to nombreMesSolo(YearMonth.of(anio, it), loc).replaceFirstChar { c -> c.titlecase(loc) }
                            },
                            onElegir = { mesElegido = it },
                            modifier = Modifier.weight(1.4f),
                        )
                    }
                }
                BotonPrincipal(
                    stringResource(R.string.accion_ver),
                    onClick = { onElegir(YearMonth.of(anio, mesElegido)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onCerrar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.accion_cancelar), color = CianNeon)
                }
                nativo?.let {
                    Spacer(Modifier.height(6.dp))
                    TarjetaNativo(it, LugarNativo.HOJA.tamano)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/** Desplegable con menu; la opcion vigente va en cian. */
@Composable
private fun <T> Desplegable(
    texto: String,
    opciones: List<Pair<T, String>>,
    onElegir: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .campo()
                .clickable(role = Role.Button) { abierto = true }
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(texto, color = BlancoEscena, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = CianNeon)
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = NocheMedia,
            modifier = Modifier.heightIn(max = ALTO_MAX_MENU),
        ) {
            opciones.forEach { (valor, etiqueta) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            etiqueta,
                            color = if (etiqueta == texto) CianNeon else BlancoEscena,
                            fontWeight = if (etiqueta == texto) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = { abierto = false; onElegir(valor) },
                )
            }
        }
    }
}

private val ALTO_MAX_MENU = 320.dp
